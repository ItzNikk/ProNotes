import React, { useCallback, useRef, useState } from 'react';

interface LiquidGlassSurfaceProps {
  enabled: boolean;
  className?: string;
  style?: React.CSSProperties;
  radius?: number;
  interactiveGlare?: boolean;
  children: React.ReactNode;
}

/**
 * Kyant0/AndroidLiquidGlass Prebuilt Catalog Components for Web & Windows:
 * - LiquidGlassSurface
 * - LiquidButton (interactive highlight + tanh damped drag stretch)
 * - LiquidToggle (capsule track + expanding refractive glass pill thumb)
 * - LiquidSlider (progress track + expanding refractive glass pill thumb with dispersion rim)
 * - LiquidBottomTabs & LiquidBottomTab (scrubbable refractive glass indicator pill)
 */
export const LiquidGlassSurface: React.FC<LiquidGlassSurfaceProps> = ({
  enabled,
  className = '',
  style,
  radius = 28,
  interactiveGlare = true,
  children,
}) => {
  const hostRef = useRef<HTMLDivElement>(null);
  const [glare, setGlare] = useState<{ x: number; y: number; active: boolean }>({
    x: 50,
    y: 30,
    active: false,
  });

  const handlePointerMove = useCallback(
    (e: React.PointerEvent<HTMLDivElement>) => {
      if (!enabled || !interactiveGlare) return;
      const rect = hostRef.current?.getBoundingClientRect();
      if (!rect || rect.width === 0 || rect.height === 0) return;
      const x = Math.max(0, Math.min(100, Math.round(((e.clientX - rect.left) / rect.width) * 100)));
      const y = Math.max(0, Math.min(100, Math.round(((e.clientY - rect.top) / rect.height) * 100)));
      setGlare({ x, y, active: true });
    },
    [enabled, interactiveGlare]
  );

  const handlePointerLeave = useCallback(() => {
    setGlare((g) => ({ ...g, active: false, x: 50, y: 30 }));
  }, []);

  if (!enabled) {
    return (
      <div ref={hostRef} className={className} style={style}>
        {children}
      </div>
    );
  }

  return (
    <div
      ref={hostRef}
      onPointerMove={handlePointerMove}
      onPointerLeave={handlePointerLeave}
      className={`relative isolate overflow-hidden ${className}`}
      style={{
        borderRadius: radius,
        ...style,
      }}
    >
      {/* Layer 1: Hardware-accelerated Optical Depth Blur */}
      <div
        className="pointer-events-none absolute inset-0 -z-30 backdrop-blur-[28px] backdrop-saturate-[190%] backdrop-contrast-[102%]"
        style={{ borderRadius: radius }}
      />

      {/* Layer 2: Translucent Liquid Body Tint */}
      <div
        className="pointer-events-none absolute inset-0 -z-20 bg-gradient-to-br from-white/55 via-white/35 to-white/22 dark:from-[#24231E]/80 dark:via-[#1C1B17]/65 dark:to-[#141411]/55"
        style={{ borderRadius: radius }}
      />

      {/* Layer 3: InteractiveHighlight Radial Glare */}
      {interactiveGlare && (
        <div
          className="pointer-events-none absolute inset-0 -z-10 transition-opacity duration-300"
          style={{
            borderRadius: radius,
            opacity: glare.active ? 1 : 0.4,
            background: `radial-gradient(circle 170px at ${glare.x}% ${glare.y}%, rgba(255, 255, 255, 0.46) 0%, rgba(255, 255, 255, 0.08) 45%, transparent 75%)`,
          }}
        />
      )}

      {/* Layer 4: Top Glossy Sheen & Refractive Rim */}
      <div
        className="pointer-events-none absolute inset-0 -z-10"
        style={{
          borderRadius: radius,
          boxShadow: `
            inset 0 1.5px 1.5px 0 rgba(255, 255, 255, 0.88),
            inset 0 -1px 2px 0 rgba(255, 255, 255, 0.18),
            inset 0 0 14px 0 rgba(255, 255, 255, 0.08),
            0 14px 36px -12px rgba(24, 28, 24, 0.20)
          `,
          border: '1px solid rgba(255, 255, 255, 0.44)',
        }}
      />

      <div className="relative z-10">{children}</div>
    </div>
  );
};

