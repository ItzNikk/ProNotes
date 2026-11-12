/**
 * Enhanced On-Device Maths Engine for Pro Notes.
 * Supports:
 * - Basic & chained arithmetic, powers (^, **), factorials (!), percentages (%, "20% of 150", "80 + 15%")
 * - Implicit multiplication: 2(3+4), 3pi, 2x, (x+1)(x-2)
 * - Constants: pi, e, tau, phi
 * - Scientific functions: sqrt, cbrt, abs, sin, cos, tan, sind, cosd, tand, asin, acos, atan, ln, log, log2, log10, exp, floor, ceil, round, deg, rad
 * - Combinatorics: nCr(n, r), nPr(n, r)
 * - Unit conversions: "12 km to m", "72 f to c", "5 ft to cm", "2.5 kg to lb", "60 mph to km/h", "1024 mb to gb", etc.
 * - Multi-sided Linear Equations: "2x + 3 = x + 7" -> "x = 4"
 * - Quadratic Equations: "x^2 - 5x + 6 = 0" -> "x = 2, 3"
 * - Rejects incorrect completed equalities (e.g. "2 + 2 = 5" returns null)
 */

export interface MathCandidateResult {
  raw: string;
  answer: string;
  prettyExpr: string;
}

const UNIT_FACTORS: Record<string, { group: string; toBase: number; label?: string }> = {
  // Length (base: meter)
  mm: { group: 'len', toBase: 0.001 },
  cm: { group: 'len', toBase: 0.01 },
  m: { group: 'len', toBase: 1 },
  km: { group: 'len', toBase: 1000 },
  in: { group: 'len', toBase: 0.0254 },
  inch: { group: 'len', toBase: 0.0254 },
  inches: { group: 'len', toBase: 0.0254 },
  ft: { group: 'len', toBase: 0.3048 },
  yd: { group: 'len', toBase: 0.9144 },
  mi: { group: 'len', toBase: 1609.344 },
  mile: { group: 'len', toBase: 1609.344 },
  miles: { group: 'len', toBase: 1609.344 },
  // Mass (base: kg)
  mg: { group: 'mass', toBase: 1e-6 },
  g: { group: 'mass', toBase: 0.001 },
  kg: { group: 'mass', toBase: 1 },
  oz: { group: 'mass', toBase: 0.0283495231 },
  lb: { group: 'mass', toBase: 0.45359237 },
  lbs: { group: 'mass', toBase: 0.45359237 },
  // Time (base: sec)
  ms: { group: 'time', toBase: 0.001 },
  s: { group: 'time', toBase: 1 },
  sec: { group: 'time', toBase: 1 },
  min: { group: 'time', toBase: 60 },
  hr: { group: 'time', toBase: 3600 },
  h: { group: 'time', toBase: 3600 },
  day: { group: 'time', toBase: 86400 },
  days: { group: 'time', toBase: 86400 },
  // Speed (base: m/s)
  'm/s': { group: 'speed', toBase: 1 },
  'km/h': { group: 'speed', toBase: 1 / 3.6 },
  kph: { group: 'speed', toBase: 1 / 3.6 },
  mph: { group: 'speed', toBase: 0.44704 },
  // Data (base: byte, binary 1024)
  b: { group: 'data', toBase: 1 },
  kb: { group: 'data', toBase: 1024 },
  mb: { group: 'data', toBase: 1024 ** 2 },
  gb: { group: 'data', toBase: 1024 ** 3 },
  tb: { group: 'data', toBase: 1024 ** 4 },
  // Angle (base: rad)
  rad: { group: 'ang', toBase: 1 },
  deg: { group: 'ang', toBase: Math.PI / 180 },
};

