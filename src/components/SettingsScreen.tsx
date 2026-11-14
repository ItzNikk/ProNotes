import React, { useState } from 'react';
import {
  ArrowLeft,
  Check,
  Clock,
  Cloud,
  Download,
  Eraser,
  Heart,
  LogIn,
  LogOut,
  Palette,
  PenTool,
  Plus,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  Trash2,
  Upload,
  Wand2,
} from 'lucide-react';
import {
  ACCENT_COLORS,
  AccentColorId,
  AppFont,
  AppSettings,
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
import {
  LiquidButton,
  LiquidGlassSurface,
  LiquidInput,
  LiquidMenuButton,
  LiquidSegmentedControl,
  LiquidSlider,
  LiquidToggle,
} from './LiquidGlassSurface';

interface SettingsScreenProps {
  settings: AppSettings;
  userEmail?: string | null;
  userName?: string | null;
  syncBusy?: boolean;
  lastSyncAt?: number | null;
  onSignInGoogle?: () => void;
  onSignOut?: () => void;
  onSyncNow?: () => void;
  onOpenSupport?: () => void;
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
  userEmail,
  userName,
  syncBusy,
  lastSyncAt,
  onSignInGoogle,
  onSignOut,
  onSyncNow,
  onOpenSupport,
  onUpdate,
  onSavePenPreset,
  onDeletePenPreset,
  onBack,
  onOpenSetupWizard,
  onExportBackup,
  onImportBackup,
}) => {
  const [backupMsg, setBackupMsg] = useState<string | null>(null);
  const [editingPresetId, setEditingPresetId] = useState<string | null>(null);
  const [editingPresetName, setEditingPresetName] = useState('');
  const [savedPenToast, setSavedPenToast] = useState(false);

  const accent = ACCENT_COLORS[settings.accent];
  const transparency = Math.max(0.15, Math.min(0.95, settings.glassTransparency ?? 0.58));
  const transparencyPct = Math.round(transparency * 100);
  const activePaperMeta = PAPER_STYLES.find((p) => p.id === settings.defaultPaper);
  const dockLabels: Record<ToolbarDock, string> = {
    HEADER: 'Fixed top bar',
    TOP: 'Floating top',
    BOTTOM: 'Floating bottom',
  };

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
      className="flex cursor-pointer items-center justify-between gap-4 px-5 py-3.5 transition hover:bg-black/[0.03] dark:hover:bg-white/[0.04]"
    >
      <div>
        <div className="text-sm font-semibold">{title}</div>
        <div className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">{sub}</div>
      </div>
      <LiquidToggle selected={checked} onSelect={onChange} accentColor={accent.primary} />
    </div>
  );

  return (
    <div className="relative flex h-screen w-screen flex-col overflow-hidden bg-[#EFEBE2] text-[#1F2421] dark:bg-[#141411] dark:text-[#EDE8DB]">
      {/* OpenLeaf Ambient Botanical Mesh (refraction backdrop for Liquid Glass) */}
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div
          className="absolute -left-24 -top-24 h-96 w-96 rounded-full opacity-35 blur-3xl dark:opacity-20"
          style={{ background: `radial-gradient(circle, ${accent.primary}44, transparent 70%)` }}
        />
        <div
          className="absolute -bottom-28 right-0 h-[420px] w-[420px] rounded-full opacity-30 blur-3xl dark:opacity-18"
          style={{ background: `radial-gradient(circle, ${accent.secondary}44, transparent 70%)` }}
        />
      </div>

      <LiquidGlassSurface
        enabled={settings.liquidGlass}
        variant="chrome"
        radius={0}
        className="relative z-20 shrink-0 border-b border-white/45 dark:border-[#EDE8DB]/12"
      >
        <header className="flex items-center justify-between gap-3 px-6 py-4">
          <div className="flex items-center gap-3">
            <LiquidButton onClick={onBack} className="!h-10 !w-10 !p-0" title="Back to Library">
              <ArrowLeft className="h-4.5 w-4.5" />
            </LiquidButton>
            <div>
              <h1 className="text-lg font-bold tracking-tight">Settings</h1>
              <p className="text-[11px] text-[#5A5F58] dark:text-[#B1AC9F]">
                Pro Notes · OpenLeaf &amp; Liquid Glass
              </p>
            </div>
          </div>
          {onOpenSetupWizard && (
            <LiquidButton onClick={onOpenSetupWizard} tint={accent.primary} className="whitespace-nowrap">
              <Wand2 className="h-3.5 w-3.5" /> Open Setup Studio
            </LiquidButton>
          )}
        </header>
      </LiquidGlassSurface>

      <div className="relative z-10 flex-1 overflow-y-auto px-6 py-6">
        <div className="mx-auto max-w-3xl space-y-6 pb-16">
          {/* Google Account & Cloud Sync */}
          <section className="space-y-2">
            <h2 className="flex items-center gap-2 px-2 text-xs font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
              <Cloud className="h-3.5 w-3.5" /> Google Account &amp; Cloud Sync
            </h2>
            <LiquidGlassSurface
              enabled={settings.liquidGlass}
              variant="frost"
              radius={24}
              className="divide-y divide-[#1F2421]/8 dark:divide-[#EDE8DB]/10"
            >
              {userEmail ? (
                <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4">
                  <div>
                    <div className="text-sm font-semibold">{userName || 'Signed in with Google'}</div>
                    <div className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                      {userEmail}
                      {lastSyncAt
                        ? ` · Synced ${new Date(lastSyncAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
                        : ''}
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {onSyncNow && (
                      <LiquidButton disabled={syncBusy} onClick={onSyncNow} tint={accent.primary}>
                        <RefreshCw className={`h-3.5 w-3.5 ${syncBusy ? 'animate-spin' : ''}`} />
                        {syncBusy ? 'Syncing...' : 'Sync Now'}
                      </LiquidButton>
                    )}
                    {onSignOut && (
                      <LiquidButton onClick={onSignOut}>
                        <LogOut className="h-3.5 w-3.5" /> Sign Out
                      </LiquidButton>
                    )}
                  </div>
                </div>
              ) : (
                <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4">
                  <div>
                    <div className="text-sm font-semibold">Sync notebooks across devices</div>
                    <div className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                      Sign in with Google to sync notes, folders, flashcards, and settings automatically
                    </div>
                  </div>
                  {onSignInGoogle && (
                    <LiquidButton onClick={onSignInGoogle} tint={accent.primary}>
                      <LogIn className="h-3.5 w-3.5" /> Sign in with Google
                    </LiquidButton>
                  )}
                </div>
              )}
            </LiquidGlassSurface>
          </section>

          {/* Appearance & Liquid Glass */}
          <section className="space-y-2">
            <h2 className="flex items-center gap-2 px-2 text-xs font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
              <Palette className="h-3.5 w-3.5" /> Appearance &amp; Liquid Glass
            </h2>
            <LiquidGlassSurface
              enabled={settings.liquidGlass}
              variant="frost"
              radius={24}
              className="divide-y divide-[#1F2421]/8 dark:divide-[#EDE8DB]/10"
            >
              <div className="p-4">
                <div className="mb-2 text-sm font-semibold">Theme</div>
                <LiquidSegmentedControl<ThemeMode>
                  value={settings.themeMode}
                  onChange={(mode) => onUpdate({ themeMode: mode })}
                  accentColor={accent.primary}
                  options={[
                    { value: 'SYSTEM', label: 'System' },
                    { value: 'LIGHT', label: 'Light' },
                    { value: 'DARK', label: 'Dark' },
                  ]}
                />
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
                            active
                              ? 'scale-105 ring-2 ring-[#24483F] ring-offset-2 dark:ring-[#8CC1AC]'
                              : 'border-black/15 dark:border-white/15'
                          }`}
                          style={{
                            background: `linear-gradient(135deg, ${a.primary}, ${a.secondary})`,
                          }}
                        />
                        <span className="text-[11px] font-medium text-[#5A5F58] dark:text-[#B1AC9F]">
                          {a.label}
                        </span>
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
                      { id: 'MODERN_SANS', label: 'Modern Sans', sample: 'Plus Jakarta' },
                      { id: 'EDITORIAL_SERIF', label: 'Editorial', sample: 'Fraunces' },
                      { id: 'STUDIO_MONO', label: 'Studio Mono', sample: 'JetBrains' },
                      { id: 'ROUNDED', label: 'Soft Rounded', sample: 'Quicksand' },
                    ] as { id: AppFont; label: string; sample: string }[]
                  ).map((f) => {
                    const active = settings.appFont === f.id;
                    return (
                      <button
                        key={f.id}
                        type="button"
                        onClick={() => onUpdate({ appFont: f.id })}
                        className={`rounded-2xl border p-2.5 text-left transition ${
                          active
                            ? 'border-[#24483F] bg-[#24483F]/12 text-[#24483F] dark:border-[#8CC1AC] dark:bg-[#8CC1AC]/16 dark:text-[#8CC1AC]'
                            : 'border-[#1F2421]/10 hover:bg-black/[0.04] dark:border-[#EDE8DB]/12 dark:hover:bg-white/[0.05]'
                        }`}
                      >
                        <div className="text-xs font-bold">{f.label}</div>
                        <div className="text-[10px] opacity-70">{f.sample}</div>
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="p-4">
                <div className="mb-2 text-sm font-semibold">Motion &amp; Spring Feel</div>
                <LiquidSegmentedControl<MotionStyle>
                  value={settings.motionStyle}
                  onChange={(motionStyle) => onUpdate({ motionStyle })}
                  accentColor={accent.primary}
                  options={[
                    { value: 'ULTRA_SPRING', label: 'Spring Fluid' },
                    { value: 'MINIMAL_CRISP', label: 'Minimal Crisp' },
                    { value: 'REDUCED', label: 'Reduced' },
                  ]}
                />
              </div>

              <ToggleRow
                title="Liquid glass toolbars & UI"
                sub="Optical frost refraction, specular rims, and adaptive contrast across every control"
                checked={settings.liquidGlass}
                onChange={(v) => onUpdate({ liquidGlass: v })}
              />

              <div className="space-y-2.5 px-5 py-4">
                <div className="flex items-center justify-between gap-2">
                  <div>
                    <div className="flex items-center gap-1.5 text-sm font-semibold">
                      <Sparkles className="h-3.5 w-3.5 text-[#24483F] dark:text-[#8CC1AC]" />
                      Glass Transparency
                    </div>
                    <div className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                      Adjust optical glass clarity while preserving crisp text contrast
                    </div>
                  </div>
                  <span className="font-mono-num shrink-0 text-xs font-bold text-[#24483F] dark:text-[#8CC1AC]">
                    {transparencyPct}% ·{' '}
                    {transparency < 0.35 ? 'Frosted' : transparency < 0.72 ? 'Balanced' : 'Crystal Clear'}
                  </span>
                </div>
                <LiquidSlider
                  min={0.15}
                  max={0.95}
                  step={0.01}
                  value={transparency}
                  onChange={(v) => onUpdate({ liquidGlass: true, glassTransparency: v })}
                  accentColor={accent.primary}
                />
                <div className="flex justify-between text-[10px] font-medium text-[#5A5F58] dark:text-[#B1AC9F]">
                  <span>15% Frosted Solid</span>
                  <span>58% Balanced</span>
                  <span>95% Clear Glass</span>
                </div>
              </div>

              <ToggleRow
                title="Stylus & pointer hover labels"
                sub="Show tool name tooltip when hovering over stretched dock icons"
                checked={settings.showHoverTooltips}
                onChange={(v) => onUpdate({ showHoverTooltips: v })}
              />
            </LiquidGlassSurface>
          </section>

          {/* Stylus & Pen Presets */}
          <section className="space-y-2">
            <h2 className="flex items-center gap-2 px-2 text-xs font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
              <PenTool className="h-3.5 w-3.5" /> Stylus &amp; Pen Presets
            </h2>
            <LiquidGlassSurface
              enabled={settings.liquidGlass}
              variant="frost"
              radius={24}
              className="divide-y divide-[#1F2421]/8 dark:divide-[#EDE8DB]/10"
            >
              <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-3.5">
                <span className="text-sm font-semibold">Default pen nib</span>
                <LiquidSegmentedControl<PenKind>
                  value={settings.penKind}
                  onChange={(k) => onUpdate({ penKind: k })}
                  accentColor={accent.primary}
                  className="w-full sm:w-auto"
                  options={[
                    { value: 'ballpoint', label: 'Ballpoint' },
                    { value: 'fountain', label: 'Fountain' },
                    { value: 'brush', label: 'Brush' },
                    { value: 'fineliner', label: 'Fineliner' },
                    { value: 'pencil', label: 'Pencil' },
                  ]}
                />
              </div>

              <div className="space-y-2.5 p-5">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold">Saved Pen Presets ({settings.penPresets.length})</span>
                  {onSavePenPreset && (
                    <LiquidButton
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
                      className="!px-3 !py-1.5 !text-xs"
                    >
                      {savedPenToast ? <Check className="h-3.5 w-3.5 text-emerald-500" /> : <Plus className="h-3.5 w-3.5" />}
                      {savedPenToast ? 'Saved Preset' : 'Save Current'}
                    </LiquidButton>
                  )}
                </div>

                <div className="grid gap-2 sm:grid-cols-2">
                  {settings.penPresets.map((preset) => (
                    <LiquidGlassSurface
                      key={preset.id}
                      enabled={settings.liquidGlass}
                      variant="frost"
                      radius={16}
                      className="flex items-center justify-between gap-2 px-3 py-2"
                    >
                      <div className="flex min-w-0 flex-1 items-center gap-2.5">
                        <span
                          className="h-5 w-5 shrink-0 rounded-full border border-black/15"
                          style={{ backgroundColor: preset.color }}
                        />
                        {editingPresetId === preset.id ? (
                          <LiquidInput
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
                            wrapperClassName="!py-1 !px-2.5 flex-1"
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
                            <div className="text-[10px] capitalize text-[#5A5F58] dark:text-[#B1AC9F]">
                              {preset.kind} · {preset.size}px
                            </div>
                          </button>
                        )}
                      </div>
                      {settings.penPresets.length > 1 && onDeletePenPreset && (
                        <button
                          type="button"
                          onClick={() => onDeletePenPreset(preset.id)}
                          className="rounded-lg p-1 text-[#5A5F58] hover:bg-red-500/10 hover:text-red-500"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      )}
                    </LiquidGlassSurface>
                  ))}
                </div>
              </div>
            </LiquidGlassSurface>
          </section>

          {/* Gestures, Eraser & Writing */}
          <section className="space-y-2">
            <h2 className="flex items-center gap-2 px-2 text-xs font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
              <Eraser className="h-3.5 w-3.5" /> Scribble, Eraser &amp; Shapes
            </h2>
            <LiquidGlassSurface
              enabled={settings.liquidGlass}
              variant="frost"
              radius={24}
              className="divide-y divide-[#1F2421]/8 dark:divide-[#EDE8DB]/10"
            >
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
                  <div className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                    How eagerly tight zig-zags trigger erase
                  </div>
                </div>
                <LiquidSegmentedControl<ScribbleSensitivity>
                  value={settings.scribbleSensitivity}
                  onChange={(sens) => onUpdate({ scribbleSensitivity: sens })}
                  accentColor={accent.primary}
                  options={[
                    { value: 'gentle', label: 'Gentle' },
                    { value: 'balanced', label: 'Balanced' },
                    { value: 'instant', label: 'Instant' },
                  ]}
                />
              </div>
              <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-3.5">
                <div>
                  <div className="text-sm font-semibold">Default Eraser Mode</div>
                  <div className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                    Whole stroke, precision segment split, or highlighter only
                  </div>
                </div>
                <LiquidSegmentedControl<EraserMode>
                  value={settings.eraserMode}
                  onChange={(mode) => onUpdate({ eraserMode: mode })}
                  accentColor={accent.primary}
                  options={[
                    { value: 'STROKE', label: 'Stroke' },
                    { value: 'PIXEL', label: 'Precision' },
                    { value: 'HIGHLIGHTER', label: 'Highlight' },
                  ]}
                />
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
            </LiquidGlassSurface>
          </section>

          {/* Paper, Whiteboard & Workspace */}
          <section className="space-y-2">
            <h2 className="px-2 text-xs font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
              Pages &amp; Workspace
            </h2>
            <LiquidGlassSurface
              enabled={settings.liquidGlass}
              variant="frost"
              radius={24}
              className="divide-y divide-[#1F2421]/8 dark:divide-[#EDE8DB]/10"
            >
              <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-3.5">
                <span className="text-sm font-semibold">Default page direction</span>
                <LiquidSegmentedControl<'VERTICAL' | 'HORIZONTAL'>
                  value={settings.scroll}
                  onChange={(dir) => onUpdate({ scroll: dir })}
                  accentColor={accent.primary}
                  options={[
                    { value: 'VERTICAL', label: 'Vertical' },
                    { value: 'HORIZONTAL', label: 'Horizontal' },
                  ]}
                />
              </div>
              <div className="flex items-center justify-between gap-3 px-5 py-3.5">
                <span className="text-sm font-semibold">Default paper pattern</span>
                <LiquidMenuButton
                  label={activePaperMeta?.label || settings.defaultPaper}
                  items={PAPER_STYLES.map((p) => ({
                    id: p.id,
                    label: p.label,
                    subtitle: p.group,
                    selected: settings.defaultPaper === p.id,
                    onSelect: () => onUpdate({ defaultPaper: p.id as PaperStyle }),
                  }))}
                />
              </div>
              <div className="flex items-center justify-between gap-3 px-5 py-3.5">
                <span className="text-sm font-semibold">Default paper tint</span>
                <div className="flex gap-2">
                  {PAPER_TINTS.map((t) => (
                    <button
                      key={t.id}
                      type="button"
                      onClick={() => onUpdate({ paperTint: t.color })}
                      className={`h-6 w-6 rounded-full border ${
                        settings.paperTint === t.color
                          ? 'ring-2 ring-[#24483F] ring-offset-1 dark:ring-[#8CC1AC]'
                          : 'border-black/20'
                      }`}
                      style={{ backgroundColor: t.color }}
                      title={t.label}
                    />
                  ))}
                </div>
              </div>
              <div className="flex items-center justify-between gap-3 px-5 py-3.5">
                <span className="text-sm font-semibold">Toolbar placement</span>
                <LiquidMenuButton
                  label={dockLabels[settings.toolbarDock] || 'Fixed top bar'}
                  items={(['HEADER', 'TOP', 'BOTTOM'] as ToolbarDock[]).map((dock) => ({
                    id: dock,
                    label: dockLabels[dock],
                    selected: settings.toolbarDock === dock,
                    onSelect: () => onUpdate({ toolbarDock: dock }),
                  }))}
                />
              </div>
              <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-3.5">
                <span className="inline-flex items-center gap-2 text-sm font-semibold">
                  <Clock className="h-4 w-4 text-amber-500" /> Pomodoro Focus Duration
                </span>
                <LiquidSegmentedControl<string>
                  value={String(settings.pomodoroMinutes || 25)}
                  onChange={(v) => onUpdate({ pomodoroMinutes: Number(v) })}
                  accentColor={accent.primary}
                  options={[
                    { value: '15', label: '15m' },
                    { value: '25', label: '25m' },
                    { value: '45', label: '45m' },
                  ]}
                />
              </div>
              <ToggleRow
                title="Snap Mind Map nodes to grid"
                sub="Align mind map cards gently to spatial grid increments"
                checked={settings.whiteboardGridSnap}
                onChange={(v) => onUpdate({ whiteboardGridSnap: v })}
              />
            </LiquidGlassSurface>
          </section>

          {/* Support Pro Notes & Reminders */}
          <section className="space-y-2">
            <h2 className="flex items-center gap-2 px-2 text-xs font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
              <Heart className="h-3.5 w-3.5 text-rose-500" /> Community &amp; Support
            </h2>
            <LiquidGlassSurface
              enabled={settings.liquidGlass}
              variant="frost"
              radius={24}
              className="divide-y divide-[#1F2421]/8 dark:divide-[#EDE8DB]/10"
            >
              <ToggleRow
                title="Support appreciation reminders"
                sub="Show gentle periodic support & release note highlights"
                checked={settings.supportReminders}
                onChange={(v) => onUpdate({ supportReminders: v })}
              />
              {onOpenSupport && (
                <button
                  type="button"
                  onClick={onOpenSupport}
                  className="flex w-full items-center justify-between px-5 py-3.5 text-left transition hover:bg-black/[0.03] dark:hover:bg-white/[0.04]"
                >
                  <div>
                    <div className="text-sm font-semibold text-rose-600 dark:text-rose-400">
                      Open Support Note &amp; Share Pro Notes
                    </div>
                    <div className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                      Send appreciation, leave feature feedback, or share with classmates
                    </div>
                  </div>
                  <Heart className="h-4 w-4 text-rose-500" />
                </button>
              )}
            </LiquidGlassSurface>
          </section>

          {/* Library Backup */}
          <section className="space-y-2">
            <h2 className="flex items-center gap-2 px-2 text-xs font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
              <ShieldCheck className="h-3.5 w-3.5" /> Library &amp; Backup
            </h2>
            <LiquidGlassSurface
              enabled={settings.liquidGlass}
              variant="frost"
              radius={24}
              className="divide-y divide-[#1F2421]/8 dark:divide-[#EDE8DB]/10"
            >
              <button
                type="button"
                onClick={handleDownloadBackup}
                className="flex w-full items-center gap-3 px-5 py-3.5 text-left transition hover:bg-black/[0.03] dark:hover:bg-white/[0.04]"
              >
                <Download className="h-4 w-4 text-[#24483F] dark:text-[#8CC1AC]" />
                <div>
                  <div className="text-sm font-semibold">Export full library backup (JSON)</div>
                  <div className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                    Save notebooks, folders, flashcards &amp; settings
                  </div>
                </div>
              </button>
              <label className="flex w-full cursor-pointer items-center gap-3 px-5 py-3.5 text-left transition hover:bg-black/[0.03] dark:hover:bg-white/[0.04]">
                <Upload className="h-4 w-4 text-[#24483F] dark:text-[#8CC1AC]" />
                <div>
                  <div className="text-sm font-semibold">Restore from backup file</div>
                  <div className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                    Adds notebooks without overwriting existing work
                  </div>
                </div>
                <input type="file" accept=".json,application/json" onChange={handleRestoreFile} className="hidden" />
              </label>
              {backupMsg && (
                <div className="px-5 py-2.5 text-xs font-semibold text-emerald-700 dark:text-emerald-400">
                  {backupMsg}
                </div>
              )}
            </LiquidGlassSurface>
          </section>
        </div>
      </div>
    </div>
  );
};