/**
 * Prebuilt Kyant0 LiquidButton:
 * Capsule button with InteractiveHighlight radial pointer sheen + tanh damped drag stretch.
 */
export const LiquidButton: React.FC<{
  onClick?: () => void;
  tint?: string;
  surfaceColor?: string;
  className?: string;
  title?: string;
  disabled?: boolean;
  children: React.ReactNode;
}> = ({ onClick, tint, surfaceColor, className = '', title, disabled, children }) => {
  const ref = useRef<HTMLButtonElement>(null);
  const [pressed, setPressed] = useState(false);
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const [spot, setSpot] = useState({ x: 50, y: 50 });
  const startRef = useRef<{ x: number; y: number } | null>(null);

  const scaleX = pressed ? 1 + Math.tanh(offset.x / 90) * 0.09 + 0.04 : 1;
  const scaleY = pressed ? 1 + Math.tanh(offset.y / 60) * 0.09 + 0.04 : 1;

  return (
    <button
      ref={ref}
      type="button"
      title={title}
      disabled={disabled}
      onClick={onClick}
      onPointerDown={(e) => {
        if (disabled) return;
        setPressed(true);
        startRef.current = { x: e.clientX, y: e.clientY };
      }}
      onPointerMove={(e) => {
        const rect = ref.current?.getBoundingClientRect();
        if (rect && rect.width > 0 && rect.height > 0) {
          setSpot({
            x: Math.max(0, Math.min(100, ((e.clientX - rect.left) / rect.width) * 100)),
            y: Math.max(0, Math.min(100, ((e.clientY - rect.top) / rect.height) * 100)),
          });
        }
        if (pressed && startRef.current) {
          setOffset({
            x: e.clientX - startRef.current.x,
            y: e.clientY - startRef.current.y,
          });
        }
      }}
      onPointerUp={() => {
        setPressed(false);
        setOffset({ x: 0, y: 0 });
        startRef.current = null;
      }}
      onPointerLeave={() => {
        setPressed(false);
        setOffset({ x: 0, y: 0 });
        startRef.current = null;
      }}
      style={{
        transform: `scale(${scaleX.toFixed(3)}, ${scaleY.toFixed(3)})`,
        backgroundColor: tint || surfaceColor || undefined,
      }}
      className={`relative isolate inline-flex items-center justify-center gap-2 overflow-hidden rounded-full px-4 py-2 text-sm font-semibold transition-transform duration-200 select-none ${
        tint
          ? 'text-white shadow-[0_10px_24px_-6px_rgba(36,72,63,0.42)]'
          : 'border border-white/55 bg-white/75 text-[#1D211E] shadow-[0_8px_24px_rgba(20,24,21,0.12)] backdrop-blur-xl dark:border-white/15 dark:bg-[#22211C]/80 dark:text-[#ECE8DE]'
      } disabled:opacity-40 ${className}`}
    >
      <span
        className="pointer-events-none absolute inset-0 -z-10 transition-opacity duration-200"
        style={{
          opacity: pressed ? 0.95 : 0.4,
          background: `radial-gradient(circle 90px at ${spot.x}% ${spot.y}%, rgba(255,255,255,0.42), transparent 75%)`,
        }}
      />
      <span
        className="pointer-events-none absolute inset-0 -z-10 rounded-full"
        style={{
          boxShadow: 'inset 0 1px 1px rgba(255,255,255,0.75), inset 0 -1px 1px rgba(0,0,0,0.08)',
        }}
      />
      {children}
    </button>
  );
};

/**
 * Prebuilt Kyant0 LiquidToggle:
 * Capsule track (64x28px) + refractive glass pill thumb (40x24px) that expands and refracts when pressed.
 */
