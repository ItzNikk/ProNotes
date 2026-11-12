import {
  InkItem,
  MindNodeItem,
  Page,
  PageItem,
  PaperStyle,
  StickyItem,
  TapeItem,
  TextItem,
} from '../types/models';
import { getItemBounds } from './geometry';

function hexToRgba(hex: string, alpha: number): string {
  const clean = hex.replace('#', '');
  const full = clean.length === 3 ? clean.split('').map((c) => c + c).join('') : clean.slice(0, 6);
  const num = parseInt(full || '3D6CF4', 16);
  const r = (num >> 16) & 255;
  const g = (num >> 8) & 255;
  const b = num & 255;
  return `rgba(${r},${g},${b},${alpha})`;
}

export function isColorDark(hex: string): boolean {
  const clean = hex.replace('#', '');
  const num = parseInt(clean.slice(0, 6) || 'ffffff', 16);
  const r = ((num >> 16) & 255) / 255;
  const g = ((num >> 8) & 255) / 255;
  const b = (num & 255) / 255;
  return 0.2126 * r + 0.7152 * g + 0.0722 * b < 0.42;
}

/** Deterministic PRNG for cover/paper textures */
function mulberry32(seed: number) {
  let a = seed | 0;
  return function () {
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function drawCoverOnCanvas(
  ctx: CanvasRenderingContext2D,
  w: number,
  h: number,
  spec: string,
  title = ''
): void {
  const parts = spec.split(':');
  const kind = parts[0];
  const color = parts[parts.length - 1]?.startsWith('#') ? parts[parts.length - 1] : '#3D6CF4';
  const pattern = kind === 'pattern' ? parts[1] : null;

  ctx.save();
  ctx.fillStyle = color;
  ctx.fillRect(0, 0, w, h);

  const seed = (pattern || 'solid').split('').reduce((s, c) => s * 31 + c.charCodeAt(0), 7);
  const rnd = mulberry32(seed);

  if (pattern === 'dots') {
    ctx.fillStyle = 'rgba(255,255,255,0.28)';
    const st = w / 8;
    let row = 0;
    for (let y = st / 2; y < h; y += st) {
      for (let x = row % 2 === 0 ? st / 2 : st; x < w; x += st) {
        ctx.beginPath();
        ctx.arc(x, y, st * 0.17, 0, Math.PI * 2);
        ctx.fill();
      }
      row++;
    }
  } else if (pattern === 'grid') {
    ctx.strokeStyle = 'rgba(255,255,255,0.26)';
    ctx.lineWidth = Math.max(1, w / 220);
    const st = w / 7;
    for (let x = 0; x < w; x += st) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, h);
      ctx.stroke();
    }
    for (let y = 0; y < h; y += st) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(w, y);
      ctx.stroke();
    }
  } else if (pattern === 'waves') {
    ctx.strokeStyle = 'rgba(255,255,255,0.26)';
    ctx.lineWidth = Math.max(2, h / 42);
    const st = h / 9;
    for (let y = 0; y < h + st; y += st) {
      ctx.beginPath();
      for (let x = 0; x <= w; x += w / 30) {
        const wy = y + Math.sin((x / w) * 12.56) * st * 0.28;
        if (x === 0) ctx.moveTo(x, wy);
        else ctx.lineTo(x, wy);
      }
      ctx.stroke();
    }
  } else if (pattern === 'scales') {
    ctx.strokeStyle = 'rgba(255,255,255,0.25)';
    ctx.lineWidth = Math.max(1.5, w / 65);
    const r = w / 6;
    let row = 0;
    for (let y = 0; y < h + r; y += r * 0.9) {
      for (let x = row % 2 === 0 ? 0 : r; x < w + r; x += r * 2) {
        ctx.beginPath();
        ctx.arc(x, y, r, 0, Math.PI * 2);
        ctx.stroke();
      }
      row++;
    }
  } else if (pattern === 'linen') {
    ctx.strokeStyle = 'rgba(255,255,255,0.09)';
    ctx.lineWidth = Math.max(0.6, w / 240);
    const step = Math.max(2, w / 65);
    for (let y = 0; y < h; y += step) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(w, y);
      ctx.stroke();
    }
    ctx.strokeStyle = 'rgba(0,0,0,0.07)';
    for (let x = 0; x < w; x += step) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, h);
      ctx.stroke();
    }
  } else if (pattern === 'leather') {
    for (let i = 0; i < 420; i++) {
      ctx.fillStyle = i % 2 === 0 ? 'rgba(0,0,0,0.14)' : 'rgba(255,255,255,0.09)';
      ctx.beginPath();
      ctx.arc(rnd() * w, rnd() * h, w / 160 + rnd() * (w / 140), 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.strokeStyle = 'rgba(255,255,255,0.42)';
    ctx.lineWidth = Math.max(1.2, w / 140);
    ctx.setLineDash([Math.max(3, w / 35), Math.max(2, w / 55)]);
    ctx.strokeRect(w * 0.07, h * 0.04, w * 0.86, h * 0.92);
    ctx.setLineDash([]);
  } else if (pattern === 'kraft') {
    ctx.lineWidth = Math.max(0.8, w / 320);
    for (let i = 0; i < 320; i++) {
      ctx.strokeStyle = rnd() > 0.5 ? 'rgba(255,255,255,0.15)' : 'rgba(0,0,0,0.12)';
      const x = rnd() * w;
      const y = rnd() * h;
      const l = (w / 24) * rnd();
      ctx.beginPath();
      ctx.moveTo(x, y);
      ctx.lineTo(x + l, y + l * (rnd() - 0.5));
      ctx.stroke();
    }
  } else if (pattern === 'marble') {
    ctx.lineCap = 'round';
    for (let i = 0; i < 12; i++) {
      ctx.strokeStyle = i % 3 === 0 ? 'rgba(255,255,255,0.34)' : 'rgba(255,255,255,0.14)';
      ctx.lineWidth = w / 160 + rnd() * (w / 70);
      ctx.beginPath();
      let x = rnd() * w;
      let y = -h * 0.1;
      ctx.moveTo(x, y);
      while (y < h * 1.1) {
        x += (rnd() - 0.45) * (w / 6);
        y += h / 12;
        ctx.lineTo(x, y);
      }
      ctx.stroke();
    }
  } else if (pattern === 'gradient') {
    const g = ctx.createLinearGradient(0, 0, w, h);
    g.addColorStop(0, 'rgba(255,255,255,0.36)');
    g.addColorStop(0.5, 'rgba(255,255,255,0)');
    g.addColorStop(1, 'rgba(0,0,0,0.28)');
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, w, h);
  } else if (pattern === 'geometric') {
    const st = w / 4;
    let row = 0;
    for (let y = 0; y < h; y += st) {
      for (let x = row % 2 === 0 ? 0 : -st / 2; x < w; x += st / 2) {
        ctx.fillStyle = (row + Math.floor(x / st)) % 2 === 0 ? 'rgba(255,255,255,0.16)' : 'rgba(0,0,0,0.13)';
        ctx.beginPath();
        ctx.moveTo(x, y + st);
        ctx.lineTo(x + st / 2, y);
        ctx.lineTo(x + st, y + st);
        ctx.closePath();
        ctx.fill();
      }
      row++;
    }
  } else if (pattern === 'terrazzo') {
    const tones = ['rgba(255,255,255,0.45)', 'rgba(0,0,0,0.2)', 'rgba(255,214,165,0.45)', 'rgba(160,196,255,0.45)', 'rgba(255,173,173,0.45)'];
    for (let i = 0; i < 95; i++) {
      ctx.fillStyle = tones[Math.floor(rnd() * tones.length)];
      const cx = rnd() * w;
      const cy = rnd() * h;
      const s = w / 50 + rnd() * (w / 24);
      ctx.beginPath();
      ctx.moveTo(cx, cy - s);
      ctx.lineTo(cx + s * (0.5 + rnd()), cy);
      ctx.lineTo(cx, cy + s * 0.8);
      ctx.lineTo(cx - s, cy + s * 0.2);
      ctx.closePath();
      ctx.fill();
    }
  } else if (pattern === 'plaid') {
    const st = w / 5;
    for (let x = 0; x < w; x += st) {
      ctx.fillStyle = 'rgba(255,255,255,0.14)';
      ctx.fillRect(x, 0, st * 0.35, h);
      ctx.fillStyle = 'rgba(0,0,0,0.2)';
      ctx.fillRect(x + st * 0.6, 0, st * 0.05, h);
    }
    for (let y = 0; y < h; y += st) {
      ctx.fillStyle = 'rgba(255,255,255,0.14)';
      ctx.fillRect(0, y, w, st * 0.35);
      ctx.fillStyle = 'rgba(0,0,0,0.2)';
      ctx.fillRect(0, y + st * 0.6, w, st * 0.05);
    }
  } else if (pattern === 'spiral') {
    ctx.strokeStyle = '#C5CBD5';
    ctx.lineWidth = Math.max(1.5, w / 72);
    for (let y = h * 0.07; y < h * 0.94; y += h / 16) {
      ctx.fillStyle = 'rgba(0,0,0,0.34)';
      ctx.beginPath();
      ctx.arc(w * 0.09, y, w / 55, 0, Math.PI * 2);
      ctx.fill();
      ctx.beginPath();
      ctx.arc(w * 0.04, y, w / 22, -Math.PI * 0.7, Math.PI * 0.6);
      ctx.stroke();
    }
  } else if (pattern === 'stripes') {
    ctx.strokeStyle = 'rgba(255,255,255,0.24)';
    ctx.lineWidth = w / 14;
    const st = w / 5;
    for (let x = -h; x < w; x += st) {
      ctx.beginPath();
      ctx.moveTo(x, h);
      ctx.lineTo(x + h, 0);
      ctx.stroke();
    }
  }

  // Spine gradient + notebook elastic band
  const spine = ctx.createLinearGradient(0, 0, w * 0.09, 0);
  spine.addColorStop(0, 'rgba(0,0,0,0.24)');
  spine.addColorStop(1, 'rgba(0,0,0,0)');
  ctx.fillStyle = spine;
  ctx.fillRect(0, 0, w * 0.09, h);

  ctx.fillStyle = 'rgba(0,0,0,0.16)';
  ctx.fillRect(w * 0.83, 0, w * 0.045, h);

  if (title) {
    const labelBg = 'rgba(255,255,255,0.92)';
    const bx = w * 0.16;
    const by = h * 0.25;
    const bw = w * 0.62;
    const bh = h * 0.26;
    ctx.fillStyle = labelBg;
    ctx.beginPath();
    ctx.roundRect(bx, by, bw, bh, Math.max(4, w * 0.04));
    ctx.fill();
    ctx.fillStyle = '#1B1F2A';
    const fontSize = Math.max(9, Math.round(w * 0.09));
    ctx.font = `600 ${fontSize}px "Plus Jakarta Sans", sans-serif`;
    const words = title.split(' ');
    let line = '';
    let ly = by + fontSize * 1.45;
    for (const word of words) {
      const test = line ? `${line} ${word}` : word;
      if (ctx.measureText(test).width > bw - w * 0.1 && line) {
        ctx.fillText(line, bx + w * 0.05, ly);
        line = word;
        ly += fontSize * 1.25;
        if (ly > by + bh - 4) break;
      } else {
        line = test;
      }
    }
    if (line && ly <= by + bh - 2) ctx.fillText(line, bx + w * 0.05, ly);
  }
  ctx.restore();
}

