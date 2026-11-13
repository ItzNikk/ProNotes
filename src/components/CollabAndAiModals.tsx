import React, { useEffect, useRef, useState } from 'react';
import {
  Check,
  Copy,
  ImagePlus,
  Loader2,
  MessageSquare,
  Mic,
  Pause,
  Play,
  Plus,
  Send,
  Sparkles,
  Square,
  Users,
  Wand2,
  X,
  Command,
  Clock,
  Download,
} from 'lucide-react';
import {
  AiImageCategory,
  AppSettings,
  CollabChatMessage,
  CollabMember,
  CollabRoom,
  LectureRecording,
  LocalModelState,
  NoteChatMessage,
  Page,
} from '../types/models';
import {
  generateAiAssetImage,
  IMAGE_MODELS_CATALOG,
  LOCAL_MODELS_CATALOG,
  runLocalNotesPdfChat,
  structureLectureRecording,
} from '../utils/localAi';
import { AiParticleBackdrop } from './AiVisuals';

// ================================================================
// 1. AI Chat About Notes & PDFs Using Downloaded / Local Model
// ================================================================

export const AiNotesChatModal: React.FC<{
  open: boolean;
  notebookTitle: string;
  pages: Page[];
  settings: AppSettings;
  modelStates: Record<string, LocalModelState>;
  onClose: () => void;
  onInsertReplyToPage: (text: string) => void;
  onOpenLocalModels: () => void;
}> = ({
  open,
  notebookTitle,
  pages,
  settings,
  modelStates,
  onClose,
  onInsertReplyToPage,
  onOpenLocalModels,
}) => {
  const [messages, setMessages] = useState<NoteChatMessage[]>([
    {
      id: 'welcome',
      role: 'assistant',
      text: `Hi! Ask me anything about "${notebookTitle}" — including your typed notes, sticky cards, mind map nodes, and imported PDF pages.`,
      createdAt: Date.now(),
    },
  ]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, busy, open]);

  if (!open) return null;

  const activeModel = LOCAL_MODELS_CATALOG.find(
    (m) => m.id === settings.activeLocalModelId && modelStates[m.id]?.status === 'READY'
  );
  const pdfCount = pages.filter((p) => Boolean(p.pdfText && p.pdfText.trim().length > 0)).length;

  const askQuestion = async (qText: string) => {
    const clean = qText.trim();
    if (!clean || busy) return;
    const userMsg: NoteChatMessage = {
      id: `u-${Date.now()}`,
      role: 'user',
      text: clean,
      createdAt: Date.now(),
    };
    const nextHist = [...messages, userMsg];
    setMessages(nextHist);
    setInput('');
    setBusy(true);
    try {
      const res = await runLocalNotesPdfChat(
        nextHist,
        clean,
        pages,
        notebookTitle,
        activeModel?.id || settings.activeLocalModelId,
        settings
      );
      setMessages((prev) => [
        ...prev,
        {
          id: `a-${Date.now()}`,
          role: 'assistant',
          text: res.reply,
          citations: res.citations,
          engineLabel: res.engineLabel,
          createdAt: Date.now(),
        },
      ]);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 backdrop-blur-sm">
      <div className="relative flex h-[84vh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/12 dark:bg-[#191A1F] animate-stagger-in">
        <AiParticleBackdrop accent="blue" variant="ai-wave" intensity={busy ? 'active' : 'soft'} />

        {/* Modern Studio Header */}
        <div className="relative z-10 flex items-center justify-between border-b border-black/8 bg-white/85 px-5 py-4 backdrop-blur-md dark:border-white/10 dark:bg-[#1E2028]/90">
          <div className="flex items-center gap-3 min-w-0">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-gradient-to-tr from-[#4285F4] via-[#9B72CB] to-[#D96570] text-white shadow-md">
              <Sparkles className="h-5 w-5" />
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <h2 className="truncate bg-gradient-to-r from-[#4285F4] via-[#9B72CB] to-[#D96570] bg-clip-text text-base font-extrabold text-transparent">
                  Study Assistant
                </h2>
                <span className="rounded-full bg-[#9B72CB]/15 px-2 py-0.5 text-[10px] font-bold text-[#9B72CB]">
                  LIVE
                </span>
              </div>
              <div className="flex flex-wrap items-center gap-2 text-[11px] text-slate-500 dark:text-slate-400">
                <span>
                  {settings.customAiEnabled
                    ? `Custom AI (${settings.customAiModel})`
                    : activeModel
                      ? `${activeModel.name} · Studio Live`
                      : 'Pro Study Engine'}
                </span>
                <span className="rounded-full bg-[#4285F4]/12 px-2 py-0.5 font-semibold text-[#4285F4]">
                  {pages.length} page{pages.length === 1 ? '' : 's'} indexed
                </span>
                {pdfCount > 0 && (
                  <span className="rounded-full bg-[#D96570]/12 px-2 py-0.5 font-semibold text-[#D96570]">
                    {pdfCount} PDF page{pdfCount === 1 ? '' : 's'}
                  </span>
                )}
              </div>
            </div>
          </div>
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={onOpenLocalModels}
              className="ios-stretch rounded-full border border-black/10 px-3 py-1.5 text-[11px] font-semibold hover:bg-black/5 dark:border-white/15 dark:hover:bg-white/10"
            >
              Models
            </button>
            <button
              type="button"
              onClick={onClose}
              className="ios-stretch rounded-full p-1.5 text-slate-500 hover:bg-black/5 dark:hover:bg-white/10"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Messages */}
        <div className="relative z-10 flex-1 space-y-3 overflow-y-auto p-5">
          {messages.length <= 1 && (
            <div className="mb-3 rounded-2xl border border-[#9B72CB]/20 bg-gradient-to-br from-[#4285F4]/8 via-[#9B72CB]/8 to-[#D96570]/8 p-4">
              <div className="bg-gradient-to-r from-[#4285F4] via-[#9B72CB] to-[#D96570] bg-clip-text text-xl font-extrabold text-transparent">
                Hello, Scholar.
              </div>
              <div className="mt-0.5 text-xs text-slate-600 dark:text-slate-300">
                Ask any question about &ldquo;{notebookTitle}&rdquo; or any study topic — I answer your exact question directly.
              </div>
            </div>
          )}
          {messages.map((m) => (
            <div
              key={m.id}
              className={`flex flex-col ${m.role === 'user' ? 'items-end' : 'items-start'}`}
            >
              <div
                className={`max-w-[88%] rounded-2xl px-4 py-3 text-xs leading-relaxed shadow-xs whitespace-pre-wrap ${
                  m.role === 'user'
                    ? 'bg-blue-600 text-white'
                    : 'border border-black/8 bg-slate-50/90 text-slate-800 dark:border-white/10 dark:bg-[#23252E]/95 dark:text-slate-100'
                }`}
              >
                {m.text}
                {m.role === 'assistant' && m.citations && m.citations.length > 0 && (
                  <div className="mt-2 flex flex-wrap gap-1">
                    {m.citations.map((c) => (
                      <span
                        key={c}
                        className="rounded-md bg-blue-500/12 px-1.5 py-0.5 text-[10px] font-bold text-blue-700 dark:text-blue-300"
                      >
                        {c}
                      </span>
                    ))}
                  </div>
                )}
              </div>
              {m.role === 'assistant' && m.id !== 'welcome' && (
                <div className="mt-1 flex items-center gap-2 px-1">
                  {m.engineLabel && (
                    <span className="text-[10px] text-slate-400">{m.engineLabel}</span>
                  )}
                  <button
                    type="button"
                    onClick={() => {
                      navigator.clipboard?.writeText(m.text);
                      setCopiedId(m.id);
                      window.setTimeout(() => setCopiedId(null), 1500);
                    }}
                    className="ios-stretch inline-flex items-center gap-1 rounded-lg px-2 py-0.5 text-[10px] font-semibold text-slate-500 hover:bg-black/5 dark:hover:bg-white/10"
                  >
                    {copiedId === m.id ? <Check className="h-3 w-3 text-emerald-500" /> : <Copy className="h-3 w-3" />}
                    {copiedId === m.id ? 'Copied' : 'Copy'}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      onInsertReplyToPage(m.text);
                      onClose();
                    }}
                    className="ios-stretch inline-flex items-center gap-1 rounded-lg bg-blue-600/10 px-2 py-0.5 text-[10px] font-bold text-blue-600 hover:bg-blue-600/20 dark:text-blue-300"
                  >
                    <Plus className="h-3 w-3" /> Add to Page
                  </button>
                </div>
              )}
            </div>
          ))}
          {busy && (
            <div className="flex items-center gap-2 text-xs font-semibold text-violet-600 dark:text-violet-300">
              <Loader2 className="h-4 w-4 animate-spin" /> Reading notes &amp; PDF pages...
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        {/* Quick prompts + Composer */}
        <div className="relative z-10 space-y-2 border-t border-black/8 bg-white/85 px-5 py-3.5 backdrop-blur-md dark:border-white/10 dark:bg-[#1E2028]/90">
          <div className="flex flex-wrap gap-1.5">
            {['Summarize notebook', 'Extract key formulas', 'Quiz me on this chapter', 'List main definitions'].map((qp) => (
              <button
                key={qp}
                type="button"
                onClick={() => void askQuestion(qp)}
                className="ios-stretch rounded-xl border border-black/10 bg-black/[0.03] px-2.5 py-1 text-[11px] font-semibold text-slate-700 hover:bg-blue-500/10 hover:text-blue-600 dark:border-white/10 dark:bg-white/[0.05] dark:text-slate-200"
              >
                {qp}
              </button>
            ))}
          </div>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              void askQuestion(input);
            }}
            className="flex items-center gap-2"
          >
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Ask a question about these notes or PDF..."
              className="flex-1 rounded-2xl border border-black/12 bg-white px-4 py-2.5 text-xs outline-none focus:border-blue-500 dark:border-white/15 dark:bg-[#252731]"
            />
            <button
              type="submit"
              disabled={!input.trim() || busy}
              className="ios-stretch inline-flex items-center gap-1.5 rounded-2xl bg-blue-600 px-4 py-2.5 text-xs font-semibold text-white shadow-sm hover:bg-blue-700 disabled:opacity-45"
            >
              <Send className="h-3.5 w-3.5" /> Ask
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