export const LiquidToggle: React.FC<{
  selected: boolean;
  onSelect: (value: boolean) => void;
  accentColor?: string;
}> = ({ selected, onSelect, accentColor = '#2F6B4F' }) => {
  const [pressed, setPressed] = useState(false);

  return (
    <button
      type="button"
      role="switch"
      aria-checked={selected}
      onPointerDown={() => setPressed(true)}
      onPointerUp={() => setPressed(false)}
      onPointerLeave={() => setPressed(false)}
      onClick={(e) => {
        e.stopPropagation();
        onSelect(!selected);
      }}
      className="relative inline-flex h-7 w-14 shrink-0 cursor-pointer items-center rounded-full p-0.5 transition-colors duration-200 select-none"
      style={{
        backgroundColor: selected ? accentColor : 'rgba(120, 124, 118, 0.26)',
        boxShadow: 'inset 0 1px 2px rgba(0,0,0,0.14)',
      }}
    >
      <span
        className="pointer-events-none inline-block h-6 w-8 rounded-full transition-all duration-200"
        style={{
          transform: `translateX(${selected ? '20px' : '0px'}) scale(${pressed ? 1.26 : 1})`,
          background: pressed
            ? 'rgba(255, 255, 255, 0.72)'
            : 'linear-gradient(180deg, #FFFFFF 0%, #F7F5EE 100%)',
          backdropFilter: 'blur(12px) saturate(180%)',
          boxShadow: pressed
            ? '0 4px 14px rgba(0,0,0,0.22), inset 0 1.5px 1px rgba(255,255,255,0.95), 0 0 0 1px rgba(147,197,253,0.45)'
            : '0 2px 6px rgba(0,0,0,0.18), inset 0 1px 1px rgba(255,255,255,0.9)',
        }}
      />
    </button>
  );
};

/**
 * Prebuilt Kyant0 LiquidSlider:
 * Capsule progress track + refractive glass pill thumb that expands on drag with dispersion rim.
 */
export const LiquidSlider: React.FC<{
  value: number;
  min: number;
  max: number;
  step?: number;
  onChange: (value: number) => void;
  accentColor?: string;
  className?: string;
}> = ({ value, min, max, step = 1, onChange, accentColor = '#24483F', className = '' }) => {
  const [dragging, setDragging] = useState(false);
  const fraction = Math.max(0, Math.min(1, (value - min) / Math.max(0.0001, max - min)));

  return (
    <div className={`relative flex h-8 items-center select-none ${className}`}>
      <div className="relative h-1.5 w-full overflow-hidden rounded-full bg-black/12 dark:bg-white/15">
        <div
          className="h-full rounded-full transition-all duration-75"
          style={{
            width: `${fraction * 100}%`,
            backgroundColor: accentColor,
          }}
        />
      </div>
      <div
        className="pointer-events-none absolute top-1/2 -translate-y-1/2 rounded-full transition-transform duration-150"
        style={{
          left: `calc(${fraction * 100}% - 18px)`,
          width: 36,
          height: 22,
          transform: `scale(${dragging ? 1.24 : 1})`,
          background: dragging
            ? 'rgba(255,255,255,0.68)'
            : 'linear-gradient(180deg, #FFFFFF 0%, #F5F2EA 100%)',
          backdropFilter: 'blur(12px) saturate(180%)',
          boxShadow: dragging
            ? '0 6px 18px rgba(0,0,0,0.24), inset 0 1.5px 1.5px rgba(255,255,255,0.95), 0 0 0 1px rgba(147,197,253,0.5)'
            : '0 2px 8px rgba(0,0,0,0.18), inset 0 1px 1px rgba(255,255,255,0.9)',
        }}
      />
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onPointerDown={() => setDragging(true)}
        onPointerUp={() => setDragging(false)}
        onPointerCancel={() => setDragging(false)}
        onChange={(e) => onChange(Number(e.target.value))}
        className="absolute inset-0 h-full w-full cursor-pointer opacity-0"
      />
    </div>
  );
};