/** Draws any of the 25 Pro Notes paper templates onto a rectangle of size (w, h). */
export function drawPaperTemplate(
  ctx: CanvasRenderingContext2D,
  w: number,
  h: number,
  paper: PaperStyle,
  bgColor: string,
  lineScale = 1,
  customLineColor?: string | null,
  isPreview = false
): void {
  const dark = isColorDark(bgColor);
  const lineCol = customLineColor || (dark ? '#3A3F48' : '#D5DEEA');
  const dotCol = dark ? '#4A505A' : '#BAC4D4';
  const labelCol = dark ? '#8A93A3' : '#909BAA';
  const marginCol = 'rgba(235, 87, 87, 0.42)';
  const g = Math.max(0.5, Math.min(2.4, lineScale));

  ctx.save();
  ctx.fillStyle = bgColor;
  ctx.fillRect(0, 0, w, h);

  ctx.strokeStyle = lineCol;
  ctx.fillStyle = dotCol;
  ctx.lineWidth = isPreview ? 1 : 1.6;

  const ruled = (start: number, gap: number, left = 0, right = w) => {
    ctx.strokeStyle = lineCol;
    ctx.lineWidth = isPreview ? 1 : 1.5;
    for (let y = start; y < h - (isPreview ? 4 : 36); y += gap * g) {
      ctx.beginPath();
      ctx.moveTo(left, y);
      ctx.lineTo(right, y);
      ctx.stroke();
    }
  };

  const grid = (step0: number, lw = isPreview ? 0.9 : 1.4) => {
    const step = step0 * g;
    ctx.strokeStyle = lineCol;
    ctx.lineWidth = lw;
    for (let x = step; x < w; x += step) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, h);
      ctx.stroke();
    }
    for (let y = step; y < h; y += step) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(w, y);
      ctx.stroke();
    }
  };

  const drawMargin = (x: number) => {
    ctx.strokeStyle = marginCol;
    ctx.lineWidth = isPreview ? 1 : 1.8;
    ctx.beginPath();
    ctx.moveTo(x, 0);
    ctx.lineTo(x, h);
    ctx.stroke();
  };

  const labelFont = `${Math.max(8, Math.round(w * 0.021))}px "Plus Jakarta Sans", sans-serif`;

  switch (paper) {
    case 'BLANK':
      break;
    case 'RULED':
      ruled(h * 0.1, h * 0.03);
      drawMargin(w * 0.105);
      break;
    case 'NARROW':
      ruled(h * 0.09, h * 0.022);
      drawMargin(w * 0.09);
      break;
    case 'COLLEGE':
      ruled(h * 0.095, h * 0.024);
      drawMargin(w * 0.097);
      break;
    case 'WIDE':
      ruled(h * 0.11, h * 0.038);
      drawMargin(w * 0.112);
      break;
    case 'LEGAL':
      ruled(h * 0.125, h * 0.029);
      drawMargin(w * 0.12);
      drawMargin(w * 0.127);
      ctx.strokeStyle = marginCol;
      ctx.beginPath();
      ctx.moveTo(0, h * 0.09);
      ctx.lineTo(w, h * 0.09);
      ctx.stroke();
      break;
    case 'HANDWRITING': {
      let y = h * 0.1;
      const band = h * 0.055 * g;
      while (y + band < h - h * 0.04) {
        ctx.strokeStyle = lineCol;
        ctx.lineWidth = isPreview ? 0.9 : 1.4;
        ctx.setLineDash([]);
        ctx.beginPath();
        ctx.moveTo(w * 0.06, y);
        ctx.lineTo(w * 0.94, y);
        ctx.stroke();

        ctx.setLineDash(isPreview ? [3, 3] : [10, 8]);
        ctx.beginPath();
        ctx.moveTo(w * 0.06, y + band / 2);
        ctx.lineTo(w * 0.94, y + band / 2);
        ctx.stroke();

        ctx.setLineDash([]);
        ctx.lineWidth = isPreview ? 1.3 : 2.2;
        ctx.beginPath();
        ctx.moveTo(w * 0.06, y + band);
        ctx.lineTo(w * 0.94, y + band);
        ctx.stroke();
        y += band * 1.65;
      }
      break;
    }
    case 'GRID':
      grid(w * 0.034);
      break;
    case 'SMALL_GRID':
      grid(w * 0.021);
      break;
    case 'GRAPH':
      grid(w * 0.017, isPreview ? 0.6 : 1);
      grid(w * 0.085, isPreview ? 1.2 : 2.2);
      break;
    case 'DOTTED': {
      const st = w * 0.034 * g;
      ctx.fillStyle = dotCol;
      for (let y = st; y < h; y += st) {
        for (let x = st; x < w; x += st) {
          ctx.beginPath();
          ctx.arc(x, y, isPreview ? 0.9 : 2.1, 0, Math.PI * 2);
          ctx.fill();
        }
      }
      break;
    }
    case 'ISOMETRIC': {
      const st = w * 0.04 * g;
      const rowH = st * 0.866;
      ctx.fillStyle = dotCol;
      let row = 0;
      for (let y = rowH; y < h; y += rowH) {
        for (let x = row % 2 === 0 ? st / 2 : st; x < w; x += st) {
          ctx.beginPath();
          ctx.arc(x, y, isPreview ? 0.9 : 2.0, 0, Math.PI * 2);
          ctx.fill();
        }
        row++;
      }
      break;
    }
    case 'HEX': {
      const r = w * 0.03 * g;
      const hh = r * 1.732;
      ctx.strokeStyle = lineCol;
      ctx.lineWidth = isPreview ? 0.8 : 1.4;
      let col = 0;
      for (let cx = r; cx < w + r; cx += r * 1.5) {
        for (let cy = col % 2 === 0 ? hh / 2 : hh; cy < h + hh; cy += hh) {
          ctx.beginPath();
          for (let k = 0; k <= 6; k++) {
            const a = (Math.PI / 3) * k;
            const px = cx + r * Math.cos(a);
            const py = cy + r * Math.sin(a);
            if (k === 0) ctx.moveTo(px, py);
            else ctx.lineTo(px, py);
          }
          ctx.stroke();
        }
        col++;
      }
      break;
    }
    case 'CORNELL':
      ctx.strokeStyle = lineCol;
      ctx.lineWidth = isPreview ? 1.4 : 2.6;
      ctx.beginPath();
      ctx.moveTo(0, h * 0.115);
      ctx.lineTo(w, h * 0.115);
      ctx.moveTo(w * 0.3, h * 0.115);
      ctx.lineTo(w * 0.3, h * 0.8);
      ctx.moveTo(0, h * 0.8);
      ctx.lineTo(w, h * 0.8);
      ctx.stroke();
      ruled(h * 0.145, h * 0.03, w * 0.3, w);
      ctx.fillStyle = labelCol;
      ctx.font = labelFont;
      ctx.fillText('CUES', w * 0.035, h * 0.14);
      ctx.fillText('SUMMARY', w * 0.035, h * 0.825);
      break;
    case 'TODO':
    case 'CHECKLIST': {
      ctx.fillStyle = labelCol;
      ctx.font = labelFont;
      ctx.fillText(paper === 'TODO' ? 'TO DO' : 'CHECKLIST', w * 0.065, h * 0.07);
      let y = h * 0.12;
      while (y < h - h * 0.05) {
        ctx.strokeStyle = lineCol;
        ctx.lineWidth = isPreview ? 1 : 2;
        if (paper === 'CHECKLIST') {
          ctx.beginPath();
          ctx.arc(w * 0.08, y - h * 0.009, w * 0.013, 0, Math.PI * 2);
          ctx.stroke();
        } else {
          ctx.strokeRect(w * 0.065, y - h * 0.019, w * 0.027, w * 0.027);
        }
        ctx.lineWidth = isPreview ? 0.9 : 1.4;
        ctx.beginPath();
        ctx.moveTo(w * 0.115, y);
        ctx.lineTo(w * 0.93, y);
        ctx.stroke();
        y += h * 0.04 * g;
      }
      break;
    }
    case 'PLANNER': {
      const days = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN', 'NOTES'];
      const top = h * 0.08;
      const cellH = (h - top - h * 0.03) / 4;
      const cellW = (w - w * 0.06) / 2;
      ctx.font = labelFont;
      days.forEach((d, i) => {
        const cx = w * 0.03 + (i % 2) * cellW;
        const cy = top + Math.floor(i / 2) * cellH;
        ctx.strokeStyle = lineCol;
        ctx.lineWidth = isPreview ? 1 : 2;
        ctx.beginPath();
        ctx.roundRect(cx + w * 0.008, cy + h * 0.005, cellW - w * 0.016, cellH - h * 0.01, isPreview ? 4 : 14);
        ctx.stroke();
        ctx.fillStyle = labelCol;
        ctx.fillText(d, cx + w * 0.025, cy + h * 0.028);
      });
      break;
    }
    case 'DAILY': {
      const split = w * 0.55;
      ctx.fillStyle = labelCol;
      ctx.font = labelFont;
      ctx.fillText('DATE', w * 0.06, h * 0.062);
      ctx.strokeStyle = lineCol;
      ctx.beginPath();
      ctx.moveTo(w * 0.14, h * 0.064);
      ctx.lineTo(w * 0.5, h * 0.064);
      ctx.moveTo(split, h * 0.09);
      ctx.lineTo(split, h * 0.95);
      ctx.stroke();
      let y = h * 0.115;
      for (let hr = 7; hr <= 20 && y < h * 0.94; hr++) {
        ctx.fillStyle = labelCol;
        ctx.fillText(`${String(hr).padStart(2, '0')}:00`, w * 0.055, y - 4);
        ctx.beginPath();
        ctx.moveTo(w * 0.13, y);
        ctx.lineTo(split - w * 0.02, y);
        ctx.stroke();
        y += (h * 0.82) / 14;
      }
      ctx.fillStyle = labelCol;
      ctx.fillText('TOP 3', split + w * 0.025, h * 0.075);
      for (let k = 0; k < 3; k++) {
        ctx.strokeRect(split + w * 0.025, h * 0.095 + k * h * 0.04, w * 0.025, w * 0.025);
      }
      ctx.fillText('NOTES', split + w * 0.025, h * 0.24);
      ruled(h * 0.27, h * 0.03, split + w * 0.025, w * 0.94);
      break;
    }
    case 'MONTHLY': {
      const names = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
      const top = h * 0.11;
      const cw = (w - w * 0.08) / 7;
      const ch = (h - top - h * 0.04) / 6;
      ctx.fillStyle = labelCol;
      ctx.font = labelFont;
      ctx.fillText('MONTH', w * 0.055, h * 0.06);
      names.forEach((n, i) => ctx.fillText(n, w * 0.04 + i * cw + 4, top - h * 0.01));
      ctx.strokeStyle = lineCol;
      for (let r = 0; r < 6; r++) {
        for (let c = 0; c < 7; c++) {
          ctx.strokeRect(w * 0.04 + c * cw, top + r * ch, cw, ch);
        }
      }
      break;
    }
    case 'MEETING':
      ctx.fillStyle = labelCol;
      ctx.font = labelFont;
      ['MEETING', 'DATE', 'ATTENDEES'].forEach((l, i) => {
        const y = h * 0.065 + i * h * 0.036;
        ctx.fillText(l, w * 0.055, y);
        ctx.beginPath();
        ctx.moveTo(w * 0.2, y + 3);
        ctx.lineTo(w * 0.94, y + 3);
        ctx.stroke();
      });
      ctx.lineWidth = isPreview ? 1.4 : 2.6;
      ctx.beginPath();
      ctx.moveTo(0, h * 0.175);
      ctx.lineTo(w, h * 0.175);
      ctx.moveTo(0, h * 0.78);
      ctx.lineTo(w, h * 0.78);
      ctx.stroke();
      ctx.fillText('NOTES', w * 0.055, h * 0.2);
      ctx.fillText('ACTION ITEMS', w * 0.055, h * 0.805);
      for (let y = h * 0.23; y < h * 0.76; y += h * 0.03 * g) {
        ctx.lineWidth = isPreview ? 0.9 : 1.4;
        ctx.beginPath();
        ctx.moveTo(w * 0.055, y);
        ctx.lineTo(w * 0.94, y);
        ctx.stroke();
      }
      break;
    case 'STUDY':
      ctx.fillStyle = labelCol;
      ctx.font = labelFont;
      ctx.fillText('TOPIC', w * 0.055, h * 0.062);
      ctx.strokeStyle = lineCol;
      ctx.beginPath();
      ctx.moveTo(w * 0.15, h * 0.065);
      ctx.lineTo(w * 0.94, h * 0.065);
      ctx.stroke();
      ctx.lineWidth = isPreview ? 1.2 : 2.2;
      ctx.beginPath();
      ctx.roundRect(w * 0.05, h * 0.09, w * 0.9, h * 0.1, isPreview ? 4 : 16);
      ctx.roundRect(w * 0.05, h * 0.82, w * 0.9, h * 0.13, isPreview ? 4 : 16);
      ctx.stroke();
      ctx.fillText('KEY IDEAS', w * 0.07, h * 0.115);
      ctx.fillText('REMEMBER', w * 0.07, h * 0.845);
      for (let y = h * 0.225; y < h * 0.8; y += h * 0.03 * g) {
        ctx.lineWidth = isPreview ? 0.9 : 1.4;
        ctx.beginPath();
        ctx.moveTo(w * 0.055, y);
        ctx.lineTo(w * 0.94, y);
        ctx.stroke();
      }
      break;
    case 'TWO_COLUMN':
      ctx.lineWidth = isPreview ? 1.4 : 2.6;
      ctx.beginPath();
      ctx.moveTo(w / 2, h * 0.07);
      ctx.lineTo(w / 2, h * 0.96);
      ctx.stroke();
      ruled(h * 0.09, h * 0.03, w * 0.035, w / 2 - w * 0.02);
      ruled(h * 0.09, h * 0.03, w / 2 + w * 0.02, w * 0.965);
      break;
    case 'STORYBOARD': {
      const cw = (w - w * 0.1) / 2;
      const ch = (h - h * 0.09) / 3;
      ctx.strokeStyle = lineCol;
      for (let r = 0; r < 3; r++) {
        for (let c = 0; c < 2; c++) {
          const x = w * 0.05 + c * cw;
          const y = h * 0.045 + r * ch;
          ctx.lineWidth = isPreview ? 1.2 : 2.2;
          ctx.beginPath();
          ctx.roundRect(x + w * 0.01, y + h * 0.006, cw - w * 0.02, ch * 0.68, isPreview ? 3 : 10);
          ctx.stroke();
          ctx.lineWidth = isPreview ? 0.8 : 1.4;
          ctx.beginPath();
          ctx.moveTo(x + w * 0.01, y + ch * 0.78);
          ctx.lineTo(x + cw - w * 0.01, y + ch * 0.78);
          ctx.moveTo(x + w * 0.01, y + ch * 0.9);
          ctx.lineTo(x + cw - w * 0.01, y + ch * 0.9);
          ctx.stroke();
        }
      }
      break;
    }
    case 'MUSIC': {
      let y = h * 0.1;
      const gap = h * 0.009 * g;
      while (y + 4 * gap < h * 0.95) {
        for (let k = 0; k < 5; k++) {
          ctx.beginPath();
          ctx.moveTo(w * 0.065, y + k * gap);
          ctx.lineTo(w * 0.935, y + k * gap);
          ctx.stroke();
        }
        y += h * 0.085;
      }
      break;
    }
    case 'TABLATURE': {
      let y = h * 0.1;
      const gap = h * 0.01 * g;
      ctx.font = labelFont;
      while (y + 5 * gap < h * 0.95) {
        for (let k = 0; k < 6; k++) {
          ctx.beginPath();
          ctx.moveTo(w * 0.075, y + k * gap);
          ctx.lineTo(w * 0.935, y + k * gap);
          ctx.stroke();
        }
        ctx.fillStyle = labelCol;
        ctx.fillText('T', w * 0.042, y + gap * 1.6);
        ctx.fillText('A', w * 0.042, y + gap * 3.1);
        ctx.fillText('B', w * 0.042, y + gap * 4.6);
        y += 5 * gap + h * 0.055;
      }
      break;
    }
  }
  ctx.restore();
}

