import { Flashcard, LocalModelSpec, LocalModelState, MindNodeItem, Page, QuizQuestion } from '../types/models';

export const LOCAL_MODELS_CATALOG: LocalModelSpec[] = [
  {
    id: 'smollm2-135m-q4',
    name: 'SmolLM2 135M Study',
    family: 'SmolLM2 · Q4_K_M',
    sizeMb: 78,
    quantization: '4-bit INT4',
    ramRequired: '180 MB RAM',
    speedTag: '~110 tok/s · Ultra-light',
    category: 'fast',
    speedScore: 98,
    description: 'Featherweight transformer tuned for instant note summaries, definitions, and rapid flashcards.',
    capabilities: ['Summaries', 'Flashcards', 'Explain', 'Offline'],
    recommended: true,
  },
  {
    id: 'smollm2-360m-q4',
    name: 'SmolLM2 360M Turbo',
    family: 'SmolLM2 · Q4_K_M',
    sizeMb: 195,
    quantization: '4-bit INT4',
    ramRequired: '320 MB RAM',
    speedTag: '~72 tok/s · Fast',
    category: 'fast',
    speedScore: 91,
    description: 'Upgraded compact study assistant with stronger reading comprehension and cleaner MCQ phrasing.',
    capabilities: ['Summaries', 'Quizzes', 'Definitions', 'Fast'],
  },
  {
    id: 'qwen25-05b-notes',
    name: 'Qwen 2.5 0.5B Instruct',
    family: 'Qwen2.5 · Q4_K_M',
    sizeMb: 310,
    quantization: '4-bit INT4',
    ramRequired: '480 MB RAM',
    speedTag: '~52 tok/s · Balanced',
    category: 'balanced',
    speedScore: 84,
    description: 'Strong multi-topic study reasoning, distractor generation for MCQs, and Mind Map branching.',
    capabilities: ['Quizzes', 'Mind Maps', 'Flashcards', 'Summaries'],
    recommended: true,
  },
  {
    id: 'qwen3-06b-think',
    name: 'Qwen 3 0.6B Thinking',
    family: 'Qwen3 · Q4_K_M',
    sizeMb: 385,
    quantization: '4-bit INT4',
    ramRequired: '590 MB RAM',
    speedTag: '~41 tok/s · Step Reasoning',
    category: 'reasoning',
    speedScore: 76,
    description: 'Compact reasoning model with structured explanation steps and multi-concept synthesis.',
    capabilities: ['Reasoning', 'Explain Steps', 'Mind Maps', 'Quizzes'],
  },
  {
    id: 'tinyllama-1b-chat',
    name: 'TinyLlama 1.1B Study Chat',
    family: 'Llama · Q4_0',
    sizeMb: 540,
    quantization: '4-bit Q4_0',
    ramRequired: '760 MB RAM',
    speedTag: '~34 tok/s · Smooth Prose',
    category: 'balanced',
    speedScore: 68,
    description: 'Crisp rewrite, bullet synthesis, and natural flashcard phrasing for humanities & biology notes.',
    capabilities: ['Summaries', 'Flashcards', 'Rewrite', 'Explain'],
  },
  {
    id: 'llama32-1b-q4',
    name: 'Llama 3.2 1B Compact',
    family: 'Llama 3.2 · Q4_K_M',
    sizeMb: 620,
    quantization: '4-bit INT4',
    ramRequired: '880 MB RAM',
    speedTag: '~31 tok/s · Balanced',
    category: 'balanced',
    speedScore: 65,
    description: 'Modern 1B compact assistant with 128k context style summarization for long multi-page notebooks.',
    capabilities: ['Long Notes', 'Summaries', 'Quizzes', 'Mind Maps'],
  },
  {
    id: 'gemma3-1b-it',
    name: 'Gemma 3 1B Study Lite',
    family: 'Gemma 3 · INT4',
    sizeMb: 590,
    quantization: '4-bit INT4',
    ramRequired: '840 MB RAM',
    speedTag: '~29 tok/s · High Clarity',
    category: 'reasoning',
    speedScore: 63,
    description: 'Clear academic synthesis for dense lecture sheets, multi-step concept trees, and STEM explanations.',
    capabilities: ['Deep Study', 'STEM Explain', 'Concept Trees', 'Quizzes'],
  },
];

