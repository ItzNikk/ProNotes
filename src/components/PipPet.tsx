import React, { useEffect, useId, useRef, useState } from 'react';
import { Check, Cookie, Hand, Heart, Pencil, Sparkles, Trophy, X } from 'lucide-react';
import { PetBondState, PetId } from '../types/models';

export interface CompanionSpec {
  id: PetId;
  name: string;
  title: string;
  accent: string;
  bgSoft: string;
  cheers: string[];
  quotes: string[];
}

export const COMPANIONS: CompanionSpec[] = [
  {
    id: 'pip',
    name: 'Pip',
    title: 'Cloud Scholar',
    accent: '#7C5DF0',
    bgSoft: 'rgba(124, 93, 240, 0.14)',
    cheers: ["You've got this!", 'Sharp thinking!', 'Keep flowing!', 'Clean notes!'],
    quotes: ['Ready when you are!', 'One concept at a time ✨'],
  },
  {
    id: 'sparkchu',
    name: 'Sparkchu',
    title: 'Volt Mouse',
    accent: '#EAB308',
    bgSoft: 'rgba(234, 179, 8, 0.16)',
    cheers: ['Zzzap! Nailed it!', 'Lightning focus!', 'High voltage brain!', '1000 IQ spark!'],
    quotes: ['Pika-focus mode active!', 'Let’s spark through this chapter!'],
  },
  {
    id: 'emberkit',
    name: 'Emberkit',
    title: 'Flame Fox',
    accent: '#F97316',
    bgSoft: 'rgba(249, 115, 22, 0.15)',
    cheers: ['Blazing progress!', 'On fire today!', 'Warm & steady!', 'Ignited mastery!'],
    quotes: ['Keep that study flame bright!', 'Nothing cools our streak!'],
  },
  {
    id: 'aquapup',
    name: 'Aquapup',
    title: 'Tide Otter',
    accent: '#0EA5E9',
    bgSoft: 'rgba(14, 165, 233, 0.15)',
    cheers: ['Smooth sailing!', 'Making waves!', 'Crystal clear!', 'Deep dive win!'],
    quotes: ['Stay hydrated & focused!', 'Flow effortlessly across the page!'],
  },
  {
    id: 'leafsprout',
    name: 'Leafsprout',
    title: 'Solar Dino',
    accent: '#22C55E',
    bgSoft: 'rgba(34, 197, 94, 0.15)',
    cheers: ['Knowledge blooming!', 'Growing stronger!', 'Fresh breakthrough!', 'Rooted & ready!'],
    quotes: ['Every line plants a seed!', 'Photosynthesizing great grades!'],
  },
  {
    id: 'eevo',
    name: 'Eevo',
    title: 'Starlight Fox',
    accent: '#C28246',
    bgSoft: 'rgba(194, 130, 70, 0.16)',
    cheers: ['Adaptive genius!', 'Evolving fast!', 'Pure starlight!', 'So fluffy & smart!'],
    quotes: ['Infinite paths to master!', 'Let’s unlock the next level!'],
  },
  {
    id: 'snorblob',
    name: 'Snorblob',
    title: 'Cozy Guardian',
    accent: '#0F766E',
    bgSoft: 'rgba(15, 118, 110, 0.15)',
    cheers: ['Calm & unstoppable!', 'Heavyweight focus!', 'Cozy victory!', 'Zen mastery!'],
    quotes: ['Breathe easy, you know this!', 'Snack break soon?'],
  },
  {
    id: 'genglet',
    name: 'Genglet',
    title: 'Shadow Imp',
    accent: '#9333EA',
    bgSoft: 'rgba(147, 51, 234, 0.16)',
    cheers: ['Wickedly clever!', 'Outsmarted it!', 'Midnight scholar!', 'Tricky problem crushed!'],
    quotes: ['Even hard exams fear us!', 'Hehe… brilliant deduction!'],
  },
];

export function getCompanionSpec(id: PetId): CompanionSpec {
  return COMPANIONS.find((c) => c.id === id) || COMPANIONS[1];
}