// ================================================================
// 2. AI Studio Image Generator (Stickers, Covers & Page Templates)
// ================================================================

export const AiAssetGeneratorModal: React.FC<{
  open: boolean;
  initialCategory?: AiImageCategory;
  settings: AppSettings;
  modelStates: Record<string, LocalModelState>;
  onSelectImageModel: (id: string) => void;
  onOpenModelsHub?: () => void;
  onApplyAsset: (category: AiImageCategory, dataUrl: string) => void;
  onClose: () => void;
}> = ({
  open,
  initialCategory = 'sticker',
  settings,
  modelStates,
  onSelectImageModel,
  onOpenModelsHub,
  onApplyAsset,
  onClose,
}) => {
  const [category, setCategory] = useState<AiImageCategory>(initialCategory);
  const [creativeStyle, setCreativeStyle] = useState<string>('3D Cartoon');
  const [liteMode, setLiteMode] = useState<boolean>(true);
  const [prompt, setPrompt] = useState('');
  const [busy, setBusy] = useState(false);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [engineUsed, setEngineUsed] = useState<string>('');

  useEffect(() => {
    if (open) {
      setCategory(initialCategory);
      setPreviewUrl(null);
    }
  }, [open, initialCategory]);

  if (!open) return null;

  const activeImgModel = settings.activeImageModelId || IMAGE_MODELS_CATALOG[0].id;
  const creativeStyles = [
    { id: '3D Cartoon', emoji: '🧸' },
    { id: 'Watercolor', emoji: '🎨' },
    { id: 'Sketch to Art', emoji: '✏️' },
    { id: 'Pop Art', emoji: '⚡' },
    { id: 'Anime Studio', emoji: '✨' },
    { id: 'Minimal Vector', emoji: '📐' },
  ];
  const presetsByCategory: Record<AiImageCategory, string[]> = {
    sticker: ['Cute Brain Bulb', 'Physics Atom Spark', 'Gold Exam Star', 'Coffee Study Cat', 'DNA Helix Badge'],
    cover: ['Deep Space Nebula', 'Botanical Emerald Journal', 'Minimalist Bauhaus Grid', 'Sunset Gradient Leather'],
    template: ['Cornell Lecture Grid', 'Organic Chemistry Sheet', 'Weekly Exam Planner', 'Two-Column Comparison'],
  };

  const handleGenerate = async (customPrompt?: string, customStyle?: string) => {
    const targetPrompt = (customPrompt ?? prompt).trim() || presetsByCategory[category][0];
    const targetStyle = customStyle ?? creativeStyle;
    if (busy) return;
    setPrompt(targetPrompt);
    setBusy(true);
    try {
      const res = await generateAiAssetImage(targetPrompt, category, activeImgModel, targetStyle, liteMode);
      setPreviewUrl(res.dataUrl);
      setEngineUsed(res.engineLabel);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 backdrop-blur-sm">
      <div className="w-full max-w-2xl overflow-hidden rounded-3xl border border-indigo-500/30 bg-white shadow-2xl dark:border-indigo-400/25 dark:bg-[#191A1F] animate-stagger-in">
        {/* Creative Studio Iridescent Header */}
        <div className="flex items-center justify-between bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-500 px-6 py-4 text-white">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-white/20 text-white shadow-sm backdrop-blur-sm">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-base font-extrabold tracking-tight">Creative Studio</h2>
              <p className="text-[11px] text-white/90">
                Drawing Assist · Lite Image Synthesis ({creativeStyle})
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setLiteMode((v) => !v)}
              className={`ios-stretch rounded-full px-3 py-1 text-[11px] font-bold transition ${
                liteMode
                  ? 'bg-white text-indigo-700 shadow-xs'
                  : 'bg-white/20 text-white'
              }`}
            >
              ⚡ {liteMode ? 'Lite Mode ON' : 'Lite Mode OFF'}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="ios-stretch rounded-full p-1.5 text-white/90 hover:bg-white/15"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        <div className="grid gap-5 p-6 md:grid-cols-12">
          {/* Controls */}
          <div className="space-y-4 md:col-span-7">
            {/* Samsung Creative Studio Style Carousel */}
            <div>
              <label className="mb-1.5 block text-[10px] font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
                Creative Style (Drawing Assist)
              </label>
              <div className="flex flex-wrap gap-1.5">
                {creativeStyles.map((st) => (
                  <button
                    key={st.id}
                    type="button"
                    onClick={() => {
                      setCreativeStyle(st.id);
                      void handleGenerate(prompt || presetsByCategory[category][0], st.id);
                    }}
                    className={`ios-stretch inline-flex items-center gap-1 rounded-full px-3 py-1.5 text-[11px] font-bold transition ${
                      creativeStyle === st.id
                        ? 'bg-indigo-600 text-white shadow-xs'
                        : 'bg-black/5 text-slate-700 hover:bg-indigo-500/10 dark:bg-white/10 dark:text-slate-200'
                    }`}
                  >
                    <span>{st.emoji}</span>
                    <span>{st.id}</span>
                  </button>
                ))}
              </div>
            </div>

            <div className="grid grid-cols-3 gap-1.5 rounded-xl bg-black/5 p-1 dark:bg-white/8">
              {(
                [
                  { id: 'sticker', label: 'Sticker' },
                  { id: 'cover', label: 'Cover' },
                  { id: 'template', label: 'Template' },
                ] as { id: AiImageCategory; label: string }[]
              ).map((c) => (
                <button
                  key={c.id}
                  type="button"
                  onClick={() => {
                    setCategory(c.id);
                    setPreviewUrl(null);
                  }}
                  className={`ios-stretch rounded-lg py-1.5 text-xs font-semibold transition ${
                    category === c.id
                      ? 'bg-white text-slate-900 shadow-sm dark:bg-[#272A34] dark:text-white'
                      : 'text-slate-500'
                  }`}
                >
                  {c.label}
                </button>
              ))}
            </div>

            <div>
              <div className="mb-1.5 flex items-center justify-between">
                <label className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
                  Lightweight Image Model
                </label>
                {onOpenModelsHub && (
                  <button
                    type="button"
                    onClick={onOpenModelsHub}
                    className="text-[11px] font-semibold text-blue-600 hover:underline dark:text-blue-400"
                  >
                    Manage Downloads
                  </button>
                )}
              </div>
              <div className="grid grid-cols-2 gap-1.5">
                {IMAGE_MODELS_CATALOG.map((m) => {
                  const ready = modelStates[m.id]?.status === 'READY';
                  const sel = activeImgModel === m.id;
                  return (
                    <button
                      key={m.id}
                      type="button"
                      onClick={() => onSelectImageModel(m.id)}
                      className={`ios-stretch rounded-xl border p-2 text-left transition ${
                        sel
                          ? 'border-violet-600 bg-violet-500/10 text-violet-700 dark:text-violet-300'
                          : 'border-black/10 hover:bg-black/[0.02] dark:border-white/10'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-1">
                        <span className="truncate text-[11px] font-bold">{m.name}</span>
                        {ready && <Check className="h-3 w-3 shrink-0 text-emerald-500" />}
                      </div>
                      <div className="text-[10px] text-slate-500">{m.sizeMb} MB · {m.steps}</div>
                    </button>
                  );
                })}
              </div>
            </div>

            <div>
              <label className="mb-1 block text-[11px] font-bold uppercase tracking-wider text-slate-500">
                Describe your {category}
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={prompt}
                  onChange={(e) => setPrompt(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') void handleGenerate();
                  }}
                  placeholder={`e.g. ${presetsByCategory[category][0]}`}
                  className="flex-1 rounded-xl border border-black/12 bg-white px-3.5 py-2 text-xs outline-none focus:border-violet-500 dark:border-white/15 dark:bg-[#22242C]"
                />
                <button
                  type="button"
                  disabled={!prompt.trim() || busy}
                  onClick={() => void handleGenerate()}
                  className="ios-stretch inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-4 py-2 text-xs font-semibold text-white shadow-sm disabled:opacity-45"
                >
                  {busy ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Wand2 className="h-3.5 w-3.5" />}
                  {busy ? 'Generating' : 'Generate'}
                </button>
              </div>
              <div className="mt-2 flex flex-wrap gap-1.5">
                {presetsByCategory[category].map((pr) => (
                  <button
                    key={pr}
                    type="button"
                    onClick={() => void handleGenerate(pr)}
                    className="ios-stretch rounded-lg bg-black/5 px-2 py-1 text-[10px] font-semibold text-slate-600 hover:bg-violet-500/12 hover:text-violet-600 dark:bg-white/8 dark:text-slate-300"
                  >
                    {pr}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Creative Studio Preview Card */}
          <div className="flex flex-col items-center justify-between rounded-3xl border border-indigo-500/20 bg-slate-50/80 p-4.5 shadow-sm dark:border-indigo-400/15 dark:bg-[#20222A] md:col-span-5">
            <div
              className={`relative flex h-64 w-full items-center justify-center overflow-hidden rounded-2xl border transition-all duration-300 ${
                busy
                  ? 'border-indigo-400/40 bg-gradient-to-tr from-indigo-600 via-purple-600 to-pink-500 shadow-xl'
                  : 'border-black/10 bg-white/70 dark:border-white/10 dark:bg-[#17181E]'
              }`}
            >
              {busy ? (
                <div className="relative z-10 flex flex-col items-center gap-3 p-4 text-center text-white">
                  <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/20 shadow-lg backdrop-blur-md animate-bounce">
                    <Sparkles className="h-6 w-6 text-white" />
                  </div>
                  <div>
                    <div className="text-sm font-extrabold tracking-tight">
                      Generating {category}…
                    </div>
                    <div className="mt-0.5 max-w-[220px] truncate text-[11px] text-white/85">
                      "{prompt || presetsByCategory[category][0]}"
                    </div>
                  </div>
                  <div className="flex items-center gap-1.5 rounded-full bg-white/20 px-3 py-1 text-[10px] font-bold backdrop-blur-sm">
                    <span className="h-2 w-2 rounded-full bg-emerald-400 animate-ping" />
                    <span>{creativeStyle} · Creative Studio</span>
                  </div>
                  <div className="h-1.5 w-36 overflow-hidden rounded-full bg-white/25">
                    <div className="h-full w-2/3 rounded-full bg-white animate-pulse" />
                  </div>
                </div>
              ) : previewUrl ? (
                <div className="relative flex h-full w-full items-center justify-center p-3">
                  <img
                    src={previewUrl}
                    alt="AI generated preview"
                    className="max-h-full max-w-full rounded-xl object-contain shadow-md transition-all duration-300 hover:scale-[1.02]"
                  />
                  <div className="absolute bottom-2 right-2 rounded-full bg-black/60 px-2.5 py-0.5 text-[10px] font-bold text-white backdrop-blur-md">
                    {creativeStyle} · {liteMode ? '⚡ Turbo' : 'HD'}
                  </div>
                </div>
              ) : (
                <div className="flex flex-col items-center gap-2 px-4 text-center text-xs text-slate-400">
                  <Wand2 className="h-6 w-6 text-indigo-400/70" />
                  <span>Enter a prompt to synthesize a prompt-driven {category}</span>
                </div>
              )}
            </div>

            {engineUsed && (
              <div className="mt-2 text-[10px] font-medium text-slate-500">
                Engine: {engineUsed}
              </div>
            )}

            <button
              type="button"
              disabled={!previewUrl || busy}
              onClick={() => {
                if (!previewUrl) return;
                onApplyAsset(category, previewUrl);
                onClose();
              }}
              className="ios-stretch mt-3 flex w-full items-center justify-center gap-1.5 rounded-xl bg-blue-600 py-2.5 text-xs font-semibold text-white shadow-sm hover:bg-blue-700 disabled:opacity-40"
            >
              <Check className="h-3.5 w-3.5" />
              {category === 'sticker'
                ? 'Insert Sticker to Page'
                : category === 'cover'
                  ? 'Apply Cover'
                  : 'Apply Template to Page'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// ================================================================
// 3. AI Recording & Structured Lecture Notes Studio
// ================================================================

export const AiRecordingModal: React.FC<{
  open: boolean;
  notebookTitle: string;
  settings: AppSettings;
  onClose: () => void;
  onInsertNotesToPage: (formattedNotes: string) => void;
}> = ({ open, notebookTitle, settings, onClose, onInsertNotesToPage }) => {
  const [recordingState, setRecordingState] = useState<'idle' | 'recording' | 'paused' | 'done'>('idle');
  const [elapsedSec, setElapsedSec] = useState(0);
  const [levels, setLevels] = useState<number[]>(new Array(18).fill(8));
  const [transcript, setTranscript] = useState('');
  const [recTitle, setRecTitle] = useState(`${notebookTitle} Lecture`);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [result, setResult] = useState<LectureRecording | null>(null);
  const [analyzing, setAnalyzing] = useState(false);

  const mediaRecRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const streamRef = useRef<MediaStream | null>(null);
  const rafRef = useRef<number>(0);
  const timerRef = useRef<number>(0);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const speechRef = useRef<any>(null);

  const cleanupStreams = () => {
    window.cancelAnimationFrame(rafRef.current);
    window.clearInterval(timerRef.current);
    try {
      speechRef.current?.stop();
    } catch {
      // ignore
    }
    streamRef.current?.getTracks().forEach((t) => t.stop());
    streamRef.current = null;
  };

  useEffect(() => {
    if (!open) {
      cleanupStreams();
      setRecordingState('idle');
    }
    return cleanupStreams;
  }, [open]);

  if (!open) return null;

  const startRecording = async () => {
    setErrorMsg(null);
    setResult(null);
    setAudioUrl(null);
    chunksRef.current = [];
    setElapsedSec(0);

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;

      // Live audio waveform analyzer
      const audioCtx = new AudioContext();
      const source = audioCtx.createMediaStreamSource(stream);
      const analyser = audioCtx.createAnalyser();
      analyser.fftSize = 64;
      source.connect(analyser);
      const data = new Uint8Array(analyser.frequencyBinCount);
      const tickWave = () => {
        analyser.getByteFrequencyData(data);
        const bars = Array.from({ length: 18 }, (_, i) =>
          Math.max(6, Math.round((data[i % data.length] / 255) * 44))
        );
        setLevels(bars);
        rafRef.current = window.requestAnimationFrame(tickWave);
      };
      tickWave();

      const rec = new MediaRecorder(stream);
      mediaRecRef.current = rec;
      rec.ondataavailable = (ev) => {
        if (ev.data.size > 0) chunksRef.current.push(ev.data);
      };
      rec.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: 'audio/webm' });
        const url = URL.createObjectURL(blob);
        setAudioUrl(url);
        cleanupStreams();
      };
      rec.start(400);
      setRecordingState('recording');

      timerRef.current = window.setInterval(() => {
        setElapsedSec((s) => s + 1);
      }, 1000);

      // Optional live Web SpeechRecognition transcript when supported
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
      if (SR) {
        const sr = new SR();
        sr.continuous = true;
        sr.interimResults = true;
        sr.lang = 'en-US';
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        sr.onresult = (e: any) => {
          let finalTxt = '';
          for (let i = 0; i < e.results.length; i++) {
            finalTxt += e.results[i][0].transcript + ' ';
          }
          if (finalTxt.trim()) setTranscript(finalTxt.trim());
        };
        sr.onerror = () => {};
        sr.start();
        speechRef.current = sr;
      }
    } catch {
      setErrorMsg('Microphone permission unavailable in this preview. Type or paste spoken notes below to generate AI lecture structure!');
      setRecordingState('done');
    }
  };

  const pauseResume = () => {
    const rec = mediaRecRef.current;
    if (!rec) return;
    if (recordingState === 'recording') {
      rec.pause();
      window.clearInterval(timerRef.current);
      setRecordingState('paused');
    } else if (recordingState === 'paused') {
      rec.resume();
      timerRef.current = window.setInterval(() => setElapsedSec((s) => s + 1), 1000);
      setRecordingState('recording');
    }
  };

  const stopRecording = () => {
    try {
      mediaRecRef.current?.stop();
    } catch {
      cleanupStreams();
    }
    setRecordingState('done');
  };

  const synthesizeNotes = async () => {
    setAnalyzing(true);
    try {
      const res = await structureLectureRecording(
        transcript,
        recTitle,
        elapsedSec,
        settings.activeLocalModelId,
        settings
      );
      setResult({
        id: `rec-${Date.now()}`,
        title: recTitle,
        durationSec: elapsedSec,
        transcript,
        summaryPoints: res.summaryPoints,
        actionItems: res.actionItems,
        createdAt: Date.now(),
      });
    } finally {
      setAnalyzing(false);
    }
  };

  const mins = String(Math.floor(elapsedSec / 60)).padStart(2, '0');
  const secs = String(elapsedSec % 60).padStart(2, '0');

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 backdrop-blur-sm">
      <div className="flex max-h-[90vh] w-full max-w-xl flex-col overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/12 dark:bg-[#191A1F] animate-stagger-in">
        <div className="flex items-center justify-between border-b border-black/8 px-6 py-4 dark:border-white/10">
          <div className="flex items-center gap-3">
            <div className={`flex h-9 w-9 items-center justify-center rounded-xl text-white shadow-sm ${recordingState === 'recording' ? 'bg-rose-600 animate-pulse' : 'bg-blue-600'}`}>
              <Mic className="h-4.5 w-4.5" />
            </div>
            <div>
              <h2 className="text-base font-bold tracking-tight">AI Lecture Recording Studio</h2>
              <p className="text-xs text-slate-500">Live audio capture + AI structured study notes</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="ios-stretch rounded-full p-1.5 text-slate-500 hover:bg-black/5 dark:hover:bg-white/10"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex-1 space-y-4 overflow-y-auto p-6">
          <input
            type="text"
            value={recTitle}
            onChange={(e) => setRecTitle(e.target.value)}
            placeholder="Lecture / recording title"
            className="w-full rounded-xl border border-black/12 bg-white px-3.5 py-2 text-sm font-semibold outline-none focus:border-blue-500 dark:border-white/15 dark:bg-[#22242C]"
          />

          {/* Waveform + Controls */}
          <div className="flex flex-col items-center gap-3 rounded-2xl border border-black/8 bg-slate-50/80 p-4 dark:border-white/10 dark:bg-[#20222A]">
            <div className="flex h-12 items-center gap-1.5">
              {levels.map((lv, idx) => (
                <div
                  key={idx}
                  className="w-1.5 rounded-full bg-gradient-to-t from-blue-600 to-rose-500 transition-all duration-75"
                  style={{ height: `${recordingState === 'recording' ? lv : 6}px` }}
                />
              ))}
            </div>
            <div className="font-mono-num text-xl font-bold tracking-tight">{mins}:{secs}</div>
            <div className="flex flex-wrap items-center gap-2">
              {recordingState === 'idle' || recordingState === 'done' ? (
                <button
                  type="button"
                  onClick={() => void startRecording()}
                  className="ios-stretch inline-flex items-center gap-1.5 rounded-xl bg-rose-600 px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-rose-700"
                >
                  <Mic className="h-3.5 w-3.5" />
                  {recordingState === 'done' ? 'Record Again' : 'Start Recording'}
                </button>
              ) : (
                <>
                  <button
                    type="button"
                    onClick={pauseResume}
                    className="ios-stretch inline-flex items-center gap-1.5 rounded-xl bg-amber-500/15 px-3.5 py-2 text-xs font-semibold text-amber-700 dark:text-amber-300"
                  >
                    {recordingState === 'recording' ? <Pause className="h-3.5 w-3.5" /> : <Play className="h-3.5 w-3.5" />}
                    {recordingState === 'recording' ? 'Pause' : 'Resume'}
                  </button>
                  <button
                    type="button"
                    onClick={stopRecording}
                    className="ios-stretch inline-flex items-center gap-1.5 rounded-xl bg-slate-900 px-4 py-2 text-xs font-semibold text-white dark:bg-white dark:text-slate-900"
                  >
                    <Square className="h-3 w-3 fill-current" /> Stop
                  </button>
                </>
              )}
              {recordingState === 'recording' && (
                <button
                  type="button"
                  onClick={() => setTranscript((t) => `${t}${t ? '\n' : ''}[${mins}:${secs}] Key point: `)}
                  className="ios-stretch rounded-xl border border-black/12 bg-white px-3 py-2 text-xs font-semibold dark:border-white/15 dark:bg-[#272A34]"
                >
                  + Timestamp Bookmark
                </button>
              )}
            </div>
            {audioUrl && (
              <audio controls src={audioUrl} className="mt-1 h-9 w-full" />
            )}
          </div>

          {errorMsg && (
            <div className="rounded-xl border border-amber-500/30 bg-amber-500/10 px-3.5 py-2 text-xs text-amber-700 dark:text-amber-300">
              {errorMsg}
            </div>
          )}

          <div>
            <label className="mb-1 block text-[11px] font-bold uppercase tracking-wider text-slate-500">
              Live Transcript / Spoken Notes
            </label>
            <textarea
              value={transcript}
              onChange={(e) => setTranscript(e.target.value)}
              rows={4}
              placeholder="Live speech-to-text appears here automatically while recording, or type/jot timestamps..."
              className="w-full rounded-xl border border-black/12 bg-white px-3.5 py-2 text-xs leading-relaxed outline-none focus:border-blue-500 dark:border-white/15 dark:bg-[#22242C]"
            />
          </div>

          {result && (
            <div className="space-y-2 rounded-2xl border border-violet-500/25 bg-violet-50/70 p-4 text-xs dark:bg-violet-950/25">
              <div className="font-bold text-violet-700 dark:text-violet-300">AI Study Notes Ready</div>
              <ul className="list-disc space-y-1 pl-4 text-slate-700 dark:text-slate-200">
                {result.summaryPoints.map((pt, i) => (
                  <li key={i}>{pt}</li>
                ))}
              </ul>
              {result.actionItems.length > 0 && (
                <div className="pt-1 font-semibold text-blue-700 dark:text-blue-300">
                  Action Items: {result.actionItems.join(' · ')}
                </div>
              )}
            </div>
          )}
        </div>

        <div className="flex items-center justify-between border-t border-black/8 px-6 py-4 dark:border-white/10">
          <button
            type="button"
            disabled={analyzing}
            onClick={() => void synthesizeNotes()}
            className="ios-stretch inline-flex items-center gap-1.5 rounded-xl bg-violet-600 px-4 py-2 text-xs font-semibold text-white hover:bg-violet-700 disabled:opacity-45"
          >
            {analyzing ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Sparkles className="h-3.5 w-3.5" />}
            AI Structure Notes
          </button>

          <button
            type="button"
            disabled={!result && !transcript.trim()}
            onClick={() => {
              const body = result
                ? `${result.title} (${mins}:${secs})\n\nKey Takeaways:\n${result.summaryPoints.map((p) => `• ${p}`).join('\n')}\n\nAction Items:\n${result.actionItems.map((a) => `- [ ] ${a}`).join('\n')}`
                : `${recTitle} (${mins}:${secs})\n\n${transcript.trim()}`;
              onInsertNotesToPage(body);
              onClose();
            }}
            className="ios-stretch inline-flex items-center gap-1.5 rounded-xl bg-blue-600 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-40"
          >
            <Plus className="h-3.5 w-3.5" /> Insert into Page
          </button>
        </div>
      </div>
    </div>
  );
};

// ================================================================
// 4. Live Multi-User Collaborative Study Room Modal & Drawer
// ================================================================

export const CollabRoomModal: React.FC<{
  open: boolean;
  isSignedIn: boolean;
  activeRoom: CollabRoom | null;
  members: CollabMember[];
  messages: CollabChatMessage[];
  onSignInGoogle: () => Promise<void>;
  onCreateRoom: (title: string) => Promise<void>;
  onJoinRoom: (code: string) => Promise<void>;
  onLeaveRoom: () => void;
  onSendMessage: (text: string) => Promise<void>;
  onClose: () => void;
}> = ({
  open,
  isSignedIn,
  activeRoom,
  members,
  messages,
  onSignInGoogle,
  onCreateRoom,
  onJoinRoom,
  onLeaveRoom,
  onSendMessage,
  onClose,
}) => {
  const [roomTitle, setRoomTitle] = useState('Live Study Session');
  const [joinCode, setJoinCode] = useState('');
  const [chatText, setChatText] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [copiedCode, setCopiedCode] = useState(false);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 backdrop-blur-sm">
      <div className="flex max-h-[88vh] w-full max-w-lg flex-col overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/12 dark:bg-[#191A1F] animate-stagger-in">
        <div className="flex items-center justify-between border-b border-black/8 px-6 py-4 dark:border-white/10">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-600 text-white shadow-sm">
              <Users className="h-4.5 w-4.5" />
            </div>
            <div>
              <h2 className="text-base font-bold tracking-tight">Live Collaborative Notes</h2>
              <p className="text-xs text-slate-500">
                Draw, annotate &amp; chat together in real time via Firestore
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="ios-stretch rounded-full p-1.5 text-slate-500 hover:bg-black/5 dark:hover:bg-white/10"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex-1 space-y-4 overflow-y-auto p-6">
          {!activeRoom ? (
            <div className="space-y-4">
              {err && (
                <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-3.5 py-2 text-xs text-red-600 dark:text-red-300">
                  {err}
                </div>
              )}

              {!isSignedIn && (
                <div className="flex items-center justify-between gap-3 rounded-2xl border border-blue-500/25 bg-blue-500/8 p-3.5">
                  <div className="text-xs text-slate-600 dark:text-slate-300">
                    <span className="font-bold text-blue-600 dark:text-blue-400">Instant Room Codes Active:</span> Join or host with a 6-digit code below, or sign in with Google to sync your identity.
                  </div>
                  <button
                    type="button"
                    onClick={() => void onSignInGoogle()}
                    className="ios-stretch shrink-0 rounded-xl bg-blue-600 px-3 py-1.5 text-[11px] font-semibold text-white"
                  >
                    Google Sign-In
                  </button>
                </div>
              )}

              <div className="space-y-2.5 rounded-2xl border border-black/8 bg-slate-50/80 p-4 dark:border-white/10 dark:bg-[#20222A]">
                <div className="text-xs font-bold uppercase tracking-wider text-slate-500">
                  Host a New Study Room (Generates Shareable Code)
                </div>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={roomTitle}
                    onChange={(e) => setRoomTitle(e.target.value)}
                    placeholder="Room name (e.g. Organic Chem Exam Prep)"
                    className="flex-1 rounded-xl border border-black/12 bg-white px-3 py-2 text-xs outline-none dark:border-white/15 dark:bg-[#191A1F]"
                  />
                  <button
                    type="button"
                    disabled={busy}
                    onClick={async () => {
                      setBusy(true);
                      setErr(null);
                      try {
                        await onCreateRoom(roomTitle);
                      } catch (e) {
                        setErr((e as Error).message);
                      } finally {
                        setBusy(false);
                      }
                    }}
                    className="ios-stretch rounded-xl bg-emerald-600 px-4 py-2 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
                  >
                    Generate Room Code
                  </button>
                </div>
              </div>

              <div className="space-y-2.5 rounded-2xl border border-black/8 bg-slate-50/80 p-4 dark:border-white/10 dark:bg-[#20222A]">
                <div className="text-xs font-bold uppercase tracking-wider text-slate-500">
                  Join Existing Room by Code
                </div>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={joinCode}
                    onChange={(e) => setJoinCode(e.target.value.toUpperCase())}
                    placeholder="Enter code (e.g. 482-910 or BIO-101)"
                    className="font-mono-num flex-1 rounded-xl border border-black/12 bg-white px-3 py-2 text-xs uppercase outline-none dark:border-white/15 dark:bg-[#191A1F]"
                  />
                  <button
                    type="button"
                    disabled={busy || !joinCode.trim()}
                    onClick={async () => {
                      setBusy(true);
                      setErr(null);
                      try {
                        await onJoinRoom(joinCode);
                      } catch (e) {
                        setErr((e as Error).message);
                      } finally {
                        setBusy(false);
                      }
                    }}
                    className="ios-stretch rounded-xl bg-blue-600 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
                  >
                    Join Room
                  </button>
                </div>
                <div className="flex flex-wrap gap-1.5 pt-1">
                  {['BIO-101', 'CALC-202', 'PHYS-301', 'CHEM-LAB'].map((preset) => (
                    <button
                      key={preset}
                      type="button"
                      onClick={() => setJoinCode(preset)}
                      className="rounded-full bg-black/5 px-2.5 py-1 text-[10px] font-bold text-slate-600 hover:bg-blue-500/15 hover:text-blue-600 dark:bg-white/10 dark:text-slate-300"
                    >
                      #{preset}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              {/* Active Room Header */}
              <div className="flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3">
                <div>
                  <div className="text-xs font-bold text-emerald-700 dark:text-emerald-300">
                    Connected: {activeRoom.title}
                  </div>
                  <div className="font-mono-num text-[11px] font-semibold text-slate-600 dark:text-slate-300">
                    Code: {activeRoom.roomId} · {members.length} active
                  </div>
                </div>
                <div className="flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => {
                      navigator.clipboard?.writeText(activeRoom.roomId);
                      setCopiedCode(true);
                      window.setTimeout(() => setCopiedCode(false), 1600);
                    }}
                    className="ios-stretch inline-flex items-center gap-1 rounded-xl bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 shadow-xs dark:bg-[#22242C] dark:text-slate-200"
                  >
                    {copiedCode ? <Check className="h-3.5 w-3.5 text-emerald-500" /> : <Copy className="h-3.5 w-3.5" />}
                    {copiedCode ? 'Copied' : 'Copy Code'}
                  </button>
                  <button
                    type="button"
                    onClick={onLeaveRoom}
                    className="ios-stretch rounded-xl bg-red-500/15 px-3 py-1.5 text-xs font-semibold text-red-600 dark:text-red-300"
                  >
                    Leave
                  </button>
                </div>
              </div>

              {/* Members */}
              <div className="flex flex-wrap gap-1.5">
                {members.map((m) => (
                  <span
                    key={m.uid}
                    className="inline-flex items-center gap-1.5 rounded-full border border-black/10 bg-white px-2.5 py-1 text-[11px] font-semibold dark:border-white/10 dark:bg-[#22242C]"
                  >
                    <span className="h-2 w-2 rounded-full" style={{ backgroundColor: m.color }} />
                    {m.displayName}
                  </span>
                ))}
              </div>

              {/* Live Room Chat */}
              <div className="flex h-52 flex-col overflow-hidden rounded-2xl border border-black/8 bg-slate-50/70 dark:border-white/10 dark:bg-[#20222A]">
                <div className="flex-1 space-y-2 overflow-y-auto p-3 text-xs">
                  {messages.length === 0 ? (
                    <div className="py-10 text-center text-slate-400">
                      Everything drawn on the canvas syncs live! Say hello to your study group below.
                    </div>
                  ) : (
                    messages.map((msg) => (
                      <div key={msg.id} className="rounded-xl bg-white px-3 py-2 shadow-2xs dark:bg-[#282B36]">
                        <span className="font-bold text-blue-600 dark:text-blue-400">{msg.authorName}: </span>
                        <span>{msg.text}</span>
                      </div>
                    ))
                  )}
                </div>
                <form
                  onSubmit={async (e) => {
                    e.preventDefault();
                    if (!chatText.trim()) return;
                    const txt = chatText;
                    setChatText('');
                    await onSendMessage(txt);
                  }}
                  className="flex gap-2 border-t border-black/8 bg-white p-2 dark:border-white/10 dark:bg-[#191A1F]"
                >
                  <input
                    type="text"
                    value={chatText}
                    onChange={(e) => setChatText(e.target.value)}
                    placeholder="Message study group..."
                    className="flex-1 rounded-xl border border-black/10 px-3 py-1.5 text-xs outline-none dark:border-white/15 dark:bg-[#22242C]"
                  />
                  <button
                    type="submit"
                    className="ios-stretch rounded-xl bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white"
                  >
                    Send
                  </button>
                </form>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// ================================================================
// 5. v10 Quick Command Palette (Ctrl/Cmd+K)
// ================================================================

export interface CommandPaletteAction {
  id: string;
  label: string;
  hint: string;
  icon: 'ai' | 'mic' | 'image' | 'collab' | 'timer' | 'export' | 'slides';
  run: () => void;
}

export const CommandPaletteModal: React.FC<{
  open: boolean;
  actions: CommandPaletteAction[];
  onClose: () => void;
}> = ({ open, actions, onClose }) => {
  const [q, setQ] = useState('');
  if (!open) return null;

  const filtered = actions.filter(
    (a) =>
      !q.trim() ||
      a.label.toLowerCase().includes(q.toLowerCase()) ||
      a.hint.toLowerCase().includes(q.toLowerCase())
  );

  return (
    <div
      onClick={onClose}
      className="fixed inset-0 z-50 flex items-start justify-center bg-black/45 p-4 pt-20 backdrop-blur-sm"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/12 dark:bg-[#191A1F] animate-stagger-in"
      >
        <div className="flex items-center gap-2 border-b border-black/8 px-4 py-3 dark:border-white/10">
          <Command className="h-4 w-4 text-blue-600" />
          <input
            autoFocus
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Run a Pro Notes v10 action..."
            className="flex-1 bg-transparent text-sm outline-none"
          />
          <button type="button" onClick={onClose} className="text-xs text-slate-400">
            ESC
          </button>
        </div>
        <div className="max-h-72 overflow-y-auto p-2 space-y-1">
          {filtered.map((a) => (
            <button
              key={a.id}
              type="button"
              onClick={() => {
                a.run();
                onClose();
              }}
              className="ios-stretch flex w-full items-center justify-between rounded-2xl px-3.5 py-2.5 text-left hover:bg-blue-500/10"
            >
              <div className="flex items-center gap-2.5">
                {a.icon === 'ai' && <MessageSquare className="h-4 w-4 text-violet-500" />}
                {a.icon === 'mic' && <Mic className="h-4 w-4 text-rose-500" />}
                {a.icon === 'image' && <ImagePlus className="h-4 w-4 text-fuchsia-500" />}
                {a.icon === 'collab' && <Users className="h-4 w-4 text-emerald-500" />}
                {a.icon === 'timer' && <Clock className="h-4 w-4 text-amber-500" />}
                {a.icon === 'export' && <Download className="h-4 w-4 text-blue-500" />}
                {a.icon === 'slides' && <Sparkles className="h-4 w-4 text-indigo-500" />}
                <span className="text-xs font-semibold">{a.label}</span>
              </div>
              <span className="text-[11px] text-slate-400">{a.hint}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};
