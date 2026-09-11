/*
 * Builds the iOS-Live-Text-style "select text on image" overlay:
 *   1) a dark scrim over the whole picture,
 *   2) with the detected text regions punched out (un-dimmed) and given a
 *      subtle bright edge, so the text visually pops,
 *   3) plus a transparent, natively-selectable text layer on top.
 *
 * Everything attaches as a sibling of the <img> inside the mover, so it inherits
 * the media viewer's zoom/pan transform and stays glued to the picture.
 */

import { OcrLine } from './recognizeImageText';

const SVG_NS = 'http://www.w3.org/2000/svg';
let maskCounter = 0;

function svgRect(x: number, y: number, w: number, h: number, attrs: Record<string, string | number>) {
  const rect = document.createElementNS(SVG_NS, 'rect');
  rect.setAttribute('x', String(x));
  rect.setAttribute('y', String(y));
  rect.setAttribute('width', String(w));
  rect.setAttribute('height', String(h));
  for (const key in attrs) {
    rect.setAttribute(key, String(attrs[key]));
  }
  return rect;
}

function buildDim(naturalW: number, naturalH: number, lines: OcrLine[], fitScale: number) {
  const maskId = 'ocr-dim-mask-' + (++maskCounter);
  const svg = document.createElementNS(SVG_NS, 'svg');
  svg.setAttribute('viewBox', `0 0 ${naturalW} ${naturalH}`);
  svg.setAttribute('preserveAspectRatio', 'none');
  svg.classList.add('media-viewer-text-layer__dim');
  svg.style.position = 'absolute';
  svg.style.inset = '0';
  svg.style.width = '100%';
  svg.style.height = '100%';
  svg.style.pointerEvents = 'none';

  const mask = document.createElementNS(SVG_NS, 'mask');
  mask.id = maskId;
  mask.append(svgRect(0, 0, naturalW, naturalH, { fill: 'white' }));

  const highlights = document.createElementNS(SVG_NS, 'g');
  for (const { bbox } of lines) {
    const h = bbox.y1 - bbox.y0;
    const pad = h * 0.14;
    const x = bbox.x0 - pad;
    const y = bbox.y0 - pad;
    const w = bbox.x1 - bbox.x0 + pad * 2;
    const rh = h + pad * 2;
    const rx = h * 0.28;

    mask.append(svgRect(x, y, w, rh, { fill: 'black', rx }));
    highlights.append(
      svgRect(x, y, w, rh, {
        fill: 'none',
        rx,
        stroke: '#388aff',
        'stroke-opacity': 0.7,
        'stroke-width': 1.5 / fitScale,
      })
    );
  }

  const defs = document.createElementNS(SVG_NS, 'defs');
  defs.append(mask);

  const scrim = svgRect(0, 0, naturalW, naturalH, { fill: '#000', 'fill-opacity': 0.45 });
  scrim.setAttribute('mask', `url(#${maskId})`);

  svg.append(defs, scrim, highlights);
  return svg;
}

export default function attachTextSelectionLayer(img: HTMLImageElement, lines: OcrLine[]): HTMLElement {
  const naturalW = img.naturalWidth || img.width;
  const naturalH = img.naturalHeight || img.height;
  const fitScale = (img.clientWidth || naturalW) / naturalW;

  const layer = document.createElement('div');
  layer.classList.add('media-viewer-text-layer');
  layer.style.position = 'absolute';
  layer.style.left = img.offsetLeft + 'px';
  layer.style.top = img.offsetTop + 'px';
  layer.style.width = img.clientWidth + 'px';
  layer.style.height = img.clientHeight + 'px';
  layer.style.zIndex = '20';
  layer.style.cursor = 'text';
  layer.style.userSelect = 'text';
  (layer.style as any).webkitUserSelect = 'text';

  ['mousedown', 'touchstart', 'pointerdown', 'click'].forEach((event) => {
    layer.addEventListener(event, (e) => e.stopPropagation());
  });

  layer.append(buildDim(naturalW, naturalH, lines, fitScale));

  if (img.parentElement) {
    img.parentElement.append(layer);
  }

  for (const line of lines) {
    const { x0, y0, x1, y1 } = line.bbox;
    const el = document.createElement('div');
    el.classList.add('media-viewer-text-layer__line');
    el.textContent = line.text;
    el.style.position = 'absolute';
    el.style.whiteSpace = 'pre';
    el.style.color = 'transparent';
    el.style.transformOrigin = '0 0';
    el.style.left = (x0 / naturalW) * 100 + '%';
    el.style.top = (y0 / naturalH) * 100 + '%';

    const heightPx = (y1 - y0) * fitScale;
    el.style.height = heightPx + 'px';
    el.style.lineHeight = heightPx + 'px';
    el.style.fontSize = heightPx * 0.82 + 'px';

    layer.append(el);

    const targetPx = (x1 - x0) * fitScale;
    const naturalPx = el.scrollWidth;
    if (naturalPx > 0) {
      el.style.transform = `scaleX(${targetPx / naturalPx})`;
    }
  }

  return layer;
}
