import React, { useRef, useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Check,
  Palette,
  PenTool,
  Sliders,
  Sparkles,
  Wand2,
  X,
} from 'lucide-react';
import {
  ACCENT_COLORS,
  AccentColorId,
  AppFont,
  AppSettings,
  LocalModelState,
  MotionStyle,
  PAPER_STYLES,
  PAPER_TINTS,
  PaperStyle,
  ThemeMode,
} from '../types/models';
import {
  LiquidButton,
  LiquidGlassSurface,
  LiquidMenuButton,
  LiquidModal,
  LiquidSegmentedControl,
  LiquidSlider,
  LiquidToggle,
} from './LiquidGlassSurface';

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
  { id: 'pen', label: 'Pen Check', icon: PenTool },
];

export const SetupWizardModal: React.FC<SetupWizardModalProps> = ({
  open,
  settings,
  onUpdateSettings,
  onFinish,
}) => {
  const [step, setStep] = useState(0);

  // Mini pen test pad state
  const padRef = useRef<HTMLCanvasElement>(null);
  const drawingRef = useRef(false);
  const lastPtRef = useRef<{ x: number; y: number } | null>(null);
  const [inkCount, setInkCount] = useState(0);

  if (!open) return null;

  const accent = ACCENT_COLORS[settings.accent];
  const transparency = Math.max(0.15, Math.min(0.95, settings.glassTransparency ?? 0.58));
  const activePaperMeta = PAPER_STYLES.find((p) => p.id === settings.defaultPaper);

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

  return (
    <LiquidModal open={open} onClose={onFinish} maxWidthClass="max-w-3xl">
      <div className="flex max-h-[92vh] flex-col">
        {/* Header + Progress Stepper */}
        <div className="border-b border-black/8 px-6 py-4 dark:border-white/10">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div
                className="flex h-10 w-10 items-center justify-center rounded-2xl text-[#F5F2E8] shadow-md"
                style={{ background: `linear-gradient(135deg, ${accent.primary}, ${accent.secondary})` }}
              >
                <Wand2 className="h-5 w-5" />
              </div>
              <div>
                <h2 className="text-base font-extrabold tracking-tight">Pro Notes Setup Studio</h2>
                <p className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                  Tune your OpenLeaf theme, Liquid Glass transparency, typography, and pen feel
                </p>
              </div>
            </div>
            <LiquidButton onClick={onFinish} className="!px-3 !py-1.5 !text-xs">
              Skip <X className="h-3.5 w-3.5" />
            </LiquidButton>
          </div>

          <div className="mt-4 grid grid-cols-4 gap-2">
            {STEPS.map((st, idx) => {
              const Icon = st.icon;
              const active = idx === step;
              const done = idx < step;
              return (
                <button
                  key={st.id}
                  type="button"
                  onClick={() => setStep(idx)}
                  className={`flex items-center justify-center gap-1.5 rounded-full px-2 py-1.5 text-[11px] font-bold transition ${
                    active
                      ? 'text-white shadow-sm'
                      : done
                        ? 'bg-emerald-500/14 text-emerald-700 dark:text-emerald-300'
                        : 'liquid-glass-frost text-[#5A5F58] dark:text-[#B1AC9F]'
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
        <div className="flex-1 overflow-y-auto p-6">
          {step === 0 && (
            <div className="space-y-5 animate-stagger-in">
              <div
                className="overflow-hidden rounded-3xl p-6 text-[#F5F2E8] shadow-xl"
                style={{ background: `linear-gradient(135deg, ${accent.primary}, ${accent.secondary})` }}
              >
                <span className="inline-flex items-center gap-1.5 rounded-full bg-white/18 px-2.5 py-1 text-[10px] font-bold uppercase tracking-widest">
                  <Sparkles className="h-3 w-3" /> OpenLeaf &amp; Liquid Glass
                </span>
                <h3 className="mt-3 text-2xl font-extrabold tracking-tight">
                  Handwritten notes, mind maps, and writable split study in one fluid canvas.
                </h3>
                <p className="mt-2 max-w-xl text-xs leading-relaxed text-white/88">
                  Pro Notes brings smooth low-latency ink, interactive checklist sticky notes, embeddable web link cards,
                  slide deck revision, writable split notebooks, and optical Liquid Glass controls.
                </p>
              </div>

              <div className="grid gap-3 sm:grid-cols-3">
                {[
                  {
                    title: 'Slide Show Revision',
                    desc: 'Build colorful chapter slide decks with active recall cards for focused study.',
                    color: accent.primary,
                  },
                  {
                    title: 'Interactive Sticky & Links',
                    desc: 'Tap checklists directly on stickies and pin live URL reference cards.',
                    color: accent.secondary,
                  },
                  {
                    title: 'Offline Notebooks & Sync',
                    desc: 'Fast offline storage on your device with optional Google Sync and live study rooms.',
                    color: '#B98A2E',
                  },
                ].map((card) => (
                  <LiquidGlassSurface
                    key={card.title}
                    enabled={settings.liquidGlass}
                    variant="frost"
                    radius={18}
                    className="p-4"
                  >
                    <div className="mb-2 h-1.5 w-10 rounded-full" style={{ backgroundColor: card.color }} />
                    <div className="text-sm font-bold">{card.title}</div>
                    <p className="mt-1 text-xs leading-relaxed text-[#5A5F58] dark:text-[#B1AC9F]">{card.desc}</p>
                  </LiquidGlassSurface>
                ))}
              </div>
            </div>
          )}

          {step === 1 && (
            <div className="grid gap-5 md:grid-cols-2 animate-stagger-in">
              <div className="space-y-4">
                <div>
                  <div className="mb-1.5 text-xs font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
                    Theme
                  </div>
                  <LiquidSegmentedControl<ThemeMode>
                    value={settings.themeMode}
                    onChange={(m) => onUpdateSettings({ themeMode: m })}
                    accentColor={accent.primary}
                    options={[
                      { value: 'SYSTEM', label: 'System' },
                      { value: 'LIGHT', label: 'Light' },
                      { value: 'DARK', label: 'Dark' },
                    ]}
                  />
                </div>

                <div>
                  <div className="mb-1.5 text-xs font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
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
                            settings.accent === id
                              ? 'scale-105 ring-2 ring-[#24483F] ring-offset-2 dark:ring-[#8CC1AC]'
                              : 'border-black/15'
                          }`}
                          style={{ background: `linear-gradient(135deg, ${a.primary}, ${a.secondary})` }}
                          title={a.label}
                        />
                      );
                    })}
                  </div>
                </div>

                <div>
                  <div className="mb-1.5 text-xs font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
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
                            ? 'border-[#24483F] bg-[#24483F]/12 text-[#24483F] dark:border-[#8CC1AC] dark:text-[#8CC1AC]'
                            : 'border-black/10 hover:bg-black/5 dark:border-white/10'
                        }`}
                      >
                        {f.label}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <div className="mb-1.5 text-xs font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
                    Motion Style
                  </div>
                  <LiquidSegmentedControl<MotionStyle>
                    value={settings.motionStyle}
                    onChange={(ms) => onUpdateSettings({ motionStyle: ms })}
                    accentColor={accent.primary}
                    options={[
                      { value: 'ULTRA_SPRING', label: 'Spring' },
                      { value: 'MINIMAL_CRISP', label: 'Minimal' },
                      { value: 'REDUCED', label: 'Reduced' },
                    ]}
                  />
                </div>
              </div>

              <LiquidGlassSurface
                enabled={settings.liquidGlass}
                variant="frost"
                radius={24}
                className="flex flex-col justify-between p-5"
              >
                <div>
                  <div className="flex items-center justify-between">
                    <span
                      className="rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-white"
                      style={{ backgroundColor: accent.primary }}
                    >
                      Live Preview
                    </span>
                    <span className="font-mono-num text-[11px] text-[#5A5F58] dark:text-[#B1AC9F]">
                      {settings.motionStyle.replace('_', ' ')}
                    </span>
                  </div>
                  <h4 className="mt-4 text-lg font-extrabold tracking-tight">Thermodynamics · Ch. 4</h4>
                  <p className="mt-1.5 text-xs leading-relaxed text-[#5A5F58] dark:text-[#B1AC9F]">
                    ΔU = Q − W. Entropy of an isolated system never decreases during spontaneous processes.
                  </p>
                  <div className="mt-4 rounded-2xl border border-black/8 bg-[#FFF3B0] p-3 text-xs text-[#1F2421] shadow-sm">
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
                          settings.paperTint === pt.color ? 'ring-2 ring-[#24483F] dark:ring-[#8CC1AC]' : 'border-black/20'
                        }`}
                        style={{ backgroundColor: pt.color }}
                        title={pt.label}
                      />
                    ))}
                  </div>
                </div>
              </LiquidGlassSurface>
            </div>
          )}

          {step === 2 && (
            <div className="space-y-4 animate-stagger-in">
              <div className="grid gap-3 sm:grid-cols-2">
                <LiquidGlassSurface enabled={settings.liquidGlass} variant="frost" radius={18} className="p-4">
                  <div className="text-sm font-bold">Page Flow Direction</div>
                  <p className="mb-3 text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                    Applies to editor &amp; side-by-side Split View
                  </p>
                  <LiquidSegmentedControl<'VERTICAL' | 'HORIZONTAL'>
                    value={settings.scroll}
                    onChange={(dir) => onUpdateSettings({ scroll: dir })}
                    accentColor={accent.primary}
                    options={[
                      { value: 'VERTICAL', label: 'Vertical' },
                      { value: 'HORIZONTAL', label: 'Horizontal' },
                    ]}
                  />
                </LiquidGlassSurface>

                <LiquidGlassSurface enabled={settings.liquidGlass} variant="frost" radius={18} className="p-4">
                  <div className="text-sm font-bold">Default Paper Template</div>
                  <p className="mb-3 text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                    Used when creating new notebooks
                  </p>
                  <LiquidMenuButton
                    label={activePaperMeta?.label || settings.defaultPaper}
                    align="left"
                    items={PAPER_STYLES.map((p) => ({
                      id: p.id,
                      label: p.label,
                      subtitle: p.group,
                      selected: settings.defaultPaper === p.id,
                      onSelect: () => onUpdateSettings({ defaultPaper: p.id as PaperStyle }),
                    }))}
                  />
                </LiquidGlassSurface>
              </div>

              <LiquidGlassSurface
                enabled={settings.liquidGlass}
                variant="frost"
                radius={18}
                className="space-y-2.5 p-4"
              >
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm font-bold">Liquid Glass Transparency</div>
                    <div className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                      Adjust surface translucency across headers, menus, and toolbars
                    </div>
                  </div>
                  <span className="font-mono-num text-xs font-bold text-[#24483F] dark:text-[#8CC1AC]">
                    {Math.round(transparency * 100)}%
                  </span>
                </div>
                <LiquidSlider
                  min={0.15}
                  max={0.95}
                  step={0.01}
                  value={transparency}
                  onChange={(v) => onUpdateSettings({ liquidGlass: true, glassTransparency: v })}
                  accentColor={accent.primary}
                />
              </LiquidGlassSurface>

              <LiquidGlassSurface
                enabled={settings.liquidGlass}
                variant="frost"
                radius={18}
                className="divide-y divide-black/6 dark:divide-white/8"
              >
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
                      <div className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">{item.sub}</div>
                    </div>
                    <LiquidToggle selected={item.checked} onSelect={item.onChange} accentColor={accent.primary} />
                  </div>
                ))}
              </LiquidGlassSurface>
            </div>
          )}

          {step === 3 && (
            <div className="space-y-4 animate-stagger-in">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold">Pen &amp; Ink Feel Check</h3>
                  <p className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                    Sketch below to test latency and smooth ink curve response before jumping in
                  </p>
                </div>
                <LiquidButton onClick={clearPad} className="!px-3 !py-1.5 !text-xs">
                  Clear Pad
                </LiquidButton>
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
              <div className="flex items-center justify-between text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                <span>
                  {inkCount > 0
                    ? 'Smooth ink verified! Tip: drag toolbar icons to reorder anytime.'
                    : 'Draw a quick loop or signature above.'}
                </span>
                <span className="font-mono-num font-semibold text-emerald-700 dark:text-emerald-400">
                  Studio Ready
                </span>
              </div>
            </div>
          )}
        </div>

        {/* Footer Nav */}
        <div className="flex items-center justify-between border-t border-black/8 px-6 py-4 dark:border-white/10">
          <LiquidButton disabled={step === 0} onClick={() => setStep((s) => Math.max(0, s - 1))}>
            <ArrowLeft className="h-3.5 w-3.5" /> Back
          </LiquidButton>
          {step < STEPS.length - 1 ? (
            <LiquidButton tint={accent.primary} onClick={() => setStep((s) => Math.min(STEPS.length - 1, s + 1))}>
              Next <ArrowRight className="h-3.5 w-3.5" />
            </LiquidButton>
          ) : (
            <LiquidButton tint={accent.primary} onClick={onFinish}>
              Launch Pro Notes <Sparkles className="h-3.5 w-3.5" />
            </LiquidButton>
          )}
        </div>
      </div>
    </LiquidModal>
  );
};
