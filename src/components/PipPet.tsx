import React, { useEffect, useRef, useState } from 'react';
import { Cookie, Hand, Heart, Sparkles, Trophy, X } from 'lucide-react';
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
          <path d={`M 20.5 ${eyeY + 1} Q 23.8 ${eyeY - 4} 27 ${eyeY + 1}`} fill="none" stroke={eyeColor} strokeWidth="2.5" strokeLinecap="round" />
          <path d={`M 37 ${eyeY + 1} Q 40.2 ${eyeY - 4} 43.5 ${eyeY + 1}`} fill="none" stroke={eyeColor} strokeWidth="2.5" strokeLinecap="round" />
        </>
      );
    }
    if (petId === 'snorblob') {
      return (
        <>
          <path d="M 20 31.5 Q 23.5 34.5 27 31.5" fill="none" stroke={eyeColor} strokeWidth="2.4" strokeLinecap="round" />
          <path d="M 37 31.5 Q 40.5 34.5 44 31.5" fill="none" stroke={eyeColor} strokeWidth="2.4" strokeLinecap="round" />
        </>
      );
    }
    return (
      <>
        <circle cx={eyeLx} cy={eyeY} r="3.2" fill={eyeColor} />
        <circle cx={eyeLx + 1} cy={eyeY - 1.1} r="1.15" fill={highlight} />
        <circle cx={eyeRx} cy={eyeY} r="3.2" fill={eyeColor} />
        <circle cx={eyeRx + 1} cy={eyeY - 1.1} r="1.15" fill={highlight} />
      </>
    );
  };

  return (
    <svg viewBox="0 0 64 64" className={`${className} drop-shadow-sm`}>
      <ellipse cx="32" cy="57.5" rx="18" ry="3.2" fill="rgba(0,0,0,0.16)" />

      {petId === 'pip' && (
        <>
          <defs>
            <linearGradient id="pet-pip-g" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#D8BEFF" />
              <stop offset="100%" stopColor="#7C5DF0" />
            </linearGradient>
          </defs>
          <ellipse cx="32" cy="34.5" rx="23" ry="20.5" fill="url(#pet-pip-g)" />
          <ellipse cx="23" cy="22" rx="7" ry="3.8" fill="rgba(255,255,255,0.34)" transform="rotate(-18 23 22)" />
          <circle cx="17.5" cy="39" r="3.5" fill="rgba(255,130,175,0.45)" />
          <circle cx="46.5" cy="39" r="3.5" fill="rgba(255,130,175,0.45)" />
          {renderEyes()}
          {thinking ? (
            <circle cx="32" cy="41.5" r="2.2" fill="none" stroke="#1E2230" strokeWidth="2.2" />
          ) : (
            <path d="M 27.5 40 Q 32 45 36.5 40" fill="none" stroke="#1E2230" strokeWidth="2.2" strokeLinecap="round" />
          )}
        </>
      )}

      {petId === 'sparkchu' && (
        <>
          {/* Tail bolt */}
          <path d="M 48 44 L 58 32 L 53 31 L 60 19 L 48 29 L 53 31 Z" fill="#F59E0B" stroke="#92400E" strokeWidth="1" strokeLinejoin="round" />
          {/* Ears with dark tips */}
          <path d="M 18 22 L 10 6 L 26 16 Z" fill="#FDE047" />
          <path d="M 10 6 L 16 10 L 14 14 Z" fill="#1E2230" />
          <path d="M 46 22 L 54 6 L 38 16 Z" fill="#FDE047" />
          <path d="M 54 6 L 48 10 L 50 14 Z" fill="#1E2230" />
          {/* Head/body */}
          <ellipse cx="32" cy="35.5" rx="21.5" ry="19.5" fill="#FACC15" />
          <ellipse cx="24" cy="23" rx="6" ry="3" fill="rgba(255,255,255,0.35)" transform="rotate(-15 24 23)" />
          {/* Electric red cheeks */}
          <circle cx="17" cy="39.5" r="4.2" fill="#EF4444" />
          <circle cx="47" cy="39.5" r="4.2" fill="#EF4444" />
          {renderEyes()}
          <path d="M 28.5 39.5 Q 30.5 42.5 32 40 Q 33.5 42.5 35.5 39.5" fill="none" stroke="#1E2230" strokeWidth="2" strokeLinecap="round" />
        </>
      )}

      {petId === 'emberkit' && (
        <>
          {/* Flame tail */}
          <path d="M 47 45 C 57 42, 60 28, 52 22 C 53 28, 49 30, 48 34 Z" fill="#F97316" />
          <path d="M 50 38 C 55 36, 56 29, 52 26 Z" fill="#FDE047" />
          {/* Fox ears */}
          <path d="M 16 24 L 12 7 L 27 17 Z" fill="#EA580C" />
          <path d="M 15 19 L 14 11 L 23 17 Z" fill="#FDE68A" />
          <path d="M 48 24 L 52 7 L 37 17 Z" fill="#EA580C" />
          <path d="M 49 19 L 50 11 L 41 17 Z" fill="#FDE68A" />
          {/* Body */}
          <ellipse cx="32" cy="35.5" rx="21.5" ry="19" fill="#FB923C" />
          <ellipse cx="32" cy="44" rx="12.5" ry="9" fill="#FEF3C7" />
          <circle cx="18" cy="38.5" r="3.2" fill="rgba(239,68,68,0.35)" />
          <circle cx="46" cy="38.5" r="3.2" fill="rgba(239,68,68,0.35)" />
          {renderEyes()}
          <polygon points="30.5,37 33.5,37 32,39" fill="#1E2230" />
          <path d="M 28.5 41 Q 32 44 35.5 41" fill="none" stroke="#1E2230" strokeWidth="2" strokeLinecap="round" />
        </>
      )}

      {petId === 'aquapup' && (
        <>
          {/* Top water fin */}
          <path d="M 32 6 C 36 11, 37 16, 32 18 C 27 16, 28 11, 32 6 Z" fill="#0284C7" />
          {/* Cheek fins */}
          <path d="M 12 34 L 5 39 L 13 43 Z" fill="#38BDF8" />
          <path d="M 52 34 L 59 39 L 51 43 Z" fill="#38BDF8" />
          {/* Body */}
          <ellipse cx="32" cy="35.5" rx="21.5" ry="19" fill="#38BDF8" />
          <ellipse cx="32" cy="40" rx="15" ry="11.5" fill="#E0F2FE" />
          <circle cx="19" cy="38.5" r="3.2" fill="rgba(244,114,182,0.4)" />
          <circle cx="45" cy="38.5" r="3.2" fill="rgba(244,114,182,0.4)" />
          {renderEyes()}
          <path d="M 28 40 Q 32 44.5 36 40" fill="none" stroke="#0F172A" strokeWidth="2.1" strokeLinecap="round" />
        </>
      )}

      {petId === 'leafsprout' && (
        <>
          {/* Sprouting leaf crest */}
          <path d="M 32 17 C 27 6, 17 7, 19 15 C 22 18, 28 17, 32 17 Z" fill="#16A34A" />
          <path d="M 32 17 C 37 5, 47 7, 45 15 C 42 18, 36 17, 32 17 Z" fill="#22C55E" />
          {/* Body */}
          <ellipse cx="32" cy="36" rx="22" ry="19" fill="#4ADE80" />
          {/* Spots */}
          <circle cx="21" cy="23" r="2.4" fill="#15803D" opacity="0.5" />
          <circle cx="43" cy="24" r="2" fill="#15803D" opacity="0.5" />
          <circle cx="18" cy="39" r="3.3" fill="rgba(244,114,182,0.4)" />
          <circle cx="46" cy="39" r="3.3" fill="rgba(244,114,182,0.4)" />
          {renderEyes('#14532D')}
          <path d="M 27.5 40.5 Q 32 44.5 36.5 40.5" fill="none" stroke="#14532D" strokeWidth="2.2" strokeLinecap="round" />
        </>
      )}

      {petId === 'eevo' && (
        <>
          {/* Long fox ears */}
          <path d="M 19 24 L 8 5 L 28 16 Z" fill="#B4733B" />
          <path d="M 17 20 L 12 9 L 25 16 Z" fill="#5B3618" />
          <path d="M 45 24 L 56 5 L 36 16 Z" fill="#B4733B" />
          <path d="M 47 20 L 52 9 L 39 16 Z" fill="#5B3618" />
          {/* Cream collar ruff */}
          <ellipse cx="32" cy="48" rx="18" ry="8" fill="#FEF3C7" />
          <ellipse cx="32" cy="34.5" rx="20.5" ry="18.5" fill="#C68446" />
          <circle cx="18.5" cy="38" r="3.2" fill="rgba(244,114,182,0.42)" />
          <circle cx="45.5" cy="38" r="3.2" fill="rgba(244,114,182,0.42)" />
          {renderEyes()}
          <polygon points="30.8,36.8 33.2,36.8 32,38.4" fill="#1E2230" />
          <path d="M 28.5 40.5 Q 32 43.5 35.5 40.5" fill="none" stroke="#1E2230" strokeWidth="2" strokeLinecap="round" />
        </>
      )}

      {petId === 'snorblob' && (
        <>
          <path d="M 16 24 L 14 11 L 25 18 Z" fill="#0F4C5C" />
          <path d="M 48 24 L 50 11 L 39 18 Z" fill="#0F4C5C" />
          <ellipse cx="32" cy="36" rx="23.5" ry="20.5" fill="#115E67" />
          <ellipse cx="32" cy="37" rx="18.5" ry="15.5" fill="#FEF3C7" />
          <circle cx="19" cy="38.5" r="3.2" fill="rgba(244,114,182,0.4)" />
          <circle cx="45" cy="38.5" r="3.2" fill="rgba(244,114,182,0.4)" />
          {renderEyes('#1E293B')}
          <path d="M 28 40 Q 32 43.5 36 40" fill="none" stroke="#1E293B" strokeWidth="2.1" strokeLinecap="round" />
          <polygon points="27,41.2 28.6,41.2 27.8,39.2" fill="#FFFFFF" />
          <polygon points="35.4,41.2 37,41.2 36.2,39.2" fill="#FFFFFF" />
        </>
      )}

      {petId === 'genglet' && (
        <>
          {/* Spiky shadow silhouette */}
          <path
            d="M 14 25 L 10 8 L 24 16 L 32 9 L 40 16 L 54 8 L 50 25 Q 56 38 48 51 Q 32 58 16 51 Q 8 38 14 25 Z"
            fill="#6D28D9"
          />
          <ellipse cx="26" cy="24" rx="5" ry="2.5" fill="rgba(255,255,255,0.16)" transform="rotate(-15 26 24)" />
          {/* Crimson eyes */}
          {blink ? (
            <>
              <line x1="20" y1={eyeY} x2="27" y2={eyeY} stroke="#FCA5A5" strokeWidth="2.5" strokeLinecap="round" />
              <line x1="37" y1={eyeY} x2="44" y2={eyeY} stroke="#FCA5A5" strokeWidth="2.5" strokeLinecap="round" />
            </>
          ) : (
            <>
              <path d="M 19 28 L 27.5 32.5 L 20.5 34.5 Z" fill="#F87171" />
              <circle cx="23.5" cy="31.8" r="1.1" fill="#1E1B4B" />
              <path d="M 45 28 L 36.5 32.5 L 43.5 34.5 Z" fill="#F87171" />
              <circle cx="40.5" cy="31.8" r="1.1" fill="#1E1B4B" />
            </>
          )}
          {/* Signature wide grin */}
          <path d="M 21 39 Q 32 48 43 39 Q 32 43 21 39 Z" fill="#FFFFFF" stroke="#1E1B4B" strokeWidth="1.2" />
          <line x1="26.5" y1="40.5" x2="26.5" y2="43.5" stroke="#CBD5E1" strokeWidth="1" />
          <line x1="32" y1="41.5" x2="32" y2="44.2" stroke="#CBD5E1" strokeWidth="1" />
          <line x1="37.5" y1="40.5" x2="37.5" y2="43.5" stroke="#CBD5E1" strokeWidth="1" />
        </>
      )}
    </svg>
  );
};

