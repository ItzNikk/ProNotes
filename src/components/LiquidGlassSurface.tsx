import React, { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Check, ChevronDown } from 'lucide-react';

export type GlassVariant = 'chrome' | 'card' | 'control' | 'frost' | 'accent';

interface LiquidGlassSurfaceProps {
  enabled?: boolean;
  variant?: GlassVariant;
  className?: string;
  style?: React.CSSProperties;
  radius?: number;
  interactiveGlare?: boolean;
  children: React.ReactNode;
}

/**
 * ntvien/liquid-glass Prebuilt Component Suite (Web & Windows parity):
 * - LiquidGlassSurface (chrome | card | control | frost | accent, zero React re-render hover glare)
 * - LiquidButton (interactive highlight + damped drag stretch)
 * - LiquidToggle (capsule track + droplet pill thumb)
 * - LiquidSlider (GPU translate3d clamped thumb + pointer capture + zero drag lag)
 * - LiquidSegmentedControl (frosted track + sliding droplet pill)
 * - LiquidMenuButton (droplet-morphing frosted menu dropdown with high-contrast scrim)
 * - LiquidModal (prebuilt liquid glass dialog backdrop & chrome panel)
 * - LiquidInput (frosted glass text/search input)
 * - LiquidBottomTabs & LiquidBottomTab (scrubbable GPU droplet pill)
 */
export const LiquidGlassSurface: React.FC<LiquidGlassSurfaceProps> = ({
  enabled = true,
  variant = 'card',
  className = '',
  style,
  radius = 24,
  interactiveGlare = true,
  children,
}) => {
  const hostRef = useRef<HTMLDivElement>(null);
  const glareRef = useRef<HTMLDivElement>(null);
  const rafRef = useRef<number | null>(null);

  const updateGlareDom = useCallback((xPct: number, yPct: number, active: boolean) => {
    const el = glareRef.current;
    if (!el) return;
    el.style.setProperty('--gx', `${xPct}%`);
    el.style.setProperty('--gy', `${yPct}%`);
    el.style.opacity = active ? '1' : '0.35';
  }, []);

  const handlePointerMove = useCallback(
    (e: React.PointerEvent<HTMLDivElement>) => {
      if (!enabled || !interactiveGlare || variant === 'frost') return;
      const host = hostRef.current;
      if (!host) return;
      const rect = host.getBoundingClientRect();
      if (rect.width <= 0 || rect.height <= 0) return;
      const x = Math.max(0, Math.min(100, Math.round(((e.clientX - rect.left) / rect.width) * 100)));
      const y = Math.max(0, Math.min(100, Math.round(((e.clientY - rect.top) / rect.height) * 100)));
      if (rafRef.current !== null) cancelAnimationFrame(rafRef.current);
      rafRef.current = requestAnimationFrame(() => {
        updateGlareDom(x, y, true);
        rafRef.current = null;
      });
    },
    [enabled, interactiveGlare, variant, updateGlareDom]
  );

  const handlePointerLeave = useCallback(() => {
    if (rafRef.current !== null) cancelAnimationFrame(rafRef.current);
    updateGlareDom(50, 28, false);
  }, [updateGlareDom]);

  useEffect(
    () => () => {
      if (rafRef.current !== null) cancelAnimationFrame(rafRef.current);
    },
    []
  );

  if (!enabled) {
    return (
      <div
        ref={hostRef}
        className={`relative border border-[#E0DBD0] bg-[#FBF9F4] dark:border-[#2E2C26] dark:bg-[#1D1C18] ${className}`}
        style={{ borderRadius: radius, ...style }}
      >
        {children}
      </div>
    );
  }

  if (variant === 'frost') {
    return (
      <div
        ref={hostRef}
        className={`liquid-glass-frost relative isolate overflow-hidden ${className}`}
        style={{ borderRadius: radius, ...style }}
      >
        <div className="relative z-10 h-full">{children}</div>
      </div>
    );
  }

  const isChrome = variant === 'chrome';
  const fillAlphaExpr = isChrome ? 'var(--lg-chrome-alpha, 0.64)' : 'var(--lg-surface-alpha, 0.56)';

  return (
    <div
      ref={hostRef}
      onPointerMove={handlePointerMove}
      onPointerLeave={handlePointerLeave}
      className={`relative isolate overflow-hidden ${className}`}
      style={{
        borderRadius: radius,
        transform: 'translateZ(0)',
        ...style,
      }}
    >
      {/* Layer 1: Adaptive GPU Blur & Saturation */}
      <div
        className="pointer-events-none absolute inset-0 -z-30"
        style={{
          borderRadius: radius,
          backdropFilter: 'blur(var(--lg-blur, 20px)) saturate(180%) contrast(102%)',
          WebkitBackdropFilter: 'blur(var(--lg-blur, 20px)) saturate(180%) contrast(102%)',
        }}
      />

      {/* Layer 2: Adaptive Contrast Scrim Tint (Light / Dark safe) */}
      <div
        className="pointer-events-none absolute inset-0 -z-20 dark:hidden"
        style={{
          borderRadius: radius,
          background: `linear-gradient(135deg, rgba(251, 249, 244, calc(${fillAlphaExpr} + 0.12)), rgba(244, 240, 231, ${fillAlphaExpr}))`,
        }}
      />
      <div
        className="pointer-events-none absolute inset-0 -z-20 hidden dark:block"
        style={{
          borderRadius: radius,
          background: `linear-gradient(135deg, rgba(34, 33, 28, calc(${fillAlphaExpr} + 0.16)), rgba(22, 21, 18, ${fillAlphaExpr}))`,
        }}
      />

      {/* Layer 3: Interactive Highlight Radial Glare (CSS var driven, zero React re-renders) */}
      {interactiveGlare && (
        <div
          ref={glareRef}
          className="pointer-events-none absolute inset-0 -z-10 transition-opacity duration-200"
          style={
            {
              borderRadius: radius,
              opacity: 0.35,
              '--gx': '50%',
              '--gy': '28%',
              background:
                'radial-gradient(circle 180px at var(--gx) var(--gy), rgba(255, 255, 255, 0.36) 0%, rgba(255, 255, 255, 0.06) 45%, transparent 75%)',
            } as React.CSSProperties
          }
        />
      )}

      {/* Layer 4: Specular Edge Lens Rim */}
      <div
        className="pointer-events-none absolute inset-0 -z-10 border border-white/55 dark:border-[#EDE8DB]/16"
        style={{
          borderRadius: radius,
          boxShadow: isChrome
            ? 'inset 0 1.5px 1.5px rgba(255,255,255,0.88), inset 0 -1px 2px rgba(255,255,255,0.16), 0 18px 40px -12px rgba(20,24,21,0.22)'
            : 'inset 0 1.5px 1.5px rgba(255,255,255,0.84), inset 0 -1px 2px rgba(255,255,255,0.14), 0 12px 28px -12px rgba(20,24,21,0.16)',
        }}
      />

      <div className="relative z-10">{children}</div>
    </div>
  );
};

