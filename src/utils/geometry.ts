import { PageItem, ShapeKind, StrokePt } from '../types/models';

export interface Pt {
  x: number;
  y: number;
}

export interface BBox {
  l: number;
  t: number;
  r: number;
  b: number;
  w: number;
  h: number;
  cx: number;
  cy: number;
  diag: number;
}

export function dist(a: Pt, b: Pt): number {
  return Math.hypot(a.x - b.x, a.y - b.y);
}

export function pathLength(pts: Pt[]): number {
  let l = 0;
  for (let i = 1; i < pts.length; i++) l += dist(pts[i - 1], pts[i]);
  return l;
}

export function getBox(pts: Pt[]): BBox {
  if (pts.length === 0) return { l: 0, t: 0, r: 0, b: 0, w: 0, h: 0, cx: 0, cy: 0, diag: 0 };
  let l = Infinity;
  let t = Infinity;
  let r = -Infinity;
  let b = -Infinity;
  for (const p of pts) {
    if (p.x < l) l = p.x;
    if (p.y < t) t = p.y;
    if (p.x > r) r = p.x;
    if (p.y > b) b = p.y;
  }
  const w = r - l;
  const h = b - t;
  return { l, t, r, b, w, h, cx: (l + r) / 2, cy: (t + b) / 2, diag: Math.hypot(w, h) };
}

function segDist(p: Pt, a: Pt, b: Pt): number {
  const vx = b.x - a.x;
  const vy = b.y - a.y;
  const len2 = vx * vx + vy * vy;
  if (len2 < 1e-6) return dist(p, a);
  const t = Math.max(0, Math.min(1, ((p.x - a.x) * vx + (p.y - a.y) * vy) / len2));
  return Math.hypot(p.x - (a.x + vx * t), p.y - (a.y + vy * t));
}

function segmentsIntersect(a1: Pt, a2: Pt, b1: Pt, b2: Pt): boolean {
  const ccw = (p1: Pt, p2: Pt, p3: Pt) => (p3.y - p1.y) * (p2.x - p1.x) > (p2.y - p1.y) * (p3.x - p1.x);
  return ccw(a1, b1, b2) !== ccw(a2, b1, b2) && ccw(a1, a2, b1) !== ccw(a1, a2, b2);
}

function turnAngleDeg(prev: Pt, cur: Pt, next: Pt): number {
  const a1 = Math.atan2(cur.y - prev.y, cur.x - prev.x);
  const a2 = Math.atan2(next.y - cur.y, next.x - cur.x);
  let d = (Math.abs(a2 - a1) * 180) / Math.PI;
  if (d > 180) d = 360 - d;
  return d;
}

export function simplify(pts: Pt[], eps: number): Pt[] {
  if (pts.length < 3) return pts;
  const a = pts[0];
  const b = pts[pts.length - 1];
  let maxD = 0;
  let idx = 0;
  for (let i = 1; i < pts.length - 1; i++) {
    const d = segDist(pts[i], a, b);
    if (d > maxD) {
      maxD = d;
      idx = i;
    }
  }
  if (maxD > eps) {
    const left = simplify(pts.slice(0, idx + 1), eps);
    const right = simplify(pts.slice(idx), eps);
    return left.slice(0, -1).concat(right);
  }
  return [a, b];
}

export function isNearlyStraight(pts: Pt[], tolerance: number): boolean {
  if (pts.length < 2) return false;
  const a = pts[0];
  const b = pts[pts.length - 1];
  if (dist(a, b) < 18) return false;
  return pts.every((p) => segDist(p, a, b) <= tolerance);
}

export function isClosedLoop(pts: Pt[]): boolean {
  if (pts.length < 8) return false;
  const bx = getBox(pts);
  const size = Math.max(bx.w, bx.h);
  if (size <= 28) return false;
  if (dist(pts[0], pts[pts.length - 1]) < size * 0.36) return true;
  const end = pts[pts.length - 1];
  const early = pts.slice(0, Math.floor(pts.length / 3));
  return early.some((p) => dist(p, end) < size * 0.28) && pathLength(pts) > size * 2.1;
}

