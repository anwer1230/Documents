/**
 * TDLibEngine.ts - Telegram Database Library (TDLib) Bridge & Execution Engine
 * Replicated from tdlib/td & TDLib JNI bindings in DrKLO/Telegram Android
 */

import { TdApi } from './tdlib/TdApi';
import { tdClient, TdClient } from './tdlib/TdClient';
import { tdlibWasm, TDLibWasmBridge } from './tdlib/TDLibWasmBridge';
import { TLRPC } from './TLRPC';
import { telegramDb } from './telegramDexieDb';
import { Chat, Message, User } from '../types';

export type EngineMode = 'gramjs' | 'tdlib_wasm';

export class TDLibEngine {
  private static instance: TDLibEngine;
  private client: TdClient;
  private wasmBridge: TDLibWasmBridge;
  private isReady = false;
  private mode: EngineMode = 'tdlib_wasm';

  public static getInstance(): TDLibEngine {
    if (!TDLibEngine.instance) {
      TDLibEngine.instance = new TDLibEngine();
    }
    return TDLibEngine.instance;
  }

  private constructor() {
    this.client = tdClient;
    this.wasmBridge = tdlibWasm;
  }

  public async initialize(): Promise<void> {
    if (this.isReady) return;
    try {
      await this.client.init({
        api_id: 2040,
        api_hash: 'b18441a1ff607e10a989891a5462e627',
        application_version: '10.14.0',
        device_model: 'Web Client (TDLib WASM)',
        system_language_code: 'ar',
        system_version: '1.0',
      });
      this.isReady = true;
      console.log('[TDLib Engine] TDLib & WebAssembly C++ Core fully initialized.');
    } catch (e) {
      console.warn('[TDLib Engine] Initialization error:', e);
    }
  }

  public getClient(): TdClient {
    return this.client;
  }

  public getWasmBridge(): TDLibWasmBridge {
    return this.wasmBridge;
  }

  public setEngineMode(mode: EngineMode): void {
    this.mode = mode;
  }

  public getEngineMode(): EngineMode {
    return this.mode;
  }

  public isWasmReady(): boolean {
    return this.wasmBridge.isWasmReady();
  }

  /**
   * Execute TDLib JSON query with WASM acceleration
   */
  public async execute<T = any>(query: { '@type': string; [key: string]: any }): Promise<T> {
    await this.initialize();
    return this.client.send<T>(query as unknown as TdApi.Object);
  }

  /**
   * Execute synchronous TDLib function directly in C++/WASM layer
   */
  public executeSync(query: { '@type': string; [key: string]: any }): any {
    const jsonStr = JSON.stringify(query);
    const resultStr = this.wasmBridge.td_json_client_execute(0, jsonStr);
    try {
      return JSON.parse(resultStr);
    } catch (_) {
      return { '@type': 'error', message: 'Failed to parse TDLib response' };
    }
  }

  /**
   * Run performance benchmark comparing C++/WASM vs JS execution speed
   */
  public async benchmarkSpeed(): Promise<{ wasmOpsPerSec: number; jsOpsPerSec: number; ratio: string }> {
    const testData = new Uint8Array(1024);
    for (let i = 0; i < 1024; i++) testData[i] = (i * 37) & 0xff;

    // Benchmark WASM CRC32 & serialization
    const startWasm = performance.now();
    let wasmCount = 0;
    while (performance.now() - startWasm < 50) {
      this.wasmBridge.calculateCrc32(testData);
      wasmCount++;
    }
    const wasmDuration = performance.now() - startWasm;
    const wasmOpsPerSec = Math.round((wasmCount / wasmDuration) * 1000);

    // Benchmark JS baseline
    const startJs = performance.now();
    let jsCount = 0;
    while (performance.now() - startJs < 50) {
      let hash = 0;
      for (let j = 0; j < testData.length; j++) {
        hash = (hash * 31 + testData[j]) | 0;
      }
      jsCount++;
    }
    const jsDuration = performance.now() - startJs;
    const jsOpsPerSec = Math.round((jsCount / jsDuration) * 1000);

    const ratio = (wasmOpsPerSec / Math.max(1, jsOpsPerSec)).toFixed(1) + 'x';
    return { wasmOpsPerSec, jsOpsPerSec, ratio };
  }
}

export const tdlib = TDLibEngine.getInstance();