/**
 * Prebuilt LiquidButton:
 * Capsule button with radial sheen + damped press stretch (zero re-render glare updates).
 */
export const LiquidButton: React.FC<{
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void;
  tint?: string;
  surfaceColor?: string;
  className?: string;
  title?: string;
  disabled?: boolean;
  children: React.ReactNode;
}> = ({ onClick, tint, surfaceColor, className = '', title, disabled, children }) => {
  const ref = useRef<HTMLButtonElement>(null);
  const spotRef = useRef<HTMLSpanElement>(null);
  const [pressed, setPressed] = useState(false);

  return (
    <button
      ref={ref}
      type="button"
      title={title}
      disabled={disabled}
      onClick={onClick}
      onPointerDown={() => !disabled && setPressed(true)}
      onPointerMove={(e) => {
        const rect = ref.current?.getBoundingClientRect();
        const spot = spotRef.current;
        if (rect && spot && rect.width > 0 && rect.height > 0) {
          const x = Math.max(0, Math.min(100, ((e.clientX - rect.left) / rect.width) * 100));
          const y = Math.max(0, Math.min(100, ((e.clientY - rect.top) / rect.height) * 100));
          spot.style.setProperty('--bx', `${x.toFixed(1)}%`);
          spot.style.setProperty('--by', `${y.toFixed(1)}%`);
        }
      }}
      onPointerUp={() => setPressed(false)}
      onPointerLeave={() => setPressed(false)}
      style={{
        transform: pressed ? 'scale(0.96, 1.03)' : 'scale(1)',
        backgroundColor: tint || surfaceColor || undefined,
      }}
      className={`relative isolate inline-flex items-center justify-center gap-2 overflow-hidden rounded-full px-4 py-2 text-sm font-semibold transition-transform duration-150 select-none ${
        tint
          ? 'text-[#F5F2E8] shadow-[0_10px_24px_-6px_rgba(36,72,63,0.40)]'
          : 'liquid-glass-frost text-[#1F2421] hover:brightness-[0.98] dark:text-[#EDE8DB]'
      } disabled:opacity-40 ${className}`}
    >
      <span
        ref={spotRef}
        className="pointer-events-none absolute inset-0 -z-10 transition-opacity duration-150"
        style={
          {
            opacity: pressed ? 0.9 : 0.35,
            '--bx': '50%',
            '--by': '50%',
            background: 'radial-gradient(circle 90px at var(--bx) var(--by), rgba(255,255,255,0.4), transparent 75%)',
          } as React.CSSProperties
        }
      />
      {children}
    </button>
  );
};

