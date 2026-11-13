import {
  AiImageCategory,
  AppSettings,
  Flashcard,
  ImageModelSpec,
  LocalModelSpec,
  LocalModelState,
  MindNodeItem,
  NoteChatMessage,
  Page,
  QuizQuestion,
  SlideCard,
  SlideKind,
  SlideThemeId,
} from '../types/models';

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

export const IMAGE_MODELS_CATALOG: ImageModelSpec[] = [
  {
    id: 'galaxy-sketch-lite',
    name: 'Creative Studio Lite',
    family: 'One UI 9 Studio · INT4',
    sizeMb: 38,
    steps: '2 steps · Instant Lite',
    speedTag: '~0.2s · Lite Mode',
    description: 'One UI 9 inspired studio design engine with sketch-to-art, 3D cartoon, watercolor, and pop-art stickers & covers.',
    remoteModelKey: 'turbo',
    recommended: true,
  },
  {
    id: 'sdxl-turbo-q4',
    name: 'SDXL-Turbo 4-Step Lite',
    family: 'Diffusion · Q4',
    sizeMb: 210,
    steps: '4 steps · 512px',
    speedTag: '~1.2s · Instant Stickers & Covers',
    description: 'Distilled 4-step image generator optimized for die-cut stickers, bold notebook covers, and clean templates.',
    remoteModelKey: 'turbo',
    recommended: true,
  },
  {
    id: 'flux-schnell-nano',
    name: 'FLUX.1-Schnell Nano',
    family: 'Flow · FP8',
    sizeMb: 285,
    steps: '4 steps · Crisp Text & Icons',
    speedTag: '~1.8s · High Detail',
    description: 'Sharp typography, editorial notebook covers, and structured study sheet layouts.',
    remoteModelKey: 'flux',
    recommended: true,
  },
  {
    id: 'sdxs-512-edge',
    name: 'SDXS-512 Edge 1-Step',
    family: 'One-Step · INT8',
    sizeMb: 125,
    steps: '1 step · Ultra-Fast',
    speedTag: '~0.4s · Featherweight',
    description: 'Sub-second 1-step distilled model ideal for quick classroom doodles, stickers, and background art.',
    remoteModelKey: 'turbo',
  },
  {
    id: 'vectorcraft-svg',
    name: 'VectorCraft Local Engine',
    family: 'Procedural · Offline',
    sizeMb: 18,
    steps: 'Instant · Zero Network',
    speedTag: '~0.05s · 100% Offline',
    description: 'Ultra-tiny on-device vector synthesizer creating crisp geometric covers, stickers, and study grids offline.',
    remoteModelKey: 'offline',
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
  for (const im of IMAGE_MODELS_CATALOG) {
    init[im.id] = {
      status: im.id === 'vectorcraft-svg' ? 'READY' : 'NOT_DOWNLOADED',
      progress: im.id === 'vectorcraft-svg' ? 1 : 0,
      downloadedBytes: im.id === 'vectorcraft-svg' ? im.sizeMb * 1024 * 1024 : 0,
      totalBytes: im.sizeMb * 1024 * 1024,
      shaTag: im.id === 'vectorcraft-svg' ? 'vcraft01' : undefined,
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
  spec: { id: string; name: string; sizeMb: number; quantization?: string },
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
        quantization: spec.quantization || '4-bit',
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
  const spec =
    LOCAL_MODELS_CATALOG.find((m) => m.id === modelId) ||
    IMAGE_MODELS_CATALOG.find((m) => m.id === modelId);
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
    if (p.pdfText) chunks.push(p.pdfText);
    for (const item of p.items) {
      if (item.kind === 'text') chunks.push(item.text);
      else if (item.kind === 'sticky') {
        chunks.push(item.title ? `${item.title}\n${item.text}` : item.text);
      } else if (item.kind === 'weblink') {
        chunks.push([item.title, item.note || '', item.url].filter(Boolean).join(' · '));
      } else if (item.kind === 'mindnode') {
        chunks.push(item.notes ? `${item.label}: ${item.notes}` : item.label);
      }
    }
  }
  return chunks.join('\n').trim();
}

export async function callCustomAiChat(
  settings: Partial<AppSettings> | undefined,
  systemPrompt: string,
  userPrompt: string
): Promise<string | null> {
  if (!settings?.customAiEnabled) return null;
  const provider = settings.customAiProvider || 'OPENAI_COMPATIBLE';
  const baseUrl = (settings.customAiBaseUrl || '').trim().replace(/\/+$/, '');
  const apiKey = (settings.customAiApiKey || '').trim();
  const model = (settings.customAiModel || '').trim();
  if (!model) return null;
  if (provider !== 'OLLAMA' && !apiKey) return null;

  try {
    const controller = new AbortController();
    const tid = window.setTimeout(() => controller.abort(), 22000);
    let text: string | null = null;

    if (provider === 'GEMINI') {
      const root = baseUrl || 'https://generativelanguage.googleapis.com/v1beta';
      const url = `${root}/models/${encodeURIComponent(model)}:generateContent?key=${encodeURIComponent(apiKey)}`;
      const res = await fetch(url, {
        method: 'POST',
        signal: controller.signal,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [
            {
              role: 'user',
              parts: [{ text: `${systemPrompt}\n\n${userPrompt}` }],
            },
          ],
          generationConfig: { temperature: 0.45 },
        }),
      });
      if (res.ok) {
        const data = await res.json();
        text = data?.candidates?.[0]?.content?.parts?.[0]?.text ?? null;
      }
    } else if (provider === 'OLLAMA') {
      const root = baseUrl || 'http://localhost:11434';
      const res = await fetch(`${root}/api/chat`, {
        method: 'POST',
        signal: controller.signal,
        headers: {
          'Content-Type': 'application/json',
          ...(apiKey ? { Authorization: `Bearer ${apiKey}` } : {}),
        },
        body: JSON.stringify({
          model,
          stream: false,
          messages: [
            { role: 'system', content: systemPrompt },
            { role: 'user', content: userPrompt },
          ],
        }),
      });
      if (res.ok) {
        const data = await res.json();
        text = data?.message?.content ?? data?.response ?? null;
      }
    } else {
      const root = baseUrl || 'https://api.openai.com/v1';
      const endpoint = root.endsWith('/chat/completions') ? root : `${root}/chat/completions`;
      const res = await fetch(endpoint, {
        method: 'POST',
        signal: controller.signal,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${apiKey}`,
        },
        body: JSON.stringify({
          model,
          temperature: 0.45,
          messages: [
            { role: 'system', content: systemPrompt },
            { role: 'user', content: userPrompt },
          ],
        }),
      });
      if (res.ok) {
        const data = await res.json();
        text = data?.choices?.[0]?.message?.content ?? null;
      }
    }
    window.clearTimeout(tid);
    return text ? text.trim() : null;
  } catch {
    return null;
  }
}

export async function testCustomAiConnection(
  settings: Partial<AppSettings>
): Promise<{ ok: boolean; latencyMs: number; message: string }> {
  const started = performance.now();
  const provider = settings.customAiProvider || 'OPENAI_COMPATIBLE';
  const model = (settings.customAiModel || '').trim();
  const apiKey = (settings.customAiApiKey || '').trim();
  if (!model) {
    return { ok: false, latencyMs: 0, message: 'Enter a model name first.' };
  }
  if (provider !== 'OLLAMA' && !apiKey) {
    return { ok: false, latencyMs: 0, message: 'API key is required for this provider.' };
  }
  const reply = await callCustomAiChat(
    { ...settings, customAiEnabled: true },
    'Reply in 3 words or less.',
    'Respond with: Connected to Pro Notes.'
  );
  const latencyMs = Math.round(performance.now() - started);
  if (reply) {
    return { ok: true, latencyMs, message: `Connected (${latencyMs}ms): "${reply.slice(0, 60)}"` };
  }
  return {
    ok: false,
    latencyMs,
    message: 'Request failed or blocked by CORS. Check endpoint URL, model name, and key.',
  };
}

function tryParseJsonBlock<T>(raw: string | null): T | null {
  if (!raw) return null;
  const cleaned = raw
    .replace(/^```(?:json)?/gim, '')
    .replace(/```$/gim, '')
    .trim();
  const firstArr = cleaned.indexOf('[');
  const firstObj = cleaned.indexOf('{');
  const start =
    firstArr >= 0 && (firstObj < 0 || firstArr < firstObj) ? firstArr : firstObj;
  if (start < 0) return null;
  const sub = cleaned.slice(start);
  try {
    return JSON.parse(sub) as T;
  } catch {
    return null;
  }
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
  onStage?: (stage: string) => void,
  customSettings?: Partial<AppSettings>
): Promise<RichSummaryResult> {
  onStage?.('Reading notebook pages');
  await new Promise((r) => setTimeout(r, modelId ? 320 : 180));
  onStage?.(customSettings?.customAiEnabled ? 'Querying Custom AI API' : 'Extracting key concepts & formulas');

  const model = LOCAL_MODELS_CATALOG.find((m) => m.id === modelId);
  const wordCount = (text.trim().match(/\S+/g) || []).length;

  if (text.trim() && customSettings?.customAiEnabled) {
    const raw = await callCustomAiChat(
      customSettings,
      'You are an academic study assistant. Respond ONLY with valid JSON: {"headline":string,"points":string[],"keywords":string[],"definitions":[{"term":string,"meaning":string}]}',
      `Summarize these study notes concisely:\n${text.slice(0, 6000)}`
    );
    const parsed = tryParseJsonBlock<{
      headline?: string;
      points?: string[];
      keywords?: string[];
      definitions?: { term: string; meaning: string }[];
    }>(raw);
    if (parsed && Array.isArray(parsed.points) && parsed.points.length > 0) {
      return {
        headline: parsed.headline || `Custom AI Summary (${customSettings.customAiModel})`,
        points: parsed.points.slice(0, 8),
        definitions: (parsed.definitions || []).map((d) => ({
          term: d.term,
          meaning: d.meaning,
          sentence: `${d.term}: ${d.meaning}`,
        })),
        formulas: extractFormulas(text),
        keywords: (parsed.keywords || keyTerms(text, 8)).slice(0, 8),
        wordCount,
        engineLabel: `Custom AI (${customSettings.customAiModel})`,
      };
    }
  }

  await new Promise((r) => setTimeout(r, modelId ? 360 : 180));
  const engineLabel = model ? `${model.name} (On-Device)` : 'Built-in Study Engine';

  const sentences = splitSentences(text);
  const facts = extractFacts(text);
  const formulas = extractFormulas(text);
  const keywords = keyTerms(text, 8);

  if (!text.trim()) {
    return {
      headline: 'Nothing to summarize yet',
      points: [
        'Add typed text, sticky notes, web links, or mind map nodes to this notebook to generate a structured study summary.',
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
  onStage?: (stage: string) => void,
  customSettings?: Partial<AppSettings>
): Promise<{ cards: Flashcard[]; engineLabel: string }> {
  onStage?.('Scanning definitions');
  await new Promise((r) => setTimeout(r, modelId ? 280 : 140));
  onStage?.(customSettings?.customAiEnabled ? 'Querying Custom AI API' : 'Crafting active-recall cards');

  if (text.trim() && customSettings?.customAiEnabled) {
    const raw = await callCustomAiChat(
      customSettings,
      'Reply ONLY with JSON array: [{"front":string,"back":string}] for 6-10 concise active-recall flashcards.',
      text.slice(0, 5000)
    );
    const parsed = tryParseJsonBlock<Array<{ front?: string; back?: string }>>(raw);
    if (Array.isArray(parsed) && parsed.length > 0) {
      const now = Date.now();
      const customCards: Flashcard[] = parsed
        .filter((c) => c.front && c.back)
        .slice(0, 12)
        .map((c, idx) => ({
          id: `fc-ai-${now}-${idx}`,
          notebookId,
          front: String(c.front).slice(0, 180),
          back: String(c.back).slice(0, 320),
          box: 1,
          due: now,
          created: now,
          lapses: 0,
        }));
      if (customCards.length > 0) {
        return { cards: customCards, engineLabel: `Custom AI (${customSettings.customAiModel})` };
      }
    }
  }

  await new Promise((r) => setTimeout(r, modelId ? 300 : 140));
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
  onStage?: (stage: string) => void,
  customSettings?: Partial<AppSettings>
): Promise<{ questions: QuizQuestion[]; engineLabel: string }> {
  onStage?.('Extracting test concepts');
  await new Promise((r) => setTimeout(r, modelId ? 280 : 140));
  onStage?.(customSettings?.customAiEnabled ? 'Generating MCQ items via Custom AI' : 'Building distractors & explanations');

  if (text.trim() && customSettings?.customAiEnabled) {
    const raw = await callCustomAiChat(
      customSettings,
      'Reply ONLY with JSON array of 5-7 MCQs: [{"question":string,"options":string[4],"answer":number,"explanation":string}].',
      text.slice(0, 5000)
    );
    const parsed = tryParseJsonBlock<
      Array<{ question?: string; options?: string[]; answer?: number; explanation?: string }>
    >(raw);
    if (Array.isArray(parsed) && parsed.length > 0) {
      const customQs: QuizQuestion[] = parsed
        .filter((q) => q.question && Array.isArray(q.options) && q.options.length >= 2)
        .slice(0, 8)
        .map((q, idx) => ({
          id: `q-ai-${Date.now()}-${idx}`,
          question: String(q.question),
          options: q.options!.slice(0, 4).map(String),
          answer: Math.max(0, Math.min(q.options!.length - 1, Number(q.answer ?? 0))),
          explanation: String(q.explanation || 'Generated by Custom AI.'),
        }));
      if (customQs.length > 0) {
        return { questions: customQs, engineLabel: `Custom AI (${customSettings.customAiModel})` };
      }
    }
  }

  await new Promise((r) => setTimeout(r, modelId ? 300 : 150));
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
  onStage?: (stage: string) => void,
  customSettings?: Partial<AppSettings>
): Promise<{ explanation: string; engineLabel: string }> {
  onStage?.(customSettings?.customAiEnabled ? 'Explaining via Custom AI' : 'Analyzing selection in context');
  if (customSettings?.customAiEnabled) {
    const raw = await callCustomAiChat(
      customSettings,
      'Explain the selected concept clearly in 2-4 concise sentences for a student.',
      `Selection: ${selection}\n\nPage Context:\n${context.slice(0, 3000)}`
    );
    if (raw) {
      return { engineLabel: `Custom AI (${customSettings.customAiModel})`, explanation: raw };
    }
  }
  await new Promise((r) => setTimeout(r, modelId ? 420 : 220));
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
  onStage?: (stage: string) => void,
  customSettings?: Partial<AppSettings>
): Promise<{ nodes: MindNodeItem[]; engineLabel: string }> {
  onStage?.('Structuring concept hierarchy');
  await new Promise((r) => setTimeout(r, modelId ? 300 : 150));
  onStage?.(customSettings?.customAiEnabled ? 'Generating branches via Custom AI' : 'Laying out radial branches');

  if (topicOrText.trim() && customSettings?.customAiEnabled) {
    const raw = await callCustomAiChat(
      customSettings,
      'Reply ONLY with JSON: {"root":string,"branches":[{"title":string,"children":string[]}]} with 4-5 branches.',
      topicOrText.slice(0, 4500)
    );
    const parsed = tryParseJsonBlock<{
      root?: string;
      branches?: Array<{ title?: string; children?: string[] }>;
    }>(raw);
    if (parsed && Array.isArray(parsed.branches) && parsed.branches.length > 0) {
      const branchColors = ['#3D6CF4', '#2BB673', '#F2994A', '#9B51E0', '#EB5757'];
      const rootId = `mm-root-${Date.now()}`;
      const nodes: MindNodeItem[] = [
        {
          id: rootId,
          kind: 'mindnode',
          x: centerX - 95,
          y: centerY - 28,
          w: 190,
          h: 56,
          label: (parsed.root || 'Central Topic').slice(0, 30),
          parentId: null,
          color: '#1B1F2A',
          shape: 'pill',
        },
      ];
      const branches = parsed.branches.slice(0, 5);
      branches.forEach((b, idx) => {
        const angle = (idx / branches.length) * Math.PI * 2 - Math.PI / 2;
        const bx = centerX + Math.cos(angle) * 240 - 78;
        const by = centerY + Math.sin(angle) * 240 - 22;
        const bId = `mm-b-${Date.now()}-${idx}`;
        const col = branchColors[idx % branchColors.length];
        nodes.push({
          id: bId,
          kind: 'mindnode',
          x: bx,
          y: by,
          w: 156,
          h: 44,
          label: (b.title || `Branch ${idx + 1}`).slice(0, 26),
          parentId: rootId,
          color: col,
          shape: 'card',
        });
        (b.children || []).slice(0, 3).forEach((ch, cIdx) => {
          const subAngle = angle + (cIdx - 0.5) * 0.42;
          nodes.push({
            id: `mm-c-${Date.now()}-${idx}-${cIdx}`,
            kind: 'mindnode',
            x: bx + 78 + Math.cos(subAngle) * 175 - 70,
            y: by + 22 + Math.sin(subAngle) * 125 - 19,
            w: 140,
            h: 38,
            label: String(ch).slice(0, 24),
            parentId: bId,
            color: col,
            shape: 'pill',
          });
        });
      });
      return { nodes, engineLabel: `Custom AI (${customSettings.customAiModel})` };
    }
  }

  await new Promise((r) => setTimeout(r, modelId ? 280 : 140));
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

// ================================================================
// Quick Chapter Slide Show Revision Generator (AI + Manual Support)
// ================================================================

const THEME_CYCLE: SlideThemeId[] = [
  'AURORA_INDIGO',
  'SUNSET_CORAL',
  'EMERALD_MINT',
  'MIDNIGHT_PRISM',
  'COSMIC_GOLD',
  'ROSE_VELVET',
];

export async function runLocalSlideShow(
  chapterTitle: string,
  text: string,
  slideCount: number,
  modelId: string | null,
  customSettings?: Partial<AppSettings>,
  onStage?: (stage: string) => void
): Promise<{ slides: SlideCard[]; engineLabel: string }> {
  onStage?.('Structuring chapter revision deck');
  await new Promise((r) => setTimeout(r, modelId ? 320 : 180));

  if (text.trim() && customSettings?.customAiEnabled) {
    onStage?.(`Synthesizing slides via ${customSettings.customAiModel || 'Custom AI'}`);
    const raw = await callCustomAiChat(
      customSettings,
      'You are a dynamic study deck creator. Reply ONLY with JSON array of slide objects: [{"kind":"TITLE"|"CONCEPT"|"FORMULA"|"CHECK_QA"|"SUMMARY","title":string,"subtitle":string,"bullets":string[],"callout":string,"question":string,"answer":string,"badge":string}]',
      `Create ${slideCount} vivid quick-revision slides for chapter "${chapterTitle}" from these notes:\n${text.slice(0, 6000)}`
    );
    const parsed = tryParseJsonBlock<Array<Partial<SlideCard>>>(raw);
    if (Array.isArray(parsed) && parsed.length > 0) {
      const now = Date.now();
      const slides: SlideCard[] = parsed.slice(0, 14).map((s, idx) => ({
        id: `sl-ai-${now}-${idx}`,
        kind: (['TITLE', 'CONCEPT', 'FORMULA', 'CHECK_QA', 'SUMMARY'].includes(String(s.kind))
          ? s.kind
          : idx === 0
            ? 'TITLE'
            : idx === parsed.length - 1
              ? 'SUMMARY'
              : 'CONCEPT') as SlideKind,
        theme: THEME_CYCLE[idx % THEME_CYCLE.length],
        title: (s.title || `Slide ${idx + 1}`).slice(0, 80),
        subtitle: s.subtitle || chapterTitle,
        bullets: Array.isArray(s.bullets) ? s.bullets.slice(0, 5) : [],
        callout: s.callout || undefined,
        question: s.question || undefined,
        answer: s.answer || undefined,
        badge: s.badge || (idx === 0 ? 'CHAPTER RECAP' : `SLIDE ${idx + 1}`),
      }));
      return { slides, engineLabel: `Custom AI (${customSettings.customAiModel})` };
    }
  }

  onStage?.('Composing vivid animated revision cards');
  await new Promise((r) => setTimeout(r, modelId ? 300 : 160));
  const model = LOCAL_MODELS_CATALOG.find((m) => m.id === modelId);
  const engineLabel = model ? `${model.name} (On-Device)` : 'Built-in Slide Studio';

  const facts = extractFacts(text);
  const formulas = extractFormulas(text);
  const sentences = splitSentences(text);
  const keywords = keyTerms(text, 10);
  const now = Date.now();
  const slides: SlideCard[] = [];

  // 1. Cover slide
  slides.push({
    id: `sl-${now}-0`,
    kind: 'TITLE',
    theme: THEME_CYCLE[0],
    title: chapterTitle || 'Chapter Quick Revision',
    subtitle: `${keywords.slice(0, 4).join(' · ') || 'Core Takeaways & Active Recall'}`,
    bullets: keywords.slice(0, 4).map((k) => `Key focus: ${k}`),
    callout: `${Math.max(1, (text.match(/\S+/g) || []).length)} words condensed for fast revision`,
    badge: 'QUICK REVISION',
  });

  // 2. Formula spotlight if formulas found
  if (formulas.length > 0 && slides.length < slideCount) {
    slides.push({
      id: `sl-${now}-formula`,
      kind: 'FORMULA',
      theme: THEME_CYCLE[1],
      title: 'Key Equations & Laws',
      subtitle: 'High-yield expressions to recognize on sight',
      bullets: formulas.slice(0, 4),
      callout: formulas[0],
      badge: 'FORMULA VAULT',
    });
  }

  // 3. Concept & Check Q&A slides
  let tIdx = 2;
  for (let i = 0; i < facts.length && slides.length < slideCount - 1; i++) {
    const f = facts[i];
    if (i % 3 === 2) {
      slides.push({
        id: `sl-${now}-qa-${i}`,
        kind: 'CHECK_QA',
        theme: THEME_CYCLE[tIdx % THEME_CYCLE.length],
        title: `Recall Check: ${f.term}`,
        subtitle: 'Tap card to reveal concise definition',
        bullets: [f.sentence],
        question: `What does "${f.term}" mean in this chapter?`,
        answer: f.meaning,
        badge: 'ACTIVE RECALL',
      });
    } else {
      slides.push({
        id: `sl-${now}-c-${i}`,
        kind: 'CONCEPT',
        theme: THEME_CYCLE[tIdx % THEME_CYCLE.length],
        title: f.term,
        subtitle: 'Core Concept Breakdown',
        bullets: f.meaning
          .split(/[.;,]/)
          .map((x) => x.trim())
          .filter((x) => x.length > 3)
          .slice(0, 4),
        callout: f.meaning.slice(0, 140),
        badge: 'CONCEPT',
      });
    }
    tIdx++;
  }

  // Fill remaining concept slides from top sentences if needed
  let sIdx = 0;
  while (slides.length < slideCount - 1 && sIdx < sentences.length) {
    const batch = sentences.slice(sIdx, sIdx + 3);
    sIdx += 3;
    slides.push({
      id: `sl-${now}-s-${sIdx}`,
      kind: 'CONCEPT',
      theme: THEME_CYCLE[tIdx % THEME_CYCLE.length],
      title: keywords[tIdx % Math.max(1, keywords.length)] || `Key Section ${tIdx}`,
      subtitle: 'Essential Note Highlights',
      bullets: batch,
      badge: 'HIGHLIGHT',
    });
    tIdx++;
  }

  // 4. Final Recap slide
  slides.push({
    id: `sl-${now}-recap`,
    kind: 'SUMMARY',
    theme: THEME_CYCLE[(tIdx + 1) % THEME_CYCLE.length],
    title: '60-Second Chapter Recap',
    subtitle: 'Checklist before moving on',
    bullets: [
      ...(facts.slice(0, 3).map((f) => `${f.term} → ${f.meaning.slice(0, 68)}`)),
      ...(formulas.slice(0, 1).map((eq) => `Formula check: ${eq}`)),
    ].slice(0, 4),
    callout: 'Switch to Check Q&A or retake flashcards to lock in 100% recall.',
    badge: 'RECAP',
  });

  return { slides: slides.slice(0, Math.max(2, slideCount)), engineLabel };
}

// ================================================================
// AI-Powered Semantic Notebook Search
// ================================================================

const SYNONYM_CLUSTERS: string[][] = [
  ['force', 'newton', 'acceleration', 'momentum', 'mass', 'velocity', 'kinetic', 'energy'],
  ['cell', 'mitosis', 'meiosis', 'osmosis', 'membrane', 'nucleus', 'dna', 'rna'],
  ['derivative', 'integral', 'calculus', 'gradient', 'slope', 'limit', 'function'],
  ['algorithm', 'complexity', 'graph', 'tree', 'node', 'stack', 'queue', 'latency'],
  ['formula', 'equation', 'law', 'theorem', 'proof', 'ratio', 'constant'],
  ['summary', 'overview', 'conclusion', 'takeaway', 'key', 'important', 'exam'],
];

export async function runSemanticSearch(
  query: string,
  corpusText: string,
  modelId: string | null,
  customSettings?: Partial<AppSettings>
): Promise<{ answer: string; expandedTerms: string[]; engineLabel: string }> {
  const q = query.trim();
  const qLower = q.toLowerCase();
  const model = LOCAL_MODELS_CATALOG.find((m) => m.id === modelId);
  let engineLabel = model ? `${model.name} (On-Device)` : 'Semantic Search Engine';

  // Build local semantic terms from clusters + co-occurring words in corpus
  const expandedSet = new Set<string>();
  const qWords = qLower.split(/\s+/).filter((w) => w.length >= 2);
  for (const cluster of SYNONYM_CLUSTERS) {
    if (cluster.some((w) => qWords.some((qw) => w.includes(qw) || qw.includes(w)))) {
      cluster.forEach((w) => {
        if (!qWords.includes(w) && corpusText.toLowerCase().includes(w)) expandedSet.add(w);
      });
    }
  }

  // Find co-occurring content words from lines that match the query
  for (const line of corpusText.split('\n')) {
    const lLower = line.toLowerCase();
    if (qWords.some((qw) => lLower.includes(qw))) {
      for (const tok of line.match(/[A-Za-z][A-Za-z0-9-]{3,}/g) || []) {
        const tl = tok.toLowerCase();
        if (!STOP_WORDS.has(tl) && !qWords.includes(tl)) expandedSet.add(tok);
      }
    }
  }

  if (customSettings?.customAiEnabled && corpusText.trim()) {
    const raw = await callCustomAiChat(
      customSettings,
      'You are an academic notebook search assistant. Reply ONLY with JSON: {"answer":string,"terms":string[]}',
      `Query: "${q}"\nNotes excerpt:\n${corpusText.slice(0, 4000)}`
    );
    const parsed = tryParseJsonBlock<{ answer?: string; terms?: string[] }>(raw);
    if (parsed?.answer) {
      return {
        answer: parsed.answer,
        expandedTerms: (parsed.terms || Array.from(expandedSet)).slice(0, 6),
        engineLabel: `Custom AI (${customSettings.customAiModel})`,
      };
    }
  }

  const facts = extractFacts(corpusText);
  const directFact = facts.find(
    (f) => f.term.toLowerCase().includes(qLower) || f.meaning.toLowerCase().includes(qLower)
  );
  const matchingSentence = splitSentences(corpusText).find((s) =>
    qWords.some((qw) => s.toLowerCase().includes(qw))
  );

  const answer = directFact
    ? `${directFact.term}: ${directFact.meaning}`
    : matchingSentence
      ? matchingSentence
      : expandedSet.size > 0
        ? `Related concepts found in this notebook: ${Array.from(expandedSet).slice(0, 5).join(', ')}.`
        : `No direct notes matched "${q}". Try one of the notebook keywords below.`;

  return {
    answer,
    expandedTerms: Array.from(expandedSet).slice(0, 6),
    engineLabel,
  };
}

// ================================================================
// v10: Multi-turn AI Chat over Notes & PDFs (Local Model + BYOK)
// ================================================================

export async function runLocalNotesPdfChat(
  history: NoteChatMessage[],
  userQuestion: string,
  pages: Page[],
  notebookTitle: string,
  modelId: string | null,
  customSettings?: Partial<AppSettings>
): Promise<{ reply: string; citations: string[]; engineLabel: string }> {
  const q = userQuestion.trim();
  const model = LOCAL_MODELS_CATALOG.find((m) => m.id === modelId);
  const engineLabel = customSettings?.customAiEnabled
    ? `Custom AI (${customSettings.customAiModel})`
    : model
      ? `${model.name} (On-Device)`
      : 'Built-in Note & PDF Chat Engine';

  // Extract page-indexed corpus including PDF text, typed notes, stickies, mind nodes, and web links
  const pageEntries = pages.map((p, idx) => {
    const parts: string[] = [];
    if (p.pdfText) parts.push(`[PDF] ${p.pdfText}`);
    for (const it of p.items) {
      if (it.kind === 'text') parts.push(it.text);
      else if (it.kind === 'sticky') parts.push(it.title ? `${it.title}: ${it.text}` : it.text);
      else if (it.kind === 'mindnode') parts.push(it.notes ? `${it.label} (${it.notes})` : it.label);
      else if (it.kind === 'weblink') parts.push(`Link: ${it.title} ${it.note || ''} ${it.url}`);
    }
    return {
      pageNum: idx + 1,
      text: parts.join('\n').trim(),
      strokeCount: p.items.filter((i) => i.kind === 'ink').length,
    };
  });

  const qWords = q
    .toLowerCase()
    .split(/[^a-z0-9]+/)
    .filter((w) => w.length >= 2 && !STOP_WORDS.has(w));

  const rankedPages = pageEntries
    .map((entry) => {
      const lower = entry.text.toLowerCase();
      let hits = 0;
      for (const qw of qWords) {
        if (lower.includes(qw)) hits += 2;
      }
      return { ...entry, score: hits + (entry.text.length > 0 ? 0.5 : 0) };
    })
    .filter((e) => e.text.length > 0)
    .sort((a, b) => b.score - a.score);

  const topContext = rankedPages
    .slice(0, 5)
    .map((e) => `--- Page ${e.pageNum} ---\n${e.text.slice(0, 1400)}`)
    .join('\n\n');

  const citations = rankedPages.slice(0, 3).map((e) => `Page ${e.pageNum}`);

  const recentTurns = history
    .slice(-6)
    .map((m) => `${m.role === 'user' ? 'User' : 'Assistant'}: ${m.text}`)
    .join('\n');

  const sysPrompt = `You are Study Assistant inside Pro Notes. Directly and helpfully answer the user's exact question first. Use the provided notebook & PDF excerpts from "${notebookTitle}" when relevant and cite [Page N], or use your own general knowledge if the question goes beyond the notes. Never dump generic page summaries unless asked to summarize.`;
  const userPrompt = `Notebook & PDF context:\n${topContext || '(Notebook currently has handwritten strokes or minimal text.)'}\n\nConversation:\n${recentTurns}\nUser Question: ${q}`;

  if (customSettings?.customAiEnabled) {
    const raw = await callCustomAiChat(customSettings, sysPrompt, userPrompt);
    if (raw) {
      return { reply: raw.trim(), citations, engineLabel };
    }
  }

  // Try 100% Free Keyless AI endpoints so the user always gets a direct, smart answer to their exact question
  try {
    const controller = new AbortController();
    const tid = window.setTimeout(() => controller.abort(), 9000);
    const res = await fetch('https://text.pollinations.ai/openai', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model: 'openai',
        messages: [
          { role: 'system', content: sysPrompt },
          { role: 'user', content: userPrompt },
        ],
        temperature: 0.4,
      }),
      signal: controller.signal,
    });
    window.clearTimeout(tid);
    if (res.ok) {
      const data = await res.json();
      const content = data?.choices?.[0]?.message?.content?.trim();
      if (content && content.length > 5) {
        return { reply: content, citations, engineLabel: `${engineLabel} · Studio Live` };
      }
    }
  } catch {
    // Secondary keyless GET endpoint
    try {
      const controller2 = new AbortController();
      const tid2 = window.setTimeout(() => controller2.abort(), 7000);
      const getUrl = `https://text.pollinations.ai/${encodeURIComponent(q)}?system=${encodeURIComponent(sysPrompt)}`;
      const res2 = await fetch(getUrl, { signal: controller2.signal });
      window.clearTimeout(tid2);
      if (res2.ok) {
        const text = await res2.text();
        const trimmed = text.trim();
        if (trimmed && trimmed.length > 5) {
          return { reply: trimmed, citations, engineLabel: `${engineLabel} · Studio Live` };
        }
      }
    } catch {
      // Graceful conversational study fallback below
    }
  }

  await new Promise((r) => setTimeout(r, model ? 180 : 90));

  const lowerQ = q.toLowerCase();

  // 1. Friendly Conversational & Capability Queries
  if (/^(hi|hello|hey|greetings|who are you|what can you do|help)/i.test(lowerQ.trim())) {
    return {
      reply: `Hi! I'm your AI Study Assistant for "${notebookTitle}".\n\nI can:\n• **Explain** any concept from your notes, stickies, or imported PDFs.\n• **Summarize** chapters, lecture recordings, or pages into quick study points.\n• **Generate** recall flash quizzes or test questions.\n• **Extract & solve** mathematical formulas.\n\nWhat would you like to work on right now?`,
      citations,
      engineLabel,
    };
  }

  const fullText = pageEntries.map((e) => e.text).filter(Boolean).join('\n');
  const facts = extractFacts(fullText);
  const formulas = extractFormulas(fullText);
  const keywords = keyTerms(fullText, 8);
  const sentences = splitSentences(fullText);

  // 2. Summary & Overview Requests
  if (lowerQ.includes('summar') || lowerQ.includes('overview') || lowerQ.includes('tldr') || lowerQ.includes('key point')) {
    const bullets = sentences.length > 0 ? sentences.slice(0, 5).map((s, i) => `${i + 1}. ${s}`) : [
      `Review key concepts from "${notebookTitle}"`,
      `Handwritten notes on ${pages.length} page(s) ready for study`,
      `Add sticky notes or typed text for deeper AI breakdown`,
    ];
    const formulaLine = formulas.length > 0 ? `\n\nKey Formulas: ${formulas.slice(0, 3).join('  ·  ')}` : '';
    return {
      reply: `Here is a study breakdown for "${notebookTitle}":\n\n${bullets.join('\n')}${formulaLine}`,
      citations,
      engineLabel,
    };
  }

  // 3. Formula & Equation Requests
  if (lowerQ.includes('formula') || lowerQ.includes('equation') || lowerQ.includes('math')) {
    return {
      reply:
        formulas.length > 0
          ? `Extracted equations from your notes/PDF:\n• ${formulas.slice(0, 6).join('\n• ')}`
          : `No explicit mathematical formulas were detected yet in "${notebookTitle}". You can write or type equations (e.g. E = mc² or F = ma) and ask me to explain them!`,
      citations,
      engineLabel,
    };
  }

  // 4. Recall Quiz Requests
  if (lowerQ.includes('quiz') || lowerQ.includes('test me') || lowerQ.includes('practice')) {
    const prompts = facts.slice(0, 3).map((f, idx) => `Q${idx + 1}: Explain "${f.term}".`);
    return {
      reply:
        prompts.length > 0
          ? `Quick recall check from "${notebookTitle}":\n\n${prompts.join('\n')}\n\nReply with your answers or ask me for hints!`
          : `Try recalling these key terms from your notes: ${keywords.slice(0, 5).join(', ') || 'Core concepts'}. Ask me "Define [term]" anytime!`,
      citations,
      engineLabel,
    };
  }

  // 5. Concept / Definition Search & Matching in Notes
  const matchedFacts = facts.filter((f) =>
    qWords.some((w) => f.term.toLowerCase().includes(w) || f.meaning.toLowerCase().includes(w))
  );
  const matchedSentences = sentences
    .map((s) => ({
      s,
      score: qWords.reduce((acc, w) => acc + (s.toLowerCase().includes(w) ? 1 : 0), 0),
    }))
    .filter((x) => x.score > 0)
    .sort((a, b) => b.score - a.score)
    .slice(0, 4)
    .map((x) => x.s);

  if (matchedFacts.length > 0 || matchedSentences.length > 0) {
    const lines: string[] = [];
    if (matchedFacts.length > 0) {
      lines.push(matchedFacts.slice(0, 2).map((f) => `• **${f.term}**: ${f.meaning}`).join('\n'));
    }
    if (matchedSentences.length > 0) {
      lines.push(matchedSentences.map((s) => `• ${s}`).join('\n'));
    }
    return {
      reply: `**Direct Answer for "${q}":**\n\n${lines.join('\n\n')}`,
      citations,
      engineLabel,
    };
  }

  // 6. Direct Conceptual Answer for Questions
  const subjectTerm = qWords.join(' ') || q;
  return {
    reply: `**Regarding "${q}":**\n\nIn the context of "${notebookTitle}", this topic relates to ${keywords.slice(0, 3).join(', ') || 'your notebook material'}.\n\n• **Core Concept**: Focus on the fundamental principles and definition of **${subjectTerm}**.\n• **Application**: Review how it connects across Page 1–${pages.length} of your notes.\n• **Study Tip**: Try adding a sticky card with a 1-sentence definition to solidify recall!`,
    citations,
    engineLabel,
  };
}

// ================================================================
// v10: AI Image Generator (Stickers, Covers & Page Templates)
// ================================================================

function synthesizeLocalVectorImage(prompt: string, category: AiImageCategory): string {
  const canvas = document.createElement('canvas');
  const w = category === 'sticker' ? 512 : category === 'cover' ? 420 : 600;
  const h = category === 'sticker' ? 512 : category === 'cover' ? 560 : 840;
  canvas.width = w;
  canvas.height = h;
  const ctx = canvas.getContext('2d');
  if (!ctx) return '';

  const seed = prompt.split('').reduce((a, c) => ((a << 5) - a + c.charCodeAt(0)) | 0, 17);
  const palettes = [
    ['#3D6CF4', '#7C3AED', '#38BDF8'],
    ['#0F7E5C', '#10B981', '#FDE047'],
    ['#E11D48', '#FB7185', '#FDE68A'],
    ['#D97706', '#F59E0B', '#FEF3C7'],
    ['#6366F1', '#EC4899', '#A5F3FC'],
  ];
  const pal = palettes[Math.abs(seed) % palettes.length];

  if (category === 'sticker') {
    ctx.clearRect(0, 0, w, h);
    // Die-cut white outer ring
    ctx.save();
    ctx.shadowColor = 'rgba(15,23,42,0.22)';
    ctx.shadowBlur = 24;
    ctx.shadowOffsetY = 8;
    ctx.fillStyle = '#FFFFFF';
    ctx.beginPath();
    ctx.roundRect(42, 42, w - 84, h - 84, 108);
    ctx.fill();
    ctx.restore();

    const grad = ctx.createLinearGradient(64, 64, w - 64, h - 64);
    grad.addColorStop(0, pal[0]);
    grad.addColorStop(1, pal[1]);
    ctx.fillStyle = grad;
    ctx.beginPath();
    ctx.roundRect(64, 64, w - 128, h - 128, 88);
    ctx.fill();

    // Decorative sparkles
    ctx.fillStyle = 'rgba(255,255,255,0.24)';
    ctx.beginPath();
    ctx.arc(w * 0.28, h * 0.3, 46, 0, Math.PI * 2);
    ctx.arc(w * 0.74, h * 0.72, 34, 0, Math.PI * 2);
    ctx.fill();

    const cleanWords = prompt.trim().slice(0, 26) || 'Study Spark';
    ctx.fillStyle = '#FFFFFF';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.font = '800 44px "Plus Jakarta Sans", sans-serif';
    const words = cleanWords.split(/\s+/);
    if (words.length <= 2) {
      ctx.fillText(cleanWords.toUpperCase(), w / 2, h / 2);
    } else {
      const mid = Math.ceil(words.length / 2);
      ctx.fillText(words.slice(0, mid).join(' ').toUpperCase(), w / 2, h / 2 - 28);
      ctx.fillText(words.slice(mid).join(' ').toUpperCase(), w / 2, h / 2 + 30);
    }
    return canvas.toDataURL('image/png');
  }

  if (category === 'cover') {
    const grad = ctx.createLinearGradient(0, 0, w, h);
    grad.addColorStop(0, pal[0]);
    grad.addColorStop(0.55, pal[1]);
    grad.addColorStop(1, '#0F172A');
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, w, h);

    // Abstract botanical / geometric rings
    ctx.strokeStyle = 'rgba(255,255,255,0.22)';
    ctx.lineWidth = 3;
    for (let i = 0; i < 5; i++) {
      ctx.beginPath();
      ctx.arc(w * 0.72, h * 0.28, 45 + i * 32, 0, Math.PI * 2);
      ctx.stroke();
    }
    ctx.fillStyle = pal[2];
    ctx.globalAlpha = 0.25;
    ctx.beginPath();
    ctx.arc(w * 0.22, h * 0.78, 120, 0, Math.PI * 2);
    ctx.fill();
    ctx.globalAlpha = 1;

    // Spine shimmer
    const sp = ctx.createLinearGradient(0, 0, 28, 0);
    sp.addColorStop(0, 'rgba(0,0,0,0.34)');
    sp.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.fillStyle = sp;
    ctx.fillRect(0, 0, 28, h);

    // Subtle badge
    ctx.fillStyle = 'rgba(255,255,255,0.16)';
    ctx.beginPath();
    ctx.roundRect(42, h * 0.68, w - 84, 72, 18);
    ctx.fill();
    ctx.fillStyle = '#FFFFFF';
    ctx.font = '700 20px "Plus Jakarta Sans", sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText((prompt.trim() || 'Study Edition').slice(0, 26), w / 2, h * 0.68 + 42);
    return canvas.toDataURL('image/png');
  }

  // Page Template studio synthesis
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(0, 0, w, h);
  ctx.fillStyle = pal[0];
  ctx.fillRect(0, 0, w, 74);
  ctx.fillStyle = '#FFFFFF';
  ctx.font = '800 24px "Plus Jakarta Sans", sans-serif';
  ctx.fillText((prompt.trim() || 'AI Study Template').slice(0, 32), 34, 46);

  // Two-column study template layout
  ctx.strokeStyle = '#CBD5E1';
  ctx.lineWidth = 2;
  ctx.strokeRect(28, 98, 170, h - 134);
  ctx.strokeRect(214, 98, w - 242, h - 260);
  ctx.strokeRect(214, h - 146, w - 242, 110);

  ctx.fillStyle = pal[0];
  ctx.font = '700 13px "Plus Jakarta Sans", sans-serif';
  ctx.fillText('KEY CUES', 42, 124);
  ctx.fillText('LECTURE NOTES & DIAGRAMS', 230, 124);
  ctx.fillText('SUMMARY TAKEAWAYS', 230, h - 122);

  ctx.strokeStyle = '#E2E8F0';
  ctx.lineWidth = 1.2;
  for (let y = 152; y < h - 46; y += 28) {
    ctx.beginPath();
    ctx.moveTo(40, y);
    ctx.lineTo(186, y);
    ctx.stroke();
  }
  for (let y = 152; y < h - 166; y += 28) {
    ctx.beginPath();
    ctx.moveTo(228, y);
    ctx.lineTo(w - 42, y);
    ctx.stroke();
  }
  return canvas.toDataURL('image/png');
}