export const PetAvatarSvg: React.FC<{
  petId: PetId;
  writing?: boolean;
  thinking?: boolean;
  blink?: boolean;
  happy?: boolean;
  className?: string;
}> = ({ petId, writing = false, thinking = false, blink = false, happy = false, className = 'h-full w-full' }) => {
  const uid = useId().replace(/:/g, '');
  const eyeY = writing ? 35 : thinking ? 29 : 32;
  const eyeLx = thinking ? 25 : 23.5;
  const eyeRx = thinking ? 41 : 40.5;

  const renderEyes = (eyeColor = '#1E2230', highlight = '#FFFFFF') => {
    if (blink) {
      return (
        <>
          <line x1="20.5" y1={eyeY} x2="27" y2={eyeY} stroke={eyeColor} strokeWidth="2.4" strokeLinecap="round" />
          <line x1="37" y1={eyeY} x2="43.5" y2={eyeY} stroke={eyeColor} strokeWidth="2.4" strokeLinecap="round" />
        </>
      );
    }
    if (happy) {
      return (
        <>
          <path d={`M 20.2 ${eyeY + 1} Q 23.8 ${eyeY - 4.8} 27.3 ${eyeY + 1}`} fill="none" stroke={eyeColor} strokeWidth="2.6" strokeLinecap="round" />
          <path d={`M 36.7 ${eyeY + 1} Q 40.2 ${eyeY - 4.8} 43.8 ${eyeY + 1}`} fill="none" stroke={eyeColor} strokeWidth="2.6" strokeLinecap="round" />
        </>
      );
    }
    if (petId === 'snorblob') {
      return (
        <>
          <path d="M 19.5 31.5 Q 23.5 35 27.5 31.5" fill="none" stroke={eyeColor} strokeWidth="2.5" strokeLinecap="round" />
          <path d="M 36.5 31.5 Q 40.5 35 44.5 31.5" fill="none" stroke={eyeColor} strokeWidth="2.5" strokeLinecap="round" />
        </>
      );
    }
    return (
      <>
        <circle cx={eyeLx} cy={eyeY} r="3.4" fill={eyeColor} />
        <circle cx={eyeLx + 1.05} cy={eyeY - 1.15} r="1.25" fill={highlight} />
        <circle cx={eyeLx - 0.9} cy={eyeY + 1.1} r="0.55" fill={highlight} opacity="0.7" />
        <circle cx={eyeRx} cy={eyeY} r="3.4" fill={eyeColor} />
        <circle cx={eyeRx + 1.05} cy={eyeY - 1.15} r="1.25" fill={highlight} />
        <circle cx={eyeRx - 0.9} cy={eyeY + 1.1} r="0.55" fill={highlight} opacity="0.7" />
      </>
    );
  };

  return (
    <svg viewBox="0 0 64 64" className={`${className} drop-shadow-sm`}>
      <defs>
        <linearGradient id={`${uid}-pip`} x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#E9D5FF" />
          <stop offset="55%" stopColor="#A78BFA" />
          <stop offset="100%" stopColor="#6D28D9" />
        </linearGradient>
        <radialGradient id={`${uid}-spark`} cx="35%" cy="30%" r="70%">
          <stop offset="0%" stopColor="#FEF08A" />
          <stop offset="70%" stopColor="#FACC15" />
          <stop offset="100%" stopColor="#EAB308" />
        </radialGradient>
        <radialGradient id={`${uid}-ember`} cx="35%" cy="30%" r="72%">
          <stop offset="0%" stopColor="#FDBA74" />
          <stop offset="65%" stopColor="#FB923C" />
          <stop offset="100%" stopColor="#EA580C" />
        </radialGradient>
        <radialGradient id={`${uid}-aqua`} cx="35%" cy="28%" r="72%">
          <stop offset="0%" stopColor="#7DD3FC" />
          <stop offset="65%" stopColor="#38BDF8" />
          <stop offset="100%" stopColor="#0284C7" />
        </radialGradient>
        <radialGradient id={`${uid}-leaf`} cx="35%" cy="30%" r="72%">
          <stop offset="0%" stopColor="#86EFAC" />
          <stop offset="65%" stopColor="#4ADE80" />
          <stop offset="100%" stopColor="#16A34A" />
        </radialGradient>
        <radialGradient id={`${uid}-eevo`} cx="35%" cy="30%" r="72%">
          <stop offset="0%" stopColor="#E5A96D" />
          <stop offset="65%" stopColor="#C68446" />
          <stop offset="100%" stopColor="#925422" />
        </radialGradient>
        <radialGradient id={`${uid}-geng`} cx="40%" cy="30%" r="72%">
          <stop offset="0%" stopColor="#A855F7" />
          <stop offset="60%" stopColor="#7E22CE" />
          <stop offset="100%" stopColor="#4C1D95" />
        </radialGradient>
      </defs>

      <ellipse cx="32" cy="57.5" rx="18.5" ry="3.2" fill="rgba(0,0,0,0.16)" />

      {petId === 'pip' && (
        <>
          <circle cx="15" cy="14" r="2.2" fill="#DDD6FE" opacity="0.7" />
          <circle cx="51" cy="17" r="1.6" fill="#C4B5FD" opacity="0.8" />
          <ellipse cx="32" cy="34.5" rx="23" ry="20.5" fill={`url(#${uid}-pip)`} />
          <ellipse cx="32" cy="43" rx="14" ry="8.5" fill="rgba(255,255,255,0.22)" />
          <ellipse cx="22.5" cy="21" rx="7.5" ry="3.6" fill="rgba(255,255,255,0.38)" transform="rotate(-18 22.5 21)" />
          <circle cx="17" cy="39" r="3.8" fill="rgba(255,130,175,0.52)" />
          <circle cx="47" cy="39" r="3.8" fill="rgba(255,130,175,0.52)" />
          {renderEyes()}
          {thinking ? (
            <circle cx="32" cy="41.5" r="2.2" fill="none" stroke="#1E2230" strokeWidth="2.2" />
          ) : (
            <path d="M 27.5 40 Q 32 45.2 36.5 40" fill="none" stroke="#1E2230" strokeWidth="2.3" strokeLinecap="round" />
          )}
        </>
      )}

      {petId === 'sparkchu' && (
        <>
          <path d="M 48 44 L 58.5 32 L 53.5 31 L 60.5 18.5 L 48 29 L 53 31 Z" fill="#F59E0B" stroke="#78350F" strokeWidth="1" strokeLinejoin="round" />
          <path d="M 18 22 L 9.5 5.5 L 26.5 16 Z" fill="#FDE047" />
          <path d="M 9.5 5.5 L 16 9.8 L 13.8 14 Z" fill="#1E2230" />
          <path d="M 46 22 L 54.5 5.5 L 37.5 16 Z" fill="#FDE047" />
          <path d="M 54.5 5.5 L 48 9.8 L 50.2 14 Z" fill="#1E2230" />
          <ellipse cx="32" cy="35.5" rx="21.5" ry="19.5" fill={`url(#${uid}-spark)`} />
          <ellipse cx="32" cy="44" rx="13" ry="8" fill="rgba(254,249,195,0.42)" />
          <ellipse cx="23.5" cy="22.5" rx="6.8" ry="3.2" fill="rgba(255,255,255,0.42)" transform="rotate(-15 23.5 22.5)" />
          <circle cx="17" cy="39.5" r="4.4" fill="#EF4444" />
          <circle cx="15.8" cy="38.2" r="1.2" fill="rgba(255,255,255,0.45)" />
          <circle cx="47" cy="39.5" r="4.4" fill="#EF4444" />
          <circle cx="45.8" cy="38.2" r="1.2" fill="rgba(255,255,255,0.45)" />
          {renderEyes()}
          <path d="M 28.5 39.5 Q 30.5 42.8 32 40 Q 33.5 42.8 35.5 39.5" fill="none" stroke="#1E2230" strokeWidth="2.1" strokeLinecap="round" />
        </>
      )}

      {petId === 'emberkit' && (
        <>
          <path d="M 47 45 C 58 42, 60.5 27, 52 21 C 53 27.5, 49 30, 48 34 Z" fill="#F97316" />
          <path d="M 50 38 C 55.5 36, 56.5 29, 52 25.5 Z" fill="#FDE047" />
          <path d="M 16 24 L 11.5 6.5 L 27.5 17 Z" fill="#EA580C" />
          <path d="M 15 19 L 13.8 10.5 L 23 17 Z" fill="#FDE68A" />
          <path d="M 48 24 L 52.5 6.5 L 36.5 17 Z" fill="#EA580C" />
          <path d="M 49 19 L 50.2 10.5 L 41 17 Z" fill="#FDE68A" />
          <ellipse cx="32" cy="35.5" rx="21.5" ry="19" fill={`url(#${uid}-ember)`} />
          <ellipse cx="32" cy="44" rx="13" ry="9" fill="#FEF3C7" />
          <ellipse cx="23.5" cy="22.5" rx="6.5" ry="3" fill="rgba(255,255,255,0.34)" transform="rotate(-15 23.5 22.5)" />
          <circle cx="18" cy="38.5" r="3.4" fill="rgba(239,68,68,0.38)" />
          <circle cx="46" cy="38.5" r="3.4" fill="rgba(239,68,68,0.38)" />
          {renderEyes()}
          <polygon points="30.5,37 33.5,37 32,39.2" fill="#1E2230" />
          <path d="M 28.5 41 Q 32 44.5 35.5 41" fill="none" stroke="#1E2230" strokeWidth="2.1" strokeLinecap="round" />
        </>
      )}

      {petId === 'aquapup' && (
        <>
          <path d="M 32 5.5 C 36.5 10.5, 37.5 16, 32 18.5 C 26.5 16, 27.5 10.5, 32 5.5 Z" fill="#0284C7" />
          <path d="M 12 33.5 L 4.5 39 L 13 43.5 Z" fill="#0EA5E9" />
          <path d="M 52 33.5 L 59.5 39 L 51 43.5 Z" fill="#0EA5E9" />
          <ellipse cx="32" cy="35.5" rx="21.5" ry="19" fill={`url(#${uid}-aqua)`} />
          <ellipse cx="32" cy="40.5" rx="15.5" ry="11.5" fill="#E0F2FE" />
          <ellipse cx="23.5" cy="22" rx="6.2" ry="3" fill="rgba(255,255,255,0.4)" transform="rotate(-15 23.5 22)" />
          <circle cx="19" cy="38.5" r="3.4" fill="rgba(244,114,182,0.44)" />
          <circle cx="45" cy="38.5" r="3.4" fill="rgba(244,114,182,0.44)" />
          {renderEyes()}
          <path d="M 28 40 Q 32 45 36 40" fill="none" stroke="#0F172A" strokeWidth="2.2" strokeLinecap="round" />
        </>
      )}

      {petId === 'leafsprout' && (
        <>
          <path d="M 32 17 C 27 5.5, 16.5 7, 19 15 C 22 18, 28 17, 32 17 Z" fill="#15803D" />
          <path d="M 32 17 C 37 4.5, 47.5 7, 45 15 C 42 18, 36 17, 32 17 Z" fill="#22C55E" />
          <ellipse cx="32" cy="36" rx="22" ry="19" fill={`url(#${uid}-leaf)`} />
          <ellipse cx="32" cy="44" rx="13.5" ry="8" fill="rgba(220,252,231,0.45)" />
          <ellipse cx="23" cy="23" rx="6" ry="2.8" fill="rgba(255,255,255,0.34)" transform="rotate(-14 23 23)" />
          <circle cx="20.5" cy="24" r="2.3" fill="#15803D" opacity="0.48" />
          <circle cx="43.5" cy="24.5" r="1.9" fill="#15803D" opacity="0.48" />
          <circle cx="18" cy="39" r="3.5" fill="rgba(244,114,182,0.44)" />
          <circle cx="46" cy="39" r="3.5" fill="rgba(244,114,182,0.44)" />
          {renderEyes('#14532D')}
          <path d="M 27.5 40.5 Q 32 45 36.5 40.5" fill="none" stroke="#14532D" strokeWidth="2.3" strokeLinecap="round" />
        </>
      )}

      {petId === 'eevo' && (
        <>
          <path d="M 19 24 L 8 4.5 L 28.5 16 Z" fill="#B4733B" />
          <path d="M 17 20 L 12 8.8 L 25 16 Z" fill="#5B3618" />
          <path d="M 45 24 L 56 4.5 L 35.5 16 Z" fill="#B4733B" />
          <path d="M 47 20 L 52 8.8 L 39 16 Z" fill="#5B3618" />
          <ellipse cx="32" cy="48.5" rx="19" ry="8.5" fill="#FEF3C7" />
          <ellipse cx="32" cy="34.5" rx="20.5" ry="18.5" fill={`url(#${uid}-eevo)`} />
          <ellipse cx="24" cy="22" rx="6" ry="2.8" fill="rgba(255,255,255,0.3)" transform="rotate(-14 24 22)" />
          <circle cx="18.5" cy="38" r="3.4" fill="rgba(244,114,182,0.44)" />
          <circle cx="45.5" cy="38" r="3.4" fill="rgba(244,114,182,0.44)" />
          {renderEyes()}
          <polygon points="30.8,36.8 33.2,36.8 32,38.6" fill="#1E2230" />
          <path d="M 28.5 40.5 Q 32 44 35.5 40.5" fill="none" stroke="#1E2230" strokeWidth="2.1" strokeLinecap="round" />
        </>
      )}

      {petId === 'snorblob' && (
        <>
          <path d="M 16 24 L 13.5 10.5 L 25.5 18 Z" fill="#0F4C5C" />
          <path d="M 48 24 L 50.5 10.5 L 38.5 18 Z" fill="#0F4C5C" />
          <ellipse cx="32" cy="36" rx="23.5" ry="20.5" fill="#115E67" />
          <ellipse cx="32" cy="37.5" rx="18.8" ry="15.5" fill="#FEF3C7" />
          <circle cx="19" cy="38.5" r="3.4" fill="rgba(244,114,182,0.44)" />
          <circle cx="45" cy="38.5" r="3.4" fill="rgba(244,114,182,0.44)" />
          {renderEyes('#1E293B')}
          <path d="M 28 40 Q 32 44 36 40" fill="none" stroke="#1E293B" strokeWidth="2.2" strokeLinecap="round" />
          <polygon points="27,41.4 28.8,41.4 27.9,39.2" fill="#FFFFFF" />
          <polygon points="35.2,41.4 37,41.4 36.1,39.2" fill="#FFFFFF" />
        </>
      )}

      {petId === 'genglet' && (
        <>
          <path
            d="M 14 25 L 9.5 7.5 L 24 16 L 32 8.5 L 40 16 L 54.5 7.5 L 50 25 Q 56.5 38 48 51 Q 32 58 16 51 Q 7.5 38 14 25 Z"
            fill={`url(#${uid}-geng)`}
          />
          <ellipse cx="26" cy="24" rx="5.5" ry="2.6" fill="rgba(255,255,255,0.22)" transform="rotate(-15 26 24)" />
          {blink ? (
            <>
              <line x1="20" y1={eyeY} x2="27" y2={eyeY} stroke="#FCA5A5" strokeWidth="2.5" strokeLinecap="round" />
              <line x1="37" y1={eyeY} x2="44" y2={eyeY} stroke="#FCA5A5" strokeWidth="2.5" strokeLinecap="round" />
            </>
          ) : (
            <>
              <path d="M 18.8 27.8 L 27.8 32.5 L 20.5 34.8 Z" fill="#F87171" />
              <circle cx="23.5" cy="31.8" r="1.2" fill="#1E1B4B" />
              <path d="M 45.2 27.8 L 36.2 32.5 L 43.5 34.8 Z" fill="#F87171" />
              <circle cx="40.5" cy="31.8" r="1.2" fill="#1E1B4B" />
            </>
          )}
          <path d="M 20.5 39 Q 32 48.5 43.5 39 Q 32 43 20.5 39 Z" fill="#FFFFFF" stroke="#1E1B4B" strokeWidth="1.2" />
          <line x1="26.5" y1="40.5" x2="26.5" y2="43.8" stroke="#CBD5E1" strokeWidth="1" />
          <line x1="32" y1="41.5" x2="32" y2="44.5" stroke="#CBD5E1" strokeWidth="1" />
          <line x1="37.5" y1="40.5" x2="37.5" y2="43.8" stroke="#CBD5E1" strokeWidth="1" />
        </>
      )}

      {(happy || thinking) && (
        <>
          <path d="M 55 11 L 56.2 14 L 59 15.2 L 56.2 16.4 L 55 19.4 L 53.8 16.4 L 51 15.2 L 53.8 14 Z" fill="#FDE047" />
          <circle cx="11" cy="18" r="1.7" fill="#60A5FA" />
        </>
      )}
    </svg>
  );
};