/**
 * Prebuilt LiquidToggle:
 * Capsule track + frosted refractive glass pill thumb.
 */
export const LiquidToggle: React.FC<{
  selected: boolean;
  onSelect: (value: boolean) => void;
  accentColor?: string;
}> = ({ selected, onSelect, accentColor = '#24483F' }) => {
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
        backgroundColor: selected ? accentColor : 'rgba(120, 124, 118, 0.28)',
        boxShadow: 'inset 0 1px 2px rgba(0,0,0,0.16), 0 0 0 1px rgba(255,255,255,0.22)',
      }}
    >
      <span
        className="pointer-events-none inline-block h-6 w-8 rounded-full transition-transform duration-200"
        style={{
          transform: `translate3d(${selected ? '20px' : '0px'}, 0, 0) scale(${pressed ? 1.2 : 1})`,
          background: pressed
            ? 'rgba(255, 255, 255, 0.85)'
            : 'linear-gradient(180deg, #FFFFFF 0%, #F7F5EE 100%)',
          boxShadow: pressed
            ? '0 4px 14px rgba(0,0,0,0.22), inset 0 1.5px 1px rgba(255,255,255,0.95), 0 0 0 1px rgba(36,72,63,0.35)'
            : '0 2px 6px rgba(0,0,0,0.20), inset 0 1px 1px rgba(255,255,255,0.95)',
        }}
      />
    </button>
  );
};