export function isLooseLoop(pts: Pt[]): boolean {
  if (isClosedLoop(pts)) return true;
  if (pts.length < 10) return false;
  let cx = 0;
  let cy = 0;
  for (const p of pts) {
    cx += p.x;
    cy += p.y;
  }
  cx /= pts.length;
  cy /= pts.length;
  let turned = 0;
  let prev = Math.atan2(pts[0].y - cy, pts[0].x - cx);
  for (let i = 1; i < pts.length; i++) {
    const a = Math.atan2(pts[i].y - cy, pts[i].x - cx);
    let d = a - prev;
    if (d > Math.PI) d -= 2 * Math.PI;
    if (d < -Math.PI) d += 2 * Math.PI;
    turned += d;
    prev = a;
  }
  return Math.abs(turned) > (300 * Math.PI) / 180;
}

/**
 * Enhanced Scribble-to-Erase (Zig-Zag + PCA + Multi-directional).
 * Works both on finished strokes (`isScribble`) AND continuously while drawing (`isLiveScribble`).
 */
function axisReversals(pts: Pt[], threshFrac: number): { reversals: number; extent: number } {
  let mx = 0;
  let my = 0;
  for (const p of pts) {
    mx += p.x;
    my += p.y;
  }
  mx /= pts.length;
  my /= pts.length;

  let sxx = 0;
  let syy = 0;
  let sxy = 0;
  for (const p of pts) {
    const dx = p.x - mx;
    const dy = p.y - my;
    sxx += dx * dx;
    syy += dy * dy;
    sxy += dx * dy;
  }
  const theta = 0.5 * Math.atan2(2 * sxy, sxx - syy);
  const ux = Math.cos(theta);
  const uy = Math.sin(theta);

  // Also check orthogonal axis so diagonal/vertical zig-zags work reliably
  const proj1 = pts.map((p) => p.x * ux + p.y * uy);
  const proj2 = pts.map((p) => -p.x * uy + p.y * ux);

  const countRevs = (proj: number[]) => {
    let minV = Infinity;
    let maxV = -Infinity;
    for (const v of proj) {
      if (v < minV) minV = v;
      if (v > maxV) maxV = v;
    }
    const extent = maxV - minV;
    if (extent < 14) return { reversals: 0, extent };
    const thresh = Math.max(8, extent * threshFrac);
    let dir = 0;
    let anchor = proj[0];
    let reversals = 0;
    for (const v of proj) {
      if (dir === 0) {
        if (v - anchor > thresh) {
          dir = 1;
          anchor = v;
        } else if (anchor - v > thresh) {
          dir = -1;
          anchor = v;
        }
      } else if (dir === 1) {
        if (v > anchor) anchor = v;
        else if (anchor - v > thresh) {
          reversals++;
          dir = -1;
          anchor = v;
        }
      } else {
        if (v < anchor) anchor = v;
        else if (v - anchor > thresh) {
          reversals++;
          dir = 1;
          anchor = v;
        }
      }
    }
    return { reversals, extent };
  };

  const r1 = countRevs(proj1);
  const r2 = countRevs(proj2);
  return r1.reversals >= r2.reversals ? r1 : r2;
}

export function isScribble(pts: Pt[]): boolean {
  if (pts.length < 7) return false;
  const bx = getBox(pts);
  if (bx.diag < 16) return false;
  const len = pathLength(pts);
  if (len < bx.diag * 2.1) return false;

  // 1. PCA axis reversals (catches horizontal, vertical, diagonal back-and-forth)
  const { reversals } = axisReversals(pts, 0.24);
  if (reversals >= 3) return true;

  // 2. Sharp zig-zag turns
  const s = simplify(pts, Math.max(2.5, bx.diag * 0.035));
  if (s.length >= 5) {
    let sharp = 0;
    for (let i = 1; i < s.length - 1; i++) {
      if (turnAngleDeg(s[i - 1], s[i], s[i + 1]) > 92) sharp++;
    }
    if (sharp >= 3 && len >= bx.diag * 2.6) return true;
  }

  // 3. Tight overlapping circular scrub
  if (len >= bx.diag * 4.5 && reversals >= 2) return true;

  return false;
}

