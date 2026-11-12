/**
 * Advanced On-Device Maths Engine for Pro Notes.
 * Supports:
 * - Multi-line variable assignments: "a = 3; b = 4; sqrt(a^2 + b^2)"
 * - Any variable equation: linear, quadratic (real & complex roots), cubic, exponential (2^x = 16), numeric bisection/Newton fallback
 * - Symbolic polynomials up to degree 4 & derivatives: d/dx(3x^3 - 2x + 1) -> 9x^2 - 2
 * - Unicode superscripts (⁰¹²³⁴⁵⁶⁷⁸⁹⁻⁺), implicit multiplication (2(x+1), 2x(x+1), (x+1)(x-2))
 * - Scientific functions: sqrt, cbrt, abs, sin, cos, tan, sind, cosd, tand, asin, acos, atan, ln, log, log2, log10, exp, floor, ceil, round, deg, rad, nCr, nPr
 * - Exact fraction hints + unit conversions ("72 f to c", "60 mph to km/h", "1024 mb to gb")
 * - Step-by-step explanation breakdown via `solveMathDetailed`
 */

export interface MathCandidateResult {
  raw: string;
  answer: string;
  prettyExpr: string;
}

export interface MathDetailedResult {
  raw: string;
  answer: string;
  prettyExpr: string;
  category: 'arithmetic' | 'algebra' | 'calculus' | 'units' | 'variables';
  steps: string[];
  fractionHint?: string;
}

const SUPERSCRIPT_MAP: Record<string, string> = {
  '⁰': '0', '¹': '1', '²': '2', '³': '3', '⁴': '4',
  '⁵': '5', '⁶': '6', '⁷': '7', '⁸': '8', '⁹': '9',
  '⁺': '+', '⁻': '-', '⁽': '(', '⁾': ')',
};

const KNOWN_FUNCS = new Set([
  'sqrt', 'cbrt', 'abs', 'sin', 'cos', 'tan', 'sind', 'cosd', 'tand',
  'asin', 'acos', 'atan', 'ln', 'log2', 'log10', 'log', 'floor', 'ceil',
  'round', 'exp', 'deg', 'rad', 'ncr', 'npr',
]);

const KNOWN_CONSTS = new Set(['pi', 'e', 'tau', 'phi']);

const UNIT_FACTORS: Record<string, { group: string; toBase: number }> = {
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
  mg: { group: 'mass', toBase: 1e-6 },
  g: { group: 'mass', toBase: 0.001 },
  kg: { group: 'mass', toBase: 1 },
  oz: { group: 'mass', toBase: 0.0283495231 },
  lb: { group: 'mass', toBase: 0.45359237 },
  lbs: { group: 'mass', toBase: 0.45359237 },
  ms: { group: 'time', toBase: 0.001 },
  s: { group: 'time', toBase: 1 },
  sec: { group: 'time', toBase: 1 },
  min: { group: 'time', toBase: 60 },
  hr: { group: 'time', toBase: 3600 },
  h: { group: 'time', toBase: 3600 },
  day: { group: 'time', toBase: 86400 },
  days: { group: 'time', toBase: 86400 },
  'm/s': { group: 'speed', toBase: 1 },
  'km/h': { group: 'speed', toBase: 1 / 3.6 },
  kph: { group: 'speed', toBase: 1 / 3.6 },
  mph: { group: 'speed', toBase: 0.44704 },
  b: { group: 'data', toBase: 1 },
  kb: { group: 'data', toBase: 1024 },
  mb: { group: 'data', toBase: 1024 ** 2 },
  gb: { group: 'data', toBase: 1024 ** 3 },
  tb: { group: 'data', toBase: 1024 ** 4 },
  rad: { group: 'ang', toBase: 1 },
  deg: { group: 'ang', toBase: Math.PI / 180 },
};

function expandUnicodeSuperscripts(raw: string): string {
  return raw.replace(/[⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁽⁾]+/g, (seq) => {
    const mapped = Array.from(seq).map((ch) => SUPERSCRIPT_MAP[ch] ?? ch).join('');
    return mapped.length === 1 && /\d/.test(mapped) ? `^${mapped}` : `^(${mapped})`;
  });
}

