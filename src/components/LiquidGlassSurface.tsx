import React, { useEffect, useRef, useState } from 'react';
import { LiquidGlass } from 'liquid-glass-web-react';

interface LiquidGlassSurfaceProps {
  enabled: boolean;
  className?: string;
  style?: React.CSSProperties;
  radius?: number;
  children: React.ReactNode;
}

/**
 * Optional real SVG-displacement Liquid Glass surface powered by `liquid-glass-web-react`.
 * When `enabled` is false, renders a clean lightweight container without refraction overhead.
 */
export const LiquidGlassSurface: React.FC<LiquidGlassSurfaceProps> = ({
  enabled,
  className = '',
  style,
  radius = 24,
  children,
}) => {
  const hostRef = useRef<HTMLDivElement>(null);
  const [box, setBox] = useState({ w: 280, h: 56 });

  useEffect(() => {
    if (!enabled) return;
    const el = hostRef.current;
    if (!el) return;
    const update = () => {
      const r = el.getBoundingClientRect();
      if (r.width > 16 && r.height > 16) {
        setBox({ w: Math.round(r.width), h: Math.round(r.height) });
      }
    };
    update();
    const ro = new ResizeObserver(update);
    ro.observe(el);
    return () => ro.disconnect();
  }, [enabled]);

  if (!enabled) {
    return (
      <div ref={hostRef} className={className} style={style}>
        {children}
      </div>
    );
  }

  return (
    <div ref={hostRef} className={`relative ${className}`} style={style}>
      <div className="pointer-events-none absolute inset-0 overflow-hidden" style={{ borderRadius: radius }}>
        <LiquidGlass
          x={0.5}
          y={0.5}
          width={Math.max(40, box.w)}
          height={Math.max(32, box.h)}
          radius={radius}
          strength={0.08}
          chromaticAberration={0.18}
          blur={6}
          curvature={0.55}
          glow={0.16}
          edgeHighlight={0.34}
          shadow={false}
          className="h-full w-full"
        >
          <div className="liquid-glass-surface h-full w-full" style={{ borderRadius: radius }} />
        </LiquidGlass>
      </div>
      <div className="relative z-10 flex items-center">{children}</div>
    </div>
  );
};