/**
 * Live Scribble Check during `pointermove` (works while actively drawing!).
 * Inspects the recent sliding window (~45 points) as well as the whole stroke so
 * users can scribble at any moment—even mid-sentence—to erase ink immediately.
 */
export function isLiveScribble(pts: Pt[]): boolean {
  if (pts.length < 8) return false;
  const windowPts = pts.length > 48 ? pts.slice(-48) : pts;
  if (isScribble(windowPts)) return true;
  if (pts.length > 48 && isScribble(pts)) return true;
  return false;
}

export function getItemBounds(item: PageItem): BBox {
  if (item.kind === 'ink') {
    const b = getBox(item.points);
    const pad = item.size / 2 + 4;
    return {
      l: b.l - pad,
      t: b.t - pad,
      r: b.r + pad,
      b: b.b + pad,
      w: b.w + pad * 2,
      h: b.h + pad * 2,
      cx: b.cx,
      cy: b.cy,
      diag: Math.hypot(b.w + pad * 2, b.h + pad * 2),
    };
  }
  if (item.kind === 'fill') return getBox(item.points);
  if (item.kind === 'text') {
    const lines = item.text.split('\n');
    const maxLen = Math.max(1, ...lines.map((l) => l.length));
    const w = Math.max(item.size, maxLen * item.size * 0.56);
    const h = lines.length * item.size * 1.32;
    return {
      l: item.x,
      t: item.y,
      r: item.x + w,
      b: item.y + h,
      w,
      h,
      cx: item.x + w / 2,
      cy: item.y + h / 2,
      diag: Math.hypot(w, h),
    };
  }
  const { x, y, w, h } = item;
  return { l: x, t: y, r: x + w, b: y + h, w, h, cx: x + w / 2, cy: y + h / 2, diag: Math.hypot(w, h) };
}

/** Checks if a scribble stroke touches or scrubs over an ink/text item. */
export function scribbleHitsItem(scribblePts: Pt[], item: PageItem, strokeIsHighlighter: boolean): boolean {
  if (item.kind === 'ink') {
    if (item.isHighlighter !== strokeIsHighlighter) return false;
  } else if (item.kind === 'text') {
    if (strokeIsHighlighter) return false;
  } else {
    return false;
  }

  const sb = getBox(scribblePts);
  const pad = 12;
  const ib = getItemBounds(item);
  // Quick reject
  if (sb.r + pad < ib.l || sb.l - pad > ib.r || sb.b + pad < ib.t || sb.t - pad > ib.b) {
    return false;
  }

  // Exact point/segment proximity for ink strokes (handles thin underlines & small symbols!)
  if (item.kind === 'ink') {
    const threshold = Math.max(18, item.size * 1.4);
    const sStep = Math.max(1, Math.floor(scribblePts.length / 36));
    const iStep = Math.max(1, Math.floor(item.points.length / 36));
    for (let i = 0; i < scribblePts.length; i += sStep) {
      const sp = scribblePts[i];
      for (let j = 0; j < item.points.length; j += iStep) {
        if (dist(sp, item.points[j]) <= threshold) return true;
      }
      if (i + sStep < scribblePts.length && item.points.length >= 2) {
        const sp2 = scribblePts[Math.min(scribblePts.length - 1, i + sStep)];
        for (let j = 0; j < item.points.length - 1; j += iStep) {
          const ip2 = item.points[Math.min(item.points.length - 1, j + iStep)];
          if (segmentsIntersect(sp, sp2, item.points[j], ip2)) return true;
        }
      }
    }
    return false;
  }

  // For TextItem, check overlap or scribble points inside expanded text bounds
  return scribblePts.some(
    (p) => p.x >= ib.l - 10 && p.x <= ib.r + 10 && p.y >= ib.t - 10 && p.y <= ib.b + 10
  );
}

/** Point-in-polygon check for Lasso selection */
export function pointInPolygon(pt: Pt, poly: Pt[]): boolean {
  let inside = false;
  for (let i = 0, j = poly.length - 1; i < poly.length; j = i++) {
    const xi = poly[i].x, yi = poly[i].y;
    const xj = poly[j].x, yj = poly[j].y;
    const intersect = yi > pt.y !== yj > pt.y && pt.x < ((xj - xi) * (pt.y - yi)) / (yj - yi + 1e-9) + xi;
    if (intersect) inside = !inside;
  }
  return inside;
}

