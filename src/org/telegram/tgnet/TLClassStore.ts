/**
 * Telegram Official TL Class Store & Deserialization Registry
 * Replicates org.telegram.tgnet.TLClassStore & Native TL Layer
 */

import { TLRPC } from './TLRPC';

export class TLClassStore {
  private static instance: TLClassStore;
  private classMap: Map<number, any> = new Map();

  private constructor() {
    this.registerConstructors();
  }

  public static Instance(): TLClassStore {
    if (!TLClassStore.instance) {
      TLClassStore.instance = new TLClassStore();
    }
    return TLClassStore.instance;
  }

  private registerConstructors(): void {
    // Standard Telegram Constructor IDs
    this.classMap.set(0xc4b9f9bb, TLRPC.TL_error);
  }

  public TLdeserialize(stream: any, constructor: number, exception: boolean = true): TLRPC.TLObject | null {
    const clazz = this.classMap.get(constructor);
    if (clazz && typeof clazz.TLdeserialize === 'function') {
      return clazz.TLdeserialize(stream);
    }
    if (exception) {
      console.warn(`[TLClassStore] Free-standing constructor 0x${constructor.toString(16)} not found in store`);
    }
    return null;
  }
}