/** Draws the unbounded Infinite Whiteboard engineering dot grid & origin anchor. */
export function drawWhiteboardBackdrop(
  ctx: CanvasRenderingContext2D,
  viewW: number,
  viewH: number,
  panX: number,
  panY: number,
  zoom: number,
  bgColor: string
): void {
  const dark = isColorDark(bgColor);
  ctx.save();
  ctx.fillStyle = bgColor;
  ctx.fillRect(0, 0, viewW, viewH);

  // Adaptive grid spacing based on zoom
  const baseStep = zoom < 0.45 ? 160 : zoom > 1.8 ? 40 : 80;
  const minorStep = baseStep / 2;

  const worldLeft = -panX / zoom;
  const worldTop = -panY / zoom;
  const worldRight = (viewW - panX) / zoom;
  const worldBottom = (viewH - panY) / zoom;

  // Faint major grid lines
  ctx.strokeStyle = dark ? 'rgba(255,255,255,0.045)' : 'rgba(15,23,42,0.045)';
  ctx.lineWidth = 1;
  const startX = Math.floor(worldLeft / baseStep) * baseStep;
  const startY = Math.floor(worldTop / baseStep) * baseStep;

  for (let wx = startX; wx <= worldRight; wx += baseStep) {
    const sx = wx * zoom + panX;
    ctx.beginPath();
    ctx.moveTo(sx, 0);
    ctx.lineTo(sx, viewH);
    ctx.stroke();
  }
  for (let wy = startY; wy <= worldBottom; wy += baseStep) {
    const sy = wy * zoom + panY;
    ctx.beginPath();
    ctx.moveTo(0, sy);
    ctx.lineTo(viewW, sy);
    ctx.stroke();
  }

  // Crisp spatial dots
  ctx.fillStyle = dark ? 'rgba(255,255,255,0.16)' : 'rgba(15,23,42,0.14)';
  const dStartX = Math.floor(worldLeft / minorStep) * minorStep;
  const dStartY = Math.floor(worldTop / minorStep) * minorStep;
  for (let wx = dStartX; wx <= worldRight; wx += minorStep) {
    for (let wy = dStartY; wy <= worldBottom; wy += minorStep) {
      const sx = wx * zoom + panX;
      const sy = wy * zoom + panY;
      ctx.fillRect(sx - 1, sy - 1, 2, 2);
    }
  }

  // Origin crosshair at (0,0)
  const ox = panX;
  const oy = panY;
  if (ox >= -40 && ox <= viewW + 40 && oy >= -40 && oy <= viewH + 40) {
    ctx.strokeStyle = dark ? 'rgba(99,145,255,0.35)' : 'rgba(42,120,214,0.3)';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(ox - 14, oy);
    ctx.lineTo(ox + 14, oy);
    ctx.moveTo(ox, oy - 14);
    ctx.lineTo(ox, oy + 14);
    ctx.stroke();
  }
  ctx.restore();
}

