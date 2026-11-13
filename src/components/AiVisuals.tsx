import React, { useEffect, useRef } from 'react';
import { Sparkles } from 'lucide-react';

export type MotionBackdropVariant = 'ai-wave' | 'pen-setup' | 'space-nebula' | 'quiz-challenge';

/**
 * High-FPS looping Canvas animation backdrop supporting 4 dedicated motion scenes:
 * - 'pen-setup': Solid black canvas with glowing blue light particles moving in opposite circles
 * - 'ai-wave': Abstract blue tech particle data-flow futuristic network digital wave
 * - 'space-nebula': Atmospheric outer space with drifting gas clouds, 3D parallax stars & space dust
 * - 'quiz-challenge': Energetic quiz challenge game spotlight beams & floating A/B/C/D option badges
 */
export const AiParticleBackdrop: React.FC<{
  variant?: MotionBackdropVariant;
  accent?: 'violet' | 'blue' | 'amber';
  intensity?: 'soft' | 'active';
  className?: string;
}> = ({ variant = 'ai-wave', accent = 'blue', intensity = 'soft', className = '' }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    let raf = 0;

    const rand = (seed: number) => {
      const s = Math.sin(seed * 127.1 + 311.7) * 43758.5453;
      return s - Math.floor(s);
    };

    // Scene 1: Pen Setup — glowing blue light particles moving in opposite circles on solid black
    const ringParticles = Array.from({ length: 84 }, (_, i) => {
      const layer = i % 3; // 0: outer CW, 1: middle CCW, 2: inner CW
      return {
        layer,
        dir: layer === 1 ? -1 : 1,
        angle: (i / 28) * Math.PI * 2 + rand(i) * 0.4,
        speed: (layer === 0 ? 0.55 : layer === 1 ? 0.78 : 1.05) * (0.82 + rand(i + 9) * 0.36),
        radiusJitter: (rand(i + 21) - 0.5) * 0.08,
        size: 1.5 + rand(i + 41) * 3.1,
        phase: rand(i + 61) * Math.PI * 2,
      };
    });

    // Scene 2: AI Data Wave — futuristic blue tech particle digital wave & network links
    const waveCols = 22;
    const waveRows = 7;
    const dataPackets = Array.from({ length: 16 }, (_, i) => ({
      row: i % waveRows,
      u: rand(i * 3 + 1),
      speed: 0.12 + rand(i * 5 + 2) * 0.18,
    }));

    // Scene 3: Outer Space — 3D starfield & space dust floating past the camera + drifting gas clouds
    const spaceParticles = Array.from({ length: 120 }, (_, i) => ({
      x: (rand(i * 3 + 1) - 0.5) * 2.2,
      y: (rand(i * 3 + 2) - 0.5) * 2.2,
      z: 0.08 + rand(i * 3 + 3) * 0.92,
      speed: (i % 4 === 0 ? 0.24 : 0.13) * (0.7 + rand(i + 77) * 0.6),
      size: i % 5 === 0 ? 3.1 : 1.3 + rand(i + 88) * 1.4,
      hue: i % 3 === 0 ? '147,197,253' : i % 3 === 1 ? '196,181,253' : '240,249,255',
      isDust: i % 6 === 0,
    }));
    const gasClouds = [
      { bx: 0.24, by: 0.3, col: '79,70,229', r: 0.56, sp: 0.14 },
      { bx: 0.74, by: 0.65, col: '14,165,233', r: 0.52, sp: 0.11 },
      { bx: 0.48, by: 0.48, col: '147,51,234', r: 0.62, sp: 0.09 },
      { bx: 0.65, by: 0.22, col: '56,189,248', r: 0.4, sp: 0.16 },
    ];

    // Scene 4: Quiz Challenge — option selection cards & challenge game motifs
    const optionBadges = ['A', 'B', 'C', 'D', '?', '✓', 'A', 'B', 'C', 'D', '★', '?'].map((label, i) => ({
      label,
      x: 0.06 + rand(i * 7 + 1) * 0.88,
      y: rand(i * 7 + 2),
      vy: 0.028 + rand(i * 7 + 3) * 0.032,
      sway: rand(i * 7 + 4) * Math.PI * 2,
      size: 22 + rand(i * 7 + 5) * 14,
      tint:
        i % 4 === 0
          ? '59,130,246'
          : i % 4 === 1
            ? '16,185,129'
            : i % 4 === 2
              ? '139,92,246'
              : '245,158,11',
    }));

    const render = (now: number) => {
      raf = requestAnimationFrame(render);
      const rect = canvas.getBoundingClientRect();
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const w = Math.max(1, Math.round(rect.width * dpr));
      const h = Math.max(1, Math.round(rect.height * dpr));
      if (canvas.width !== w || canvas.height !== h) {
        canvas.width = w;
        canvas.height = h;
      }
      const ctx = canvas.getContext('2d');
      if (!ctx) return;
      ctx.clearRect(0, 0, w, h);

      const t = now * 0.001;
      const boost = intensity === 'active' ? 1.25 : 1.0;

      if (variant === 'pen-setup') {
        // Solid jet-black canvas with glowing blue opposite-rotating light particles
        ctx.fillStyle = '#02040A';
        ctx.fillRect(0, 0, w, h);

        const cx = w * 0.5;
        const cy = h * 0.5;
        const baseR = Math.min(w, h) * 0.36;

        const coreGlow = ctx.createRadialGradient(cx, cy, 0, cx, cy, baseR * 1.45);
        coreGlow.addColorStop(0, 'rgba(37,99,235,0.22)');
        coreGlow.addColorStop(0.55, 'rgba(14,165,233,0.10)');
        coreGlow.addColorStop(1, 'rgba(0,0,0,0)');
        ctx.fillStyle = coreGlow;
        ctx.fillRect(0, 0, w, h);

        ctx.save();
        ctx.globalCompositeOperation = 'lighter';

        // Faint glowing blue orbital guide rings
        [0.98, 0.72, 0.46].forEach((scale, idx) => {
          ctx.strokeStyle = idx === 1 ? 'rgba(56,189,248,0.28)' : 'rgba(59,130,246,0.20)';
          ctx.lineWidth = (idx === 1 ? 1.8 : 1.3) * dpr;
          ctx.beginPath();
          ctx.arc(cx, cy, baseR * scale, 0, Math.PI * 2);
          ctx.stroke();
        });

        for (let i = 0; i < ringParticles.length; i++) {
          const p = ringParticles[i];
          const ringScale = p.layer === 0 ? 0.98 : p.layer === 1 ? 0.72 : 0.46;
          const r = baseR * (ringScale + p.radiusJitter);
          const ang = p.angle + t * p.speed * boost * p.dir;
          const px = cx + Math.cos(ang) * r;
          const py = cy + Math.sin(ang) * r;
          const pulse = 0.55 + 0.45 * Math.sin(t * 3.1 + p.phase);

          // Luminous comet tail trailing opposite to motion direction
          const tailAng = ang - p.dir * 0.24;
          const tx = cx + Math.cos(tailAng) * r;
          const ty = cy + Math.sin(tailAng) * r;
          const grad = ctx.createLinearGradient(tx, ty, px, py);
          grad.addColorStop(0, 'rgba(14,165,233,0)');
          grad.addColorStop(1, p.layer === 1 ? `rgba(125,211,252,${0.65 * pulse})` : `rgba(59,130,246,${0.65 * pulse})`);
          ctx.strokeStyle = grad;
          ctx.lineWidth = p.size * 0.85 * dpr;
          ctx.lineCap = 'round';
          ctx.beginPath();
          ctx.arc(cx, cy, r, tailAng, ang, p.dir < 0);
          ctx.stroke();

          // Bright blue/white glowing orb
          const glow = ctx.createRadialGradient(px, py, 0, px, py, p.size * 3.4 * dpr);
          glow.addColorStop(0, `rgba(224,242,254,${0.95 * pulse})`);
          glow.addColorStop(0.35, `rgba(56,189,248,${0.68 * pulse})`);
          glow.addColorStop(1, 'rgba(37,99,235,0)');
          ctx.fillStyle = glow;
          ctx.beginPath();
          ctx.arc(px, py, p.size * 3.4 * dpr, 0, Math.PI * 2);
          ctx.fill();
        }
        ctx.restore();
        return;
      }

      if (variant === 'space-nebula') {
        // Atmospheric outer space with drifting gas clouds, stars, and floating space dust
        const bg = ctx.createLinearGradient(0, 0, w, h);
        bg.addColorStop(0, '#030511');
        bg.addColorStop(0.5, '#070B1E');
        bg.addColorStop(1, '#0C0922');
        ctx.fillStyle = bg;
        ctx.fillRect(0, 0, w, h);

        // Voluminous drifting nebula gas clouds
        for (let k = 0; k < gasClouds.length; k++) {
          const g = gasClouds[k];
          const gx = w * (g.bx + Math.sin(t * g.sp + k * 1.9) * 0.14);
          const gy = h * (g.by + Math.cos(t * g.sp * 0.8 + k * 2.3) * 0.14);
          const gr = Math.max(w, h) * g.r;
          const grad = ctx.createRadialGradient(gx, gy, 0, gx, gy, gr);
          grad.addColorStop(0, `rgba(${g.col},0.32)`);
          grad.addColorStop(0.48, `rgba(${g.col},0.12)`);
          grad.addColorStop(1, `rgba(${g.col},0)`);
          ctx.fillStyle = grad;
          ctx.fillRect(0, 0, w, h);
        }

        // 3D depth stars + soft space dust floating toward and past the camera
        const cx = w * 0.5;
        const cy = h * 0.48;
        const fov = Math.max(w, h) * 0.55;
        for (let i = 0; i < spaceParticles.length; i++) {
          const sp = spaceParticles[i];
          const z = ((sp.z - ((t * sp.speed) % 1) + 1) % 1) * 0.94 + 0.06;
          const px = cx + (sp.x / z) * fov * 0.46;
          const py = cy + (sp.y / z) * fov * 0.46;
          if (px < -40 || px > w + 40 || py < -40 || py > h + 40) continue;

          const nearness = 1 - z;
          const rad = sp.size * (0.4 + nearness * 2.1) * dpr;
          const alpha = Math.min(0.95, 0.2 + nearness * 0.85);

          if (sp.isDust) {
            const dGrad = ctx.createRadialGradient(px, py, 0, px, py, rad * 4.5);
            dGrad.addColorStop(0, `rgba(${sp.hue},${alpha * 0.45})`);
            dGrad.addColorStop(1, `rgba(${sp.hue},0)`);
            ctx.fillStyle = dGrad;
            ctx.beginPath();
            ctx.arc(px, py, rad * 4.5, 0, Math.PI * 2);
            ctx.fill();
          } else {
            const zPrev = Math.min(1, z + 0.045);
            const tx = cx + (sp.x / zPrev) * fov * 0.46;
            const ty = cy + (sp.y / zPrev) * fov * 0.46;
            ctx.strokeStyle = `rgba(${sp.hue},${alpha * 0.48})`;
            ctx.lineWidth = Math.max(1, rad * 0.65);
            ctx.lineCap = 'round';
            ctx.beginPath();
            ctx.moveTo(tx, ty);
            ctx.lineTo(px, py);
            ctx.stroke();

            ctx.fillStyle = `rgba(255,255,255,${alpha})`;
            ctx.beginPath();
            ctx.arc(px, py, rad, 0, Math.PI * 2);
            ctx.fill();
          }
        }
        return;
      }

      if (variant === 'quiz-challenge') {
        // Quiz game challenge stage: spotlight sweeps, concentric stage rings, and option badges
        const glowTop = ctx.createRadialGradient(w * 0.5, 0, 0, w * 0.5, 0, Math.max(w, h) * 0.85);
        glowTop.addColorStop(0, 'rgba(37,99,235,0.18)');
        glowTop.addColorStop(0.5, 'rgba(124,58,237,0.09)');
        glowTop.addColorStop(1, 'rgba(16,185,129,0)');
        ctx.fillStyle = glowTop;
        ctx.fillRect(0, 0, w, h);

        // Sweeping stage spotlight cones
        for (let b = 0; b < 2; b++) {
          const sx = b === 0 ? w * 0.18 : w * 0.82;
          const targetX = w * (0.5 + Math.sin(t * 0.7 + b * 2.1) * 0.35);
          const beam = ctx.createLinearGradient(sx, 0, targetX, h);
          beam.addColorStop(0, b === 0 ? 'rgba(56,189,248,0.16)' : 'rgba(167,139,250,0.16)');
          beam.addColorStop(1, 'rgba(0,0,0,0)');
          ctx.fillStyle = beam;
          ctx.beginPath();
          ctx.moveTo(sx - 25 * dpr, 0);
          ctx.lineTo(sx + 25 * dpr, 0);
          ctx.lineTo(targetX + 140 * dpr, h);
          ctx.lineTo(targetX - 140 * dpr, h);
          ctx.closePath();
          ctx.fill();
        }

        // Expanding challenge radar pulse rings
        const cx = w * 0.5;
        const cy = h * 0.52;
        const maxR = Math.min(w, h) * 0.55;
        for (let k = 0; k < 3; k++) {
          const prog = (t * 0.28 + k / 3) % 1;
          ctx.strokeStyle = `rgba(56,189,248,${(1 - prog) * 0.22})`;
          ctx.lineWidth = 1.6 * dpr;
          ctx.beginPath();
          ctx.arc(cx, cy, maxR * (0.18 + prog * 0.85), 0, Math.PI * 2);
          ctx.stroke();
        }

        // Drifting option-selection game tiles (A, B, C, D, ?, ✓)
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        for (let i = 0; i < optionBadges.length; i++) {
          const ob = optionBadges[i];
          const py = ((ob.y - t * ob.vy) % 1 + 1) % 1 * (h + 60 * dpr) - 30 * dpr;
          const px = ob.x * w + Math.sin(t * 1.1 + ob.sway) * 18 * dpr;
          const sz = ob.size * dpr;
          const alpha = 0.22 + 0.12 * Math.sin(t * 2 + ob.sway);

          ctx.save();
          ctx.translate(px, py);
          ctx.rotate(Math.sin(t * 0.8 + ob.sway) * 0.14);
          ctx.fillStyle = `rgba(${ob.tint},${alpha * 0.65})`;
          ctx.strokeStyle = `rgba(${ob.tint},${alpha * 1.4})`;
          ctx.lineWidth = 1.4 * dpr;
          ctx.beginPath();
          ctx.roundRect(-sz / 2, -sz / 2, sz, sz, 7 * dpr);
          ctx.fill();
          ctx.stroke();

          ctx.fillStyle = `rgba(255,255,255,${alpha * 2.1})`;
          ctx.font = `800 ${Math.round(sz * 0.48)}px system-ui, sans-serif`;
          ctx.fillText(ob.label, 0, 1);
          ctx.restore();
        }
        return;
      }

      // Default / 'ai-wave': Abstract blue tech particle data-flow futuristic network digital wave
      const wavePalette =
        accent === 'amber'
          ? { p: '56,189,248', s: '245,158,11', line: '96,165,250' }
          : { p: '56,189,248', s: '37,99,235', line: '96,165,250' };

      const aurora = ctx.createRadialGradient(w * 0.5, h * 0.55, 0, w * 0.5, h * 0.55, Math.max(w, h) * 0.68);
      aurora.addColorStop(0, intensity === 'active' ? 'rgba(14,165,233,0.22)' : 'rgba(37,99,235,0.12)');
      aurora.addColorStop(0.6, intensity === 'active' ? 'rgba(37,99,235,0.10)' : 'rgba(56,189,248,0.05)');
      aurora.addColorStop(1, 'rgba(0,0,0,0)');
      ctx.fillStyle = aurora;
      ctx.fillRect(0, 0, w, h);

      const grid: { x: number; y: number; crest: number }[][] = [];
      for (let r = 0; r < waveRows; r++) {
        const rowPts: { x: number; y: number; crest: number }[] = [];
        const depth = r / (waveRows - 1);
        const baseY = h * (0.28 + depth * 0.56);
        const amp = h * (0.09 + depth * 0.07) * boost;
        for (let c = 0; c < waveCols; c++) {
          const u = c / (waveCols - 1);
          const px = w * (-0.04 + u * 1.08);
          const wave =
            Math.sin(u * 5.2 - t * (1.45 * boost) + r * 0.65) * 0.6 +
            Math.cos(u * 3.1 + t * (1.05 * boost) - r * 0.9) * 0.4;
          rowPts.push({ x: px, y: baseY + wave * amp, crest: (wave + 1) * 0.5 });
        }
        grid.push(rowPts);
      }

      const baseA = intensity === 'active' ? 0.52 : 0.24;
      // Network wave mesh connections
      for (let r = 0; r < waveRows; r++) {
        for (let c = 0; c < waveCols; c++) {
          const pt = grid[r][c];
          if (c + 1 < waveCols) {
            const nx = grid[r][c + 1];
            ctx.strokeStyle = `rgba(${wavePalette.line},${baseA * (0.32 + pt.crest * 0.45)})`;
            ctx.lineWidth = (0.8 + (r / waveRows) * 0.7) * dpr;
            ctx.beginPath();
            ctx.moveTo(pt.x, pt.y);
            ctx.lineTo(nx.x, nx.y);
            ctx.stroke();
          }
          if (r + 1 < waveRows) {
            const dn = grid[r + 1][c];
            ctx.strokeStyle = `rgba(${wavePalette.s},${baseA * 0.28})`;
            ctx.lineWidth = 0.75 * dpr;
            ctx.beginPath();
            ctx.moveTo(pt.x, pt.y);
            ctx.lineTo(dn.x, dn.y);
            ctx.stroke();
          }
        }
      }

      // Glowing wave data nodes
      for (let r = 0; r < waveRows; r++) {
        const sizeScale = 0.7 + (r / waveRows) * 0.85;
        for (let c = 0; c < waveCols; c++) {
          const pt = grid[r][c];
          const nodeAlpha = Math.min(0.92, baseA * (0.65 + pt.crest * 0.9));
          ctx.fillStyle = `rgba(${wavePalette.p},${nodeAlpha})`;
          ctx.beginPath();
          ctx.arc(pt.x, pt.y, (1.6 + pt.crest * 1.7) * sizeScale * dpr, 0, Math.PI * 2);
          ctx.fill();
        }
      }

      // Fast digital data packets streaming across wave rows
      for (let i = 0; i < dataPackets.length; i++) {
        const dp = dataPackets[i];
        const posU = (dp.u + t * dp.speed * boost) % 1;
        const fCol = posU * (waveCols - 1);
        const c0 = Math.floor(fCol);
        const c1 = Math.min(waveCols - 1, c0 + 1);
        const frac = fCol - c0;
        const p0 = grid[dp.row][c0];
        const p1 = grid[dp.row][c1];
        const px = p0.x + (p1.x - p0.x) * frac;
        const py = p0.y + (p1.y - p0.y) * frac;

        const pkt = ctx.createRadialGradient(px, py, 0, px, py, 7 * dpr);
        pkt.addColorStop(0, 'rgba(224,242,254,0.95)');
        pkt.addColorStop(0.45, 'rgba(56,189,248,0.65)');
        pkt.addColorStop(1, 'rgba(37,99,235,0)');
        ctx.fillStyle = pkt;
        ctx.beginPath();
        ctx.arc(px, py, 7 * dpr, 0, Math.PI * 2);
        ctx.fill();
      }
    };

    raf = requestAnimationFrame(render);
    return () => cancelAnimationFrame(raf);
  }, [variant, accent, intensity]);

  return (
    <canvas
      ref={canvasRef}
      className={`pointer-events-none absolute inset-0 h-full w-full ${className}`}
    />
  );
};