/**
 * Fixed Prebuilt LiquidSlider:
 * Pointer-captured horizontal scrubbing, GPU translate3d thumb clamped cleanly inside track bounds,
 * and zero sluggish CSS transitions during drag.
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
  const trackRef = useRef<HTMLDivElement>(null);
  const [dragging, setDragging] = useState(false);
  const [trackWidth, setTrackWidth] = useState(220);
  const thumbWidth = 36;

  useLayoutEffect(() => {
    const el = trackRef.current;
    if (!el) return;
    const measure = () => {
      const w = el.getBoundingClientRect().width;
      if (w > 0) setTrackWidth(w);
    };
    measure();
    if (typeof ResizeObserver !== 'undefined') {
      const ro = new ResizeObserver(measure);
      ro.observe(el);
      return () => ro.disconnect();
    }
  }, []);

  const span = Math.max(0.0001, max - min);
  const clampedValue = Math.max(min, Math.min(max, value));
  const fraction = Math.max(0, Math.min(1, (clampedValue - min) / span));
  const usableWidth = Math.max(1, trackWidth - thumbWidth);
  const thumbX = fraction * usableWidth;

  const commitFromClientX = useCallback(
    (clientX: number) => {
      const el = trackRef.current;
      if (!el) return;
      const rect = el.getBoundingClientRect();
      const usable = Math.max(1, rect.width - thumbWidth);
      const rawFrac = (clientX - rect.left - thumbWidth / 2) / usable;
      const clampedFrac = Math.max(0, Math.min(1, rawFrac));
      const rawVal = min + clampedFrac * span;
      const stepped =
        step > 0 ? Math.round((rawVal - min) / step) * step + min : rawVal;
      const decimals = step < 1 ? String(step).split('.')[1]?.length || 2 : 0;
      const rounded = Number(Math.max(min, Math.min(max, stepped)).toFixed(decimals));
      onChange(rounded);
    },
    [min, max, span, step, onChange]
  );

  return (
    <div
      ref={trackRef}
      role="slider"
      aria-valuemin={min}
      aria-valuemax={max}
      aria-valuenow={clampedValue}
      tabIndex={0}
      onKeyDown={(e) => {
        const delta = step > 0 ? step : span / 20;
        if (e.key === 'ArrowRight' || e.key === 'ArrowUp') {
          e.preventDefault();
          onChange(Math.min(max, Number((clampedValue + delta).toFixed(4))));
        } else if (e.key === 'ArrowLeft' || e.key === 'ArrowDown') {
          e.preventDefault();
          onChange(Math.max(min, Number((clampedValue - delta).toFixed(4))));
        }
      }}
      onPointerDown={(e) => {
        if (e.button !== 0 && e.pointerType === 'mouse') return;
        e.preventDefault();
        setDragging(true);
        try {
          e.currentTarget.setPointerCapture(e.pointerId);
        } catch {}
        commitFromClientX(e.clientX);
      }}
      onPointerMove={(e) => {
        if (!dragging) return;
        commitFromClientX(e.clientX);
      }}
      onPointerUp={(e) => {
        if (!dragging) return;
        setDragging(false);
        try {
          e.currentTarget.releasePointerCapture(e.pointerId);
        } catch {}
      }}
      onPointerCancel={() => setDragging(false)}
      className={`relative flex h-8 cursor-pointer items-center touch-none select-none ${className}`}
    >
      <div className="relative h-1.5 w-full overflow-hidden rounded-full border border-white/40 bg-black/12 dark:border-white/10 dark:bg-white/16">
        <div
          className="h-full rounded-full"
          style={{
            width: `${Math.max(4, fraction * 100)}%`,
            backgroundColor: accentColor,
            transition: dragging ? 'none' : 'width 120ms cubic-bezier(0.22, 1, 0.36, 1)',
          }}
        />
      </div>
      <div
        className="pointer-events-none absolute top-1/2 left-0 rounded-full"
        style={{
          width: thumbWidth,
          height: 22,
          transform: `translate3d(${thumbX.toFixed(1)}px, -50%, 0) scale(${dragging ? 1.16 : 1})`,
          transition: dragging ? 'transform 90ms ease-out' : 'transform 160ms cubic-bezier(0.22, 1.32, 0.36, 1)',
          background: dragging
            ? 'rgba(255, 255, 255, 0.88)'
            : 'linear-gradient(180deg, #FFFFFF 0%, #F7F4EC 100%)',
          boxShadow: dragging
            ? `0 6px 18px rgba(0,0,0,0.24), inset 0 1.5px 1.5px rgba(255,255,255,0.95), 0 0 0 1.5px ${accentColor}66`
            : '0 2px 8px rgba(0,0,0,0.20), inset 0 1px 1px rgba(255,255,255,0.95), 0 0 0 1px rgba(31,36,33,0.12)',
        }}
      />
    </div>
  );
};

/**
 * Prebuilt LiquidSegmentedControl (ntvien/liquid-glass parity):
 * Clear frosted track with raised sliding droplet pill indicator.
 */
export interface SegmentedOption<T extends string> {
  value: T;
  label: string;
}

export function LiquidSegmentedControl<T extends string>({
  options,
  value,
  onChange,
  accentColor = '#24483F',
  className = '',
}: {
  options: SegmentedOption<T>[];
  value: T;
  onChange: (next: T) => void;
  accentColor?: string;
  className?: string;
}) {
  const activeIdx = Math.max(0, options.findIndex((o) => o.value === value));
  const count = Math.max(1, options.length);

  return (
    <div
      role="tablist"
      className={`liquid-glass-frost relative grid items-center rounded-full p-1 select-none ${className}`}
      style={{ gridTemplateColumns: `repeat(${count}, minmax(0, 1fr))` }}
    >
      <div
        className="pointer-events-none absolute top-1 bottom-1 left-1 rounded-full transition-transform duration-200 ease-[cubic-bezier(0.22,1.25,0.36,1)]"
        style={{
          width: `calc((100% - 8px) / ${count})`,
          transform: `translate3d(${activeIdx * 100}%, 0, 0)`,
          background: accentColor,
          boxShadow: 'inset 0 1px 1px rgba(255,255,255,0.35), 0 4px 12px rgba(24,36,31,0.18)',
        }}
      />
      {options.map((opt) => {
        const active = opt.value === value;
        return (
          <button
            key={opt.value}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onChange(opt.value)}
            className={`relative z-10 truncate rounded-full px-3 py-1.5 text-center text-xs font-semibold transition-colors duration-150 ${
              active
                ? 'text-[#F5F2E8]'
                : 'text-[#5A5F58] hover:text-[#1F2421] dark:text-[#B1AC9F] dark:hover:text-[#EDE8DB]'
            }`}
          >
            {opt.label}
          </button>
        );
      })}
    </div>
  );
}