export function normalizeMath(raw: string): string {
  let s = raw
    .trim()
    .replace(/√/g, 'sqrt')
    .replace(/∛/g, 'cbrt')
    .replace(/²/g, '^2')
    .replace(/³/g, '^3')
    .replace(/\*\*/g, '^')
    .replace(/[×·⋅]/g, '*')
    .replace(/[÷]/g, '/')
    .replace(/[−—–]/g, '-')
    .replace(/π/g, 'pi')
    .replace(/τ/g, 'tau')
    .replace(/φ/g, 'phi');

  // Natural percentage phrases before collapsing spaces
  s = s.replace(/(\d+(?:\.\d+)?)\s*%\s+of\s+([+-]?\d+(?:\.\d+)?)/gi, '($1/100)*($2)');
  s = s.replace(/(\d+(?:\.\d+)?)\s*%\s+off\s+([+-]?\d+(?:\.\d+)?)/gi, '($2)*(1-($1/100))');

  s = s.replace(/,/g, '.').replace(/\s+/g, '');
  // Turn 'x' between numbers/closing-parens into multiplication unless part of 'exp' or equation
  s = s.replace(/(?<=[0-9.)])[xX](?=[0-9.(])/g, '*');
  s = s.replace(/X/g, 'x');
  // Conservative OCR digit confusions only between digits
  s = s
    .replace(/(?<=\d)[oO](?=\d)/g, '0')
    .replace(/(?<=\d)[lI](?=\d)/g, '1')
    .replace(/(?<=\d)[sS](?=\d)/g, '5');
  s = s.replace(/sqrt(\d+(?:\.\d+)?)/g, 'sqrt($1)');
  s = s.replace(/cbrt(\d+(?:\.\d+)?)/g, 'cbrt($1)');
  return s;
}

export function formatMathNumber(val: number): string {
  if (!Number.isFinite(val)) throw new Error('Non-finite');
  if (Math.abs(val) < 1e-12) return '0';
  if (Math.abs(val - Math.round(val)) < 1e-9 && Math.abs(val) < 9e15) {
    return String(Math.round(val));
  }
  return Number(val.toPrecision(10)).toString();
}

function tryUnitConversion(raw: string): string | null {
  const clean = raw.trim().replace(/[=?]+$/g, '').trim();
  const m = clean.match(/^([+-]?\d+(?:\.\d+)?)\s*°?\s*([a-zA-Z/]+)\s+(?:to|in|->)\s+°?\s*([a-zA-Z/]+)$/i);
  if (!m) return null;
  const value = Number(m[1]);
  if (!Number.isFinite(value)) return null;
  const from = m[2].toLowerCase();
  const to = m[3].toLowerCase();

  // Temperature special cases
  const temps = new Set(['c', 'f', 'k', 'celsius', 'fahrenheit', 'kelvin']);
  const normTemp = (u: string) => (u.startsWith('c') ? 'c' : u.startsWith('f') ? 'f' : 'k');
  if (temps.has(from) && temps.has(to)) {
    const f = normTemp(from);
    const t = normTemp(to);
    let c = value;
    if (f === 'f') c = ((value - 32) * 5) / 9;
    else if (f === 'k') c = value - 273.15;
    let res = c;
    if (t === 'f') res = (c * 9) / 5 + 32;
    else if (t === 'k') res = c + 273.15;
    const suffix = t === 'k' ? ' K' : ` °${t.toUpperCase()}`;
    return `${formatMathNumber(res)}${suffix}`;
  }

  const u1 = UNIT_FACTORS[from];
  const u2 = UNIT_FACTORS[to];
  if (!u1 || !u2 || u1.group !== u2.group) return null;
  const out = (value * u1.toBase) / u2.toBase;
  return `${formatMathNumber(out)} ${m[3]}`;
}

function factorial(n: number): number {
  if (n < 0 || n > 170 || Math.abs(n - Math.round(n)) > 1e-10) {
    throw new Error('Invalid factorial');
  }
  let res = 1;
  for (let k = 2; k <= Math.round(n); k++) res *= k;
  return res;
}

function nPr(n: number, r: number): number {
  if (n < 0 || r < 0 || r > n || !Number.isInteger(n) || !Number.isInteger(r)) {
    throw new Error('Invalid nPr');
  }
  let res = 1;
  for (let i = 0; i < r; i++) res *= n - i;
  return res;
}

