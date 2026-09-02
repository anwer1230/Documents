/**
 * org.telegram.tgnet.ConnectionsManager
 * Replicated directly from ConnectionsManager.java in DrKLO/Telegram Android
 */

import { connectionsManager, ConnectionsManager as CoreConnectionsManager, RpcCallback } from '../../../core/ConnectionsManager';
import { TLRPC } from '../../../core/TLRPC';

export interface RequestDelegate {
  run(response: TLRPC.TLObject | any, error: TLRPC.TL_error | null): void;
}

export class ConnectionsManager {
  public static readonly RequestFlagEnableUnauthorized = 1;
  public static readonly RequestFlagFailOnServerErrors = 2;
  public static readonly RequestFlagCanCompress = 4;
  public static readonly RequestFlagWithoutLogin = 8;
  public static readonly RequestFlagTryDifferentDc = 16;
  public static readonly RequestFlagAddressInv = 32;
  public static readonly RequestFlagInvokeAfter = 64;
  public static readonly RequestFlagNeedQuickAck = 128;

  private static instances = new Map<number, ConnectionsManager>();
  private accountNum: number;

  public static getInstance(accountNum: number = 0): ConnectionsManager {
    if (!ConnectionsManager.instances.has(accountNum)) {
      ConnectionsManager.instances.set(accountNum, new ConnectionsManager(accountNum));
    }
    return ConnectionsManager.instances.get(accountNum)!;
  }

  private constructor(accountNum: number = 0) {
    this.accountNum = accountNum;
  }

  public sendRequest(
    request: TLRPC.TLObject | string | any,
    onComplete?: RequestDelegate | ((response: any, error: any) => void) | any,
    _flags: number = ConnectionsManager.RequestFlagCanCompress
  ): any {
    // If called as sendRequest(methodName, params) returning Promise
    if (typeof request === 'string') {
      const params = onComplete && typeof onComplete === 'object' && typeof onComplete.run !== 'function' ? onComplete : {};
      return connectionsManager.sendRequest({ _: request, ...params });
    }

    if (!onComplete || (typeof onComplete !== 'function' && typeof (onComplete as any)?.run !== 'function')) {
      return connectionsManager.sendRequest(request);
    }

    const callback: RpcCallback = {
      onSuccess: (response: any) => {
        if (typeof onComplete === 'function') {
          onComplete(response, null);
        } else if (onComplete && typeof onComplete.run === 'function') {
          onComplete.run(response, null);
        }
      },
      onError: (error: TLRPC.TL_error) => {
        if (typeof onComplete === 'function') {
          onComplete(null, error);
        } else if (onComplete && typeof onComplete.run === 'function') {
          onComplete.run(null, error);
        }
      },
    };

    const targetReq = request && request._ ? request : { ...request, _: 'TL_messages_sendMessage' };
    connectionsManager.sendRequest(targetReq, callback);
  }
}

export { CoreConnectionsManager };
