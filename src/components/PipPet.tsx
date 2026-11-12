import React, { useEffect, useState } from 'react';
import { Sparkles } from 'lucide-react';

interface PipPetProps {
  writing?: boolean;
  thinking?: boolean;
  cheerTrigger?: number;
  compact?: boolean;
  speechOverride?: string | null;
}

const CHEERS = [
  "You've got this!",
  'Keep going!',
  'Nice note!',
  'Study time ✨',
  'Proud of you!',
  'Sharp thinking!',
];

export const PipPet: React.FC<PipPetProps> = ({
  writing = false,
  thinking = false,
  cheerTrigger = 0,
  compact = true,
  speechOverride = null,
}) => {
  const [blink, setBlink] = useState(false);
  const [cheering, setCheering] = useState(false);
  const [say, setSay] = useState<string | null>(null);

  useEffect(() => {
    const id = setInterval(() => {
      setBlink(true);
      setTimeout(() => setBlink(false), 140);
    }, 3200);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    if (cheerTrigger > 0) {
      setCheering(true);
      setSay(CHEERS[cheerTrigger % CHEERS.length]);
      const t1 = setTimeout(() => setCheering(false), 700);
      const t2 = setTimeout(() => setSay(null), 2000);
      return () => {
        clearTimeout(t1);
        clearTimeout(t2);
      };
    }
  }, [cheerTrigger]);

  const handleTap = () => {
    setCheering(true);
    const msg = CHEERS[Math.floor(Math.random() * CHEERS.length)];
    setSay(msg);
    setTimeout(() => setCheering(false), 700);
    setTimeout(() => setSay((cur) => (cur === msg ? null : cur)), 1900);
  };

  const bubble = speechOverride || say || (thinking ? 'Thinking…' : null);

  return (
    <div className="relative inline-flex items-center select-none">
      {bubble && (
        <div className="pointer-events-none absolute left-1/2 top-full z-50 mt-2 -translate-x-1/2 whitespace-nowrap rounded-xl border border-black/10 bg-white px-2.5 py-1 text-xs font-medium text-slate-800 shadow-lg dark:border-white/15 dark:bg-[#22242B] dark:text-slate-100 animate-stagger-in">
          {bubble}
        </div>
      )}
      <button
        type="button"
        onClick={handleTap}
        title="Pip, your study buddy (tap to cheer)"
        className={`group relative flex items-center justify-center rounded-full transition-transform active:scale-90 ${
          compact ? 'h-9 w-9' : 'h-14 w-14'
        } ${cheering ? 'animate-pip-cheer' : 'animate-pip-float'}`}
      >
        {thinking && (
          <span className="absolute -inset-1 rounded-full bg-gradient-to-tr from-violet-500/40 via-fuchsia-400/35 to-sky-400/40 blur-[4px] animate-ai-orb" />
        )}
        <svg viewBox="0 0 64 64" className="relative h-full w-full drop-shadow-sm">
          <defs>
            <linearGradient id="pipGrad" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#D3B4FF" />
              <stop offset="100%" stopColor="#7C5DF0" />
            </linearGradient>
          </defs>
          {/* Soft shadow */}
          <ellipse cx="32" cy="57" rx="19" ry="3.5" fill="rgba(0,0,0,0.16)" />
          {/* Squishy body */}
          <ellipse cx="32" cy="34" rx="24" ry="21" fill="url(#pipGrad)" />
          {/* Specular highlight */}
          <ellipse cx="23" cy="22" rx="7" ry="4" fill="rgba(255,255,255,0.34)" transform="rotate(-18 23 22)" />
          {/* Blush cheeks */}
          <circle cx="17.5" cy="39" r="3.6" fill="rgba(255,130,175,0.45)" />
          <circle cx="46.5" cy="39" r="3.6" fill="rgba(255,130,175,0.45)" />
          {/* Eyes */}
          {blink ? (
            <>
              <line x1="21" y1="32" x2="27" y2="32" stroke="#1E2230" strokeWidth="2.5" strokeLinecap="round" />
              <line x1="37" y1="32" x2="43" y2="32" stroke="#1E2230" strokeWidth="2.5" strokeLinecap="round" />
            </>
          ) : (
            <>
              <circle cx={thinking ? 25.5 : 24} cy={writing ? 35 : thinking ? 29 : 32} r="3.2" fill="#1E2230" />
              <circle cx={thinking ? 26.5 : 25} cy={(writing ? 35 : thinking ? 29 : 32) - 1.2} r="1.1" fill="#FFFFFF" />
              <circle cx={thinking ? 41.5 : 40} cy={writing ? 35 : thinking ? 29 : 32} r="3.2" fill="#1E2230" />
              <circle cx={thinking ? 42.5 : 41} cy={(writing ? 35 : thinking ? 29 : 32) - 1.2} r="1.1" fill="#FFFFFF" />
            </>
          )}
          {/* Mouth */}
          {thinking ? (
            <circle cx="32" cy="41.5" r="2.3" fill="none" stroke="#1E2230" strokeWidth="2.2" />
          ) : (
            <path
              d="M 27 40 Q 32 45 37 40"
              fill="none"
              stroke="#1E2230"
              strokeWidth="2.3"
              strokeLinecap="round"
            />
          )}
        </svg>
      </button>
    </div>
  );
};

export const AiThinkingPill: React.FC<{ label?: string; modelName?: string }> = ({
  label = 'Thinking',
  modelName,
}) => {
  return (
    <div className="pointer-events-none fixed left-1/2 top-20 z-50 -translate-x-1/2 animate-stagger-in">
      <div className="relative overflow-hidden rounded-full border border-white/20 bg-[#181B24]/95 px-5 py-2.5 text-white shadow-2xl backdrop-blur-xl">
        {/* Moving light sweep */}
        <div className="pointer-events-none absolute inset-0 w-1/2 bg-gradient-to-r from-transparent via-violet-400/25 to-transparent animate-ai-shimmer" />
        <div className="relative flex items-center gap-3">
          <div className="relative flex h-6 w-6 items-center justify-center">
            <span className="absolute inset-0 rounded-full bg-gradient-to-tr from-violet-500 via-sky-400 to-amber-300 opacity-75 blur-[3px] animate-ai-orb" />
            <Sparkles className="relative h-4 w-4 text-violet-200" />
          </div>
          <div className="flex flex-col">
            <div className="flex items-center gap-1.5 text-sm font-semibold tracking-tight">
              <span>{label}</span>
              <span className="inline-flex items-center gap-1 pl-0.5">
                <span className="h-1.5 w-1.5 rounded-full bg-violet-300 animate-ai-dot-1" />
                <span className="h-1.5 w-1.5 rounded-full bg-sky-300 animate-ai-dot-2" />
                <span className="h-1.5 w-1.5 rounded-full bg-amber-300 animate-ai-dot-3" />
              </span>
            </div>
            {modelName && (
              <span className="text-[10px] font-medium text-slate-300/80">{modelName}</span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