/**
 * Looping 60fps Canvas Particle & Aurora Backdrop for ALL AI Tasks
 * (Summary, Flashcards, Quiz, Local Models, Live Thinking).
 */
export const AiParticleBackdrop: React.FC<{
  accent?: 'violet' | 'blue' | 'amber';
  intensity?: 'soft' | 'active';
  className?: string;
}> = ({ accent = 'violet', intensity = 'soft', className = '' }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    let raf = 0;

    const palettes = {
      violet: ['139,92,246', '56,189,248', '244,114,182'],
      blue: ['59,130,246', '14,165,233', '168,85,247'],
      amber: ['245,158,11', '251,146,60', '139,92,246'],
    }[accent];

    const count = intensity === 'active' ? 38 : 24;
    const particles = Array.from({ length: count }, (_, i) => ({
      x: Math.random(),
      y: Math.random(),
      vx: (Math.random() - 0.5) * (intensity === 'active' ? 0.0014 : 0.00075),
      vy: -0.0004 - Math.random() * (intensity === 'active' ? 0.0012 : 0.0006),
      r: 1.6 + Math.random() * 3.4,
      phase: Math.random() * Math.PI * 2,
      col: palettes[i % palettes.length],
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
      // Soft drifting aurora glow blobs
      for (let k = 0; k < 2; k++) {
        const cx = w * (0.25 + k * 0.5 + Math.sin(t * 0.45 + k * 2.1) * 0.14);
        const cy = h * (0.3 + k * 0.4 + Math.cos(t * 0.38 + k * 1.3) * 0.14);
        const rad = Math.max(w, h) * 0.48;
        const grad = ctx.createRadialGradient(cx, cy, 0, cx, cy, rad);
        const a = intensity === 'active' ? 0.16 : 0.085;
        grad.addColorStop(0, `rgba(${palettes[k]}, ${a})`);
        grad.addColorStop(1, `rgba(${palettes[k]}, 0)`);
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, w, h);
      }

      // Drifting glowing particles + subtle neural connections when close
      for (let i = 0; i < particles.length; i++) {
        const p = particles[i];
        p.x = (p.x + p.vx + 1) % 1;
        p.y = (p.y + p.vy + 1) % 1;
        const px = p.x * w;
        const py = p.y * h;
        const twinkle = 0.45 + 0.55 * Math.sin(t * 2.4 + p.phase);
        const alpha = (intensity === 'active' ? 0.58 : 0.34) * twinkle;

        ctx.fillStyle = `rgba(${p.col}, ${alpha})`;
        ctx.beginPath();
        ctx.arc(px, py, p.r * dpr, 0, Math.PI * 2);
        ctx.fill();

        for (let j = i + 1; j < particles.length; j++) {
          const q = particles[j];
          const qx = q.x * w;
          const qy = q.y * h;
          const dist2 = (px - qx) * (px - qx) + (py - qy) * (py - qy);
          const maxD = (intensity === 'active' ? 115 : 85) * dpr;
          if (dist2 < maxD * maxD) {
            const lineA = (1 - Math.sqrt(dist2) / maxD) * (intensity === 'active' ? 0.16 : 0.08);
            ctx.strokeStyle = `rgba(${p.col}, ${lineA})`;
            ctx.lineWidth = dpr;
            ctx.beginPath();
            ctx.moveTo(px, py);
            ctx.lineTo(qx, qy);
            ctx.stroke();
          }
        }
      }
    };

    raf = requestAnimationFrame(render);
    return () => cancelAnimationFrame(raf);
  }, [accent, intensity]);

  return (
    <canvas
      ref={canvasRef}
      className={`pointer-events-none absolute inset-0 h-full w-full ${className}`}
    />
  );
};