/**
 * Prebuilt Kyant0 LiquidBottomTab & LiquidBottomTabs:
 * Capsule bottom navigation bar with a scrubbable/draggable refractive glass pill indicator.
 */
export const LiquidBottomTab: React.FC<{
  active?: boolean;
  onClick: () => void;
  children: React.ReactNode;
}> = ({ active, onClick, children }) => (
  <button
    type="button"
    onClick={onClick}
    className={`relative z-10 flex flex-1 flex-col items-center justify-center gap-0.5 rounded-full px-4 py-1.5 text-[11px] font-bold transition-transform duration-200 select-none ${
      active
        ? 'scale-105 text-[#24483F] dark:text-[#8CC1AC]'
        : 'text-[#5A5E58] hover:text-[#1D211E] dark:text-[#A4A198] dark:hover:text-[#ECE8DE]'
    }`}
  >
    {children}
  </button>
);

export const LiquidBottomTabs: React.FC<{
  selectedTabIndex: number;
  tabCount: number;
  onTabSelected: (index: number) => void;
  enabled?: boolean;
  className?: string;
  children: React.ReactNode;
}> = ({ selectedTabIndex, tabCount, onTabSelected, enabled = true, className = '', children }) => {
  const rowRef = useRef<HTMLDivElement>(null);
  const [scrubbing, setScrubbing] = useState(false);

  const handlePointer = (clientX: number) => {
    const rect = rowRef.current?.getBoundingClientRect();
    if (!rect || rect.width <= 0 || tabCount <= 0) return;
    const rel = clientX - rect.left;
    const idx = Math.max(0, Math.min(tabCount - 1, Math.floor((rel / rect.width) * tabCount)));
    onTabSelected(idx);
  };

  return (
    <LiquidGlassSurface
      enabled={enabled}
      radius={999}
      className={`pointer-events-auto rounded-full border border-white/55 bg-[#FBF9F4]/80 shadow-[0_14px_38px_rgba(24,28,24,0.16)] backdrop-blur-2xl transition-transform duration-200 dark:border-white/15 dark:bg-[#1D1C18]/82 ${
        scrubbing ? 'scale-[1.035]' : 'scale-100'
      } ${className}`}
    >
      <div
        ref={rowRef}
        onPointerDown={(e) => {
          if (e.button !== 0) return;
          setScrubbing(true);
          e.currentTarget.setPointerCapture(e.pointerId);
          handlePointer(e.clientX);
        }}
        onPointerMove={(e) => {
          if (scrubbing) handlePointer(e.clientX);
        }}
        onPointerUp={(e) => {
          if (scrubbing) {
            try {
              e.currentTarget.releasePointerCapture(e.pointerId);
            } catch {}
            setScrubbing(false);
          }
        }}
        onPointerCancel={() => setScrubbing(false)}
        className="relative flex items-center px-1.5 py-1 touch-none"
      >
        {/* Sliding Refractive Pill Indicator */}
        {tabCount > 0 && selectedTabIndex >= 0 && selectedTabIndex < tabCount && (
          <div
            className="pointer-events-none absolute top-1 bottom-1 rounded-full transition-all duration-200"
            style={{
              width: `calc((100% - 12px) / ${tabCount})`,
              left: `calc(6px + ${selectedTabIndex} * ((100% - 12px) / ${tabCount}))`,
              transform: scrubbing ? 'scale(1.08, 1.12)' : 'scale(1)',
              background:
                'linear-gradient(135deg, rgba(255,255,255,0.78) 0%, rgba(244,238,223,0.48) 100%)',
              boxShadow:
                'inset 0 1.5px 1px rgba(255,255,255,0.92), 0 4px 14px rgba(36,72,63,0.14), 0 0 0 1px rgba(36,72,63,0.14)',
            }}
          />
        )}
        {children}
      </div>
    </LiquidGlassSurface>
  );
};