export function normalizeMath(raw: string): string {
  let s = expandUnicodeSuperscripts(raw.trim())
    .replace(/√/g, 'sqrt')
    .replace(/∛/g, 'cbrt')
    .replace(/\*\*/g, '^')
    .replace(/[×·⋅]/g, '*')
    .replace(/[÷]/g, '/')
    .replace(/[−—–]/g, '-')
    .replace(/π/g, 'pi')
    .replace(/τ/g, 'tau')
    .replace(/φ/g, 'phi');

  // Natural percentage phrases
  s = s.replace(/(\d+(?:\.\d+)?)\s*%\s+of\s+([+-]?\d+(?:\.\d+)?)/gi, '($1/100)*($2)');
  s = s.replace(/(\d+(?:\.\d+)?)\s*%\s+off\s+([+-]?\d+(?:\.\d+)?)/gi, '($2)*(1-($1/100))');

  // Only convert standalone 'x' flanked by spaces & numbers/parens ("4 x 5") — preserve variable x in "2x(x+1)"!
  s = s.replace(/(?<=\d|\))\s+[xX]\s+(?=\d|\()/g, '*');
  // Also preserve comma inside nCr(n, r) -> ';' before collapsing
  s = s.replace(/(\w+\([^)]*?),([^)]*?\))/g, '$1;$2');
  s = s.replace(/(\d),(\d)/g, '$1.$2').replace(/\s+/g, '');

  // Safe OCR digit repair only inside numeric runs
  s = s
    .replace(/(?<=\d)[oO](?=\d)/g, '0')
    .replace(/(?<=\d)[lI](?=\d)/g, '1');

  s = s.replace(/sqrt(\d+(?:\.\d+)?)/g, 'sqrt($1)');
  s = s.replace(/cbrt(\d+(?:\.\d+)?)/g, 'cbrt($1)');
  return s;
}

export function formatMathNumber(val: number): string {
  if (!Number.isFinite(val)) throw new Error('Non-finite');
  if (Math.abs(val) < 1e-11) return '0';
  if (Math.abs(val - Math.round(val)) < 1e-9 && Math.abs(val) < 9e15) {
    return String(Math.round(val));
  }
  return Number(val.toPrecision(10)).toString();
}

function toExactFraction(val: number): string | undefined {
  if (!Number.isFinite(val) || Math.abs(val - Math.round(val)) < 1e-9 || Math.abs(val) > 1e5) return undefined;
  const sign = val < 0 ? '-' : '';
  const abs = Math.abs(val);
  for (let den = 2; den <= 64; den++) {
    const num = Math.round(abs * den);
    if (Math.abs(abs - num / den) < 1e-9) {
      return `${sign}${num}/${den}`;
    }
  }
  return undefined;
}

function tryUnitConversion(raw: string): string | null {
  const clean = raw.trim().replace(/[=?]+$/g, '').trim();
  const m = clean.match(/^([+-]?\d+(?:\.\d+)?)\s*°?\s*([a-zA-Z/]+)\s+(?:to|in|->)\s+°?\s*([a-zA-Z/]+)$/i);
  if (!m) return null;
  const value = Number(m[1]);
  if (!Number.isFinite(value)) return null;
  const from = m[2].toLowerCase();
  const to = m[3].toLowerCase();

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
  if (n < 0 || n > 170 || Math.abs(n - Math.round(n)) > 1e-10) throw new Error('Invalid factorial');
  let res = 1;
  for (let k = 2; k <= Math.round(n); k++) res *= k;
  return res;
}

function nPr(n: number, r: number): number {
  if (n < 0 || r < 0 || r > n || !Number.isInteger(n) || !Number.isInteger(r)) throw new Error('Invalid nPr');
  let res = 1;
  for (let i = 0; i < r; i++) res *= n - i;
  return res;
}

function nCr(n: number, r: number): number {
  if (n < 0 || r < 0 || r > n || !Number.isInteger(n) || !Number.isInteger(r)) throw new Error('Invalid nCr');
  const k = Math.min(r, n - r);
  let res = 1;
  for (let i = 1; i <= k; i++) res = (res * (n - k + i)) / i;
  return Math.round(res);
}

/**
 * Evaluator supporting arbitrary variable bindings (e.g. `{ x: 4, y: 2 }`) and implicit multiplication (`2x(x+1)`, `3xy`).
 */
class ExprEvaluator {
  private i = 0;
  constructor(private readonly s: string, private readonly vars: Record<string, number> = {}) {}

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
    return /[0-9.(a-zA-Z]/.test(this.s[at]);
  }