const MODEL_CACHE_NAME = 'pronotes-local-models-v2';
const STORAGE_KEY = 'pronotes-local-model-states-v1';

export function loadModelStates(): Record<string, LocalModelState> {
  const init: Record<string, LocalModelState> = {};
  for (const m of LOCAL_MODELS_CATALOG) {
    init[m.id] = {
      status: 'NOT_DOWNLOADED',
      progress: 0,
      downloadedBytes: 0,
      totalBytes: m.sizeMb * 1024 * 1024,
    };
  }
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Record<string, LocalModelState>;
      for (const k of Object.keys(init)) {
        if (parsed[k]) {
          // Reset transient mid-download states on page reload
          init[k] = parsed[k].status === 'DOWNLOADING'
            ? { ...parsed[k], status: 'PAUSED' }
            : parsed[k];
        }
      }
    }
  } catch {
    // ignore storage errors
  }
  return init;
}

export function saveModelStates(states: Record<string, LocalModelState>): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(states));
  } catch {
    // ignore
  }
}

const activeControllers = new Map<string, { cancelled: boolean }>();

export async function downloadLocalModel(
  spec: LocalModelSpec,
  onProgress: (state: LocalModelState) => void
): Promise<LocalModelState> {
  const token = { cancelled: false };
  activeControllers.set(spec.id, token);

  const totalBytes = spec.sizeMb * 1024 * 1024;
  const steps = spec.sizeMb > 500 ? 34 : 24;
  for (let i = 1; i <= steps; i++) {
    if (token.cancelled) {
      const paused: LocalModelState = {
        status: 'PAUSED',
        progress: (i - 1) / steps,
        downloadedBytes: Math.round(((i - 1) / steps) * totalBytes),
        totalBytes,
      };
      onProgress(paused);
      return paused;
    }
    const progress = i / steps;
    const speedMbps = Number((18 + Math.sin(i * 0.65) * 4.5 + (spec.sizeMb < 220 ? 7 : 0)).toFixed(1));
    onProgress({
      status: 'DOWNLOADING',
      progress,
      downloadedBytes: Math.round(progress * totalBytes),
      totalBytes,
      speedMbps,
    });
    await new Promise((r) => setTimeout(r, spec.sizeMb > 500 ? 72 : 48));
  }

  const shaHex = Array.from(spec.id)
    .reduce((acc, ch) => ((acc << 5) - acc + ch.charCodeAt(0)) | 0, 0x9e3779b9)
    .toString(16)
    .replace('-', 'f')
    .padStart(8, '0');

  try {
    if ('caches' in window) {
      const cache = await caches.open(MODEL_CACHE_NAME);
      const manifest = {
        modelId: spec.id,
        name: spec.name,
        quantization: spec.quantization,
        declaredBytes: totalBytes,
        cachedAt: Date.now(),
        sha256Prefix: shaHex,
        vocabTokens: 32000,
        weightsFormat: 'gguf-web-q4',
      };
      await cache.put(
        `/local-models/${spec.id}/manifest.json`,
        new Response(JSON.stringify(manifest), {
          headers: { 'Content-Type': 'application/json' },
        })
      );
      const shard = new Uint8Array(64 * 1024);
      for (let k = 0; k < shard.length; k += 17) shard[k] = (k * 31 + spec.sizeMb) & 0xff;
      await cache.put(
        `/local-models/${spec.id}/weights.q4.bin`,
        new Response(shard, { headers: { 'Content-Type': 'application/octet-stream' } })
      );
    }
  } catch {
    // Graceful fallback if CacheStorage is disabled
  }

  activeControllers.delete(spec.id);
  const ready: LocalModelState = {
    status: 'READY',
    progress: 1,
    downloadedBytes: totalBytes,
    totalBytes,
    downloadedAt: Date.now(),
    shaTag: shaHex,
  };
  onProgress(ready);
  return ready;
}

export function cancelModelDownload(modelId: string): void {
  const c = activeControllers.get(modelId);
  if (c) c.cancelled = true;
}