export function lassoSelectsItem(poly: Pt[], item: PageItem, touching = false): boolean {
  if (poly.length < 3) return false;
  const bx = getBox(poly);
  const ib = getItemBounds(item);
  if (bx.r < ib.l || bx.l > ib.r || bx.b < ib.t || bx.t > ib.b) return false;

  if (item.kind === 'ink') {
    const pts = item.points;
    if (pts.length === 0) return false;
    const step = Math.max(1, Math.floor(pts.length / 32));
    let inside = 0;
    let total = 0;
    for (let i = 0; i < pts.length; i += step) {
      total++;
      if (pointInPolygon(pts[i], poly)) inside++;
    }
    return touching ? inside > 0 : inside >= total * 0.55;
  }
  const corners = [
    { x: ib.cx, y: ib.cy },
    { x: ib.l, y: ib.t },
    { x: ib.r, y: ib.t },
    { x: ib.l, y: ib.b },
    { x: ib.r, y: ib.b },
  ];
  return touching ? corners.some((c) => pointInPolygon(c, poly)) : pointInPolygon(corners[0], poly);
}

// ================================================================ Shapes

export function densify(corners: Pt[], step = 6): Pt[] {
  if (corners.length < 2) return corners;
  const out: Pt[] = [];
  for (let i = 0; i < corners.length - 1; i++) {
    const a = corners[i];
    const b = corners[i + 1];
    const n = Math.max(1, Math.floor(dist(a, b) / step));
    for (let k = 0; k < n; k++) {
      const t = k / n;
      out.push({ x: a.x + (b.x - a.x) * t, y: a.y + (b.y - a.y) * t });
    }
  }
  out.push(corners[corners.length - 1]);
  return out;
}

function closedCorners(raw: Pt[], eps: number): Pt[] {
  const start = raw[0];
  let far = 0;
  let farD = -1;
  for (let i = 0; i < raw.length; i++) {
    const d = dist(raw[i], start);
    if (d > farD) {
      farD = d;
      far = i;
    }
  }
  if (far === 0) return [];
  const a = simplify(raw.slice(0, far + 1), eps);
  const b = simplify([...raw.slice(far), start], eps);
  const corners = [...a.slice(0, -1), ...b.slice(0, -1)];
  let changed = true;
  while (changed && corners.length > 3) {
    changed = false;
    for (let i = 0; i < corners.length; i++) {
      const prev = corners[(i - 1 + corners.length) % corners.length];
      const cur = corners[i];
      const next = corners[(i + 1) % corners.length];
      if (dist(prev, cur) < eps * 1.2 || turnAngleDeg(prev, cur, next) < 28) {
        corners.splice(i, 1);
        changed = true;
        break;
      }
    }
  }
  return corners;
}

function polygonError(raw: Pt[], poly: Pt[], diag: number): number {
  if (poly.length < 3) return Infinity;
  let sum = 0;
  for (const p of raw) {
    let best = Infinity;
    for (let i = 0; i < poly.length; i++) {
      best = Math.min(best, segDist(p, poly[i], poly[(i + 1) % poly.length]));
    }
    sum += best;
  }
  return sum / raw.length / diag;
}

