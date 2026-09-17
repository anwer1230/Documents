import fs from 'fs';
import path from 'path';

export function ensureTelegramPatch() {
  try {
    const authenticatorPath = path.join(process.cwd(), 'node_modules', 'telegram', 'network', 'Authenticator.js');
    if (fs.existsSync(authenticatorPath)) {
      let content = fs.readFileSync(authenticatorPath, 'utf-8');
      if (!content.includes('gabBytes.length < 256')) {
        const oldTarget = `    const authKey = new AuthKey_1.AuthKey();
    await authKey.setKey((0, Helpers_1.getByteArray)(gab));`;
        const newTarget = `    const authKey = new AuthKey_1.AuthKey();
    const rawGab = (0, Helpers_1.getByteArray)(gab);
    let gabBytes = rawGab;
    if (gabBytes.length < 256) {
        gabBytes = Buffer.concat([Buffer.alloc(256 - gabBytes.length, 0), gabBytes]);
    }
    await authKey.setKey(gabBytes);`;
        if (content.includes(oldTarget)) {
          content = content.replace(oldTarget, newTarget);
          fs.writeFileSync(authenticatorPath, content, 'utf-8');
          console.log('[Telegram Patch] Successfully patched Authenticator.js with 256-bit DH key padding.');
        }
      }
    }
  } catch (err) {
    console.warn('[Telegram Patch] Warning while verifying patch:', err);
  }
}