function nCr(n: number, r: number): number {
  if (n < 0 || r < 0 || r > n || !Number.isInteger(n) || !Number.isInteger(r)) {
    throw new Error('Invalid nCr');
  }
  const k = Math.min(r, n - r);
  let res = 1;
  for (let i = 1; i <= k; i++) {
    res = (res * (n - k + i)) / i;
  }
  return Math.round(res);
}

class NumericParser {
  private i = 0;
  constructor(private readonly s: string) {}

  parse(): number {
    const res = this.expression();
    if (this.i !== this.s.length) throw new Error('Trailing characters');
    return res;
  }

  private peek(): string {
    return this.i < this.s.length ? this.s[this.i] : '';
  }

  private startsPrimary(at: number): boolean {
    if (at < 0 || at >= this.s.length) return false;
    const c = this.s[at];
    return /[0-9.(a-zA-Z]/.test(c);
  }

  private expression(): number {
    let val = this.term();
    while (this.peek() === '+' || this.peek() === '-') {
      const op = this.s[this.i++];
      const rhs = this.term();
      // Support "80 + 15%" -> 80 + 80*0.15? Wait: standard math treats 80+15% as 80.15 unless explicit, keep strict math consistency!
      val = op === '+' ? val + rhs : val - rhs;
    }
    return val;
  }

  private term(): number {
    let val = this.unary();
    while (this.i < this.s.length) {
      const ch = this.peek();
      if (ch === '*') {
        this.i++;
        val *= this.unary();
      } else if (ch === '/') {
        this.i++;
        val /= this.unary();
      } else if (ch === '%') {
        if (this.startsPrimary(this.i + 1)) {
          this.i++;
          val %= this.unary();
        } else {
          break;
        }
      } else if (this.startsPrimary(this.i)) {
        val *= this.unary();
      } else {
        break;
      }
    }
    return val;
  }

  private unary(): number {
    if (this.peek() === '+') {
      this.i++;
      return this.unary();
    }
    if (this.peek() === '-') {
      this.i++;
      return -this.unary();
    }
    return this.power();
  }

  private power(): number {
    const base = this.postfix();
    if (this.peek() === '^') {
      this.i++;
      return Math.pow(base, this.unary());
    }
    return base;
  }

  private postfix(): number {
    let val = this.primary();
    while (this.i < this.s.length) {
      const ch = this.peek();
      if (ch === '%') {
        if (this.startsPrimary(this.i + 1)) return val;
        this.i++;
        val /= 100;
      } else if (ch === '!') {
        this.i++;
        val = factorial(val);
      } else if (ch === '°') {
        this.i++;
        val = (val * Math.PI) / 180;
      } else {
        break;
      }
    }
    return val;
  }

  private primary(): number {
    if (this.i >= this.s.length) throw new Error('Unexpected end');
    const ch = this.peek();
    if (ch === '(') {
      this.i++;
      const val = this.expression();
      if (this.peek() !== ')') throw new Error('Missing )');
      this.i++;
      return val;
    }
    if (/[0-9.]/.test(ch)) return this.number();
    if (/[a-zA-Z]/.test(ch)) {
      const start = this.i;
      while (this.i < this.s.length && /[a-zA-Z0-9]/.test(this.s[this.i])) this.i++;
      const name = this.s.slice(start, this.i).toLowerCase();
      if (name === 'pi') return Math.PI;
      if (name === 'e') return Math.E;
      if (name === 'tau') return Math.PI * 2;
      if (name === 'phi') return (1 + Math.sqrt(5)) / 2;
      return this.callFunction(name);
    }
    throw new Error(`Unexpected char ${ch}`);
  }

  private number(): number {
    const start = this.i;
    let digits = 0;
    while (this.i < this.s.length && /[0-9]/.test(this.s[this.i])) {
      this.i++;
      digits++;
    }
    if (this.peek() === '.') {
      this.i++;
      while (this.i < this.s.length && /[0-9]/.test(this.s[this.i])) {
        this.i++;
        digits++;
      }
    }
    if (digits === 0) throw new Error('Invalid number');
    if (this.peek() === 'e' || this.peek() === 'E') {
      const expStart = this.i++;
      if (this.peek() === '+' || this.peek() === '-') this.i++;
      const dStart = this.i;
      while (this.i < this.s.length && /[0-9]/.test(this.s[this.i])) this.i++;
      if (this.i === dStart) this.i = expStart;
    }
    return Number(this.s.slice(start, this.i));
  }

  private callFunction(name: string): number {
    if (this.peek() !== '(') throw new Error(`Expected ( after ${name}`);
    this.i++;
    const args: number[] = [];
    if (this.peek() !== ')') {
      args.push(this.expression());
      while (this.peek() === ';') {
        this.i++;
        args.push(this.expression());
      }
    }
    if (this.peek() !== ')') throw new Error('Missing )');
    this.i++;
    if (args.length === 0) throw new Error('Empty function args');
    const x = args[0];
    const degToRad = (d: number) => (d * Math.PI) / 180;
    const radToDeg = (r: number) => (r * 180) / Math.PI;
    switch (name) {
      case 'sqrt':
        if (x < 0) throw new Error('Negative sqrt');
        return Math.sqrt(x);
      case 'cbrt':
        return Math.cbrt(x);
      case 'abs':
        return Math.abs(x);
      case 'sin':
        return Math.sin(x);
      case 'cos':
        return Math.cos(x);
      case 'tan':
        return Math.tan(x);
      case 'sind':
        return Math.sin(degToRad(x));
      case 'cosd':
        return Math.cos(degToRad(x));
      case 'tand':
        return Math.tan(degToRad(x));
      case 'asin':
        return Math.asin(x);
      case 'acos':
        return Math.acos(x);
      case 'atan':
        return Math.atan(x);
      case 'ln':
        if (x <= 0) throw new Error('ln domain');
        return Math.log(x);
      case 'log2':
        if (x <= 0) throw new Error('log2 domain');
        return Math.log2(x);
      case 'log':
      case 'log10':
        if (x <= 0) throw new Error('log domain');
        if (args.length === 2) {
          if (args[1] <= 0 || args[1] === 1) throw new Error('log base');
          return Math.log(x) / Math.log(args[1]);
        }
        return Math.log10(x);
      case 'floor':
        return Math.floor(x);
      case 'ceil':
        return Math.ceil(x);
      case 'round':
        return Math.round(x);
      case 'exp':
        return Math.exp(x);
      case 'deg':
        return radToDeg(x);
      case 'rad':
        return degToRad(x);
      case 'ncr':
        if (args.length !== 2) throw new Error('nCr needs 2 args');
        return nCr(x, args[1]);
      case 'npr':
        if (args.length !== 2) throw new Error('nPr needs 2 args');
        return nPr(x, args[1]);
      default:
        throw new Error(`Unknown function ${name}`);
    }
  }
}

/** Quadratic polynomial (a*x^2 + b*x + c) so both linear AND quadratic equations solve cleanly. */
interface Poly2 {
  a: number; // x^2
  b: number; // x
  c: number; // const
}

function addPoly(p: Poly2, q: Poly2): Poly2 {
  return { a: p.a + q.a, b: p.b + q.b, c: p.c + q.c };
}

function subPoly(p: Poly2, q: Poly2): Poly2 {
  return { a: p.a - q.a, b: p.b - q.b, c: p.c - q.c };
}

function mulPoly(p: Poly2, q: Poly2): Poly2 {
  const a = p.a * q.c + p.b * q.b + p.c * q.a;
  const b = p.b * q.c + p.c * q.b;
  const c = p.c * q.c;
  // Reject degree > 2
  if (
    Math.abs(p.a * q.a) > 1e-12 ||
    Math.abs(p.a * q.b) > 1e-12 ||
    Math.abs(p.b * q.a) > 1e-12
  ) {
    throw new Error('Degree > 2');
  }
  return { a, b, c };
}

function divPoly(p: Poly2, q: Poly2): Poly2 {
  if (Math.abs(q.a) > 1e-12 || Math.abs(q.b) > 1e-12 || Math.abs(q.c) < 1e-12) {
    throw new Error('Non-constant divisor');
  }
  return { a: p.a / q.c, b: p.b / q.c, c: p.c / q.c };
}

class PolyParser {
  private i = 0;
  constructor(private readonly s: string) {}