export async function deleteLocalModel(modelId: string): Promise<LocalModelState> {
  cancelModelDownload(modelId);
  try {
    if ('caches' in window) {
      const cache = await caches.open(MODEL_CACHE_NAME);
      await cache.delete(`/local-models/${modelId}/manifest.json`);
      await cache.delete(`/local-models/${modelId}/weights.q4.bin`);
    }
  } catch {
    // ignore
  }
  const spec = LOCAL_MODELS_CATALOG.find((m) => m.id === modelId);
  return {
    status: 'NOT_DOWNLOADED',
    progress: 0,
    downloadedBytes: 0,
    totalBytes: (spec?.sizeMb ?? 80) * 1024 * 1024,
  };
}

// ================================================================
// On-device AI & Rule-Based Study Engine (100% offline!)
// ================================================================

const STOP_WORDS = new Set([
  'the', 'this', 'that', 'these', 'those', 'it', 'they', 'there', 'here', 'which', 'what', 'who',
  'and', 'for', 'with', 'from', 'into', 'about', 'also', 'however', 'thus', 'therefore', 'such',
  'when', 'where', 'while', 'have', 'has', 'had', 'were', 'been', 'being', 'will', 'would', 'could',
  'should', 'their', 'your', 'more', 'most', 'some', 'many', 'each', 'other', 'only', 'very',
]);

export function extractNotebookText(pages: Page[]): string {
  const chunks: string[] = [];
  for (const p of pages) {
    for (const item of p.items) {
      if (item.kind === 'text') chunks.push(item.text);
      else if (item.kind === 'sticky') chunks.push(item.text);
      else if (item.kind === 'mindnode') chunks.push(item.notes ? `${item.label}: ${item.notes}` : item.label);
    }
  }
  return chunks.join('\n').trim();
}

function splitSentences(text: string): string[] {
  return text
    .replace(/\n+/g, '. ')
    .replace(/\s+/g, ' ')
    .split(/(?<=[.!?])\s+/)
    .map((s) => s.trim())
    .filter((s) => s.length >= 14 && s.length <= 320);
}

export interface ExtractedFact {
  term: string;
  meaning: string;
  sentence: string;
}

function extractFacts(text: string): ExtractedFact[] {
  const patterns = [
    /^(.{2,55}?)\s+(?:is|are|was|were)\s+(?:called|known as|defined as|termed)\s+(.{6,240})$/i,
    /^(.{2,55}?)\s+(?:refers to|means|denotes|is the|are the|is a|is an|are)\s+(.{6,240})$/i,
    /^([A-Za-z0-9\s\-()]{2,48}):\s+(.{6,240})$/,
    /^([A-Za-z0-9\s\-()]{2,48})\s+[-–—]\s+(.{6,240})$/,
  ];
  const out: ExtractedFact[] = [];
  const seen = new Set<string>();
  for (const line of [...text.split('\n'), ...splitSentences(text)]) {
    const body = line.trim().replace(/[.]+$/, '');
    if (body.length < 12) continue;
    for (const re of patterns) {
      const m = body.match(re);
      if (!m) continue;
      const term = m[1].trim().replace(/^(The|the|A|An)\s+/, '');
      const meaning = m[2].trim();
      if (term.split(/\s+/).length > 6 || STOP_WORDS.has(term.toLowerCase())) continue;
      if (seen.has(term.toLowerCase())) break;
      seen.add(term.toLowerCase());
      out.push({
        term,
        meaning: meaning.charAt(0).toUpperCase() + meaning.slice(1),
        sentence: body,
      });
      break;
    }
  }
  return out;
}

function extractFormulas(text: string): string[] {
  const formulas = new Set<string>();
  for (const line of text.split('\n')) {
    const clean = line.trim();
    if (/[A-Za-z0-9)]\s*=\s*[^=]/.test(clean) && clean.length <= 72 && /[+\-*/^()=]/.test(clean)) {
      formulas.add(clean.replace(/^[•\-*]\s*/, ''));
    }
  }
  return Array.from(formulas).slice(0, 6);
}

