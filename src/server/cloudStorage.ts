import fs from 'fs';
import path from 'path';

export interface StoredSession {
  phone: string;
  sessionString: string;
  name: string;
  username?: string;
  userId?: string;
  savedAt: string;
}

const DATA_DIR = path.join(process.cwd(), 'data');
const SESSIONS_FILE = path.join(DATA_DIR, 'telegram_sessions.json');
const SESSIONS_BACKUP_FILE = path.join(DATA_DIR, 'telegram_sessions.backup.json');

// In-memory runtime cache for microsecond lookups
const memoryCache = new Map<string, StoredSession>();
let lastSyncTimestamp = new Date().toISOString();
let isCloudSynced = false;
let cloudProviderName = 'local_file';

/**
 * Determine Google Cloud Project ID if running in GCP, Firebase, or Cloud Run
 */
function getGcpProjectId(): string | null {
  return (
    process.env.FIRESTORE_PROJECT_ID ||
    process.env.FIREBASE_PROJECT_ID ||
    process.env.GOOGLE_CLOUD_PROJECT ||
    process.env.GCLOUD_PROJECT ||
    process.env.GCP_PROJECT ||
    null
  );
}

/**
 * Read sessions from disk with error recovery and backup fallback
 */
function readFromDisk(): Record<string, StoredSession> {
  try {
    if (fs.existsSync(SESSIONS_FILE)) {
      const raw = fs.readFileSync(SESSIONS_FILE, 'utf-8');
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed.sessions === 'object') {
        return parsed.sessions;
      }
    }
  } catch (err) {
    console.warn('[CloudStorage] Primary sessions file corrupted, trying backup...', err);
  }

  try {
    if (fs.existsSync(SESSIONS_BACKUP_FILE)) {
      const raw = fs.readFileSync(SESSIONS_BACKUP_FILE, 'utf-8');
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed.sessions === 'object') {
        return parsed.sessions;
      }
    }
  } catch (err) {
    console.error('[CloudStorage] Backup sessions file could not be read:', err);
  }

  return {};
}

/**
 * Write sessions to disk and maintain a resilient backup
 */
function writeToDisk(sessions: Record<string, StoredSession>): void {
  try {
    if (!fs.existsSync(DATA_DIR)) {
      fs.mkdirSync(DATA_DIR, { recursive: true });
    }
    const payload = JSON.stringify({ sessions, updatedAt: new Date().toISOString() }, null, 2);
    fs.writeFileSync(SESSIONS_FILE, payload, 'utf-8');
    // Mirror to backup
    fs.writeFileSync(SESSIONS_BACKUP_FILE, payload, 'utf-8');
  } catch (err) {
    console.error('[CloudStorage] Failed to write sessions to disk:', err);
  }
}

/**
 * Read sessions from Firestore REST API if cloud project is configured
 */
async function fetchFromFirestore(): Promise<Record<string, StoredSession> | null> {
  const projectId = getGcpProjectId();
  if (!projectId) return null;

  try {
    const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/telegram_sessions`;
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };
    if (process.env.FIRESTORE_AUTH_TOKEN) {
      headers['Authorization'] = `Bearer ${process.env.FIRESTORE_AUTH_TOKEN}`;
    }

    const res = await fetch(url, { method: 'GET', headers });
    if (!res.ok) {
      // 404 means collection or docs don't exist yet, which is normal on fresh boot
      return null;
    }

    const data = await res.json();
    if (!data || !Array.isArray(data.documents)) return null;

    const results: Record<string, StoredSession> = {};
    for (const doc of data.documents) {
      const fields = doc.fields || {};
      const phone = fields.phone?.stringValue;
      const sessionString = fields.sessionString?.stringValue;
      if (phone && sessionString) {
        results[phone] = {
          phone,
          sessionString,
          name: fields.name?.stringValue || 'حساب تيليجرام',
          username: fields.username?.stringValue || undefined,
          userId: fields.userId?.stringValue || undefined,
          savedAt: fields.savedAt?.stringValue || new Date().toISOString(),
        };
      }
    }

    if (Object.keys(results).length > 0) {
      isCloudSynced = true;
      cloudProviderName = 'firebase_firestore';
      console.log(`[CloudStorage] Retrieved ${Object.keys(results).length} session(s) from Firestore`);
      return results;
    }
  } catch (err: any) {
    console.warn('[CloudStorage] Firestore REST query failed:', err?.message || err);
  }

  return null;
}

/**
 * Save a session document to Firestore REST API
 */
async function syncSessionToFirestore(session: StoredSession): Promise<boolean> {
  const projectId = getGcpProjectId();
  if (!projectId) return false;

  try {
    const docId = session.phone.replace(/[^\w]/g, '_');
    const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/telegram_sessions/${docId}`;
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };
    if (process.env.FIRESTORE_AUTH_TOKEN) {
      headers['Authorization'] = `Bearer ${process.env.FIRESTORE_AUTH_TOKEN}`;
    }

    const body = {
      fields: {
        phone: { stringValue: session.phone },
        sessionString: { stringValue: session.sessionString },
        name: { stringValue: session.name || '' },
        username: { stringValue: session.username || '' },
        userId: { stringValue: session.userId || '' },
        savedAt: { stringValue: session.savedAt || new Date().toISOString() },
      },
    };

    const res = await fetch(url, {
      method: 'PATCH',
      headers,
      body: JSON.stringify(body),
    });

    if (res.ok) {
      isCloudSynced = true;
      cloudProviderName = 'firebase_firestore';
      console.log(`[CloudStorage] Synced session for ${session.phone} to Firestore successfully`);
      return true;
    }
  } catch (err: any) {
    console.warn('[CloudStorage] Firestore save error:', err?.message || err);
  }

  return false;
}

