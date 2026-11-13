import React, { useState } from 'react';
import {
  ArrowLeft,
  Check,
  Cpu,
  Download,
  Eraser,
  Globe,
  Palette,
  PenTool,
  Plus,
  ShieldCheck,
  Sparkles,
  Trash2,
  Upload,
  Wand2,
  Zap,
} from 'lucide-react';
import {
  ACCENT_COLORS,
  AccentColorId,
  AppFont,
  AppSettings,
  CustomAiProvider,
  EraserMode,
  MotionStyle,
  PAPER_STYLES,
  PAPER_TINTS,
  PaperStyle,
  PenKind,
  PenPreset,
  ScribbleSensitivity,
  ThemeMode,
  ToolbarDock,
} from '../types/models';
import { testCustomAiConnection } from '../utils/localAi';

interface SettingsScreenProps {
  settings: AppSettings;
  onUpdate: (patch: Partial<AppSettings>) => void;
  onSavePenPreset?: (preset: PenPreset) => void;
  onDeletePenPreset?: (id: string) => void;
  onBack: () => void;
  onOpenLocalModels: () => void;
  onOpenSetupWizard?: () => void;
  onExportBackup: () => string;
  onImportBackup: (raw: string) => number;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  settings,
  onUpdate,
  onSavePenPreset,
  onDeletePenPreset,
  onBack,
  onOpenLocalModels,
  onOpenSetupWizard,
  onExportBackup,
  onImportBackup,
}) => {
  const [backupMsg, setBackupMsg] = useState<string | null>(null);
  const [editingPresetId, setEditingPresetId] = useState<string | null>(null);
  const [editingPresetName, setEditingPresetName] = useState('');
  const [savedPenToast, setSavedPenToast] = useState(false);
  const [testingApi, setTestingApi] = useState(false);
  const [apiTestResult, setApiTestResult] = useState<{ ok: boolean; message: string } | null>(null);

  const handleDownloadBackup = () => {
    const json = onExportBackup();
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `ProNotes-backup-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
    setBackupMsg('Backup exported.');
  };

  const handleRestoreFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const n = onImportBackup(reader.result as string);
        setBackupMsg(`Restored ${n} notebook(s).`);
      } catch (err) {
        setBackupMsg(`Restore failed: ${(err as Error).message}`);
      }
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  const handleTestCustomApi = async () => {
    setTestingApi(true);
    setApiTestResult(null);
    const res = await testCustomAiConnection(settings);
    setApiTestResult(res);
    setTestingApi(false);
  };

  const ToggleRow: React.FC<{
    title: string;
    sub: string;
    checked: boolean;
    onChange: (v: boolean) => void;
  }> = ({ title, sub, checked, onChange }) => (
    <div
      onClick={() => onChange(!checked)}
      className="flex cursor-pointer items-center justify-between gap-4 px-5 py-3.5 transition hover:bg-black/[0.02] dark:hover:bg-white/[0.02]"
    >
      <div>
        <div className="text-sm font-semibold">{title}</div>
        <div className="text-xs text-slate-500 dark:text-slate-400">{sub}</div>
      </div>
      <button
        type="button"
        onClick={(e) => {
          e.stopPropagation();
          onChange(!checked);
        }}
        className={`relative h-6 w-11 shrink-0 rounded-full transition ${
          checked ? 'bg-blue-600' : 'bg-slate-300 dark:bg-slate-700'
        }`}
      >
        <span
          className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform ${
            checked ? 'left-[22px]' : 'left-0.5'
          }`}
        />
      </button>
    </div>
  );

  return (
    <div className="flex h-screen w-screen flex-col overflow-hidden bg-[#F7F7F5] text-[#1B1F2A] dark:bg-[#0F0F0F] dark:text-[#F3F4F6]">
      <header className="flex items-center justify-between gap-3 border-b border-black/8 bg-white/80 px-6 py-4 backdrop-blur-md dark:border-white/8 dark:bg-[#141415]/80">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={onBack}
            className="rounded-xl p-2 hover:bg-black/5 dark:hover:bg-white/10"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-lg font-bold tracking-tight">Settings</h1>
            <p className="text-[11px] text-slate-500">Pro Notes v9.11.0 (Ultra)</p>
          </div>
        </div>
        {onOpenSetupWizard && (
          <button
            type="button"
            onClick={onOpenSetupWizard}
            className="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-blue-600 to-violet-600 px-3.5 py-2 text-xs font-semibold text-white shadow-sm hover:opacity-95"
          >
            <Wand2 className="h-3.5 w-3.5" /> Open Ultra Setup Studio
          </button>
        )}
      </header>

      <div className="flex-1 overflow-y-auto px-6 py-6">
        <div className="mx-auto max-w-3xl space-y-6 pb-16">
          {/* Ultra Look & Motion */}
          <section className="space-y-2">
            <h2 className="flex items-center gap-2 px-2 text-xs font-bold uppercase tracking-wider text-slate-500">
              <Palette className="h-3.5 w-3.5" /> Appearance &amp; Ultra Aesthetics
            </h2>
            <div className="divide-y divide-black/6 overflow-hidden rounded-3xl border border-black/8 bg-white dark:divide-white/8 dark:border-white/10 dark:bg-[#1A1B20]">
              <div className="p-4">
                <div className="mb-2 text-sm font-semibold">Theme</div>
                <div className="grid grid-cols-3 gap-1.5 rounded-xl bg-black/5 p-1 dark:bg-white/6">
                  {(['SYSTEM', 'LIGHT', 'DARK'] as ThemeMode[]).map((mode) => (
                    <button
                      key={mode}
                      type="button"
                      onClick={() => onUpdate({ themeMode: mode })}
                      className={`rounded-lg py-1.5 text-xs font-semibold transition ${
                        settings.themeMode === mode
                          ? 'bg-white text-slate-900 shadow-sm dark:bg-[#2A2C34] dark:text-white'
                          : 'text-slate-500'
                      }`}
                    >
                      {mode.charAt(0) + mode.slice(1).toLowerCase()}
                    </button>
                  ))}
                </div>
              </div>

              <div className="p-4">
                <div className="mb-2.5 text-sm font-semibold">Accent Color</div>
                <div className="flex flex-wrap gap-3">
                  {(Object.keys(ACCENT_COLORS) as AccentColorId[]).map((id) => {
                    const a = ACCENT_COLORS[id];
                    const active = settings.accent === id;
                    return (
                      <button
                        key={id}
                        type="button"
                        onClick={() => onUpdate({ accent: id })}
                        className="flex flex-col items-center gap-1"
                      >
                        <span
                          className={`h-8 w-11 rounded-xl border transition ${
                            active ? 'scale-105 ring-2 ring-blue-600 ring-offset-2' : 'border-black/10'
                          }`}
                          style={{
                            background: `linear-gradient(135deg, ${a.primary}, ${a.secondary})`,
                          }}
                        />
                        <span className="text-[11px] font-medium text-slate-500">{a.label}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="p-4">
                <div className="mb-2 text-sm font-semibold">App Typography</div>
                <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                  {(
                    [
                      { id: 'MODERN_SANS', label: 'Modern Sans', sample: 'Inter UI' },
                      { id: 'EDITORIAL_SERIF', label: 'Editorial', sample: 'Fraunces' },
                      { id: 'STUDIO_MONO', label: 'Studio Mono', sample: 'JetBrains' },
                      { id: 'ROUNDED', label: 'Soft Rounded', sample: 'Quicksand' },
                    ] as { id: AppFont; label: string; sample: string }[]
                  ).map((f) => (
                    <button
                      key={f.id}
                      type="button"
                      onClick={() => onUpdate({ appFont: f.id })}
                      className={`rounded-2xl border p-2.5 text-left transition ${
                        settings.appFont === f.id
                          ? 'border-blue-600 bg-blue-500/10 text-blue-700 dark:text-blue-300'
                          : 'border-black/10 hover:bg-black/[0.02] dark:border-white/10'
                      }`}
                    >
                      <div className="text-xs font-bold">{f.label}</div>
                      <div className="text-[10px] text-slate-500">{f.sample}</div>
                    </button>
                  ))}
                </div>
              </div>

              <div className="p-4">
                <div className="mb-2 text-sm font-semibold">Motion &amp; Spring Feel</div>
                <div className="grid grid-cols-3 gap-1.5 rounded-xl bg-black/5 p-1 dark:bg-white/6">
                  {(
                    [
                      { id: 'ULTRA_SPRING', label: 'Ultra Spring' },
                      { id: 'MINIMAL_CRISP', label: 'Minimal Crisp' },
                      { id: 'REDUCED', label: 'Reduced' },
                    ] as { id: MotionStyle; label: string }[]
                  ).map((m) => (
                    <button
                      key={m.id}
                      type="button"
                      onClick={() => onUpdate({ motionStyle: m.id })}
                      className={`rounded-lg py-1.5 text-xs font-semibold transition ${
                        settings.motionStyle === m.id
                          ? 'bg-white text-slate-900 shadow-sm dark:bg-[#2A2C34] dark:text-white'
                          : 'text-slate-500'
                      }`}
                    >
                      {m.label}
                    </button>
                  ))}
                </div>
              </div>

              <ToggleRow
                title="Liquid glass toolbars"
                sub="Translucent frost with specular edge highlights and rubber-band toolbar icons"
                checked={settings.liquidGlass}
                onChange={(v) => onUpdate({ liquidGlass: v })}
              />
              <ToggleRow
                title="Stylus & pointer hover labels"
                sub="Show tool name tooltip when hovering over stretched dock icons"
                checked={settings.showHoverTooltips}
                onChange={(v) => onUpdate({ showHoverTooltips: v })}
              />
            </div>
          </section>

          {/* Stylus & Pen Presets */}
          <section className="space-y-2">
            <h2 className="flex items-center gap-2 px-2 text-xs font-bold uppercase tracking-wider text-slate-500">
              <PenTool className="h-3.5 w-3.5" /> Stylus &amp; Pen Presets
            </h2>
            <div className="divide-y divide-black/6 overflow-hidden rounded-3xl border border-black/8 bg-white dark:divide-white/8 dark:border-white/10 dark:bg-[#1A1B20]">
              <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-3.5">
                <span className="text-sm font-semibold">Default pen nib</span>
                <div className="flex flex-wrap gap-1.5">
                  {(['ballpoint', 'fountain', 'brush', 'fineliner', 'pencil'] as PenKind[]).map((k) => (
                    <button
                      key={k}
                      type="button"
                      onClick={() => onUpdate({ penKind: k })}
                      className={`rounded-xl px-3 py-1.5 text-xs font-semibold capitalize transition ${
                        settings.penKind === k
                          ? 'bg-blue-600 text-white'
                          : 'bg-black/5 text-slate-600 hover:bg-black/10 dark:bg-white/10 dark:text-slate-300'
                      }`}
                    >
                      {k}
                    </button>
                  ))}
                </div>
              </div>

              <div className="space-y-2.5 p-5">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold">Saved Pen Presets ({settings.penPresets.length})</span>
                  {onSavePenPreset && (
                    <button
                      type="button"
                      onClick={() => {
                        const kindLabel = settings.penKind.charAt(0).toUpperCase() + settings.penKind.slice(1);
                        onSavePenPreset({
                          id: `preset-${Date.now()}`,
                          name: `${kindLabel} ${settings.penSize}px`,
                          kind: settings.penKind,
                          color: settings.penColor,
                          size: settings.penSize,
                          opacity: settings.penOpacity,
                          stabilizer: settings.penStabilizer,
                        });
                        setSavedPenToast(true);
                        window.setTimeout(() => setSavedPenToast(false), 1400);
                      }}
                      className="inline-flex items-center gap-1 rounded-xl bg-blue-600/10 px-3 py-1.5 text-xs font-semibold text-blue-700 hover:bg-blue-600/20 dark:text-blue-300"
                    >
                      {savedPenToast ? <Check className="h-3.5 w-3.5 text-emerald-500" /> : <Plus className="h-3.5 w-3.5" />}
                      {savedPenToast ? 'Saved Preset' : 'Save Current as Preset'}
                    </button>
                  )}
                </div>

                <div className="grid gap-2 sm:grid-cols-2">
                  {settings.penPresets.map((preset) => (
                    <div
                      key={preset.id}
                      className="flex items-center justify-between gap-2 rounded-2xl border border-black/8 bg-slate-50/70 px-3 py-2 dark:border-white/10 dark:bg-[#22242C]"
                    >
                      <div className="flex min-w-0 flex-1 items-center gap-2.5">
                        <span
                          className="h-5 w-5 shrink-0 rounded-full border border-black/15"
                          style={{ backgroundColor: preset.color }}
                        />
                        {editingPresetId === preset.id ? (
                          <input
                            autoFocus
                            value={editingPresetName}
                            onChange={(e) => setEditingPresetName(e.target.value)}
                            onBlur={() => {
                              if (editingPresetName.trim()) {
                                onSavePenPreset?.({ ...preset, name: editingPresetName.trim() });
                              }
                              setEditingPresetId(null);
                            }}
                            onKeyDown={(e) => {
                              if (e.key === 'Enter') {
                                if (editingPresetName.trim()) {
                                  onSavePenPreset?.({ ...preset, name: editingPresetName.trim() });
                                }
                                setEditingPresetId(null);
                              }
                            }}
                            className="w-full rounded-lg border border-blue-500 bg-white px-2 py-0.5 text-xs font-semibold dark:bg-[#191A1F]"
                          />
                        ) : (
                          <button
                            type="button"
                            onClick={() => {
                              setEditingPresetId(preset.id);
                              setEditingPresetName(preset.name);
                            }}
                            title="Tap to rename preset"
                            className="min-w-0 flex-1 text-left"
                          >
                            <div className="truncate text-xs font-bold">{preset.name}</div>
                            <div className="text-[10px] capitalize text-slate-500">
                              {preset.kind} · {preset.size}px
                            </div>
                          </button>
                        )}
                      </div>
                      {settings.penPresets.length > 1 && onDeletePenPreset && (
                        <button
                          type="button"
                          onClick={() => onDeletePenPreset(preset.id)}
                          className="rounded-lg p-1 text-slate-400 hover:bg-red-500/10 hover:text-red-500"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </section>

          {/* Gestures, Eraser & Writing */}
          <section className="space-y-2">
            <h2 className="flex items-center gap-2 px-2 text-xs font-bold uppercase tracking-wider text-slate-500">
              <Eraser className="h-3.5 w-3.5" /> Scribble, Eraser &amp; Shapes
            </h2>
            <div className="divide-y divide-black/6 overflow-hidden rounded-3xl border border-black/8 bg-white dark:divide-white/8 dark:border-white/10 dark:bg-[#1A1B20]">
              <ToggleRow
                title="Scribble to erase"
                sub="Zig-zag across strokes or text to erase them cleanly"
                checked={settings.scribbleToErase}
                onChange={(v) => onUpdate({ scribbleToErase: v })}
              />
              <ToggleRow
                title="Live erase while writing"
                sub="Crumble and erase ink immediately mid-stroke without lifting your pen"
                checked={settings.liveScribbleErase}
                onChange={(v) => onUpdate({ liveScribbleErase: v })}
              />
              <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-3.5">
                <div>
                  <div className="text-sm font-semibold">Scribble sensitivity</div>
                  <div className="text-xs text-slate-500">How eagerly tight zig-zags trigger erase</div>
                </div>
                <div className="flex gap-1.5">
                  {(['gentle', 'balanced', 'instant'] as ScribbleSensitivity[]).map((sens) => (
                    <button
                      key={sens}
                      type="button"
                      onClick={() => onUpdate({ scribbleSensitivity: sens })}
                      className={`rounded-xl px-3 py-1.5 text-xs font-semibold capitalize transition ${
                        settings.scribbleSensitivity === sens
                          ? 'bg-blue-600 text-white'
                          : 'bg-black/5 text-slate-600 dark:bg-white/10 dark:text-slate-300'
                      }`}
                    >
                      {sens}
                    </button>
                  ))}
                </div>
              </div>
              <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-3.5">
                <div>
                  <div className="text-sm font-semibold">Default Eraser Mode</div>
                  <div className="text-xs text-slate-500">Whole stroke, precision segment split, or highlighter only</div>
                </div>
                <div className="flex gap-1.5">
                  {(
                    [
                      { id: 'STROKE' as EraserMode, label: 'Stroke' },
                      { id: 'PIXEL' as EraserMode, label: 'Precision' },
                      { id: 'HIGHLIGHTER' as EraserMode, label: 'Highlight Only' },
                    ]
                  ).map((m) => (
                    <button
                      key={m.id}
                      type="button"
                      onClick={() => onUpdate({ eraserMode: m.id })}
                      className={`rounded-xl px-3 py-1.5 text-xs font-semibold transition ${
                        settings.eraserMode === m.id
                          ? 'bg-blue-600 text-white'
                          : 'bg-black/5 text-slate-600 dark:bg-white/10 dark:text-slate-300'
                      }`}
                    >
                      {m.label}
                    </button>
                  ))}
                </div>
              </div>
              <ToggleRow
                title="Hold to snap shapes"
                sub="Pause briefly at the end of a drawing to snap into crisp geometry"
                checked={settings.holdToShape}
                onChange={(v) => onUpdate({ holdToShape: v })}
              />
              <ToggleRow
                title="Circle to lasso"
                sub="Draw a closed loop and hold briefly to select items inside"
                checked={settings.circleToLasso}
                onChange={(v) => onUpdate({ circleToLasso: v })}
              />
              <ToggleRow
                title="Highlighter behind ink"
                sub="Keep pen ink & text crisp above highlight strokes"
                checked={settings.highlighterBehind}
                onChange={(v) => onUpdate({ highlighterBehind: v })}
              />
            </div>
          </section>

          {/* Local AI & Custom AI API (BYOK) */}
          <section className="space-y-2">
            <h2 className="flex items-center gap-2 px-2 text-xs font-bold uppercase tracking-wider text-slate-500">
              <Sparkles className="h-3.5 w-3.5" /> Local AI &amp; Custom AI API (BYOK)
            </h2>
            <div className="divide-y divide-black/6 overflow-hidden rounded-3xl border border-black/8 bg-white dark:divide-white/8 dark:border-white/10 dark:bg-[#1A1B20]">
              <div
                onClick={onOpenLocalModels}
                className="flex cursor-pointer items-center justify-between px-5 py-4 hover:bg-black/[0.02] dark:hover:bg-white/[0.02]"
              >
                <div className="flex items-center gap-3">
                  <Cpu className="h-5 w-5 text-violet-600" />
                  <div>
                    <div className="text-sm font-semibold">Manage downloadable local AI models</div>
                    <div className="text-xs text-slate-500">
                      Download SmolLM2, Qwen 2.5/3, Llama 3.2, or Gemma 3 for offline study &amp; semantic search
                    </div>
                  </div>
                </div>
                <span className="rounded-lg bg-violet-500/12 px-2.5 py-1 text-xs font-semibold text-violet-700 dark:text-violet-300">
                  Open Hub
                </span>
              </div>

              <ToggleRow
                title="Enhanced AI visual animations"
                sub="Orbital shimmer sweeps, flowing particle waves, and animated Slide Show cards"
                checked={settings.aiAnimations}
                onChange={(v) => onUpdate({ aiAnimations: v })}
              />

              <ToggleRow
                title="Enable Custom AI Endpoint"
                sub="Route Summaries, Quizzes, Slide Show & Semantic Search via your own OpenAI-compatible, Gemini, or Ollama endpoint"
                checked={settings.customAiEnabled}
                onChange={(v) => onUpdate({ customAiEnabled: v })}
              />

              {settings.customAiEnabled && (
                <div className="space-y-3 bg-slate-50/70 p-5 dark:bg-[#20222A]/60">
                  <div className="grid gap-3 sm:grid-cols-3">
                    {(
                      [
                        { id: 'OPENAI_COMPATIBLE', label: 'OpenAI / Groq / Together' },
                        { id: 'GEMINI', label: 'Google Gemini REST' },
                        { id: 'OLLAMA', label: 'Local Ollama' },
                      ] as { id: CustomAiProvider; label: string }[]
                    ).map((p) => (
                      <button
                        key={p.id}
                        type="button"
                        onClick={() => {
                          const defaultUrl =
                            p.id === 'GEMINI'
                              ? 'https://generativelanguage.googleapis.com'
                              : p.id === 'OLLAMA'
                                ? 'http://localhost:11434'
                                : 'https://api.openai.com/v1';
                          const defaultModel =
                            p.id === 'GEMINI'
                              ? 'gemini-2.5-flash'
                              : p.id === 'OLLAMA'
                                ? 'qwen2.5:3b'
                                : 'gpt-4o-mini';
                          onUpdate({
                            customAiProvider: p.id,
                            customAiBaseUrl: defaultUrl,
                            customAiModel: defaultModel,
                          });
                        }}
                        className={`rounded-xl border px-3 py-2 text-xs font-semibold transition ${
                          settings.customAiProvider === p.id
                            ? 'border-blue-600 bg-blue-600 text-white'
                            : 'border-black/10 bg-white dark:border-white/10 dark:bg-[#191A1F]'
                        }`}
                      >
                        {p.label}
                      </button>
                    ))}
                  </div>

                  <div className="grid gap-3 sm:grid-cols-2">
                    <div>
                      <label className="mb-1 block text-[11px] font-bold uppercase text-slate-500">
                        Base URL
                      </label>
                      <input
                        type="url"
                        value={settings.customAiBaseUrl}
                        onChange={(e) => onUpdate({ customAiBaseUrl: e.target.value })}
                        placeholder="https://api.openai.com/v1"
                        className="w-full rounded-xl border border-black/12 bg-white px-3 py-2 text-xs dark:border-white/15 dark:bg-[#191A1F]"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-[11px] font-bold uppercase text-slate-500">
                        Model Name
                      </label>
                      <input
                        type="text"
                        value={settings.customAiModel}
                        onChange={(e) => onUpdate({ customAiModel: e.target.value })}
                        placeholder="gpt-4o-mini"
                        className="w-full rounded-xl border border-black/12 bg-white px-3 py-2 text-xs dark:border-white/15 dark:bg-[#191A1F]"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="mb-1 block text-[11px] font-bold uppercase text-slate-500">
                      API Key (stored locally in your browser)
                    </label>
                    <input
                      type="password"
                      value={settings.customAiApiKey}
                      onChange={(e) => onUpdate({ customAiApiKey: e.target.value })}
                      placeholder={settings.customAiProvider === 'OLLAMA' ? 'Optional for Ollama' : 'sk-... / AIza...'}
                      className="w-full rounded-xl border border-black/12 bg-white px-3 py-2 text-xs dark:border-white/15 dark:bg-[#191A1F]"
                    />
                  </div>

                  <div className="flex flex-wrap items-center justify-between gap-2 pt-1">
                    <button
                      type="button"
                      disabled={testingApi}
                      onClick={handleTestCustomApi}
                      className="inline-flex items-center gap-1.5 rounded-xl bg-blue-600 px-3.5 py-2 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
                    >
                      <Globe className="h-3.5 w-3.5" />
                      {testingApi ? 'Testing Endpoint...' : 'Test Connection'}
                    </button>
                    {apiTestResult && (
                      <span
                        className={`inline-flex items-center gap-1 text-xs font-semibold ${
                          apiTestResult.ok ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-500'
                        }`}
                      >
                        <Zap className="h-3.5 w-3.5" /> {apiTestResult.message}
                      </span>
                    )}
                  </div>
                </div>
              )}
            </div>
          </section>

          {/* Paper, Whiteboard & Workspace */}
          <section className="space-y-2">
            <h2 className="px-2 text-xs font-bold uppercase tracking-wider text-slate-500">
              Pages &amp; Workspace
            </h2>
            <div className="divide-y divide-black/6 overflow-hidden rounded-3xl border border-black/8 bg-white dark:divide-white/8 dark:border-white/10 dark:bg-[#1A1B20]">
              <div className="flex items-center justify-between px-5 py-3.5">
                <span className="text-sm font-semibold">Default page direction</span>
                <div className="flex gap-1.5">
                  {(['VERTICAL', 'HORIZONTAL'] as const).map((dir) => (
                    <button
                      key={dir}
                      type="button"
                      onClick={() => onUpdate({ scroll: dir })}
                      className={`rounded-xl px-3 py-1.5 text-xs font-semibold transition ${
                        settings.scroll === dir
                          ? 'bg-blue-600 text-white'
                          : 'bg-black/5 text-slate-600 dark:bg-white/10 dark:text-slate-300'
                      }`}
                    >
                      {dir === 'VERTICAL' ? 'Vertical' : 'Horizontal'}
                    </button>
                  ))}
                </div>
              </div>
              <div className="flex items-center justify-between px-5 py-3.5">
                <span className="text-sm font-semibold">Default paper pattern</span>
                <select
                  value={settings.defaultPaper}
                  onChange={(e) => onUpdate({ defaultPaper: e.target.value as PaperStyle })}
                  className="rounded-xl border border-black/10 bg-transparent px-3 py-1.5 text-sm font-medium dark:border-white/15"
                >
                  {PAPER_STYLES.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="flex items-center justify-between px-5 py-3.5">
                <span className="text-sm font-semibold">Default paper tint</span>
                <div className="flex gap-2">
                  {PAPER_TINTS.map((t) => (
                    <button
                      key={t.id}
                      type="button"
                      onClick={() => onUpdate({ paperTint: t.color })}
                      className={`h-6 w-6 rounded-full border ${
                        settings.paperTint === t.color ? 'ring-2 ring-blue-600' : 'border-black/20'
                      }`}
                      style={{ backgroundColor: t.color }}
                    />
                  ))}
                </div>
              </div>
              <div className="flex items-center justify-between px-5 py-3.5">
                <span className="text-sm font-semibold">Toolbar placement</span>
                <select
                  value={settings.toolbarDock}
                  onChange={(e) => onUpdate({ toolbarDock: e.target.value as ToolbarDock })}
                  className="rounded-xl border border-black/10 bg-transparent px-3 py-1.5 text-sm font-medium dark:border-white/15"
                >
                  <option value="HEADER">Fixed top bar</option>
                  <option value="TOP">Floating top</option>
                  <option value="BOTTOM">Floating bottom</option>
                </select>
              </div>
              <ToggleRow
                title="Snap Mind Map nodes to grid"
                sub="Align mind map cards gently to spatial grid increments"
                checked={settings.whiteboardGridSnap}
                onChange={(v) => onUpdate({ whiteboardGridSnap: v })}
              />
            </div>
          </section>

          {/* Library Backup */}
          <section className="space-y-2">
            <h2 className="flex items-center gap-2 px-2 text-xs font-bold uppercase tracking-wider text-slate-500">
              <ShieldCheck className="h-3.5 w-3.5" /> Library &amp; Backup
            </h2>
            <div className="divide-y divide-black/6 overflow-hidden rounded-3xl border border-black/8 bg-white dark:divide-white/8 dark:border-white/10 dark:bg-[#1A1B20]">
              <button
                type="button"
                onClick={handleDownloadBackup}
                className="flex w-full items-center gap-3 px-5 py-3.5 text-left hover:bg-black/[0.02] dark:hover:bg-white/[0.02]"
              >
                <Download className="h-4 w-4 text-blue-600" />
                <div>
                  <div className="text-sm font-semibold">Export full library backup (JSON)</div>
                  <div className="text-xs text-slate-500">Save notebooks, folders, flashcards &amp; settings</div>
                </div>
              </button>
              <label className="flex w-full cursor-pointer items-center gap-3 px-5 py-3.5 text-left hover:bg-black/[0.02] dark:hover:bg-white/[0.02]">
                <Upload className="h-4 w-4 text-blue-600" />
                <div>
                  <div className="text-sm font-semibold">Restore from backup file</div>
                  <div className="text-xs text-slate-500">Adds notebooks without overwriting existing work</div>
                </div>
                <input type="file" accept=".json,application/json" onChange={handleRestoreFile} className="hidden" />
              </label>
              {backupMsg && (
                <div className="px-5 py-2.5 text-xs font-medium text-emerald-600 dark:text-emerald-400">
                  {backupMsg}
                </div>
              )}
            </div>
          </section>
        </div>
      </div>
    </div>
  );
};