export const ShapeRecognizer = {
  recognize(raw: Pt[]): Pt[] | null {
    if (raw.length < 4) return null;
    const len = pathLength(raw);
    if (len < 20) return null;
    const bx = getBox(raw);
    const diag = Math.max(1, bx.diag);

    // Straight line
    if (dist(raw[0], raw[raw.length - 1]) / len > 0.9) {
      return densify([raw[0], raw[raw.length - 1]]);
    }

    if (!isClosedLoop(raw)) {
      const pts = simplify(raw, diag * 0.06);
      if (pts.length >= 3 && pts.length <= 4 && Math.abs(pathLength(pts) - len) / len < 0.12) {
        return densify(pts);
      }
      return null;
    }

    const rx = bx.w / 2;
    const ry = bx.h / 2;
    let ellipseErr = Infinity;
    if (rx > 5 && ry > 5) {
      let err = 0;
      for (const p of raw) err += Math.abs(Math.hypot((p.x - bx.cx) / rx, (p.y - bx.cy) / ry) - 1);
      ellipseErr = err / raw.length;
    }

    const corners = closedCorners(raw, diag * 0.06);
    const polyErr = polygonError(raw, corners, diag);
    const n = corners.length;

    if (n >= 9 && n <= 11 && polyErr < 0.055) {
      const d = corners.map((c) => Math.hypot(c.x - bx.cx, c.y - bx.cy));
      let alt = 0;
      for (let i = 0; i < n; i++) {
        if ((d[i] > d[(i + 1) % n]) !== (d[(i + 1) % n] > d[(i + 2) % n])) alt++;
      }
      if (alt >= n - 2) return ShapeBuilder.build('STAR', bx.l, bx.t, bx.r, bx.b)[0];
    }

    if (n >= 3 && n <= 8 && polyErr < 0.048 && (polyErr < ellipseErr * 0.55 || ellipseErr > 0.08)) {
      if (n === 4) {
        const angles = corners.map((c, i) => {
          const nxt = corners[(i + 1) % 4];
          const ang = (Math.atan2(nxt.y - c.y, nxt.x - c.x) * 180) / Math.PI;
          return ((ang % 90) + 90) % 90;
        });
        const axis = angles.every((a) => a < 14 || a > 76);
        const diagonal = angles.every((a) => a >= 31 && a <= 59);
        if (axis) {
          let { l, t, r, b } = bx;
          if (Math.abs(bx.w - bx.h) / Math.max(bx.w, bx.h) < 0.12) {
            const s = (bx.w + bx.h) / 2;
            l = bx.cx - s / 2;
            r = bx.cx + s / 2;
            t = bx.cy - s / 2;
            b = bx.cy + s / 2;
          }
          return densify([{ x: l, y: t }, { x: r, y: t }, { x: r, y: b }, { x: l, y: b }, { x: l, y: t }]);
        }
        if (diagonal) {
          return densify([
            { x: bx.cx, y: bx.t },
            { x: bx.r, y: bx.cy },
            { x: bx.cx, y: bx.b },
            { x: bx.l, y: bx.cy },
            { x: bx.cx, y: bx.t },
          ]);
        }
      }
      return densify([...corners, corners[0]]);
    }

    if (ellipseErr < 0.28) {
      let erx = rx;
      let ery = ry;
      if (Math.abs(erx - ery) / Math.max(erx, ery) < 0.15) {
        const rad = (erx + ery) / 2;
        erx = rad;
        ery = rad;
      }
      const out: Pt[] = [];
      for (let i = 0; i <= 96; i++) {
        const a = (i / 96) * 2 * Math.PI;
        out.push({ x: bx.cx + erx * Math.cos(a), y: bx.cy + ery * Math.sin(a) });
      }
      return out;
    }

    return null;
  },
};

function regularPoly(cx: number, cy: number, rx: number, ry: number, n: number): Pt[] {
  const pts: Pt[] = [];
  for (let i = 0; i < n; i++) {
    const a = -Math.PI / 2 + (i * 2 * Math.PI) / n;
    pts.push({ x: cx + rx * Math.cos(a), y: cy + ry * Math.sin(a) });
  }
  return densify([...pts, pts[0]]);
}

function starPoly(cx: number, cy: number, rx: number, ry: number, points = 5, inner = 0.42): Pt[] {
  const pts: Pt[] = [];
  for (let i = 0; i < points * 2; i++) {
    const a = -Math.PI / 2 + (i * Math.PI) / points;
    const k = i % 2 === 0 ? 1 : inner;
    pts.push({ x: cx + rx * k * Math.cos(a), y: cy + ry * k * Math.sin(a) });
  }
  return densify([...pts, pts[0]]);
}

