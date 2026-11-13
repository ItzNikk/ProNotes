import React, { useRef, useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Check,
  Cpu,
  Download,
  Palette,
  PenTool,
  Sliders,
  Sparkles,
  Wand2,
  X,
  Zap,
} from 'lucide-react';
import {
  ACCENT_COLORS,
  AccentColorId,
  AppFont,
  AppSettings,
  CustomAiProvider,
  LocalModelState,
  MotionStyle,
  PAPER_STYLES,
  PAPER_TINTS,
  ThemeMode,
} from '../types/models';
import { LOCAL_MODELS_CATALOG, testCustomAiConnection } from '../utils/localAi';
import { AiParticleBackdrop } from './AiVisuals';

interface SetupWizardModalProps {
  open: boolean;
  settings: AppSettings;
  modelStates: Record<string, LocalModelState>;
  onUpdateSettings: (patch: Partial<AppSettings>) => void;
  onDownloadModel: (id: string) => void;
  onFinish: () => void;
}

const STEPS = [
  { id: 'intro', label: 'Welcome', icon: Sparkles },
  { id: 'look', label: 'Look Studio', icon: Palette },
  { id: 'workspace', label: 'Workspace', icon: Sliders },
  { id: 'ai', label: 'AI Studio', icon: Cpu },
  { id: 'pen', label: 'Pen Check', icon: PenTool },
];

