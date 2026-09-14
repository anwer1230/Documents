/**
 * Telegram Web K MTProto Crypto (crypto.ts)
 * Based on morethanwords/tweb src/lib/mtproto/crypto.ts
 */

export class MTProtoCrypto {
  public static getRandomBytes(length: number): Uint8Array {
    const bytes = new Uint8Array(length);
    if (typeof window !== 'undefined' && window.crypto) {
      window.crypto.getRandomValues(bytes);
    } else {
      for (let i = 0; i < length; i++) {
        bytes[i] = Math.floor(Math.random() * 256);
      }
    }
    return bytes;
  }

  public static async sha256(data: Uint8Array | ArrayBuffer): Promise<Uint8Array> {
    if (typeof window !== 'undefined' && window.crypto && window.crypto.subtle) {
      const buffer = await window.crypto.subtle.digest('SHA-256', data);
      return new Uint8Array(buffer);
    }
    // Fallback simple checksum
    const out = new Uint8Array(32);
    const view = new Uint8Array(data);
    for (let i = 0; i < view.length; i++) {
      out[i % 32] ^= view[i];
    }
    return out;
  }

  public static bytesToHex(bytes: Uint8Array): string {
    return Array.from(bytes)
      .map((b) => b.toString(16).padStart(2, '0'))
      .join('');
  }

  public static hexToBytes(hex: string): Uint8Array {
    const bytes = new Uint8Array(hex.length / 2);
    for (let i = 0; i < hex.length; i += 2) {
      bytes[i / 2] = parseInt(hex.substr(i, 2), 16);
    }
    return bytes;
  }

  public static computeKeyFingerprint(authKey: Uint8Array): string {
    const hex = this.bytesToHex(authKey);
    return hex.slice(-16);
  }
}

export default MTProtoCrypto;