function arrowHead(x0: number, y0: number, x1: number, y1: number): Pt[] {
  const ang = Math.atan2(y1 - y0, x1 - x0);
  const len = Math.max(14, Math.min(54, Math.hypot(x1 - x0, y1 - y0) * 0.18));
  const a1 = ang + Math.PI * 0.82;
  const a2 = ang - Math.PI * 0.82;
  return densify([
    { x: x1 + len * Math.cos(a1), y: y1 + len * Math.sin(a1) },
    { x: x1, y: y1 },
    { x: x1 + len * Math.cos(a2), y: y1 + len * Math.sin(a2) },
  ]);
}

function roundedRect(l: number, t: number, r: number, b: number, rad: number): Pt[] {
  const out: Pt[] = [];
  const arc = (cx: number, cy: number, start: number) => {
    for (let k = 0; k <= 10; k++) {
      const a = start + (k / 10) * (Math.PI / 2);
      out.push({ x: cx + rad * Math.cos(a), y: cy + rad * Math.sin(a) });
    }
  };
  arc(r - rad, t + rad, -Math.PI / 2);
  arc(r - rad, b - rad, 0);
  arc(l + rad, b - rad, Math.PI / 2);
  arc(l + rad, t + rad, Math.PI);
  out.push(out[0]);
  return densify(out);
}

export const SHAPE_KINDS: { id: ShapeKind; label: string; closed: boolean }[] = [
  { id: 'AUTO', label: 'Auto', closed: false },
  { id: 'LINE', label: 'Line', closed: false },
  { id: 'ARROW', label: 'Arrow', closed: false },
  { id: 'DOUBLE_ARROW', label: 'Double arrow', closed: false },
  { id: 'RECT', label: 'Rectangle', closed: true },
  { id: 'ROUNDED', label: 'Rounded', closed: true },
  { id: 'SQUIRCLE', label: 'Squircle', closed: true },
  { id: 'PILL', label: 'Pill', closed: true },
  { id: 'ELLIPSE', label: 'Ellipse', closed: true },
  { id: 'TRIANGLE', label: 'Triangle', closed: true },
  { id: 'RIGHT_TRIANGLE', label: 'Right tri', closed: true },
  { id: 'DIAMOND', label: 'Diamond', closed: true },
  { id: 'PENTAGON', label: 'Pentagon', closed: true },
  { id: 'HEXAGON', label: 'Hexagon', closed: true },
  { id: 'OCTAGON', label: 'Octagon', closed: true },
  { id: 'STAR', label: 'Star', closed: true },
  { id: 'SPARKLE', label: 'Sparkle', closed: true },
  { id: 'SUNNY', label: 'Sunny', closed: true },
  { id: 'BURST', label: 'Burst', closed: true },
  { id: 'FLOWER', label: 'Flower', closed: true },
  { id: 'CLOVER', label: 'Clover', closed: true },
  { id: 'HEART', label: 'Heart', closed: true },
  { id: 'CLOUD', label: 'Cloud', closed: true },
  { id: 'SPEECH', label: 'Speech', closed: true },
  { id: 'GEM', label: 'Gem', closed: true },
  { id: 'BADGE', label: 'Badge', closed: true },
];

