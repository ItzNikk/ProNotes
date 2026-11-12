import React, { useState } from 'react';
import {
  ArrowLeft,
  Cpu,
  Download,
  Palette,
  PenTool,
  ShieldCheck,
  Sparkles,
  Upload,
} from 'lucide-react';
import {
  ACCENT_COLORS,
  AccentColorId,
  AppSettings,
  PAPER_STYLES,
  PAPER_TINTS,
  PaperStyle,
  ThemeMode,
  ToolbarDock,
} from '../types/models';

interface SettingsScreenProps {
  settings: AppSettings;
  onUpdate: (patch: Partial<AppSettings>) => void;
  onBack: () => void;
  onOpenLocalModels: () => void;
  onExportBackup: () => string;
  onImportBackup: (raw: string) => number;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  settings,
  onUpdate,
  onBack,
  onOpenLocalModels,
  onExportBackup,
  onImportBackup,
}) => {
  const [backupMsg, setBackupMsg] = useState<string | null>(null);

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
      <header className="flex items-center gap-3 border-b border-black/8 bg-white/80 px-6 py-4 backdrop-blur-md dark:border-white/8 dark:bg-[#141415]/80">
        <button
          type="button"
          onClick={onBack}
          className="rounded-xl p-2 hover:bg-black/5 dark:hover:bg-white/10"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <h1 className="text-lg font-bold tracking-tight">Settings</h1>
      </header>

      <div className="flex-1 overflow-y-auto px-6 py-6">
        <div className="mx-auto max-w-3xl space-y-6 pb-16">
          {/* Appearance */}
          <section className="space-y-2">
            <h2 className="flex items-center gap-2 px-2 text-xs font-bold uppercase tracking-wider text-slate-500">
              <Palette className="h-3.5 w-3.5" /> Appearance
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

              <ToggleRow
                title="Liquid glass toolbars"
                sub="Translucent frost with specular edge highlights"
                checked={settings.liquidGlass}
                onChange={(v) => onUpdate({ liquidGlass: v })}
              />
              <ToggleRow
                title="Pip study buddy"
                sub="Animated companion that cheers and reacts in the editor"
                checked={settings.pet}
                onChange={(v) => onUpdate({ pet: v })}
              />
            </div>
          </section>

          {/* Gestures & Writing */}
          <section className="space-y-2">
            <h2 className="flex items-center gap-2 px-2 text-xs font-bold uppercase tracking-wider text-slate-500">
              <PenTool className="h-3.5 w-3.5" /> Writing & Gestures
            </h2>
            <div className="divide-y divide-black/6 overflow-hidden rounded-3xl border border-black/8 bg-white dark:divide-white/8 dark:border-white/10 dark:bg-[#1A1B20]">
              <ToggleRow
                title="Scribble to erase"
                sub="Zig-zag across strokes or text to erase them cleanly"
                checked={settings.scribbleToErase}
                onChange={(v) => onUpdate({ scribbleToErase: v })}
              />
              <ToggleRow
                title="Live erase while drawing"
                sub="Crumble and erase ink immediately during a scribble without lifting the pen"
                checked={settings.liveScribbleErase}
                onChange={(v) => onUpdate({ liveScribbleErase: v })}
              />
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
                title="Solve maths as you write ="
                sub="Evaluate equations, powers, trig, units & quadratics on the page"
                checked={settings.mathAssist}
                onChange={(v) => onUpdate({ mathAssist: v })}
              />
              <ToggleRow
                title="Highlighter behind ink"
                sub="Keep pen ink & text crisp above highlight strokes"
                checked={settings.highlighterBehind}
                onChange={(v) => onUpdate({ highlighterBehind: v })}
              />
            </div>
          </section>

          {/* Local AI & Animations */}
          <section className="space-y-2">
            <h2 className="flex items-center gap-2 px-2 text-xs font-bold uppercase tracking-wider text-slate-500">
              <Sparkles className="h-3.5 w-3.5" /> Local AI & Animations
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
                      Download SmolLM2, Qwen 2.5, or Gemma Lite directly onto your device
                    </div>
                  </div>
                </div>
                <span className="rounded-lg bg-violet-500/12 px-2.5 py-1 text-xs font-semibold text-violet-700 dark:text-violet-300">
                  Open Hub
                </span>
              </div>
              <ToggleRow
                title="Enhanced AI & math reveal animations"
                sub="Glowing light sweeps, blooming answers, and staggered study cards"
                checked={settings.aiAnimations}
                onChange={(v) => onUpdate({ aiAnimations: v })}
              />
            </div>
          </section>

          {/* Paper, Whiteboard & Workspace */}
          <section className="space-y-2">
            <h2 className="px-2 text-xs font-bold uppercase tracking-wider text-slate-500">
              Pages & Workspace
            </h2>
            <div className="divide-y divide-black/6 overflow-hidden rounded-3xl border border-black/8 bg-white dark:divide-white/8 dark:border-white/10 dark:bg-[#1A1B20]">
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
              <ShieldCheck className="h-3.5 w-3.5" /> Library & Backup
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
                  <div className="text-xs text-slate-500">Save notebooks, folders, flashcards & settings</div>
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