export function drawInkStroke(
  ctx: CanvasRenderingContext2D,
  item: InkItem,
  darkBg: boolean,
  alphaOverride?: number
): void {
  const pts = item.points;
  if (pts.length === 0) return;

  ctx.save();
  if (item.isHighlighter) {
    ctx.globalCompositeOperation = darkBg ? 'lighten' : 'multiply';
    ctx.globalAlpha = (item.opacity ?? 0.52) * (alphaOverride ?? 1);
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.strokeStyle = item.color;
    ctx.lineWidth = item.size;
    ctx.beginPath();
    ctx.moveTo(pts[0].x, pts[0].y);
    for (let i = 1; i < pts.length; i++) {
      const prev = pts[i - 1];
      const cur = pts[i];
      ctx.quadraticCurveTo(prev.x, prev.y, (prev.x + cur.x) / 2, (prev.y + cur.y) / 2);
    }
    ctx.lineTo(pts[pts.length - 1].x, pts[pts.length - 1].y);
    ctx.stroke();
    ctx.restore();
    return;
  }

  ctx.globalAlpha = alphaOverride ?? 1;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.strokeStyle = item.color;

  if (pts.length === 1) {
    ctx.fillStyle = item.color;
    ctx.beginPath();
    ctx.arc(pts[0].x, pts[0].y, Math.max(1, item.size / 2), 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();
    return;
  }

  const hasPressure = item.penKind === 'fountain' || item.penKind === 'brush';
  if (!hasPressure) {
    ctx.lineWidth = item.size;
    ctx.beginPath();
    ctx.moveTo(pts[0].x, pts[0].y);
    for (let i = 1; i < pts.length - 1; i++) {
      const mx = (pts[i].x + pts[i + 1].x) / 2;
      const my = (pts[i].y + pts[i + 1].y) / 2;
      ctx.quadraticCurveTo(pts[i].x, pts[i].y, mx, my);
    }
    ctx.lineTo(pts[pts.length - 1].x, pts[pts.length - 1].y);
    ctx.stroke();
  } else {
    for (let i = 1; i < pts.length; i++) {
      const a = pts[i - 1];
      const b = pts[i];
      const p = b.p ?? 0.65;
      const mult = item.penKind === 'brush' ? 0.55 + p * 0.9 : 0.7 + p * 0.55;
      ctx.lineWidth = Math.max(1, item.size * mult);
      ctx.beginPath();
      ctx.moveTo(a.x, a.y);
      ctx.lineTo(b.x, b.y);
      ctx.stroke();
    }
  }
  ctx.restore();
}

export function drawTapeItem(ctx: CanvasRenderingContext2D, item: TapeItem): void {
  ctx.save();
  const cx = item.x + item.w / 2;
  const cy = item.y + item.h / 2;
  if (item.rot) {
    ctx.translate(cx, cy);
    ctx.rotate((item.rot * Math.PI) / 180);
    ctx.translate(-cx, -cy);
  }
  ctx.globalAlpha = item.revealed ? 0.25 : 0.92;
  ctx.fillStyle = item.color;
  ctx.beginPath();
  ctx.roundRect(item.x, item.y, item.w, item.h, 4);
  ctx.fill();

  if (!item.revealed) {
    ctx.save();
    ctx.beginPath();
    ctx.roundRect(item.x, item.y, item.w, item.h, 4);
    ctx.clip();
    ctx.strokeStyle = 'rgba(255,255,255,0.42)';
    ctx.fillStyle = 'rgba(255,255,255,0.42)';

    if (item.pattern === 'stripes') {
      ctx.lineWidth = 6;
      for (let x = item.x - item.h; x < item.x + item.w + item.h; x += 22) {
        ctx.beginPath();
        ctx.moveTo(x, item.y + item.h);
        ctx.lineTo(x + item.h, item.y);
        ctx.stroke();
      }
    } else if (item.pattern === 'dots') {
      for (let x = item.x + 12; x < item.x + item.w; x += 22) {
        for (let y = item.y + 12; y < item.y + item.h; y += 22) {
          ctx.beginPath();
          ctx.arc(x, y, 4.5, 0, Math.PI * 2);
          ctx.fill();
        }
      }
    } else if (item.pattern === 'grid') {
      ctx.lineWidth = 2;
      for (let x = item.x; x < item.x + item.w; x += 18) {
        ctx.beginPath();
        ctx.moveTo(x, item.y);
        ctx.lineTo(x, item.y + item.h);
        ctx.stroke();
      }
      for (let y = item.y; y < item.y + item.h; y += 18) {
        ctx.beginPath();
        ctx.moveTo(item.x, y);
        ctx.lineTo(item.x + item.w, y);
        ctx.stroke();
      }
    } else if (item.pattern === 'checks') {
      const sz = 16;
      for (let ix = 0; ix * sz < item.w; ix++) {
        for (let iy = 0; iy * sz < item.h; iy++) {
          if ((ix + iy) % 2 === 0) {
            ctx.fillRect(item.x + ix * sz, item.y + iy * sz, sz, sz);
          }
        }
      }
    } else if (item.pattern === 'zigzag') {
      ctx.lineWidth = 3;
      ctx.beginPath();
      let up = true;
      for (let x = item.x; x <= item.x + item.w + 16; x += 14) {
        const y = up ? item.y + 10 : item.y + item.h - 10;
        if (x === item.x) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
        up = !up;
      }
      ctx.stroke();
    } else if (item.pattern === 'waves') {
      ctx.lineWidth = 3;
      ctx.beginPath();
      for (let x = item.x; x <= item.x + item.w; x += 4) {
        const y = cy + Math.sin((x - item.x) * 0.14) * (item.h * 0.25);
        if (x === item.x) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.stroke();
    } else if (item.pattern === 'stars' || item.pattern === 'hearts' || item.pattern === 'flowers') {
      const sym = item.pattern === 'stars' ? '★' : item.pattern === 'hearts' ? '♥' : '✿';
      ctx.font = '14px sans-serif';
      for (let x = item.x + 10; x < item.x + item.w - 6; x += 26) {
        ctx.fillText(sym, x, cy + 5);
      }
    }
    ctx.restore();
  }
  ctx.restore();
}

export function drawStickyItem(ctx: CanvasRenderingContext2D, item: StickyItem): void {
  ctx.save();
  const cx = item.x + item.w / 2;
  const cy = item.y + item.h / 2;
  if (item.rot) {
    ctx.translate(cx, cy);
    ctx.rotate((item.rot * Math.PI) / 180);
    ctx.translate(-cx, -cy);
  }
  ctx.shadowColor = 'rgba(0,0,0,0.16)';
  ctx.shadowBlur = 14;
  ctx.shadowOffsetY = 5;
  ctx.fillStyle = item.color;
  ctx.beginPath();
  ctx.roundRect(item.x, item.y, item.w, item.h, 8);
  ctx.fill();
  ctx.shadowColor = 'transparent';

  // Subtle folded bottom-right corner
  ctx.fillStyle = 'rgba(0,0,0,0.08)';
  ctx.beginPath();
  ctx.moveTo(item.x + item.w - 20, item.y + item.h);
  ctx.lineTo(item.x + item.w, item.y + item.h - 20);
  ctx.lineTo(item.x + item.w - 20, item.y + item.h - 20);
  ctx.closePath();
  ctx.fill();

  // Text wrapping inside sticky
  ctx.fillStyle = '#1F232E';
  const fontSize = Math.max(14, Math.min(26, item.w * 0.085));
  ctx.font = `500 ${fontSize}px "DM Sans", sans-serif`;
  const maxW = item.w - 28;
  const lineH = fontSize * 1.32;
  let ty = item.y + 22 + fontSize;
  for (const para of item.text.split('\n')) {
    const words = para.split(' ');
    let line = '';
    for (const w of words) {
      const test = line ? `${line} ${w}` : w;
      if (ctx.measureText(test).width > maxW && line) {
        ctx.fillText(line, item.x + 14, ty);
        line = w;
        ty += lineH;
        if (ty > item.y + item.h - 14) break;
      } else {
        line = test;
      }
    }
    if (line && ty <= item.y + item.h - 10) {
      ctx.fillText(line, item.x + 14, ty);
      ty += lineH;
    }
  }
  ctx.restore();
}

export function drawTextItem(ctx: CanvasRenderingContext2D, item: TextItem): void {
  ctx.save();
  const b = getItemBounds(item);
  if (item.rot) {
    ctx.translate(b.cx, b.cy);
    ctx.rotate((item.rot * Math.PI) / 180);
    ctx.translate(-b.cx, -b.cy);
  }
  const fam =
    item.font === 'SERIF'
      ? 'Georgia, serif'
      : item.font === 'MONO'
        ? '"JetBrains Mono", monospace'
        : item.font === 'HAND' || item.font === 'CURSIVE'
          ? '"Caveat", cursive'
          : '"DM Sans", sans-serif';
  const weight = item.bold ? '700' : item.isMathAnswer ? '700' : '500';
  const style = item.italic ? 'italic' : 'normal';
  ctx.font = `${style} ${weight} ${item.size}px ${fam}`;

  if (item.gradient) {
    const g = ctx.createLinearGradient(b.l, b.t, b.r, b.b);
    g.addColorStop(0, item.color);
    g.addColorStop(1, item.gradient);
    ctx.fillStyle = g;
  } else {
    ctx.fillStyle = item.color;
  }

  // Apple-Notes-inspired soft golden glow for recently created math answers
  if (item.isMathAnswer && item.createdAt) {
    const age = Date.now() - item.createdAt;
    if (age < 1800) {
      const fade = 1 - age / 1800;
      ctx.shadowColor = `rgba(242, 169, 42, ${0.55 * fade})`;
      ctx.shadowBlur = 16 * fade;
    }
  }

  const lh = item.size * 1.32;
  item.text.split('\n').forEach((line, i) => {
    const yPos = item.y + item.size + i * lh;
    ctx.fillText(line, item.x, yPos);
    if (item.underline) {
      const lw = ctx.measureText(line).width;
      ctx.strokeStyle = item.color;
      ctx.lineWidth = Math.max(1.5, item.size * 0.06);
      ctx.beginPath();
      ctx.moveTo(item.x, yPos + 3);
      ctx.lineTo(item.x + lw, yPos + 3);
      ctx.stroke();
    }
  });
  ctx.restore();
}

/** Draws Mind Map Bezier links between parent & visible child nodes */
export function drawMindMapEdges(
  ctx: CanvasRenderingContext2D,
  nodes: MindNodeItem[],
  hiddenIds: Set<string>,
  darkBg: boolean
): void {
  const map = new Map(nodes.map((n) => [n.id, n]));
  ctx.save();
  for (const node of nodes) {
    if (!node.parentId || hiddenIds.has(node.id)) continue;
    const parent = map.get(node.parentId);
    if (!parent || hiddenIds.has(parent.id)) continue;

    const px = parent.x + parent.w / 2;
    const py = parent.y + parent.h / 2;
    const cx = node.x + node.w / 2;
    const cy = node.y + node.h / 2;
    const dx = cx - px;

    ctx.strokeStyle = hexToRgba(node.color, darkBg ? 0.85 : 0.78);
    ctx.lineWidth = parent.parentId === null ? 3.5 : 2.5;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(px, py);
    ctx.bezierCurveTo(px + dx * 0.48, py, cx - dx * 0.48, cy, cx, cy);
    ctx.stroke();
  }
  ctx.restore();
}

export function drawMindMapNode(
  ctx: CanvasRenderingContext2D,
  node: MindNodeItem,
  isSelected: boolean,
  childCount: number,
  darkBg: boolean
): void {
  const isRoot = node.parentId === null;
  ctx.save();

  ctx.shadowColor = isSelected ? hexToRgba(node.color, 0.35) : 'rgba(0,0,0,0.12)';
  ctx.shadowBlur = isSelected ? 18 : 10;
  ctx.shadowOffsetY = 3;

  const fill = isRoot ? node.color : darkBg ? '#1F2229' : '#FFFFFF';
  ctx.fillStyle = fill;
  ctx.strokeStyle = isSelected ? '#3D6CF4' : node.color;
  ctx.lineWidth = isSelected ? 3 : isRoot ? 0 : 2.2;

  const radius = node.shape === 'pill' ? node.h / 2 : node.shape === 'card' ? 12 : 4;
  ctx.beginPath();
  if (node.shape === 'diamond') {
    const cx = node.x + node.w / 2;
    const cy = node.y + node.h / 2;
    ctx.moveTo(cx, node.y);
    ctx.lineTo(node.x + node.w, cy);
    ctx.lineTo(cx, node.y + node.h);
    ctx.lineTo(node.x, cy);
    ctx.closePath();
  } else {
    ctx.roundRect(node.x, node.y, node.w, node.h, radius);
  }
  ctx.fill();
  ctx.shadowColor = 'transparent';
  if (ctx.lineWidth > 0) ctx.stroke();

  // Accent stripe on left of non-root cards
  if (!isRoot && node.shape === 'card') {
    ctx.fillStyle = node.color;
    ctx.beginPath();
    ctx.roundRect(node.x + 2, node.y + 6, 4.5, node.h - 12, 2);
    ctx.fill();
  }

  // Label
  ctx.fillStyle = isRoot ? '#FFFFFF' : darkBg ? '#F3F4F6' : '#1B1F2A';
  ctx.font = `${isRoot ? '700 16px' : '600 14px'} "Plus Jakarta Sans", sans-serif`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  const maxTextW = node.w - 24;
  let txt = node.label;
  while (txt.length > 3 && ctx.measureText(txt).width > maxTextW) {
    txt = txt.slice(0, -2) + '…';
  }
  ctx.fillText(txt, node.x + node.w / 2, node.y + node.h / 2 + 1);

  // Collapsed badge
  if (node.collapsed && childCount > 0) {
    ctx.fillStyle = node.color;
    ctx.beginPath();
    ctx.arc(node.x + node.w + 4, node.y + node.h / 2, 11, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = '#FFFFFF';
    ctx.font = '700 11px "JetBrains Mono", monospace';
    ctx.fillText(`+${childCount}`, node.x + node.w + 4, node.y + node.h / 2 + 0.5);
  }

  ctx.restore();
}

export function getHiddenMindNodeIds(nodes: MindNodeItem[]): Set<string> {
  const hidden = new Set<string>();
  const collapsed = new Set(nodes.filter((n) => n.collapsed).map((n) => n.id));
  if (collapsed.size === 0) return hidden;

  const byParent = new Map<string, string[]>();
  for (const n of nodes) {
    if (n.parentId) {
      const list = byParent.get(n.parentId) || [];
      list.push(n.id);
      byParent.set(n.parentId, list);
    }
  }
  const hideDescendants = (id: string) => {
    for (const child of byParent.get(id) || []) {
      hidden.add(child);
      hideDescendants(child);
    }
  };
  collapsed.forEach((cid) => hideDescendants(cid));
  return hidden;
}

export function renderPageItems(
  ctx: CanvasRenderingContext2D,
  page: Page,
  imageCache: Map<string, HTMLImageElement>,
  selectedIds: Set<string>,
  highlighterBehind = true
): void {
  const bgColor = page.paperColor || '#FFFFFF';
  const darkBg = isColorDark(bgColor);

  const mindNodes = page.items.filter((it): it is MindNodeItem => it.kind === 'mindnode');
  const hiddenMind = getHiddenMindNodeIds(mindNodes);

  const fills: PageItem[] = [];
  const highlights: PageItem[] = [];
  const rest: PageItem[] = [];

  for (const it of page.items) {
    if (it.kind === 'fill') fills.push(it);
    else if (highlighterBehind && it.kind === 'ink' && it.isHighlighter) highlights.push(it);
    else if (it.kind === 'mindnode') continue;
    else rest.push(it);
  }

  const drawSingle = (item: PageItem) => {
    if (item.kind === 'fill') {
      if (item.points.length < 3) return;
      ctx.save();
      ctx.fillStyle = item.color;
      ctx.beginPath();
      item.points.forEach((p, idx) => (idx === 0 ? ctx.moveTo(p.x, p.y) : ctx.lineTo(p.x, p.y)));
      ctx.closePath();
      ctx.fill();
      ctx.restore();
    } else if (item.kind === 'ink') {
      drawInkStroke(ctx, item, darkBg);
    } else if (item.kind === 'text') {
      drawTextItem(ctx, item);
    } else if (item.kind === 'sticky') {
      drawStickyItem(ctx, item);
    } else if (item.kind === 'tape') {
      drawTapeItem(ctx, item);
    } else if (item.kind === 'image') {
      let img = imageCache.get(item.src);
      if (!img) {
        img = new Image();
        img.src = item.src;
        imageCache.set(item.src, img);
      }
      if (img.complete && img.naturalWidth > 0) {
        ctx.save();
        if (item.rot) {
          const cx = item.x + item.w / 2;
          const cy = item.y + item.h / 2;
          ctx.translate(cx, cy);
          ctx.rotate((item.rot * Math.PI) / 180);
          ctx.translate(-cx, -cy);
        }
        ctx.beginPath();
        ctx.roundRect(item.x, item.y, item.w, item.h, 8);
        ctx.clip();
        ctx.drawImage(img, item.x, item.y, item.w, item.h);
        ctx.restore();
      }
    }
  };

  fills.forEach(drawSingle);
  highlights.forEach(drawSingle);
  rest.forEach(drawSingle);

  // Mind Map layer (edges behind nodes)
  if (mindNodes.length > 0) {
    drawMindMapEdges(ctx, mindNodes, hiddenMind, darkBg);
    const childCounts = new Map<string, number>();
    for (const n of mindNodes) {
      if (n.parentId) childCounts.set(n.parentId, (childCounts.get(n.parentId) || 0) + 1);
    }
    for (const node of mindNodes) {
      if (hiddenMind.has(node.id)) continue;
      drawMindMapNode(ctx, node, selectedIds.has(node.id), childCounts.get(node.id) || 0, darkBg);
    }
  }
}
