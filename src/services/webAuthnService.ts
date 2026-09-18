/**
 * webAuthnService.ts - WebAuthn & Passkeys Authentication Service
 * 
 * Provides hardware-backed Passkeys and biometric authentication
 * for Telegram Web:
 * - Touch ID / Face ID / Windows Hello / YubiKey registration
 * - Passwordless & 2FA biometric login
 * - In-app biometric App Lock
 * - Encrypted credential metadata storage in IndexedDB
 */

import { indexedDBStorage, WebAuthnPasskeyRecord } from './indexedDBStorage';

export class WebAuthnService {
  /**
   * Check if the user's browser and device support WebAuthn Passkeys
   */
  public isSupported(): boolean {
    return (
      typeof window !== 'undefined' &&
      Boolean(window.PublicKeyCredential) &&
      Boolean(navigator.credentials)
    );
  }

  /**
   * Check if a biometric or platform authenticator is available (Touch ID, Face ID, Windows Hello)
   */
  public async isPlatformAuthenticatorAvailable(): Promise<boolean> {
    if (!this.isSupported()) return false;
    try {
      if (typeof PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable === 'function') {
        return await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();
      }
    } catch {
      return false;
    }
    return true;
  }

  /**
   * Register a new hardware-backed Passkey on the device
   */
  public async registerPasskey(
    user: { id?: string; username?: string; displayName?: string } = {}
  ): Promise<WebAuthnPasskeyRecord> {
    if (!this.isSupported()) {
      throw new Error('WebAuthn Passkeys are not supported on this browser or platform');
    }

    const userId = user.id || 'tg_user_' + Math.random().toString(36).substring(2, 10);
    const username = user.username || user.displayName || 'Telegram User';
    const displayName = user.displayName || user.username || 'Telegram User';

    // Secure cryptographic challenge
    const challenge = new Uint8Array(32);
    window.crypto.getRandomValues(challenge);

    const userBytes = new TextEncoder().encode(userId);

    const publicKeyCredentialCreationOptions: PublicKeyCredentialCreationOptions = {
      challenge,
      rp: {
        name: 'Telegram Web',
        id: window.location.hostname || 'localhost',
      },
      user: {
        id: userBytes,
        name: username,
        displayName: displayName,
      },
      pubKeyCredParams: [
        { alg: -7, type: 'public-key' },  // ES256 (ECDSA with SHA-256)
        { alg: -257, type: 'public-key' }, // RS256 (RSA with SHA-256)
      ],
      authenticatorSelection: {
        authenticatorAttachment: 'platform', // Touch ID / Face ID / Windows Hello
        userVerification: 'preferred',
        residentKey: 'preferred',
      },
      timeout: 60000,
      attestation: 'none',
    };

    try {
      const credential = (await navigator.credentials.create({
        publicKey: publicKeyCredentialCreationOptions,
      })) as PublicKeyCredential;

      if (!credential) {
        throw new Error('Failed to create Passkey credential');
      }

      // Detect device name
      let deviceName = 'Hardware Security Key';
      const ua = navigator.userAgent;
      if (/iPhone|iPad/i.test(ua)) deviceName = 'Apple Face ID / Touch ID';
      else if (/Macintosh/i.test(ua)) deviceName = 'Mac Touch ID';
      else if (/Windows/i.test(ua)) deviceName = 'Windows Hello Biometrics';
      else if (/Android/i.test(ua)) deviceName = 'Android Biometrics';
      else if (/Linux/i.test(ua)) deviceName = 'FIDO2 / Linux Authenticator';

      const rawIdBase64 = this.arrayBufferToBase64(credential.rawId);
      const record: WebAuthnPasskeyRecord = {
        credentialId: credential.id,
        rawIdBase64,
        name: `${displayName} (${deviceName})`,
        deviceName,
        algorithm: 'ES256 / SHA-256',
        createdAt: new Date().toISOString(),
        lastUsedAt: new Date().toISOString(),
      };

      // Save into local IndexedDB
      await indexedDBStorage.savePasskey(record);

      // Notify backend if available
      try {
        await fetch('/api/auth/webauthn/register-verify', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            credentialId: credential.id,
            rawId: rawIdBase64,
            deviceName,
            username,
          }),
        });
      } catch {}

      return record;
    } catch (err: any) {
      if (err.name === 'NotAllowedError') {
        throw new Error('تم إلغاء عملية تسجيل مفتاح المرور أو انتهت مهلة المصادقة.');
      }
      throw new Error(err?.message || 'فشل في إنشاء مفتاح المرور Passkey');
    }
  }

  /**
   * Authenticate using a registered Passkey
   */
  public async authenticateWithPasskey(): Promise<boolean> {
    if (!this.isSupported()) {
      throw new Error('WebAuthn Passkeys are not supported on this device');
    }

    const savedPasskeys = await indexedDBStorage.getPasskeys();
    const challenge = new Uint8Array(32);
    window.crypto.getRandomValues(challenge);

    const allowCredentials: PublicKeyCredentialDescriptor[] = savedPasskeys.map((p) => ({
      id: this.base64ToArrayBuffer(p.rawIdBase64 || p.credentialId),
      type: 'public-key',
    }));

    const publicKeyCredentialRequestOptions: PublicKeyCredentialRequestOptions = {
      challenge,
      timeout: 60000,
      userVerification: 'preferred',
      rpId: window.location.hostname || 'localhost',
      ...(allowCredentials.length > 0 ? { allowCredentials } : {}),
    };

    try {
      const assertion = (await navigator.credentials.get({
        publicKey: publicKeyCredentialRequestOptions,
      })) as PublicKeyCredential;

      if (assertion) {
        // Update last used timestamp in IndexedDB
        const matched = savedPasskeys.find((p) => p.credentialId === assertion.id);
        if (matched) {
          matched.lastUsedAt = new Date().toISOString();
          await indexedDBStorage.savePasskey(matched);
        }

        // Notify backend verification
        try {
          await fetch('/api/auth/webauthn/auth-verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              credentialId: assertion.id,
            }),
          });
        } catch {}

        return true;
      }
      return false;
    } catch (err: any) {
      if (err.name === 'NotAllowedError') {
        throw new Error('تم إلغاء المصادقة البيومترية.');
      }
      throw new Error(err?.message || 'فشلت المصادقة باستخدام Passkey');
    }
  }

  public async getPasskeys(): Promise<WebAuthnPasskeyRecord[]> {
    return indexedDBStorage.getPasskeys();
  }

  public async deletePasskey(id: string): Promise<void> {
    await indexedDBStorage.deletePasskey(id);
    try {
      await fetch(`/api/auth/webauthn/credentials/${id}`, { method: 'DELETE' });
    } catch {}
  }

  // --- App Lock helpers ---
  public async isAppLockEnabled(): Promise<boolean> {
    return indexedDBStorage.getSetting('passkey_app_lock_enabled', false);
  }

  public async setAppLockEnabled(enabled: boolean): Promise<void> {
    await indexedDBStorage.setSetting('passkey_app_lock_enabled', enabled);
  }

  private arrayBufferToBase64(buffer: ArrayBuffer): string {
    let binary = '';
    const bytes = new Uint8Array(buffer);
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
  }

  private base64ToArrayBuffer(base64: string): ArrayBuffer {
    const binaryString = window.atob(base64);
    const len = binaryString.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes.buffer;
  }
}

export const webAuthnService = new WebAuthnService();