  private expression(): number {
    let val = this.term();
    while (this.peek() === '+' || this.peek() === '-') {
      const op = this.s[this.i++];
      const rhs = this.term();
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
      const rawName = this.s.slice(start, this.i);
      const lower = rawName.toLowerCase();

      // Exact variable match
      if (rawName in this.vars) return this.vars[rawName];
      if (lower in this.vars && !KNOWN_FUNCS.has(lower)) return this.vars[lower];

      if (lower === 'pi') return Math.PI;
      if (lower === 'e' && !(lower in this.vars)) return Math.E;
      if (lower === 'tau') return Math.PI * 2;
      if (lower === 'phi') return (1 + Math.sqrt(5)) / 2;
      if (KNOWN_FUNCS.has(lower) && this.peek() === '(') return this.callFunction(lower);

      // Handle chained single-letter variables/numbers like "xy" or "2x"
      if (rawName.length > 1) {
        let prod = 1;
        let idx = 0;
        while (idx < rawName.length) {
          const rest = rawName.slice(idx);
          const restLow = rest.toLowerCase();
          if (KNOWN_FUNCS.has(restLow) && this.peek() === '(') {
            // Leave remainder to callFunction
            return prod * this.callFunction(restLow);
          }
          const ch1 = rawName[idx];
          if (ch1 in this.vars) prod *= this.vars[ch1];
          else if (ch1.toLowerCase() in this.vars) prod *= this.vars[ch1.toLowerCase()];
          else if (ch1 === 'e') prod *= Math.E;
          else throw new Error(`Unknown symbol ${rawName}`);
          idx++;
        }
        return prod;
      }

      throw new Error(`Unknown symbol ${rawName}`);
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
    if (args.length === 0) throw new Error('Empty args');
    const x = args[0];
    const degToRad = (d: number) => (d * Math.PI) / 180;
    const radToDeg = (r: number) => (r * 180) / Math.PI;
    switch (name) {
      case 'sqrt': if (x < 0) throw new Error('Negative sqrt'); return Math.sqrt(x);
      case 'cbrt': return Math.cbrt(x);
      case 'abs': return Math.abs(x);
      case 'sin': return Math.sin(x);
      case 'cos': return Math.cos(x);
      case 'tan': return Math.tan(x);
      case 'sind': return Math.sin(degToRad(x));
      case 'cosd': return Math.cos(degToRad(x));
      case 'tand': return Math.tan(degToRad(x));
      case 'asin': return Math.asin(x);
      case 'acos': return Math.acos(x);
      case 'atan': return Math.atan(x);
      case 'ln': if (x <= 0) throw new Error('ln domain'); return Math.log(x);
      case 'log2': if (x <= 0) throw new Error('log2 domain'); return Math.log2(x);
      case 'log':
      case 'log10':
        if (x <= 0) throw new Error('log domain');
        if (args.length === 2) {
          if (args[1] <= 0 || args[1] === 1) throw new Error('log base');
          return Math.log(x) / Math.log(args[1]);
        }
        return Math.log10(x);
      case 'floor': return Math.floor(x);
      case 'ceil': return Math.ceil(x);
      case 'round': return Math.round(x);
      case 'exp': return Math.exp(x);
      case 'deg': return radToDeg(x);
      case 'rad': return degToRad(x);
      case 'ncr': if (args.length !== 2) throw new Error('nCr'); return nCr(x, args[1]);
      case 'npr': if (args.length !== 2) throw new Error('nPr'); return nPr(x, args[1]);
      default: throw new Error(`Unknown fn ${name}`);
    }
  }
}

export function evaluateNumeric(expression: string, vars: Record<string, number> = {}): number | null {
  try {
    const norm = normalizeMath(expression);
    if (!norm) return null;
    const val = new ExprEvaluator(norm, vars).parse();
    if (!Number.isFinite(val) || Math.abs(val) > 1e100) return null;
    return val;
  } catch {
    return null;
  }
}

/** Polynomial up to degree 4 for single variable `v` */
type Poly = number[]; // index = degree [c0, c1, c2, c3, c4]

function polyAdd(a: Poly, b: Poly): Poly {
  const len = Math.max(a.length, b.length);
  return Array.from({ length: len }, (_, i) => (a[i] || 0) + (b[i] || 0));
}

function polySub(a: Poly, b: Poly): Poly {
  const len = Math.max(a.length, b.length);
  return Array.from({ length: len }, (_, i) => (a[i] || 0) - (b[i] || 0));
}

function polyMul(a: Poly, b: Poly): Poly {
  const out = new Array(a.length + b.length - 1).fill(0);
  for (let i = 0; i < a.length; i++) {
    for (let j = 0; j < b.length; j++) out[i + j] += a[i] * b[j];
  }
  while (out.length > 1 && Math.abs(out[out.length - 1]) < 1e-12) out.pop();
  if (out.length > 6) throw new Error('Degree > 5');
  return out;
}

class PolyParser {
  private i = 0;
  constructor(private readonly s: string, private readonly varChar: string) {}

