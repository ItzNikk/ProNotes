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
