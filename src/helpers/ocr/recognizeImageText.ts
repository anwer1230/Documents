/*
 * On-device OCR for the media viewer's "select text on image" feature.
 * Ported from tweb (feat/ocr-select-text-on-image).
 * Dynamic import of tesseract.js ensures zero initial bundle footprint.
 */

export interface OcrWordBox {
  text: string;
  confidence: number;
  bbox: { x0: number; y0: number; x1: number; y1: number };
}

export interface OcrLine {
  text: string;
  bbox: { x0: number; y0: number; x1: number; y1: number };
}

export interface OcrResult {
  lines: OcrLine[];
  text: string;
  confidence: number;
}

const IDLE_UNLOAD_MS = 5 * 60 * 1000;
let cachedWorker: { langs: string; workerPromise: Promise<any> } | undefined;
let unloadTimeout: any;

function cancelUnload() {
  if (unloadTimeout) {
    clearTimeout(unloadTimeout);
    unloadTimeout = undefined;
  }
}

function scheduleUnload() {
  cancelUnload();
  const current = cachedWorker;
  unloadTimeout = setTimeout(() => {
    unloadTimeout = undefined;
    if (cachedWorker !== current) return;
    cachedWorker = undefined;
    current?.workerPromise.then((worker: any) => worker.terminate?.()).catch(() => {});
  }, IDLE_UNLOAD_MS);
}

function getWorker(langs: string = 'ara+eng') {
  cancelUnload();

  if (cachedWorker?.langs === langs) {
    return cachedWorker.workerPromise;
  }

  // Language changed, terminate old worker
  cachedWorker?.workerPromise.then((worker: any) => worker.terminate?.()).catch(() => {});

  const workerPromise = import('tesseract.js').then(async (mod) => {
    const createWorker = mod.createWorker || (mod as any).default?.createWorker;
    // Tesseract.js v5 supports createWorker(langs, oem, options)
    const worker = await createWorker(langs, 1);
    return worker;
  });

  cachedWorker = { langs, workerPromise };
  return workerPromise;
}

const WORD_CONFIDENCE_MIN = 50;

function hasAlnum(s: string) {
  return /[\p{L}\p{N}]/u.test(s);
}

function collectLines(data: any): OcrLine[] {
  const out: OcrLine[] = [];

  const consider = (line: any) => {
    if (!line?.bbox) return;

    const words = (line.words || []).filter(
      (w: any) => (w?.confidence ?? 0) >= WORD_CONFIDENCE_MIN && hasAlnum(w?.text || '')
    );

    if (words.length) {
      out.push({
        text: words.map((w: any) => w.text).join(' ').trim(),
        bbox: {
          x0: Math.min(...words.map((w: any) => w.bbox.x0)),
          y0: Math.min(...words.map((w: any) => w.bbox.y0)),
          x1: Math.max(...words.map((w: any) => w.bbox.x1)),
          y1: Math.max(...words.map((w: any) => w.bbox.y1)),
        },
      });
      return;
    }

    if (!line.words && (line.confidence ?? 0) >= WORD_CONFIDENCE_MIN && hasAlnum(line.text || '')) {
      out.push({
        text: (line.text || '').trim(),
        bbox: line.bbox,
      });
    }
  };

  if (data?.blocks) {
    for (const block of data.blocks) {
      for (const paragraph of block.paragraphs || []) {
        for (const line of paragraph.lines || []) {
          consider(line);
        }
      }
    }
  }

  if (!out.length && data?.lines) {
    data.lines.forEach(consider);
  }

  return out;
}

export default async function recognizeImageText(
  image: HTMLImageElement | HTMLCanvasElement | string
): Promise<OcrResult> {
  const worker = await getWorker('ara+eng');
  try {
    const { data } = await worker.recognize(image, {}, { blocks: true });
    return {
      lines: collectLines(data),
      text: (data.text || '').trim(),
      confidence: Math.round(data.confidence || 0),
    };
  } finally {
    scheduleUnload();
  }
}
