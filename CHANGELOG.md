# Changelog

## [Security Overhaul] - Comprehensive Hardening

### Security
- Mandatory WebSocket authentication (no guest connections)
- Origin validation on WS + HTTP write routes
- Per-session & per-IP rate limiting
- Native ping/pong heartbeat for WS
- HTTP security headers (CSP, HSTS, X-Frame-Options, etc.)
- Cookie hardening: httpOnly=true, sameSite=lax, maxAge=30d
- CSRF protection via double-submit token
- Input validation & sanitization on all endpoints
- Size limits on uploads (multer)

### Fixed
- FAB: real `channels.createChannel`
- Contacts: real `contacts.getContacts`
- Profile: real `account.updateProfile`
- Cache clear: real endpoint
- Archive: real `folders.EditPeerFolders`
- 2FA: added set/change via SRP
- Bug report: persisted to SQLite

### Added
- `/server/security/` module
- `/server/routes/` modular routes
- `/tests/security/` integration tests
- Structured logging (JSON)
- Session store with expiry