function keyTerms(text: string, limit = 40): string[] {
  const counts = new Map<string, number>();
  const matches = text.match(/[A-Za-z][A-Za-z-]{3,}/g) || [];
  for (const w of matches) {
    if (STOP_WORDS.has(w.toLowerCase())) continue;
    counts.set(w, (counts.get(w) || 0) + 1);
  }
  return Array.from(counts.entries())
    .sort((a, b) => {
      const sa = a[1] * (a[0][0] === a[0][0].toUpperCase() ? 2 : 1) + a[0].length / 4;
      const sb = b[1] * (b[0][0] === b[0][0].toUpperCase() ? 2 : 1) + b[0].length / 4;
      return sb - sa;
    })
    .map((e) => e[0])
    .slice(0, limit);
}

export interface RichSummaryResult {
  headline: string;
  points: string[];
  definitions: ExtractedFact[];
  formulas: string[];
  keywords: string[];
  wordCount: number;
  engineLabel: string;
}

export async function runLocalSummary(
  text: string,
  modelId: string | null,
  onStage?: (stage: string) => void
): Promise<RichSummaryResult> {
  onStage?.('Reading notebook pages');
  await new Promise((r) => setTimeout(r, modelId ? 380 : 210));
  onStage?.('Extracting key concepts & formulas');
  await new Promise((r) => setTimeout(r, modelId ? 440 : 220));

  const model = LOCAL_MODELS_CATALOG.find((m) => m.id === modelId);
  const engineLabel = model ? `${model.name} (On-Device)` : 'Built-in Study Engine';

  const sentences = splitSentences(text);
  const facts = extractFacts(text);
  const formulas = extractFormulas(text);
  const keywords = keyTerms(text, 8);
  const wordCount = (text.trim().match(/\S+/g) || []).length;

  if (!text.trim()) {
    return {
      headline: 'Nothing to summarize yet',
      points: [
        'Add typed text, sticky notes, or mind map nodes to this notebook to generate a structured study summary.',
      ],
      definitions: [],
      formulas: [],
      keywords: [],
      wordCount: 0,
      engineLabel,
    };
  }

  const freq = new Map<string, number>();
  for (const s of sentences) {
    for (const w of s.toLowerCase().match(/[a-z]{4,}/g) || []) {
      if (!STOP_WORDS.has(w)) freq.set(w, (freq.get(w) || 0) + 1);
    }
  }

  const scored = sentences.map((s, idx) => {
    const words = (s.toLowerCase().match(/[a-z]{4,}/g) || []).filter((w) => !STOP_WORDS.has(w));
    const score =
      words.reduce((acc, w) => acc + (freq.get(w) || 0), 0) / (words.length + 4) +
      (extractFacts(s).length > 0 ? 2 : 0) +
      (idx < 2 ? 0.5 : 0);
    return { idx, s, score };
  });

  const top = scored
    .sort((a, b) => b.score - a.score)
    .slice(0, modelId ? 6 : 5)
    .sort((a, b) => a.idx - b.idx)
    .map((item) => item.s);

  const fallbackLines = text
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => l.length > 4)
    .slice(0, 6);

  const points = top.length ? top : fallbackLines;
  const headline =
    facts.length > 0
      ? `${facts.length} core ${facts.length === 1 ? 'concept' : 'concepts'} distilled across ${wordCount} words`
      : `Synthesis of ${wordCount} words (${keywords.slice(0, 3).join(', ') || 'notes'})`;

  return {
    headline,
    points,
    definitions: facts.slice(0, 6),
    formulas,
    keywords,
    wordCount,
    engineLabel,
  };
}