interface PipPetProps {
  petId?: PetId;
  petName?: string;
  bond?: PetBondState;
  writing?: boolean;
  thinking?: boolean;
  cheerTrigger?: number;
  compact?: boolean;
  align?: 'left' | 'right';
  speechOverride?: string | null;
  onSwitchPet?: (id: PetId) => void;
  onRenamePet?: (name: string) => void;
  onPetAction?: (kind: 'pet' | 'treat' | 'study', xpGain?: number) => void;
}

export const PipPet: React.FC<PipPetProps> = ({
  petId = 'sparkchu',
  petName = '',
  bond,
  writing = false,
  thinking = false,
  cheerTrigger = 0,
  compact = true,
  align = 'left',
  speechOverride = null,
  onSwitchPet,
  onRenamePet,
  onPetAction,
}) => {
  const [blink, setBlink] = useState(false);
  const [cheering, setCheering] = useState(false);
  const [say, setSay] = useState<string | null>(null);
  const [panelOpen, setPanelOpen] = useState(false);
  const [editingName, setEditingName] = useState(false);
  const [nameDraft, setNameDraft] = useState(petName);
  const containerRef = useRef<HTMLDivElement>(null);

  const spec = getCompanionSpec(petId);
  const displayName = petName?.trim() || spec.name;
  const xp = bond?.xp ?? 45;
  const level = bond?.level ?? Math.max(1, Math.floor(xp / 50) + 1);
  const levelProgress = (xp % 50) / 50;

  useEffect(() => {
    setNameDraft(petName);
  }, [petName]);

  useEffect(() => {
    const id = window.setInterval(() => {
      setBlink(true);
      window.setTimeout(() => setBlink(false), 140);
    }, 3400);
    return () => window.clearInterval(id);
  }, []);

  useEffect(() => {
    if (cheerTrigger > 0) {
      setCheering(true);
      const msg = spec.cheers[cheerTrigger % spec.cheers.length];
      setSay(msg);
      const t1 = window.setTimeout(() => setCheering(false), 700);
      const t2 = window.setTimeout(() => setSay(null), 2200);
      return () => {
        window.clearTimeout(t1);
        window.clearTimeout(t2);
      };
    }
  }, [cheerTrigger, spec.cheers]);

  useEffect(() => {
    if (!panelOpen) {
      setEditingName(false);
      return;
    }
    const closeOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setPanelOpen(false);
      }
    };
    window.addEventListener('mousedown', closeOutside);
    return () => window.removeEventListener('mousedown', closeOutside);
  }, [panelOpen]);

  const triggerReaction = (kind: 'pet' | 'treat' | 'study', msg: string, gain: number) => {
    setCheering(true);
    setSay(msg);
    onPetAction?.(kind, gain);
    window.setTimeout(() => setCheering(false), 700);
    window.setTimeout(() => setSay((cur) => (cur === msg ? null : cur)), 2000);
  };

  const bubble = speechOverride || say || (thinking ? `${displayName} is thinking…` : null);

  return (
    <div ref={containerRef} className="relative inline-flex items-center select-none">
      {bubble && !panelOpen && (
        <div className="pointer-events-none absolute left-1/2 top-full z-50 mt-2 -translate-x-1/2 whitespace-nowrap rounded-xl border border-black/10 bg-white px-2.5 py-1 text-xs font-semibold text-slate-800 shadow-lg dark:border-white/15 dark:bg-[#22242B] dark:text-slate-100 animate-stagger-in">
          {bubble}
        </div>
      )}

      <button
        type="button"
        onClick={() => setPanelOpen((v) => !v)}
        title={`${displayName} · Lv.${level} Study Companion (tap to interact)`}
        className={`group relative flex items-center justify-center rounded-full transition-transform active:scale-90 ${
          compact ? 'h-9 w-9' : 'h-14 w-14'
        } ${cheering ? 'animate-pip-cheer' : 'animate-pip-float'}`}
      >
        {thinking && (
          <span
            className="absolute -inset-1 rounded-full opacity-75 blur-[4px] animate-ai-orb"
            style={{ background: `linear-gradient(135deg, ${spec.accent}, #3D6CF4)` }}
          />
        )}
        <PetAvatarSvg
          petId={petId}
          writing={writing}
          thinking={thinking}
          blink={blink}
          happy={cheering}
        />
        <span
          className="font-mono-num absolute -bottom-0.5 -right-0.5 rounded-full border border-white px-1 text-[8px] font-extrabold text-white shadow dark:border-[#141415]"
          style={{ backgroundColor: spec.accent }}
        >
          {level}
        </span>
      </button>

      {panelOpen && (
        <div
          className={`absolute top-full z-50 mt-2.5 w-80 overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/12 dark:bg-[#191A1F] animate-stagger-in ${
            align === 'right' ? 'right-0' : 'left-0'
          }`}
        >
          {/* Companion Header */}
          <div
            className="flex items-center justify-between px-4 py-3.5"
            style={{ backgroundColor: spec.bgSoft }}
          >
            <div className="flex min-w-0 flex-1 items-center gap-3">
              <div className={`h-14 w-14 shrink-0 ${cheering ? 'animate-pip-cheer' : 'animate-pip-float'}`}>
                <PetAvatarSvg petId={petId} blink={blink} happy={cheering} />
              </div>
              <div className="min-w-0 flex-1">
                {editingName ? (
                  <div className="flex items-center gap-1">
                    <input
                      autoFocus
                      type="text"
                      value={nameDraft}
                      onChange={(e) => setNameDraft(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          onRenamePet?.(nameDraft.trim());
                          setEditingName(false);
                        }
                      }}
                      placeholder={spec.name}
                      className="w-28 rounded-lg border border-black/15 bg-white px-2 py-0.5 text-xs font-bold outline-none dark:border-white/20 dark:bg-[#14151A]"
                    />
                    <button
                      type="button"
                      onClick={() => {
                        onRenamePet?.(nameDraft.trim());
                        setEditingName(false);
                      }}
                      className="rounded-lg bg-blue-600 p-1 text-white"
                    >
                      <Check className="h-3 w-3" />
                    </button>
                  </div>
                ) : (
                  <div className="flex items-center gap-1.5">
                    <span className="truncate text-sm font-bold text-slate-900 dark:text-white">
                      {displayName}
                    </span>
                    {onRenamePet && (
                      <button
                        type="button"
                        onClick={() => {
                          setNameDraft(petName);
                          setEditingName(true);
                        }}
                        title="Rename companion"
                        className="rounded p-0.5 text-slate-500 hover:bg-black/8 dark:hover:bg-white/10"
                      >
                        <Pencil className="h-3 w-3" />
                      </button>
                    )}
                    <span
                      className="font-mono-num shrink-0 rounded-md px-1.5 py-0.5 text-[10px] font-bold text-white"
                      style={{ backgroundColor: spec.accent }}
                    >
                      Lv.{level}
                    </span>
                  </div>
                )}
                <div className="text-[11px] font-medium text-slate-600 dark:text-slate-300">
                  {spec.title}
                </div>
                <div className="mt-1.5 flex items-center gap-2">
                  <div className="h-1.5 w-24 overflow-hidden rounded-full bg-black/10 dark:bg-white/15">
                    <div
                      className="h-full rounded-full transition-all duration-300"
                      style={{ width: `${Math.max(8, Math.round(levelProgress * 100))}%`, backgroundColor: spec.accent }}
                    />
                  </div>
                  <span className="font-mono-num text-[10px] font-semibold text-slate-500 dark:text-slate-400">
                    {xp % 50}/50 XP
                  </span>
                </div>
              </div>
            </div>
            <button
              type="button"
              onClick={() => setPanelOpen(false)}
              className="rounded-full p-1 text-slate-500 hover:bg-black/8 dark:hover:bg-white/10"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          <div className="space-y-3 p-4">
            {/* Motivational speech line */}
            <div className="rounded-2xl bg-slate-50 px-3 py-2 text-xs font-medium text-slate-700 dark:bg-[#21232B] dark:text-slate-200">
              “{say || spec.quotes[Math.floor((xp + cheerTrigger) % spec.quotes.length)]}”
            </div>

            {/* Interactive actions */}
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() =>
                  triggerReaction('pet', spec.cheers[Math.floor(Math.random() * spec.cheers.length)], 5)
                }
                className="flex flex-col items-center gap-1 rounded-2xl border border-black/8 bg-white py-2 text-[11px] font-semibold text-slate-700 transition hover:border-rose-400/50 hover:bg-rose-50/60 dark:border-white/10 dark:bg-[#22242C] dark:text-slate-200 dark:hover:bg-rose-950/25"
              >
                <Heart className="h-4 w-4 text-rose-500" />
                Pet (+5 XP)
              </button>
              <button
                type="button"
                onClick={() =>
                  triggerReaction('treat', 'Yum!! Study energy +100%', 15)
                }
                className="flex flex-col items-center gap-1 rounded-2xl border border-black/8 bg-white py-2 text-[11px] font-semibold text-slate-700 transition hover:border-amber-400/50 hover:bg-amber-50/60 dark:border-white/10 dark:bg-[#22242C] dark:text-slate-200 dark:hover:bg-amber-950/25"
              >
                <Cookie className="h-4 w-4 text-amber-500" />
                Treat (+15)
              </button>
              <button
                type="button"
                onClick={() =>
                  triggerReaction('study', 'High-five! Keep crushing it!', 8)
                }
                className="flex flex-col items-center gap-1 rounded-2xl border border-black/8 bg-white py-2 text-[11px] font-semibold text-slate-700 transition hover:border-blue-400/50 hover:bg-blue-50/60 dark:border-white/10 dark:bg-[#22242C] dark:text-slate-200 dark:hover:bg-blue-950/25"
              >
                <Hand className="h-4 w-4 text-blue-500" />
                Cheer (+8)
              </button>
            </div>

            {/* Companion Picker */}
            <div>
              <div className="mb-1.5 flex items-center justify-between text-[11px] font-bold uppercase tracking-wider text-slate-400">
                <span>Switch Companion</span>
                <span className="font-mono-num inline-flex items-center gap-1 text-slate-500">
                  <Trophy className="h-3 w-3 text-amber-500" /> {bond?.treatsGiven ?? 0} treats
                </span>
              </div>
              <div className="grid grid-cols-4 gap-1.5">
                {COMPANIONS.map((c) => {
                  const active = c.id === petId;
                  return (
                    <button
                      key={c.id}
                      type="button"
                      onClick={() => {
                        onSwitchPet?.(c.id);
                        setSay(c.quotes[0]);
                      }}
                      className={`flex flex-col items-center rounded-xl border p-1.5 transition ${
                        active
                          ? 'border-blue-500 bg-blue-500/10'
                          : 'border-transparent hover:bg-black/5 dark:hover:bg-white/8'
                      }`}
                    >
                      <PetAvatarSvg petId={c.id} className="h-8 w-8" />
                      <span className="mt-0.5 w-full truncate text-center text-[10px] font-semibold">
                        {c.name}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
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