export const AiThinkingPill: React.FC<{
  label?: string;
  stage?: string | null;
  modelName?: string;
}> = ({ label = 'Processing', stage, modelName }) => {
  return (
    <div className="pointer-events-none fixed left-1/2 top-20 z-50 -translate-x-1/2 animate-stagger-in">
      <div className="relative min-w-[260px] overflow-hidden rounded-2xl border border-white/20 bg-[#151822]/95 px-4 py-2.5 text-white shadow-2xl backdrop-blur-xl">
        {/* Moving diagonal light sweep */}
        <div className="pointer-events-none absolute inset-0 w-1/2 bg-gradient-to-r from-transparent via-violet-400/25 to-transparent animate-ai-shimmer" />
        <div className="relative flex items-center gap-3">
          <div className="relative flex h-8 w-8 shrink-0 items-center justify-center">
            <span className="absolute inset-0 rounded-full border border-violet-400/40 animate-ai-ring" />
            <span className="absolute inset-0.5 rounded-full bg-gradient-to-tr from-violet-500 via-sky-400 to-amber-300 opacity-75 blur-[3px] animate-ai-orb" />
            <span className="absolute h-1.5 w-1.5 rounded-full bg-amber-300 animate-spark-orbit" />
            <Sparkles className="relative h-4 w-4 text-white" />
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center justify-between gap-2 text-xs font-bold tracking-tight">
              <span className="truncate">{label}</span>
              {modelName && (
                <span className="shrink-0 rounded-md bg-white/10 px-1.5 py-0.5 text-[10px] font-semibold text-violet-200">
                  {modelName}
                </span>
              )}
            </div>
            {stage && (
              <div className="mt-0.5 truncate text-[11px] font-medium text-slate-300">
                {stage}
              </div>
            )}
            <div className="mt-1.5 h-1 w-full overflow-hidden rounded-full bg-white/15">
              <div className="h-full w-1/3 rounded-full bg-gradient-to-r from-violet-400 via-sky-300 to-emerald-300 animate-ai-bar" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