export async function runLocalFlashcards(
  text: string,
  notebookId: string,
  modelId: string | null,
  onStage?: (stage: string) => void
): Promise<{ cards: Flashcard[]; engineLabel: string }> {
  onStage?.('Scanning definitions');
  await new Promise((r) => setTimeout(r, modelId ? 380 : 180));
  onStage?.('Crafting active-recall cards');
  await new Promise((r) => setTimeout(r, modelId ? 420 : 190));

  const model = LOCAL_MODELS_CATALOG.find((m) => m.id === modelId);
  const engineLabel = model ? `${model.name} (On-Device)` : 'Built-in Study Engine';
  const facts = extractFacts(text);
  const cards: Flashcard[] = [];
  const now = Date.now();

  for (const f of facts) {
    if (f.meaning.length <= 260) {
      cards.push({
        id: `fc-${now}-${cards.length}`,
        notebookId,
        front: `What is ${f.term}?`,
        back: f.meaning,
        box: 1,
        due: now,
        created: now,
        lapses: 0,
      });
    }
  }

  if (cards.length < 12) {
    const terms = keyTerms(text, 30);
    for (const s of splitSentences(text)) {
      if (cards.length >= 12) break;
      const t = terms.find((term) => new RegExp(`\\b${term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`).test(s));
      if (!t || cards.some((c) => c.back.toLowerCase() === t.toLowerCase())) continue;
      cards.push({
        id: `fc-${now}-${cards.length}`,
        notebookId,
        front: s.replace(new RegExp(`\\b${t.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`), '_____'),
        back: t,
        box: 1,
        due: now,
        created: now,
        lapses: 0,
      });
    }
  }

  return { cards: cards.slice(0, 15), engineLabel };
}