export const SetupWizardModal: React.FC<SetupWizardModalProps> = ({
  open,
  settings,
  modelStates,
  onUpdateSettings,
  onDownloadModel,
  onFinish,
}) => {
  const [step, setStep] = useState(0);
  const [testStatus, setTestStatus] = useState<{ ok: boolean; msg: string } | null>(null);
  const [testing, setTesting] = useState(false);

  // Mini pen test pad state
  const padRef = useRef<HTMLCanvasElement>(null);
  const drawingRef = useRef(false);
  const lastPtRef = useRef<{ x: number; y: number } | null>(null);
  const [inkCount, setInkCount] = useState(0);

  if (!open) return null;

  const accent = ACCENT_COLORS[settings.accent];
  const recommendedModel = LOCAL_MODELS_CATALOG[0];
  const recState = modelStates[recommendedModel.id];

  const handlePadDown = (e: React.PointerEvent<HTMLCanvasElement>) => {
    const canvas = padRef.current;
    if (!canvas) return;
    canvas.setPointerCapture(e.pointerId);
    const rect = canvas.getBoundingClientRect();
    drawingRef.current = true;
    lastPtRef.current = { x: e.clientX - rect.left, y: e.clientY - rect.top };
  };

  const handlePadMove = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (!drawingRef.current || !lastPtRef.current || !padRef.current) return;
    const canvas = padRef.current;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const rect = canvas.getBoundingClientRect();
    const nx = e.clientX - rect.left;
    const ny = e.clientY - rect.top;
    ctx.strokeStyle = accent.primary;
    ctx.lineWidth = 3.2;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.beginPath();
    ctx.moveTo(lastPtRef.current.x, lastPtRef.current.y);
    ctx.lineTo(nx, ny);
    ctx.stroke();
    lastPtRef.current = { x: nx, y: ny };
    setInkCount((c) => c + 1);
  };

  const handlePadUp = () => {
    drawingRef.current = false;
    lastPtRef.current = null;
  };

  const clearPad = () => {
    const canvas = padRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    ctx?.clearRect(0, 0, canvas.width, canvas.height);
    setInkCount(0);
  };

  const handleTestApi = async () => {
    setTesting(true);
    setTestStatus(null);
    const res = await testCustomAiConnection(settings);
    setTesting(false);
    setTestStatus({ ok: res.ok, msg: res.message });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 p-4 backdrop-blur-md">
      <div className="relative flex max-h-[92vh] w-full max-w-3xl flex-col overflow-hidden rounded-[28px] border border-white/15 bg-white shadow-2xl dark:bg-[#14161D] animate-stagger-in">
        <AiParticleBackdrop accent="blue" variant="space-nebula" intensity="soft" />

        {/* Header + Progress Stepper */}
        <div className="relative z-10 border-b border-black/8 bg-white/85 px-6 py-4 backdrop-blur-md dark:border-white/10 dark:bg-[#14161D]/90">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div
                className="flex h-10 w-10 items-center justify-center rounded-2xl text-white shadow-md"
                style={{ background: `linear-gradient(135deg, ${accent.primary}, ${accent.secondary})` }}
              >
                <Wand2 className="h-5 w-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-base font-extrabold tracking-tight">Pro Notes Ultra Studio</h2>
                  <span className="rounded-md bg-blue-600/12 px-2 py-0.5 font-mono-num text-[10px] font-bold text-blue-600 dark:text-blue-300">
                    v9.11
                  </span>
                </div>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Craft your workspace, typography, motion, and AI study engine in 30 seconds
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onFinish}
              className="inline-flex items-center gap-1 rounded-xl px-3 py-1.5 text-xs font-semibold text-slate-500 hover:bg-black/5 dark:hover:bg-white/10"
            >
              Skip <X className="h-3.5 w-3.5" />
            </button>
          </div>

          <div className="mt-4 grid grid-cols-5 gap-2">
            {STEPS.map((st, idx) => {
              const Icon = st.icon;
              const active = idx === step;
              const done = idx < step;
              return (
                <button
                  key={st.id}
                  type="button"
                  onClick={() => setStep(idx)}
                  className={`flex items-center justify-center gap-1.5 rounded-xl px-2 py-1.5 text-[11px] font-bold transition ${
                    active
                      ? 'text-white shadow-sm'
                      : done
                        ? 'bg-emerald-500/12 text-emerald-700 dark:text-emerald-300'
                        : 'bg-black/[0.04] text-slate-500 dark:bg-white/[0.06] dark:text-slate-400'
                  }`}
                  style={active ? { backgroundColor: accent.primary } : undefined}
                >
                  {done ? <Check className="h-3.5 w-3.5 shrink-0" /> : <Icon className="h-3.5 w-3.5 shrink-0" />}
                  <span className="hidden truncate sm:inline">{st.label}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Step Body */}
        <div className="relative z-10 flex-1 overflow-y-auto p-6">
          {step === 0 && (
            <div className="space-y-5 animate-stagger-in">
              <div
                className="overflow-hidden rounded-3xl p-6 text-white shadow-xl"
                style={{ background: `linear-gradient(135deg, ${accent.primary}, #4338CA)` }}
              >
                <span className="inline-flex items-center gap-1.5 rounded-lg bg-white/15 px-2.5 py-1 text-[10px] font-bold uppercase tracking-widest">
                  <Sparkles className="h-3 w-3" /> Welcome to v9.11 Ultra
                </span>
                <h3 className="mt-3 text-2xl font-extrabold tracking-tight">
                  Your notes, slides, mind maps & AI tutor in one fluid canvas.
                </h3>
                <p className="mt-2 max-w-xl text-xs leading-relaxed text-white/85">
                  Pro Notes Ultra brings interactive checklist sticky notes, embeddable web link cards,
                  animated AI chapter slide shows, semantic notebook search, custom AI APIs, and spring-physics toolbars.
                </p>
              </div>

              <div className="grid gap-3 sm:grid-cols-3">
                {[
                  {
                    title: 'Slide Show Revision',
                    desc: 'Turn any chapter into a colorful kinetic slide deck with 3D recall cards.',
                    color: '#7C3AED',
                  },
                  {
                    title: 'Smart Sticky & Web Links',
                    desc: 'Tap checkboxes directly on stickies and pin live URL reference cards.',
                    color: '#0F7E5C',
                  },
                  {
                    title: 'Local or Custom AI',
                    desc: 'Run lightweight offline models or plug in OpenAI, Google AI, or Ollama.',
                    color: '#3D6CF4',
                  },
                ].map((card) => (
                  <div
                    key={card.title}
                    className="rounded-2xl border border-black/8 bg-slate-50/80 p-4 dark:border-white/10 dark:bg-[#1C1E27]"
                  >
                    <div
                      className="mb-2 h-2 w-10 rounded-full"
                      style={{ backgroundColor: card.color }}
                    />
                    <div className="text-sm font-bold">{card.title}</div>
                    <p className="mt-1 text-xs leading-relaxed text-slate-500 dark:text-slate-400">
                      {card.desc}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {step === 1 && (
            <div className="grid gap-5 md:grid-cols-2 animate-stagger-in">
              <div className="space-y-4">
                <div>
                  <div className="mb-1.5 text-xs font-bold uppercase tracking-wider text-slate-500">Theme</div>
                  <div className="grid grid-cols-3 gap-1.5 rounded-xl bg-black/5 p-1 dark:bg-white/8">
                    {(['SYSTEM', 'LIGHT', 'DARK'] as ThemeMode[]).map((m) => (
                      <button
                        key={m}
                        type="button"
                        onClick={() => onUpdateSettings({ themeMode: m })}
                        className={`rounded-lg py-2 text-xs font-bold transition ${
                          settings.themeMode === m
                            ? 'bg-white text-slate-900 shadow-sm dark:bg-[#2A2C34] dark:text-white'
                            : 'text-slate-500'
                        }`}
                      >
                        {m.charAt(0) + m.slice(1).toLowerCase()}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <div className="mb-1.5 text-xs font-bold uppercase tracking-wider text-slate-500">
                    Accent Color
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {(Object.keys(ACCENT_COLORS) as AccentColorId[]).map((id) => {
                      const a = ACCENT_COLORS[id];
                      return (
                        <button
                          key={id}
                          type="button"
                          onClick={() => onUpdateSettings({ accent: id })}
                          className={`h-8 w-10 rounded-xl border transition ${
                            settings.accent === id ? 'scale-105 ring-2 ring-blue-500 ring-offset-2' : 'border-black/10'
                          }`}
                          style={{ background: `linear-gradient(135deg, ${a.primary}, ${a.secondary})` }}
                          title={a.label}
                        />
                      );
                    })}
                  </div>
                </div>

                <div>
                  <div className="mb-1.5 text-xs font-bold uppercase tracking-wider text-slate-500">
                    Typography Family
                  </div>
                  <div className="grid grid-cols-2 gap-1.5">
                    {(
                      [
                        { id: 'MODERN_SANS' as AppFont, label: 'Modern Sans' },
                        { id: 'EDITORIAL_SERIF' as AppFont, label: 'Editorial Serif' },
                        { id: 'STUDIO_MONO' as AppFont, label: 'Studio Mono' },
                        { id: 'ROUNDED' as AppFont, label: 'Soft Rounded' },
                      ]
                    ).map((f) => (
                      <button
                        key={f.id}
                        type="button"
                        onClick={() => onUpdateSettings({ appFont: f.id })}
                        className={`rounded-xl border px-3 py-2 text-left text-xs font-bold transition ${
                          settings.appFont === f.id
                            ? 'border-blue-600 bg-blue-600/10 text-blue-700 dark:text-blue-300'
                            : 'border-black/10 hover:bg-black/5 dark:border-white/10'
                        }`}
                      >
                        {f.label}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <div className="mb-1.5 text-xs font-bold uppercase tracking-wider text-slate-500">
                    Motion Style
                  </div>
                  <div className="grid grid-cols-3 gap-1.5">
                    {(
                      [
                        { id: 'ULTRA_SPRING' as MotionStyle, label: 'Ultra Spring' },
                        { id: 'MINIMAL_CRISP' as MotionStyle, label: 'Minimal' },
                        { id: 'REDUCED' as MotionStyle, label: 'Reduced' },
                      ]
                    ).map((ms) => (
                      <button
                        key={ms.id}
                        type="button"
                        onClick={() => onUpdateSettings({ motionStyle: ms.id })}
                        className={`rounded-xl border px-2 py-2 text-xs font-bold transition ${
                          settings.motionStyle === ms.id
                            ? 'border-blue-600 bg-blue-600/10 text-blue-700 dark:text-blue-300'
                            : 'border-black/10 dark:border-white/10'
                        }`}
                      >
                        {ms.label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Live Preview Card */}
              <div className="flex flex-col justify-between rounded-3xl border border-black/10 bg-slate-50 p-5 dark:border-white/10 dark:bg-[#1B1D26]">
                <div>
                  <div className="flex items-center justify-between">
                    <span
                      className="rounded-lg px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-white"
                      style={{ backgroundColor: accent.primary }}
                    >
                      Live Preview
                    </span>
                    <span className="font-mono-num text-[11px] text-slate-400">
                      {settings.motionStyle.replace('_', ' ')}
                    </span>
                  </div>
                  <h4 className="mt-4 text-lg font-extrabold tracking-tight">
                    Thermodynamics · Ch. 4
                  </h4>
                  <p className="mt-1.5 text-xs leading-relaxed text-slate-600 dark:text-slate-300">
                    ΔU = Q − W. Entropy of an isolated system never decreases during spontaneous processes.
                  </p>
                  <div className="mt-4 rounded-2xl border border-black/8 bg-[#FFF3B0] p-3 text-xs text-slate-800 shadow-sm">
                    <div className="font-bold">Revision Tasks</div>
                    <div className="mt-1 space-y-1">
                      <div>☑ Review Carnot cycle efficiency</div>
                      <div>☐ Solve 3 practice problems</div>
                    </div>
                  </div>
                </div>

                <div className="mt-4 flex items-center justify-between border-t border-black/8 pt-3 text-xs dark:border-white/10">
                  <span className="font-semibold">Paper Tint</span>
                  <div className="flex gap-1.5">
                    {PAPER_TINTS.map((pt) => (
                      <button
                        key={pt.id}
                        type="button"
                        onClick={() => onUpdateSettings({ paperTint: pt.color })}
                        className={`h-6 w-6 rounded-full border ${
                          settings.paperTint === pt.color ? 'ring-2 ring-blue-600' : 'border-black/20'
                        }`}
                        style={{ backgroundColor: pt.color }}
                        title={pt.label}
                      />
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )}

          {step === 2 && (
            <div className="space-y-4 animate-stagger-in">
              <div className="grid gap-3 sm:grid-cols-2">
                <div className="rounded-2xl border border-black/10 p-4 dark:border-white/10">
                  <div className="text-sm font-bold">Page Flow Direction</div>
                  <p className="mb-3 text-xs text-slate-500">Applies to editor & side-by-side Split View</p>
                  <div className="grid grid-cols-2 gap-2">
                    {(['VERTICAL', 'HORIZONTAL'] as const).map((dir) => (
                      <button
                        key={dir}
                        type="button"
                        onClick={() => onUpdateSettings({ scroll: dir })}
                        className={`rounded-xl border py-2 text-xs font-bold transition ${
                          settings.scroll === dir
                            ? 'border-blue-600 bg-blue-600/12 text-blue-700 dark:text-blue-300'
                            : 'border-black/10 dark:border-white/10'
                        }`}
                      >
                        {dir === 'VERTICAL' ? 'Vertical Scroll' : 'Horizontal Pages'}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="rounded-2xl border border-black/10 p-4 dark:border-white/10">
                  <div className="text-sm font-bold">Default Paper Template</div>
                  <p className="mb-3 text-xs text-slate-500">Used when creating new quick notebooks</p>
                  <select
                    value={settings.defaultPaper}
                    onChange={(e) => onUpdateSettings({ defaultPaper: e.target.value as AppSettings['defaultPaper'] })}
                    className="w-full rounded-xl border border-black/12 bg-transparent px-3 py-2 text-xs font-semibold dark:border-white/15"
                  >
                    {PAPER_STYLES.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="divide-y divide-black/6 overflow-hidden rounded-2xl border border-black/10 dark:divide-white/8 dark:border-white/10">
                {[
                  {
                    title: 'Stylus Hover Tooltips',
                    sub: 'Show tool name badge smoothly when hovering over dock icons',
                    checked: settings.showHoverTooltips,
                    onChange: (v: boolean) => onUpdateSettings({ showHoverTooltips: v }),
                  },
                  {
                    title: 'Live Scribble-to-Erase',
                    sub: 'Crumble ink instantly mid-stroke when scrubbing zig-zags over words',
                    checked: settings.liveScribbleErase,
                    onChange: (v: boolean) => onUpdateSettings({ liveScribbleErase: v }),
                  },
                  {
                    title: 'Hold to Snap Shapes',
                    sub: 'Pause briefly at stroke end to snap clean geometric shapes',
                    checked: settings.holdToShape,
                    onChange: (v: boolean) => onUpdateSettings({ holdToShape: v }),
                  },
                ].map((item) => (
                  <div
                    key={item.title}
                    onClick={() => item.onChange(!item.checked)}
                    className="flex cursor-pointer items-center justify-between px-4 py-3 hover:bg-black/[0.02] dark:hover:bg-white/[0.02]"
                  >
                    <div>
                      <div className="text-sm font-semibold">{item.title}</div>
                      <div className="text-xs text-slate-500">{item.sub}</div>
                    </div>
                    <span
                      className={`relative h-6 w-11 shrink-0 rounded-full transition ${
                        item.checked ? 'bg-blue-600' : 'bg-slate-300 dark:bg-slate-700'
                      }`}
                    >
                      <span
                        className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform ${
                          item.checked ? 'left-[22px]' : 'left-0.5'
                        }`}
                      />
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {step === 3 && (
            <div className="space-y-4 animate-stagger-in">
              {/* Recommended Offline Model */}
              <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-violet-500/25 bg-violet-50/70 p-4 dark:bg-violet-950/25">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold">{recommendedModel.name}</span>
                    <span className="rounded-md bg-violet-600/15 px-2 py-0.5 text-[10px] font-bold text-violet-700 dark:text-violet-300">
                      Recommended · {recommendedModel.sizeMb} MB
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-slate-600 dark:text-slate-300">
                    Boosts Semantic Search, Summaries, Quizzes, and Chapter Slide Shows offline.
                  </p>
                </div>
                {recState?.status === 'READY' ? (
                  <span className="inline-flex items-center gap-1 rounded-xl bg-emerald-600 px-3 py-1.5 text-xs font-bold text-white">
                    <Check className="h-3.5 w-3.5" /> Ready
                  </span>
                ) : recState?.status === 'DOWNLOADING' ? (
                  <span className="font-mono-num rounded-xl bg-violet-600/15 px-3 py-1.5 text-xs font-bold text-violet-700 dark:text-violet-300">
                    {Math.round(recState.progress * 100)}%…
                  </span>
                ) : (
                  <button
                    type="button"
                    onClick={() => onDownloadModel(recommendedModel.id)}
                    className="inline-flex items-center gap-1.5 rounded-xl bg-violet-600 px-3.5 py-2 text-xs font-bold text-white hover:bg-violet-700"
                  >
                    <Download className="h-3.5 w-3.5" /> Download Free
                  </button>
                )}
              </div>

              {/* Optional Custom AI API */}
              <div className="space-y-3 rounded-2xl border border-black/10 p-4 dark:border-white/10">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm font-bold">Custom AI API (Optional)</div>
                    <div className="text-xs text-slate-500">
                      Bring OpenAI-compatible, Google AI, or local Ollama endpoints
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => onUpdateSettings({ customAiEnabled: !settings.customAiEnabled })}
                    className={`rounded-xl px-3 py-1.5 text-xs font-bold transition ${
                      settings.customAiEnabled
                        ? 'bg-emerald-600 text-white'
                        : 'bg-black/5 text-slate-600 dark:bg-white/10 dark:text-slate-300'
                    }`}
                  >
                    {settings.customAiEnabled ? 'Enabled' : 'Disabled'}
                  </button>
                </div>

                {settings.customAiEnabled && (
                  <div className="space-y-2.5 pt-1">
                    <div className="grid grid-cols-3 gap-1.5">
                      {(
                        [
                          { id: 'OPENAI_COMPATIBLE' as CustomAiProvider, label: 'OpenAI / Groq' },
                          { id: 'GEMINI' as CustomAiProvider, label: 'Google AI REST' },
                          { id: 'OLLAMA' as CustomAiProvider, label: 'Ollama Local' },
                        ]
                      ).map((p) => (
                        <button
                          key={p.id}
                          type="button"
                          onClick={() =>
                            onUpdateSettings({
                              customAiProvider: p.id,
                              customAiBaseUrl:
                                p.id === 'GEMINI'
                                  ? 'https://generativelanguage.googleapis.com/v1beta'
                                  : p.id === 'OLLAMA'
                                    ? 'http://localhost:11434'
                                    : 'https://api.openai.com/v1',
                              customAiModel:
                                p.id === 'GEMINI'
                                  ? 'gemini-2.5-flash'
                                  : p.id === 'OLLAMA'
                                    ? 'llama3.2'
                                    : 'gpt-4o-mini',
                            })
                          }
                          className={`rounded-xl py-1.5 text-xs font-bold transition ${
                            settings.customAiProvider === p.id
                              ? 'bg-blue-600 text-white'
                              : 'bg-black/5 text-slate-600 dark:bg-white/10 dark:text-slate-300'
                          }`}
                        >
                          {p.label}
                        </button>
                      ))}
                    </div>

                    <div className="grid gap-2 sm:grid-cols-2">
                      <input
                        type="text"
                        value={settings.customAiBaseUrl}
                        onChange={(e) => onUpdateSettings({ customAiBaseUrl: e.target.value })}
                        placeholder="Base URL"
                        className="rounded-xl border border-black/12 bg-transparent px-3 py-1.5 text-xs outline-none focus:border-blue-500 dark:border-white/15"
                      />
                      <input
                        type="text"
                        value={settings.customAiModel}
                        onChange={(e) => onUpdateSettings({ customAiModel: e.target.value })}
                        placeholder="Model ID (e.g. gpt-4o-mini)"
                        className="rounded-xl border border-black/12 bg-transparent px-3 py-1.5 text-xs outline-none focus:border-blue-500 dark:border-white/15"
                      />
                    </div>
                    <div className="flex gap-2">
                      <input
                        type="password"
                        value={settings.customAiApiKey}
                        onChange={(e) => onUpdateSettings({ customAiApiKey: e.target.value })}
                        placeholder="API Key (stored locally in browser only)"
                        className="flex-1 rounded-xl border border-black/12 bg-transparent px-3 py-1.5 text-xs outline-none focus:border-blue-500 dark:border-white/15"
                      />
                      <button
                        type="button"
                        disabled={testing}
                        onClick={handleTestApi}
                        className="inline-flex items-center gap-1 rounded-xl bg-blue-600 px-3 py-1.5 text-xs font-bold text-white hover:bg-blue-700 disabled:opacity-50"
                      >
                        <Zap className="h-3.5 w-3.5" /> {testing ? 'Testing…' : 'Test'}
                      </button>
                    </div>
                    {testStatus && (
                      <div
                        className={`rounded-xl px-3 py-2 text-xs font-semibold ${
                          testStatus.ok
                            ? 'bg-emerald-500/12 text-emerald-700 dark:text-emerald-300'
                            : 'bg-red-500/12 text-red-600 dark:text-red-300'
                        }`}
                      >
                        {testStatus.msg}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          )}

          {step === 4 && (
            <div className="space-y-4 animate-stagger-in">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold">Pen & Ink Feel Check</h3>
                  <p className="text-xs text-slate-500">
                    Sketch below to test latency and smooth ink curve response before jumping in
                  </p>
                </div>
                <button
                  type="button"
                  onClick={clearPad}
                  className="rounded-xl bg-black/5 px-3 py-1.5 text-xs font-semibold hover:bg-black/10 dark:bg-white/10"
                >
                  Clear Pad
                </button>
              </div>
              <div
                className="overflow-hidden rounded-2xl border border-black/10 shadow-inner dark:border-white/12"
                style={{ backgroundColor: settings.paperTint || '#FFFFFF' }}
              >
                <canvas
                  ref={padRef}
                  width={680}
                  height={220}
                  onPointerDown={handlePadDown}
                  onPointerMove={handlePadMove}
                  onPointerUp={handlePadUp}
                  onPointerCancel={handlePadUp}
                  className="h-52 w-full cursor-crosshair touch-none"
                />
              </div>
              <div className="flex items-center justify-between text-xs text-slate-500">
                <span>
                  {inkCount > 0 ? 'Smooth ink verified! Tip: drag toolbar icons to reorder anytime.' : 'Draw a quick loop or signature above.'}
                </span>
                <span className="font-mono-num font-semibold text-emerald-600 dark:text-emerald-400">
                  Ultra Studio Ready
                </span>
              </div>
            </div>
          )}
        </div>

        {/* Footer Nav */}
        <div className="relative z-10 flex items-center justify-between border-t border-black/8 bg-white/85 px-6 py-4 backdrop-blur-md dark:border-white/10 dark:bg-[#14161D]/90">
          <button
            type="button"
            disabled={step === 0}
            onClick={() => setStep((s) => Math.max(0, s - 1))}
            className="inline-flex items-center gap-1.5 rounded-xl px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-black/5 disabled:opacity-35 dark:text-slate-300"
          >
            <ArrowLeft className="h-3.5 w-3.5" /> Back
          </button>
          {step < STEPS.length - 1 ? (
            <button
              type="button"
              onClick={() => setStep((s) => Math.min(STEPS.length - 1, s + 1))}
              className="inline-flex items-center gap-1.5 rounded-xl px-5 py-2 text-xs font-bold text-white shadow-sm transition hover:opacity-95"
              style={{ backgroundColor: accent.primary }}
            >
              Next <ArrowRight className="h-3.5 w-3.5" />
            </button>
          ) : (
            <button
              type="button"
              onClick={onFinish}
              className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-6 py-2 text-xs font-bold text-white shadow-md hover:bg-emerald-700"
            >
              Launch Pro Notes Ultra <Sparkles className="h-3.5 w-3.5" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