/**
 * Prebuilt LiquidMenuButton & Dropdown (ntvien/liquid-glass LiquidMenu parity):
 * Replaces native <select> and plain popovers with an optical droplet-morphing frosted menu.
 */
export interface LiquidMenuItemOption {
  id: string;
  label: string;
  subtitle?: string;
  icon?: React.ReactNode;
  selected?: boolean;
  destructive?: boolean;
  disabled?: boolean;
  dividerBefore?: boolean;
  onSelect: () => void;
}

export const LiquidMenuButton: React.FC<{
  items: LiquidMenuItemOption[];
  label?: string;
  icon?: React.ReactNode;
  trigger?: (open: boolean) => React.ReactNode;
  align?: 'left' | 'right';
  menuWidthClass?: string;
  className?: string;
  title?: string;
}> = ({
  items,
  label,
  icon,
  trigger,
  align = 'right',
  menuWidthClass = 'min-w-[210px]',
  className = '',
  title,
}) => {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false);
    };
    const onDown = (e: PointerEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    window.addEventListener('keydown', onKey);
    window.addEventListener('pointerdown', onDown, true);
    return () => {
      window.removeEventListener('keydown', onKey);
      window.removeEventListener('pointerdown', onDown, true);
    };
  }, [open]);

  return (
    <div ref={rootRef} className={`relative inline-block ${className}`}>
      <div
        role="button"
        tabIndex={0}
        title={title}
        aria-expanded={open}
        aria-haspopup="menu"
        onClick={(e) => {
          e.stopPropagation();
          setOpen((v) => !v);
        }}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            setOpen((v) => !v);
          }
        }}
        className="cursor-pointer select-none"
      >
        {trigger ? (
          trigger(open)
        ) : (
          <div className="liquid-glass-frost inline-flex items-center gap-2 rounded-full px-3.5 py-2 text-xs font-semibold text-[#1F2421] transition-transform duration-150 active:scale-95 dark:text-[#EDE8DB]">
            {icon}
            {label && <span className="max-w-[160px] truncate">{label}</span>}
            <ChevronDown
              size={14}
              className={`opacity-70 transition-transform duration-200 ${open ? 'rotate-180' : ''}`}
            />
          </div>
        )}
      </div>

      {open && (
        <div
          role="menu"
          className={`liquid-glass-menu animate-stagger-in absolute top-full z-[90] mt-2 overflow-hidden rounded-[22px] p-1.5 ${menuWidthClass} ${
            align === 'right' ? 'right-0' : 'left-0'
          }`}
        >
          <div className="max-h-[320px] space-y-0.5 overflow-y-auto">
            {items.map((item) => (
              <React.Fragment key={item.id}>
                {item.dividerBefore && (
                  <div className="my-1 h-px bg-[#1F2421]/10 dark:bg-[#EDE8DB]/12" />
                )}
                <button
                  type="button"
                  role="menuitem"
                  disabled={item.disabled}
                  onClick={(e) => {
                    e.stopPropagation();
                    if (item.disabled) return;
                    setOpen(false);
                    item.onSelect();
                  }}
                  className={`flex w-full items-center gap-2.5 rounded-2xl px-3 py-2 text-left text-xs font-semibold transition-colors ${
                    item.destructive
                      ? 'text-[#9B3B3B] hover:bg-[#9B3B3B]/10 dark:text-[#E58A8A]'
                      : item.selected
                      ? 'bg-[#24483F]/14 text-[#24483F] dark:bg-[#8CC1AC]/18 dark:text-[#8CC1AC]'
                      : 'text-[#1F2421] hover:bg-black/6 dark:text-[#EDE8DB] dark:hover:bg-white/10'
                  } disabled:opacity-40`}
                >
                  {item.icon && <span className="shrink-0 opacity-80">{item.icon}</span>}
                  <span className="min-w-0 flex-1">
                    <span className="block truncate">{item.label}</span>
                    {item.subtitle && (
                      <span className="block truncate text-[10px] font-normal opacity-65">
                        {item.subtitle}
                      </span>
                    )}
                  </span>
                  {item.selected && <Check size={14} className="shrink-0" />}
                </button>
              </React.Fragment>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

/**
 * Prebuilt LiquidInput:
 * Frosted glass input/search pill matching OpenLeaf contrast & glass transparency.
 */
export const LiquidInput: React.FC<
  React.InputHTMLAttributes<HTMLInputElement> & { icon?: React.ReactNode; wrapperClassName?: string }
> = ({ icon, wrapperClassName = '', className = '', ...props }) => (
  <div
    className={`liquid-glass-frost flex items-center gap-2 rounded-full px-3.5 py-2 text-sm text-[#1F2421] focus-within:ring-2 focus-within:ring-[#24483F]/30 dark:text-[#EDE8DB] dark:focus-within:ring-[#8CC1AC]/35 ${wrapperClassName}`}
  >
    {icon && <span className="shrink-0 opacity-65">{icon}</span>}
    <input
      {...props}
      className={`w-full bg-transparent text-xs font-medium outline-none placeholder:text-[#5A5F58] dark:placeholder:text-[#B1AC9F] ${className}`}
    />
  </div>
);

/**
 * Prebuilt LiquidModal:
 * Optical glass dialog container with backdrop scrim and high-contrast glass menu surface.
 */
export const LiquidModal: React.FC<{
  open: boolean;
  onClose?: () => void;
  maxWidthClass?: string;
  className?: string;
  children: React.ReactNode;
}> = ({ open, onClose, maxWidthClass = 'max-w-lg', className = '', children }) => {
  if (!open) return null;
  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/38 p-4 backdrop-blur-md"
      onClick={onClose}
    >
      <div
        className={`liquid-glass-menu animate-stagger-in w-full overflow-hidden rounded-[28px] text-[#1F2421] dark:text-[#EDE8DB] ${maxWidthClass} ${className}`}
        onClick={(e) => e.stopPropagation()}
      >
        {children}
      </div>
    </div>
  );
};

/**
 * Prebuilt LiquidBottomTab & LiquidBottomTabs:
 * Capsule bottom navigation bar with scrubbable GPU translate3d droplet pill indicator.
 */
export const LiquidBottomTab: React.FC<{
  active?: boolean;
  onClick: () => void;
  children: React.ReactNode;
}> = ({ active, onClick, children }) => (
  <button
    type="button"
    onClick={onClick}
    className={`relative z-10 flex flex-1 flex-col items-center justify-center gap-0.5 rounded-full px-4 py-1.5 text-[11px] font-bold transition-transform duration-150 select-none ${
      active
        ? 'scale-105 text-[#24483F] dark:text-[#8CC1AC]'
        : 'text-[#5A5F58] hover:text-[#1F2421] dark:text-[#B1AC9F] dark:hover:text-[#EDE8DB]'
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
      variant="chrome"
      radius={999}
      className={`pointer-events-auto rounded-full transition-transform duration-150 ${
        scrubbing ? 'scale-[1.03]' : 'scale-100'
      } ${className}`}
    >
      <div
        ref={rowRef}
        onPointerDown={(e) => {
          if (e.button !== 0) return;
          setScrubbing(true);
          try {
            e.currentTarget.setPointerCapture(e.pointerId);
          } catch {}
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
        {tabCount > 0 && selectedTabIndex >= 0 && selectedTabIndex < tabCount && (
          <div
            className="pointer-events-none absolute top-1 bottom-1 left-1.5 rounded-full transition-transform duration-200 ease-[cubic-bezier(0.22,1.25,0.36,1)]"
            style={{
              width: `calc((100% - 12px) / ${tabCount})`,
              transform: `translate3d(${selectedTabIndex * 100}%, 0, 0) scale(${scrubbing ? 1.08 : 1})`,
              background:
                'linear-gradient(135deg, rgba(255,255,255,0.84) 0%, rgba(244,238,223,0.62) 100%)',
              boxShadow:
                'inset 0 1.5px 1px rgba(255,255,255,0.95), 0 4px 14px rgba(36,72,63,0.16), 0 0 0 1px rgba(36,72,63,0.14)',
            }}
          />
        )}
        {children}
      </div>
    </LiquidGlassSurface>
  );
};