  parse(): Poly {
    const res = this.expr();
    if (this.i !== this.s.length) throw new Error('Trailing');
    return res;
  }

  private peek(): string {
    return this.i < this.s.length ? this.s[this.i] : '';
  }

  private startsPrimary(): boolean {
    if (this.i >= this.s.length) return false;
    const c = this.s[this.i];
    return /[0-9.(]/.test(c) || c === this.varChar;
  }

  private expr(): Poly {
    let v = this.term();
    while (this.peek() === '+' || this.peek() === '-') {
      const op = this.s[this.i++];
      const rhs = this.term();
      v = op === '+' ? polyAdd(v, rhs) : polySub(v, rhs);
    }
    return v;
  }

  private term(): Poly {
    let v = this.unary();
    while (this.i < this.s.length) {
      if (this.peek() === '*') {
        this.i++;
        v = polyMul(v, this.unary());
      } else if (this.peek() === '/') {
        this.i++;
        const d = this.unary();
        if (d.slice(1).some((c) => Math.abs(c) > 1e-12) || Math.abs(d[0] || 0) < 1e-12) throw new Error('Div');
        v = v.map((c) => c / d[0]);
      } else if (this.startsPrimary()) {
        v = polyMul(v, this.unary());
      } else {
        break;
      }
    }
    return v;
  }

  private unary(): Poly {
    if (this.peek() === '+') { this.i++; return this.unary(); }
    if (this.peek() === '-') { this.i++; return this.unary().map((c) => -c); }
    return this.power();
  }

  private power(): Poly {
    const base = this.primary();
    if (this.peek() === '^') {
      this.i++;
      const exp = this.unary();
      if (exp.slice(1).some((c) => Math.abs(c) > 1e-12)) throw new Error('Var exp');
      const n = exp[0] || 0;
      if (!Number.isInteger(n) || n < 0 || n > 5) {
        if (base.slice(1).every((c) => Math.abs(c) < 1e-12)) return [Math.pow(base[0] || 0, n)];
        throw new Error('Bad exp');
      }
      if (n === 0) return [1];
      let res: Poly = [1];
      for (let k = 0; k < n; k++) res = polyMul(res, base);
      return res;
    }
    return base;
  }

  private primary(): Poly {
    const ch = this.peek();
    if (ch === '(') {
      this.i++;
      const v = this.expr();
      if (this.peek() !== ')') throw new Error('Missing )');
      this.i++;
      return v;
    }
    if (ch === this.varChar) {
      this.i++;
      return [0, 1];
    }
    const start = this.i;
    while (this.i < this.s.length && /[0-9.]/.test(this.s[this.i])) this.i++;
    if (this.i === start) throw new Error('NaN');
    return [Number(this.s.slice(start, this.i))];
  }
}

function formatPoly(poly: Poly, varChar = 'x'): string {
  const parts: string[] = [];
  for (let deg = poly.length - 1; deg >= 0; deg--) {
    const c = poly[deg];
    if (Math.abs(c) < 1e-10) continue;
    const sign = c < 0 ? '-' : '+';
    const abs = Math.abs(c);
    const numStr = deg > 0 && Math.abs(abs - 1) < 1e-10 ? '' : formatMathNumber(abs);
    const varStr = deg === 0 ? '' : deg === 1 ? varChar : `${varChar}^${deg}`;
    if (parts.length === 0) parts.push((c < 0 ? '-' : '') + numStr + varStr);
    else parts.push(` ${sign} ${numStr}${varStr}`);
  }
  return parts.length > 0 ? parts.join('') : '0';
}

function solvePolyRoots(poly: Poly, varChar: string): { answer: string; steps: string[] } | null {
  const p = [...poly];
  while (p.length > 1 && Math.abs(p[p.length - 1]) < 1e-10) p.pop();
  const deg = p.length - 1;
  if (deg <= 0) return null;

  if (deg === 1) {
    const b = p[1], c = p[0];
    const root = -c / b;
    return {
      answer: `${varChar} = ${formatMathNumber(root)}`,
      steps: [
        `Rearrange linear equation: ${formatPoly(p, varChar)} = 0`,
        `${formatMathNumber(b)}${varChar} = ${formatMathNumber(-c)}`,
        `${varChar} = ${formatMathNumber(root)}`,
      ],
    };
  }

  if (deg === 2) {
    const a = p[2], b = p[1], c = p[0];
    const disc = b * b - 4 * a * c;
    const steps = [
      `Standard quadratic: ${formatPoly(p, varChar)} = 0`,
      `Discriminant Δ = (${formatMathNumber(b)})² − 4(${formatMathNumber(a)})(${formatMathNumber(c)}) = ${formatMathNumber(disc)}`,
    ];
    if (disc < -1e-9) {
      const real = -b / (2 * a);
      const imag = Math.sqrt(-disc) / Math.abs(2 * a);
      const ans = `${varChar} = ${formatMathNumber(real)} ± ${formatMathNumber(imag)}i`;
      return { answer: ans, steps: [...steps, `Complex roots: ${ans}`] };
    }
    if (Math.abs(disc) <= 1e-9) {
      const root = -b / (2 * a);
      const ans = `${varChar} = ${formatMathNumber(root)}`;
      return { answer: ans, steps: [...steps, `Repeated root: ${ans}`] };
    }
    const sq = Math.sqrt(disc);
    const roots = [(-b - sq) / (2 * a), (-b + sq) / (2 * a)].sort((u, v) => u - v);
    const ans = `${varChar} = ${formatMathNumber(roots[0])}, ${formatMathNumber(roots[1])}`;
    return { answer: ans, steps: [...steps, `Quadratic formula yields ${ans}`] };
  }

  // Numerical scan + bisection / Newton for degree >= 3
  const evalP = (x: number) => p.reduce((acc, c, idx) => acc + c * Math.pow(x, idx), 0);
  const roots: number[] = [];
  const lo = -100;
  const hi = 100;
  const step = 0.25;
  let prevX = lo;
  let prevY = evalP(lo);
  for (let x = lo + step; x <= hi; x += step) {
    const y = evalP(x);
    if (Math.abs(y) < 1e-10) roots.push(x);
    else if (prevY * y < 0) {
      let a = prevX, b = x, fa = prevY;
      for (let it = 0; it < 55; it++) {
        const m = (a + b) / 2;
        const fm = evalP(m);
        if (fa * fm <= 0) b = m;
        else { a = m; fa = fm; }
      }
      roots.push((a + b) / 2);
    }
    prevX = x;
    prevY = y;
  }
  const uniq: number[] = [];
  for (const r of roots.sort((a, b) => a - b)) {
    if (!uniq.some((u) => Math.abs(u - r) < 1e-5)) uniq.push(r);
  }
  if (uniq.length === 0) return null;
  const ans = `${varChar} = ${uniq.map(formatMathNumber).join(', ')}`;
  return {
    answer: ans,
    steps: [`Polynomial equation (${deg}°): ${formatPoly(p, varChar)} = 0`, `Real roots: ${ans}`],
  };
}

/** Finds non-function single-letter variables inside an expression */
function detectVariables(norm: string): string[] {
  const tokens = norm.match(/[a-zA-Z]+/g) || [];
  const vars = new Set<string>();
  for (const tok of tokens) {
    const low = tok.toLowerCase();
    if (KNOWN_FUNCS.has(low) || KNOWN_CONSTS.has(low)) continue;
    if (tok.length === 1) vars.add(tok);
    else {
      for (const ch of tok) {
        if (/[a-zA-Z]/.test(ch)) vars.add(ch);
      }
    }
  }
  return Array.from(vars);
}

/** Fallback numerical root solver for non-polynomial equations like 2^x = 16 or sin(x) = 0.5 */
function solveGeneralEquationNumeric(left: string, right: string, varChar: string): MathDetailedResult | null {
  const f = (v: number): number | null => {
    const lv = evaluateNumeric(left, { [varChar]: v });
    const rv = evaluateNumeric(right, { [varChar]: v });
    if (lv === null || rv === null) return null;
    return lv - rv;
  };

  // Search positive/negative domain grids
  const samples: number[] = [];
  for (let x = -40; x <= 40; x += 0.25) samples.push(x);
  const found: number[] = [];
  let prevX = samples[0];
  let prevY = f(prevX);
  for (let k = 1; k < samples.length; k++) {
    const x = samples[k];
    const y = f(x);
    if (y !== null && Math.abs(y) < 1e-10) {
      found.push(x);
    } else if (prevY !== null && y !== null && prevY * y < 0) {
      let a = prevX, b = x, fa = prevY;
      for (let it = 0; it < 55; it++) {
        const m = (a + b) / 2;
        const fm = f(m);
        if (fm === null) break;
        if (fa * fm <= 0) b = m;
        else { a = m; fa = fm; }
      }
      const root = (a + b) / 2;
      if (Math.abs(f(root) ?? 1) < 1e-4) found.push(root);
    }
    prevX = x;
    prevY = y;
  }

  if (found.length === 0) return null;
  const uniq: number[] = [];
  for (const r of found.sort((a, b) => a - b)) {
    if (!uniq.some((u) => Math.abs(u - r) < 1e-4)) uniq.push(r);
  }
  const shown = uniq.slice(0, 4).map(formatMathNumber).join(', ');
  const ans = `${varChar} = ${shown}`;
  return {
    raw: `${left} = ${right}`,
    answer: ans,
    prettyExpr: `${left} = ${right}`,
    category: 'algebra',
    steps: [
      `Solve ${left} = ${right} for ${varChar}`,
      `Solved numerically over domain`,
      ans,
    ],
  };
}

function tryDerivative(raw: string): MathDetailedResult | null {
  // e.g. d/dx(3x^2 + 2x - 5) or d/dt(t^3)
  const m = raw.trim().match(/^d\/d([a-zA-Z])\s*\((.+)\)(?:\s*at\s*\1\s*=\s*([+-]?\d+(?:\.\d+)?))?$/i);
  if (!m) return null;
  const v = m[1];
  const inner = normalizeMath(m[2]);
  const atVal = m[3] ? Number(m[3]) : null;
  try {
    const poly = new PolyParser(inner, v).parse();
    const deriv = poly.slice(1).map((c, idx) => c * (idx + 1));
    const derivStr = formatPoly(deriv, v);
    if (atVal !== null) {
      const val = deriv.reduce((acc, c, idx) => acc + c * Math.pow(atVal, idx), 0);
      return {
        raw,
        answer: formatMathNumber(val),
        prettyExpr: `d/d${v}(${inner}) at ${v}=${atVal}`,
        category: 'calculus',
        steps: [
          `Apply power rule: d/d${v}(${inner}) = ${derivStr}`,
          `Substitute ${v} = ${atVal}: ${formatMathNumber(val)}`,
        ],
      };
    }
    return {
      raw,
      answer: derivStr,
      prettyExpr: `d/d${v}(${inner})`,
      category: 'calculus',
      steps: [
        `Differentiate term-by-term with respect to ${v}`,
        `f'(${v}) = ${derivStr}`,
      ],
    };
  } catch {
    if (atVal !== null) {
      const h = 1e-6;
      const fp = evaluateNumeric(inner, { [v]: atVal + h });
      const fm = evaluateNumeric(inner, { [v]: atVal - h });
      if (fp !== null && fm !== null) {
        const res = formatMathNumber((fp - fm) / (2 * h));
        return {
          raw,
          answer: res,
          prettyExpr: `d/d${v}(${inner}) at ${v}=${atVal}`,
          category: 'calculus',
          steps: [`Central difference derivative at ${v} = ${atVal}`, res],
        };
      }
    }
    return null;
  }
}

function tryMultiLineVariables(raw: string): MathDetailedResult | null {
  const parts = raw
    .split(/[\n;]+/)
    .map((s) => s.trim())
    .filter(Boolean);
  if (parts.length < 2) return null;

  const vars: Record<string, number> = {};
  const steps: string[] = [];
  for (let idx = 0; idx < parts.length; idx++) {
    const line = parts[idx].replace(/[=?]+$/, '').trim();
    const assignMatch = line.match(/^([a-zA-Z][a-zA-Z0-9]*)\s*=\s*(.+)$/);
    if (assignMatch && idx < parts.length - 1) {
      const name = assignMatch[1];
      const val = evaluateNumeric(assignMatch[2], vars);
      if (val === null) return null;
      vars[name] = val;
      steps.push(`${name} = ${formatMathNumber(val)}`);
    } else if (idx === parts.length - 1) {
      const val = evaluateNumeric(line, vars);
      if (val === null) return null;
      const ans = formatMathNumber(val);
      steps.push(`Evaluate ${line} → ${ans}`);
      return {
        raw,
        answer: ans,
        prettyExpr: line,
        category: 'variables',
        steps,
        fractionHint: toExactFraction(val),
      };
    } else {
      return null;
    }
  }
  return null;
}

export function solveMathDetailed(raw: string): MathDetailedResult | null {
  const source = raw.trim().replace(/\?+$/, '').trim();
  if (!source || source.length > 260) return null;

  // 0. Symbolic/numerical derivative d/dx(...)
  const derivAns = tryDerivative(source);
  if (derivAns) return derivAns;

  // 1. Multi-line / chained variable definitions: "x = 3; y = 2x + 1; x^2 + y"
  const multiAns = tryMultiLineVariables(source);
  if (multiAns) return multiAns;

  // 2. Unit conversions
  const unitAns = tryUnitConversion(source);
  if (unitAns) {
    return {
      raw: source,
      answer: unitAns,
      prettyExpr: source,
      category: 'units',
      steps: [`Unit conversion: ${source}`, `Result: ${unitAns}`],
    };
  }

  // 3. Equations (L = R)
  const eqIdx = source.indexOf('=');
  if (eqIdx >= 0 && eqIdx < source.length - 1 && source.slice(eqIdx + 1).trim().length > 0) {
    if (source.indexOf('=', eqIdx + 1) >= 0) return null;
    const leftRaw = source.slice(0, eqIdx).trim();
    const rightRaw = source.slice(eqIdx + 1).trim();
    if (!leftRaw || !rightRaw) return null;

    const normL = normalizeMath(leftRaw);
    const normR = normalizeMath(rightRaw);
    const vars = Array.from(new Set([...detectVariables(normL), ...detectVariables(normR)]));

    if (vars.length === 1) {
      const v = vars[0];
      try {
        const pL = new PolyParser(normL, v).parse();
        const pR = new PolyParser(normR, v).parse();
        const polyAns = solvePolyRoots(polySub(pL, pR), v);
        if (polyAns) {
          return {
            raw: source,
            answer: polyAns.answer,
            prettyExpr: `${leftRaw} = ${rightRaw}`,
            category: 'algebra',
            steps: polyAns.steps,
          };
        }
      } catch {
        // Fallback to numerical solver (exponentials like 2^x = 16, trig equations, etc.)
        const numEq = solveGeneralEquationNumeric(normL, normR, v);
        if (numEq) return numEq;
      }
    }

    // Completed numeric equality check (rejects false equalities like 2+2=5)
    const lv = evaluateNumeric(leftRaw);
    const rv = evaluateNumeric(rightRaw);
    if (lv === null || rv === null) return null;
    const tol = 1e-8 * Math.max(1, Math.abs(lv), Math.abs(rv));
    if (Math.abs(lv - rv) <= tol) {
      const ans = formatMathNumber(rv);
      return {
        raw: source,
        answer: ans,
        prettyExpr: `${leftRaw} = ${rightRaw}`,
        category: 'arithmetic',
        steps: [`Both sides equal ${ans}`],
      };
    }
    return null;
  }

  // 4. Bare expression or ending in '='
  const expr = normalizeMath(source).replace(/[=?]+$/, '');
  if (!expr) return null;
  const val = evaluateNumeric(expr);
  if (val === null) return null;
  try {
    const ans = formatMathNumber(val);
    return {
      raw: source,
      answer: ans,
      prettyExpr: MathSolver.pretty(source),
      category: 'arithmetic',
      steps: [`Normalized: ${expr}`, `Result = ${ans}`],
      fractionHint: toExactFraction(val),
    };
  } catch {
    return null;
  }
}

export const MathSolver = {
  normalize: normalizeMath,
  solveDetailed: solveMathDetailed,

  solve(raw: string): string | null {
    return solveMathDetailed(raw)?.answer ?? null;
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
          result: { raw, answer, prettyExpr: this.pretty(raw) },
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
      .replace(/(?<=[0-9)a-zA-Z])-/g, ' − ')
      .replace(/\^2/g, '²')
      .replace(/\^3/g, '³')
      .replace(/\s+/g, ' ')
      .trim();
  },
};