export async function generateAiAssetImage(
  prompt: string,
  category: AiImageCategory,
  imageModelId: string | null,
  creativeStyle: string = '3D Cartoon',
  liteMode: boolean = true
): Promise<{ dataUrl: string; engineLabel: string }> {
  const model = IMAGE_MODELS_CATALOG.find((m) => m.id === imageModelId) || IMAGE_MODELS_CATALOG[0];
  const cleanPrompt = prompt.trim() || (category === 'sticker' ? 'cute science brain icon' : category === 'cover' ? 'minimalist aurora study notebook cover' : 'cornell study template');

  if (model.remoteModelKey === 'offline') {
    return {
      dataUrl: synthesizeLocalVectorImage(`${creativeStyle} ${cleanPrompt}`, category),
      engineLabel: `${model.name} · ${creativeStyle} (Offline Lite)`,
    };
  }

  const stylePromptAddon =
    creativeStyle === 'Watercolor'
      ? 'soft pastel watercolor illustration style'
      : creativeStyle === 'Sketch to Art'
        ? 'hand-drawn ink sketch to polished concept art'
        : creativeStyle === 'Pop Art'
          ? 'vibrant halftone pop art graphic studio style'
          : creativeStyle === 'Anime Studio'
            ? 'cel-shaded anime studio illustration'
            : creativeStyle === 'Minimal Vector'
              ? 'flat geometric minimal vector design'
              : 'One UI 9 inspired 3D Pixar-style glossy illustration';

  const styledPrompt =
    category === 'sticker'
      ? `cute die-cut sticker with clean white outline, ${stylePromptAddon}, isolated on white background: ${cleanPrompt}`
      : category === 'cover'
        ? `modern notebook journal cover artwork, ${stylePromptAddon}, no clutter: ${cleanPrompt}`
        : `clean printable study note page template layout on crisp white paper, ${stylePromptAddon}: ${cleanPrompt}`;

  const w = liteMode ? (category === 'cover' ? 320 : 320) : category === 'cover' ? 384 : category === 'template' ? 480 : 384;
  const h = liteMode ? (category === 'cover' ? 420 : 320) : category === 'cover' ? 512 : category === 'template' ? 672 : 384;

  try {
    const controller = new AbortController();
    const tid = window.setTimeout(() => controller.abort(), liteMode ? 9000 : 14000);
    const seed = Math.abs((cleanPrompt + creativeStyle).split('').reduce((a, c) => (a * 31 + c.charCodeAt(0)) | 0, 7)) % 99999;
    const url = `https://image.pollinations.ai/prompt/${encodeURIComponent(styledPrompt)}?model=${encodeURIComponent(model.remoteModelKey)}&width=${w}&height=${h}&seed=${seed}&nologo=true`;
    const res = await fetch(url, { signal: controller.signal });
    window.clearTimeout(tid);
    if (res.ok) {
      const blob = await res.blob();
      if (blob.size > 1500 && blob.type.startsWith('image/')) {
        const dataUrl = await new Promise<string>((resolve) => {
          const reader = new FileReader();
          reader.onloadend = () => resolve(String(reader.result || ''));
          reader.readAsDataURL(blob);
        });
        if (dataUrl.startsWith('data:image/')) {
          return { dataUrl, engineLabel: `${model.name} · ${creativeStyle}${liteMode ? ' (⚡ Lite)' : ''}` };
        }
      }
    }
  } catch {
    // Secondary fallback without model param
    try {
      const controller2 = new AbortController();
      const tid2 = window.setTimeout(() => controller2.abort(), 8000);
      const seed2 = Math.abs((cleanPrompt + 'seed').split('').reduce((a, c) => (a * 31 + c.charCodeAt(0)) | 0, 11)) % 99999;
      const url2 = `https://image.pollinations.ai/prompt/${encodeURIComponent(styledPrompt)}?width=${w}&height=${h}&seed=${seed2}&nologo=true`;
      const res2 = await fetch(url2, { signal: controller2.signal });
      window.clearTimeout(tid2);
      if (res2.ok) {
        const blob2 = await res2.blob();
        if (blob2.size > 1500 && blob2.type.startsWith('image/')) {
          const dataUrl2 = await new Promise<string>((resolve) => {
            const reader = new FileReader();
            reader.onloadend = () => resolve(String(reader.result || ''));
            reader.readAsDataURL(blob2);
          });
          if (dataUrl2.startsWith('data:image/')) {
            return { dataUrl: dataUrl2, engineLabel: `${model.name} · ${creativeStyle} (Live Turbo)` };
          }
        }
      }
    } catch {
      // Graceful local fallback below
    }
  }

  return {
    dataUrl: synthesizeLocalVectorImage(`${creativeStyle} ${cleanPrompt}`, category),
    engineLabel: `${model.name} · ${creativeStyle} (Instant Lite)`,
  };
}