export const ShapeBuilder = {
  build(kind: ShapeKind, x0: number, y0: number, x1: number, y1: number): Pt[][] {
    const l = Math.min(x0, x1);
    const r = Math.max(x0, x1);
    const t = Math.min(y0, y1);
    const b = Math.max(y0, y1);
    const cx = (l + r) / 2;
    const cy = (t + b) / 2;
    const rx = Math.max(2, (r - l) / 2);
    const ry = Math.max(2, (b - t) / 2);

    switch (kind) {
      case 'AUTO':
      case 'LINE':
        return [densify([{ x: x0, y: y0 }, { x: x1, y: y1 }])];
      case 'ARROW':
        return [densify([{ x: x0, y: y0 }, { x: x1, y: y1 }]), arrowHead(x0, y0, x1, y1)];
      case 'DOUBLE_ARROW':
        return [
          densify([{ x: x0, y: y0 }, { x: x1, y: y1 }]),
          arrowHead(x0, y0, x1, y1),
          arrowHead(x1, y1, x0, y0),
        ];
      case 'RECT':
        return [densify([{ x: l, y: t }, { x: r, y: t }, { x: r, y: b }, { x: l, y: b }, { x: l, y: t }])];
      case 'ROUNDED':
        return [roundedRect(l, t, r, b, Math.min(rx, ry) * 0.35)];
      case 'SQUIRCLE':
        return [roundedRect(l, t, r, b, Math.min(rx, ry) * 0.65)];
      case 'PILL':
        return [roundedRect(l, t, r, b, Math.min(rx, ry))];
      case 'ELLIPSE': {
        const out: Pt[] = [];
        for (let i = 0; i <= 96; i++) {
          const a = (i / 96) * 2 * Math.PI;
          out.push({ x: cx + rx * Math.cos(a), y: cy + ry * Math.sin(a) });
        }
        return [out];
      }
      case 'TRIANGLE':
      case 'SOFT_TRIANGLE':
        return [densify([{ x: cx, y: t }, { x: r, y: b }, { x: l, y: b }, { x: cx, y: t }])];
      case 'RIGHT_TRIANGLE':
        return [densify([{ x: l, y: t }, { x: r, y: b }, { x: l, y: b }, { x: l, y: t }])];
      case 'DIAMOND':
        return [densify([{ x: cx, y: t }, { x: r, y: cy }, { x: cx, y: b }, { x: l, y: cy }, { x: cx, y: t }])];
      case 'PENTAGON':
        return [regularPoly(cx, cy, rx, ry, 5)];
      case 'HEXAGON':
      case 'GEM':
        return [regularPoly(cx, cy, rx, ry, 6)];
      case 'OCTAGON':
        return [regularPoly(cx, cy, rx, ry, 8)];
      case 'STAR':
      case 'SOFT_STAR':
        return [starPoly(cx, cy, rx, ry, 5, 0.42)];
      case 'SPARKLE':
        return [starPoly(cx, cy, rx, ry, 4, 0.28)];
      case 'SUNNY':
        return [starPoly(cx, cy, rx, ry, 8, 0.78)];
      case 'BURST':
        return [starPoly(cx, cy, rx, ry, 12, 0.74)];
      case 'BADGE':
      case 'COOKIE':
        return [starPoly(cx, cy, rx, ry, 14, 0.86)];
      case 'PILL_STAR':
        return [starPoly(cx, cy, rx, ry, 10, 0.7)];
      case 'CLOVER':
      case 'FLOWER': {
        const petals = kind === 'CLOVER' ? 4 : 8;
        const out: Pt[] = [];
        for (let i = 0; i <= 160; i++) {
          const a = (i / 160) * 2 * Math.PI;
          const bump = 0.68 + 0.32 * Math.abs(Math.cos((a * petals) / 2));
          out.push({ x: cx + rx * bump * Math.cos(a), y: cy + ry * bump * Math.sin(a) });
        }
        return [out];
      }
      case 'HEART': {
        const out: Pt[] = [];
        for (let i = 0; i <= 120; i++) {
          const a = (i / 120) * 2 * Math.PI;
          const x = 16 * Math.pow(Math.sin(a), 3);
          const y = 13 * Math.cos(a) - 5 * Math.cos(2 * a) - 2 * Math.cos(3 * a) - Math.cos(4 * a);
          out.push({ x: cx + (x / 17) * rx, y: cy - (y / 17) * ry - ry * 0.08 });
        }
        return [out];
      }
      case 'CLOUD': {
        const out: Pt[] = [];
        for (let i = 0; i <= 160; i++) {
          const a = (i / 160) * 2 * Math.PI;
          const bump = 1 + 0.12 * Math.abs(Math.sin(a * 4));
          out.push({ x: cx + rx * 0.88 * bump * Math.cos(a), y: cy + ry * 0.88 * bump * Math.sin(a) });
        }
        return [out];
      }
      case 'SPEECH': {
        const tailTop = t + (b - t) * 0.75;
        const w = r - l;
        return [
          densify([
            { x: l, y: t },
            { x: r, y: t },
            { x: r, y: tailTop },
            { x: l + w * 0.45, y: tailTop },
            { x: l + w * 0.2, y: b },
            { x: l + w * 0.25, y: tailTop },
            { x: l, y: tailTop },
            { x: l, y: t },
          ]),
        ];
      }
    }
  },
};