interface PipPetProps {
  petId?: PetId;
  bond?: PetBondState;
  writing?: boolean;
  thinking?: boolean;
  cheerTrigger?: number;
  compact?: boolean;
  speechOverride?: string | null;
  onSwitchPet?: (id: PetId) => void;
  onPetAction?: (kind: 'pet' | 'treat' | 'study', xpGain?: number) => void;
}

export const PipPet: React.FC<PipPetProps> = ({
  petId = 'sparkchu',
  bond,
  writing = false,
  thinking = false,
  cheerTrigger = 0,
  compact = true,
  speechOverride = null,
  onSwitchPet,
  onPetAction,
}) => {
  const [blink, setBlink] = useState(false);
  const [cheering, setCheering] = useState(false);
  const [say, setSay] = useState<string | null>(null);
  const [panelOpen, setPanelOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const spec = getCompanionSpec(petId);
  const xp = bond?.xp ?? 45;
  const level = bond?.level ?? Math.max(1, Math.floor(xp / 50) + 1);
  const levelProgress = (xp % 50) / 50;

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
    if (!panelOpen) return;
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

  const bubble = speechOverride || say || (thinking ? `${spec.name} is thinking…` : null);

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
        title={`${spec.name} · Lv.${level} Study Companion (tap to interact)`}
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
        <div className="absolute right-0 top-full z-50 mt-2.5 w-80 overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/12 dark:bg-[#191A1F] animate-stagger-in">
          {/* Companion Header */}
          <div
            className="flex items-center justify-between px-4 py-3.5"
            style={{ backgroundColor: spec.bgSoft }}
          >
            <div className="flex items-center gap-3">
              <div className={`h-14 w-14 ${cheering ? 'animate-pip-cheer' : 'animate-pip-float'}`}>
                <PetAvatarSvg petId={petId} blink={blink} happy={cheering} />
              </div>
              <div>
                <div className="flex items-center gap-1.5">
                  <span className="text-sm font-bold text-slate-900 dark:text-white">{spec.name}</span>
                  <span
                    className="font-mono-num rounded-md px-1.5 py-0.5 text-[10px] font-bold text-white"
                    style={{ backgroundColor: spec.accent }}
                  >
                    Lv.{level}
                  </span>
                </div>
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