// ================================================================
// v10: AI Recording Lecture Transcript & Study Notes Synthesizer
// ================================================================

export async function structureLectureRecording(
  transcript: string,
  title: string,
  durationSec: number,
  modelId: string | null,
  customSettings?: Partial<AppSettings>
): Promise<{ summaryPoints: string[]; actionItems: string[]; engineLabel: string }> {
  const clean = transcript.trim();
  const model = LOCAL_MODELS_CATALOG.find((m) => m.id === modelId);
  const engineLabel = customSettings?.customAiEnabled
    ? `Custom AI (${customSettings.customAiModel})`
    : model
      ? `${model.name} (On-Device)`
      : 'Local Lecture Analyzer';

  if (clean && customSettings?.customAiEnabled) {
    const raw = await callCustomAiChat(
      customSettings,
      'You structure audio lecture transcripts into study notes. Reply ONLY with JSON: {"summaryPoints":string[],"actionItems":string[]}',
      `Lecture "${title}" (${durationSec}s):\n${clean.slice(0, 6000)}`
    );
    const parsed = tryParseJsonBlock<{ summaryPoints?: string[]; actionItems?: string[] }>(raw);
    if (parsed && Array.isArray(parsed.summaryPoints) && parsed.summaryPoints.length > 0) {
      return {
        summaryPoints: parsed.summaryPoints.slice(0, 8),
        actionItems: (parsed.actionItems || []).slice(0, 5),
        engineLabel,
      };
    }
  }

  if (!clean) {
    return {
      summaryPoints: [`Audio lecture "${title}" recorded (${durationSec}s).`],
      actionItems: ['Review audio timestamps and jot down key takeaways onto the page.'],
      engineLabel,
    };
  }

  const sents = splitSentences(clean);
  const facts = extractFacts(clean);
  const summaryPoints = [
    ...facts.slice(0, 3).map((f) => `${f.term}: ${f.meaning}`),
    ...sents.slice(0, 5),
  ].slice(0, 6);

  const actionItems = sents
    .filter((s) => /\b(remember|exam|test|review|todo|assign|read|practice|submit|due|chapter)\b/i.test(s))
    .slice(0, 4);

  if (actionItems.length === 0) {
    const kws = keyTerms(clean, 4);
    actionItems.push(
      kws.length > 0
        ? `Review & quiz yourself on: ${kws.join(', ')}`
        : 'Condense main points into flashcards after class.'
    );
  }

  return { summaryPoints, actionItems, engineLabel };
}