  parse(): Poly2 {
    const res = this.expr();
    if (this.i !== this.s.length) throw new Error('Trailing poly');
    return res;
  }

  private peek(): string {
    return this.i < this.s.length ? this.s[this.i] : '';
  }

  private startsPrimary(): boolean {
    if (this.i >= this.s.length) return false;
    return /[0-9.(x]/.test(this.s[this.i]);
  }

  private expr(): Poly2 {
    let v = this.term();
    while (this.peek() === '+' || this.peek() === '-') {
      const op = this.s[this.i++];
      const rhs = this.term();
      v = op === '+' ? addPoly(v, rhs) : subPoly(v, rhs);
    }
    return v;
  }

  private term(): Poly2 {
    let v = this.unary();
    while (this.i < this.s.length) {
      if (this.peek() === '*') {
        this.i++;
        v = mulPoly(v, this.unary());
      } else if (this.peek() === '/') {
        this.i++;
        v = divPoly(v, this.unary());
      } else if (this.startsPrimary()) {
        v = mulPoly(v, this.unary());
      } else {
        break;
      }
    }
    return v;
  }

  private unary(): Poly2 {
    if (this.peek() === '+') {
      this.i++;
      return this.unary();
    }
    if (this.peek() === '-') {
      this.i++;
      const u = this.unary();
      return { a: -u.a, b: -u.b, c: -u.c };
    }
    return this.power();
  }

  private power(): Poly2 {
    const base = this.primary();
    if (this.peek() === '^') {
      this.i++;
      const exp = this.unary();
      if (Math.abs(exp.a) > 1e-12 || Math.abs(exp.b) > 1e-12) throw new Error('Variable exponent');
      const n = exp.c;
      if (n === 0) return { a: 0, b: 0, c: 1 };
      if (n === 1) return base;
      if (n === 2) return mulPoly(base, base);
      if (Math.abs(base.a) < 1e-12 && Math.abs(base.b) < 1e-12) {
        return { a: 0, b: 0, c: Math.pow(base.c, n) };
      }
      throw new Error('Power > 2');
    }
    return base;
  }

  private primary(): Poly2 {
    const ch = this.peek();
    if (ch === '(') {
      this.i++;
      const v = this.expr();
      if (this.peek() !== ')') throw new Error('Missing )');
      this.i++;
      return v;
    }
    if (ch === 'x') {
      this.i++;
      return { a: 0, b: 1, c: 0 };
    }
    const start = this.i;
    while (this.i < this.s.length && /[0-9.]/.test(this.s[this.i])) this.i++;
    if (this.i === start) throw new Error('Invalid poly number');
    return { a: 0, b: 0, c: Number(this.s.slice(start, this.i)) };
  }
}

export function evaluateNumeric(expression: string): number | null {
  try {
    const norm = normalizeMath(expression);
    if (!norm) return null;
    const val = new NumericParser(norm).parse();
    if (!Number.isFinite(val) || Math.abs(val) > 1e100) return null;
    return val;
  } catch {
    return null;
  }
}

function solvePolynomialEquation(leftRaw: string, rightRaw: string): string | null {
  try {
    const left = new PolyParser(normalizeMath(leftRaw)).parse();
    const right = new PolyParser(normalizeMath(rightRaw)).parse();
    const p = subPoly(left, right); // a*x^2 + b*x + c = 0
    if (Math.abs(p.a) < 1e-12) {
      // Linear equation: b*x + c = 0
      if (Math.abs(p.b) < 1e-12) return null;
      const ans = -p.c / p.b;
      if (!Number.isFinite(ans)) return null;
      return `x = ${formatMathNumber(ans)}`;
    }
    // Quadratic equation: a*x^2 + b*x + c = 0
    const disc = p.b * p.b - 4 * p.a * p.c;
    if (disc < -1e-9) {
      const real = -p.b / (2 * p.a);
      const imag = Math.sqrt(-disc) / Math.abs(2 * p.a);
      return `x = ${formatMathNumber(real)} ± ${formatMathNumber(imag)}i`;
    }
    if (Math.abs(disc) <= 1e-9) {
      const root = -p.b / (2 * p.a);
      return `x = ${formatMathNumber(root)}`;
    }
    const sq = Math.sqrt(disc);
    const r1 = (-p.b - sq) / (2 * p.a);
    const r2 = (-p.b + sq) / (2 * p.a);
    const sorted = [r1, r2].sort((u, v) => u - v);
    return `x = ${formatMathNumber(sorted[0])}, ${formatMathNumber(sorted[1])}`;
  } catch {
    return null;
  }
}

export const MathSolver = {
  normalize: normalizeMath,

  solve(raw: string): string | null {
    const source = raw.trim().replace(/\?+$/, '').trim();
    if (!source || source.length > 180) return null;

    // 1. Unit conversions ("12 km to m", "72 f to c")
    const unitAns = tryUnitConversion(source);
    if (unitAns) return unitAns;

    // 2. Equations (both sides populated)
    const eqIdx = source.indexOf('=');
    if (eqIdx >= 0 && eqIdx < source.length - 1 && source.slice(eqIdx + 1).trim().length > 0) {
      if (source.indexOf('=', eqIdx + 1) >= 0) return null; // multiple '='
      const left = source.slice(0, eqIdx);
      const right = source.slice(eqIdx + 1);
      if (!left.trim() || !right.trim()) return null;

      // Try linear / quadratic first if 'x' is present
      if (/[xX]/.test(source)) {
        const eqAns = solvePolynomialEquation(left, right);
        if (eqAns) return eqAns;
      }

      // Check completed arithmetic equality: reject wrong equalities (e.g. 2+2=5)
      const lv = evaluateNumeric(left);
      const rv = evaluateNumeric(right);
      if (lv === null || rv === null) return null;
      const tol = 1e-8 * Math.max(1, Math.abs(lv), Math.abs(rv));
      return Math.abs(lv - rv) <= tol ? formatMathNumber(rv) : null;
    }

    // 3. Expression ending in '=' or bare expression
    const expr = normalizeMath(source).replace(/[=?]+$/, '');
    if (!expr || !/[0-9]/.test(expr)) return null;
    if (!/[+\-*/^%!()a-zA-Z]/.test(expr)) return null;
    const val = evaluateNumeric(expr);
    if (val === null) return null;
    try {
      return formatMathNumber(val);
    } catch {
      return null;
    }
  },

  bestCandidate(candidates: string[]): MathCandidateResult | null {
    let best: { result: MathCandidateResult; score: number } | null = null;
    candidates.forEach((raw, idx) => {
      const answer = this.solve(raw);
      if (!answer) return;
      const norm = normalizeMath(raw);
      const opens = (norm.match(/\(/g) || []).length;
      const closes = (norm.match(/\)/g) || []).length;
      const balanced = opens === closes;
      const hasEquals = raw.trimEnd().endsWith('=');
      const score = 100 + (balanced ? 4 : 0) + (hasEquals ? 3 : 0) + (candidates.length - idx) * 0.01;
      if (!best || score > best.score) {
        best = {
          score,
          result: {
            raw,
            answer,
            prettyExpr: this.pretty(raw),
          },
        };
      }
    });
    return best ? (best as { result: MathCandidateResult }).result : null;
  },

  pretty(raw: string): string {
    return normalizeMath(raw)
      .replace(/\*/g, ' × ')
      .replace(/\//g, ' ÷ ')
      .replace(/\+/g, ' + ')
      .replace(/(?<=[0-9)x])-/g, ' − ')
      .replace(/\^2/g, '²')
      .replace(/\^3/g, '³')
      .replace(/\s+/g, ' ')
      .trim();
  },
};