/**
 * Delete a session document from Firestore
 */
async function deleteSessionFromFirestore(phone: string): Promise<boolean> {
  const projectId = getGcpProjectId();
  if (!projectId) return false;

  try {
    const docId = phone.replace(/[^\w]/g, '_');
    const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/telegram_sessions/${docId}`;
    const headers: Record<string, string> = {};
    if (process.env.FIRESTORE_AUTH_TOKEN) {
      headers['Authorization'] = `Bearer ${process.env.FIRESTORE_AUTH_TOKEN}`;
    }

    const res = await fetch(url, { method: 'DELETE', headers });
    return res.ok;
  } catch {
    return false;
  }
}

/**
 * Initialize Cloud Storage layer on server boot:
 * Loads sessions from Firestore first, then merges with local disk and environment variables
 */
export async function initCloudStorage(): Promise<Record<string, StoredSession>> {
  console.log('[CloudStorage] Initializing multi-layer persistent session store...');

  // 1. Check local disk
  const diskSessions = readFromDisk();
  Object.entries(diskSessions).forEach(([k, v]) => memoryCache.set(k, v));

  // 2. Check environment-injected sessions (e.g. Render / Heroku / Container secrets)
  if (process.env.TELEGRAM_PERSISTENT_SESSIONS) {
    try {
      const parsed = JSON.parse(process.env.TELEGRAM_PERSISTENT_SESSIONS);
      if (Array.isArray(parsed)) {
        parsed.forEach((s: StoredSession) => {
          if (s.phone && s.sessionString) memoryCache.set(s.phone, s);
        });
      } else if (typeof parsed === 'object') {
        Object.entries(parsed).forEach(([k, v]) => memoryCache.set(k, v as StoredSession));
      }
      cloudProviderName = 'env_persistent';
      console.log('[CloudStorage] Loaded persistent sessions from TELEGRAM_PERSISTENT_SESSIONS env var');
    } catch {
      // ignore JSON parse error
    }
  }

  // 3. Attempt cloud query (Firestore)
  const firestoreSessions = await fetchFromFirestore();
  if (firestoreSessions) {
    Object.entries(firestoreSessions).forEach(([k, v]) => memoryCache.set(k, v));
  }

  // Persist combined state to local disk
  const combined: Record<string, StoredSession> = {};
  memoryCache.forEach((val, key) => {
    combined[key] = val;
  });
  writeToDisk(combined);

  lastSyncTimestamp = new Date().toISOString();
  console.log(`[CloudStorage] Active session cache ready with ${memoryCache.size} registered account(s).`);
  return combined;
}

/**
 * Load all stored sessions across memory, cloud, and disk
 */
export async function loadAllSessions(): Promise<Record<string, StoredSession>> {
  if (memoryCache.size === 0) {
    return await initCloudStorage();
  }
  const result: Record<string, StoredSession> = {};
  memoryCache.forEach((val, key) => {
    result[key] = val;
  });
  return result;
}

/**
 * Synchronous read from memory cache for instant MTProto handlers
 */
export function loadAllSessionsSync(): Record<string, StoredSession> {
  if (memoryCache.size === 0) {
    const disk = readFromDisk();
    Object.entries(disk).forEach(([k, v]) => memoryCache.set(k, v));
  }
  const result: Record<string, StoredSession> = {};
  memoryCache.forEach((val, key) => {
    result[key] = val;
  });
  return result;
}

/**
 * Save a session record to Memory, Disk, and Cloud (Firestore)
 */
export async function saveSession(session: StoredSession): Promise<void> {
  memoryCache.set(session.phone, session);

  // Write to disk
  const all: Record<string, StoredSession> = {};
  memoryCache.forEach((val, key) => {
    all[key] = val;
  });
  writeToDisk(all);

  // Async sync to cloud
  void syncSessionToFirestore(session);

  lastSyncTimestamp = new Date().toISOString();
}

/**
 * Delete a session by phone number across all layers
 */
export async function deleteSession(phone: string): Promise<void> {
  memoryCache.delete(phone);

  const all: Record<string, StoredSession> = {};
  memoryCache.forEach((val, key) => {
    all[key] = val;
  });
  writeToDisk(all);

  void deleteSessionFromFirestore(phone);
  lastSyncTimestamp = new Date().toISOString();
}

/**
 * Clear all sessions (e.g. on global logout)
 */
export async function clearAllSessions(): Promise<void> {
  const phones = Array.from(memoryCache.keys());
  memoryCache.clear();
  writeToDisk({});

  for (const phone of phones) {
    void deleteSessionFromFirestore(phone);
  }
  lastSyncTimestamp = new Date().toISOString();
}

/**
 * Get cloud storage diagnostics for status inspection
 */
export function getStorageDiagnostics() {
  const sessions: Array<{ phone: string; name: string; username?: string; savedAt: string }> = [];
  memoryCache.forEach(s => {
    sessions.push({
      phone: s.phone,
      name: s.name,
      username: s.username,
      savedAt: s.savedAt,
    });
  });

  return {
    isCloudActive: isCloudSynced || Boolean(getGcpProjectId()),
    provider: getGcpProjectId() ? 'firebase_firestore' : cloudProviderName,
    sessionCount: memoryCache.size,
    lastSyncedAt: lastSyncTimestamp,
    sessions,
  };
}