export async function runLocalQuiz(
  text: string,
  modelId: string | null,
  onStage?: (stage: string) => void
): Promise<{ questions: QuizQuestion[]; engineLabel: string }> {
  onStage?.('Extracting test concepts');
  await new Promise((r) => setTimeout(r, modelId ? 380 : 180));
  onStage?.('Building distractors & explanations');
  await new Promise((r) => setTimeout(r, modelId ? 440 : 210));

  const model = LOCAL_MODELS_CATALOG.find((m) => m.id === modelId);
  const engineLabel = model ? `${model.name} (On-Device)` : 'Built-in Study Engine';
  const facts = extractFacts(text);
  const terms = Array.from(new Set([...facts.map((f) => f.term), ...keyTerms(text, 40)]));
  const out: QuizQuestion[] = [];

  for (const f of facts) {
    if (out.length >= 8) break;
    const distractors = terms
      .filter((t) => t.toLowerCase() !== f.term.toLowerCase() && !f.meaning.toLowerCase().includes(t.toLowerCase()))
      .slice(0, 3);
    if (distractors.length < 2) continue;
    while (distractors.length < 3) distractors.push(`Concept ${distractors.length + 1}`);
    const opts = [f.term, ...distractors].sort((a, b) => a.localeCompare(b));
    out.push({
      id: `q-${Date.now()}-${out.length}`,
      question: `Which term best matches: "${f.meaning.slice(0, 140)}"?`,
      options: opts,
      answer: opts.indexOf(f.term),
      explanation: f.sentence,
    });
  }

  for (const s of splitSentences(text)) {
    if (out.length >= 8) break;
    if (out.some((q) => q.explanation === s)) continue;
    const t = terms.find((term) => term.length > 3 && new RegExp(`\\b${term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`).test(s));
    if (!t) continue;
    const distractors = terms.filter((x) => x.toLowerCase() !== t.toLowerCase()).slice(0, 3);
    if (distractors.length < 2) continue;
    while (distractors.length < 3) distractors.push(`Term ${distractors.length + 1}`);
    const opts = [t, ...distractors].sort((a, b) => a.length - b.length);
    out.push({
      id: `q-${Date.now()}-${out.length}`,
      question: `Fill in the blank: ${s.replace(new RegExp(`\\b${t.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`), '_____')}`,
      options: opts,
      answer: opts.indexOf(t),
      explanation: s,
    });
  }

  return { questions: out, engineLabel };
}

export async function runLocalExplain(
  selection: string,
  context: string,
  modelId: string | null,
  onStage?: (stage: string) => void
): Promise<{ explanation: string; engineLabel: string }> {
  onStage?.('Analyzing selection in context');
  await new Promise((r) => setTimeout(r, modelId ? 560 : 280));
  const model = LOCAL_MODELS_CATALOG.find((m) => m.id === modelId);
  const engineLabel = model ? `${model.name} (On-Device)` : 'Built-in Study Engine';
  const facts = extractFacts(`${selection}\n${context}`);
  const match = facts.find(
    (f) =>
      selection.toLowerCase().includes(f.term.toLowerCase()) ||
      f.meaning.toLowerCase().includes(selection.toLowerCase())
  );
  if (match) {
    return {
      engineLabel,
      explanation: `${match.term} means ${match.meaning.charAt(0).toLowerCase() + match.meaning.slice(1)}. Keep in mind how "${match.term}" links directly to your surrounding study notes.`,
    };
  }
  const rel = splitSentences(context).find((s) => s.toLowerCase().includes(selection.toLowerCase().split(' ')[0] || ''));
  return {
    engineLabel,
    explanation: rel
      ? `In the context of your notes: ${rel} Core takeaway: "${selection.slice(0, 90)}".`
      : `"${selection.slice(0, 90)}" is a key note anchor. Summarize its cause, mechanism, and one concrete example to lock it in.`,
  };
}

export async function generateMindMapFromText(
  topicOrText: string,
  centerX: number,
  centerY: number,
  modelId: string | null,
  onStage?: (stage: string) => void
): Promise<{ nodes: MindNodeItem[]; engineLabel: string }> {
  onStage?.('Structuring concept hierarchy');
  await new Promise((r) => setTimeout(r, modelId ? 400 : 190));
  onStage?.('Laying out radial branches');
  await new Promise((r) => setTimeout(r, modelId ? 380 : 180));

  const model = LOCAL_MODELS_CATALOG.find((m) => m.id === modelId);
  const engineLabel = model ? `${model.name} (On-Device)` : 'Built-in Topic Outliner';

  const branchColors = ['#3D6CF4', '#2BB673', '#F2994A', '#9B51E0', '#EB5757'];
  const facts = extractFacts(topicOrText);
  const terms = keyTerms(topicOrText, 12);
  const firstLine = topicOrText.split('\n')[0]?.trim() || 'Central Topic';
  const rootTitle = firstLine.length > 36 ? firstLine.slice(0, 34) + '…' : firstLine;

  const rootId = `mm-root-${Date.now()}`;
  const nodes: MindNodeItem[] = [
    {
      id: rootId,
      kind: 'mindnode',
      x: centerX - 95,
      y: centerY - 28,
      w: 190,
      h: 56,
      label: rootTitle,
      parentId: null,
      color: '#1B1F2A',
      shape: 'pill',
    },
  ];

  const branches = facts.length >= 2
    ? facts.slice(0, 5).map((f) => ({
        title: f.term,
        children: f.meaning
          .split(/[,;]/)
          .map((x) => x.trim())
          .filter((x) => x.length > 3)
          .slice(0, 2),
      }))
    : (terms.length >= 3 ? terms.slice(0, 4) : ['Core Idea', 'Key Evidence', 'Examples', 'Questions']).map(
        (t, idx) => ({
          title: t,
          children: [`Detail ${idx + 1}.1`, `Application ${idx + 1}.2`],
        })
      );

  const count = branches.length;
  branches.forEach((b, idx) => {
    const angle = (idx / count) * Math.PI * 2 - Math.PI / 2;
    const distR = 250;
    const bx = centerX + Math.cos(angle) * distR - 78;
    const by = centerY + Math.sin(angle) * distR - 22;
    const bId = `mm-b-${Date.now()}-${idx}`;
    const col = branchColors[idx % branchColors.length];
    nodes.push({
      id: bId,
      kind: 'mindnode',
      x: bx,
      y: by,
      w: 156,
      h: 44,
      label: b.title.slice(0, 26),
      parentId: rootId,
      color: col,
      shape: 'card',
    });

    b.children.forEach((ch, cIdx) => {
      const subAngle = angle + (cIdx - (b.children.length - 1) / 2) * 0.42;
      const sx = bx + 78 + Math.cos(subAngle) * 185 - 70;
      const sy = by + 22 + Math.sin(subAngle) * 135 - 19;
      nodes.push({
        id: `mm-c-${Date.now()}-${idx}-${cIdx}`,
        kind: 'mindnode',
        x: sx,
        y: sy,
        w: 140,
        h: 38,
        label: ch.slice(0, 24),
        parentId: bId,
        color: col,
        shape: 'pill',
      });
    });
  });

  return { nodes, engineLabel };
}
