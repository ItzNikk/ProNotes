import React, { useState } from 'react';
import { Heart, MessageSquareHeart, Share2, Sparkles, X, Check } from 'lucide-react';

interface SupportModalProps {
  open: boolean;
  onClose: () => void;
  onNeverRemind?: () => void;
}

export const SupportModal: React.FC<SupportModalProps> = ({ open, onClose, onNeverRemind }) => {
  const [shared, setShared] = useState(false);
  const [noteSent, setNoteSent] = useState(false);
  const [feedback, setFeedback] = useState('');

  if (!open) return null;

  const handleShare = async () => {
    const shareText = 'Check out Pro Notes v10 — free handwritten notes, mind maps, AI chat, lecture recording & live collaborative study!';
    try {
      if (navigator.share) {
        await navigator.share({ title: 'Pro Notes v10', text: shareText });
      } else {
        await navigator.clipboard?.writeText(shareText);
      }
      setShared(true);
      window.setTimeout(() => setShared(false), 2000);
    } catch {
      // ignore cancel
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 backdrop-blur-sm">
      <div className="w-full max-w-md overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/12 dark:bg-[#191A1F] animate-stagger-in">
        <div className="relative bg-gradient-to-br from-rose-500/18 via-violet-500/12 to-blue-500/12 px-6 py-5 border-b border-black/8 dark:border-white/10">
          <button
            type="button"
            onClick={onClose}
            className="ios-stretch absolute right-4 top-4 rounded-full p-1.5 text-slate-500 hover:bg-black/5 dark:hover:bg-white/10"
          >
            <X className="h-5 w-5" />
          </button>
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-rose-500 to-pink-600 text-white shadow-md">
              <Heart className="h-5 w-5 fill-white" />
            </div>
            <div>
              <h2 className="text-base font-bold tracking-tight">Thank You for Studying with Pro Notes</h2>
              <p className="text-xs font-semibold text-rose-600 dark:text-rose-300">
                v10.0.0 · 100% Free &amp; Open-Source
              </p>
            </div>
          </div>
        </div>

        <div className="space-y-4 p-6 text-sm">
          <p className="leading-relaxed text-slate-600 dark:text-slate-300">
            Pro Notes has zero paywalls, no ads, and free on-device AI tools, cloud Google Sync, and live study rooms built for students and creators.
          </p>

          <div className="rounded-2xl border border-black/8 bg-slate-50/80 p-3.5 text-xs space-y-1.5 dark:border-white/10 dark:bg-[#22242C]">
            <div className="flex items-center gap-1.5 font-bold text-slate-800 dark:text-slate-100">
              <Sparkles className="h-3.5 w-3.5 text-violet-500" /> What is new in v10
            </div>
            <p className="text-slate-600 dark:text-slate-300">
              Universal stretchable canvas items &amp; UI, AI Note/PDF Chat, AI Stickers/Covers/Templates, AI Lecture Recording, Writable Split Notes, Optional Liquid Glass, Google Sync &amp; Live Collab!
            </p>
          </div>

          <div className="space-y-2">
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500">
              Leave a note or feature wish
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={feedback}
                onChange={(e) => setFeedback(e.target.value)}
                placeholder="Tell us what helped you today..."
                className="flex-1 rounded-xl border border-black/12 bg-white px-3 py-2 text-xs outline-none focus:border-blue-500 dark:border-white/15 dark:bg-[#22242C]"
              />
              <button
                type="button"
                onClick={() => {
                  if (!feedback.trim()) return;
                  setFeedback('');
                  setNoteSent(true);
                  window.setTimeout(() => setNoteSent(false), 2200);
                }}
                className="ios-stretch inline-flex items-center gap-1 rounded-xl bg-slate-900 px-3.5 py-2 text-xs font-semibold text-white dark:bg-white dark:text-slate-900"
              >
                {noteSent ? <Check className="h-3.5 w-3.5 text-emerald-500" /> : <MessageSquareHeart className="h-3.5 w-3.5" />}
                {noteSent ? 'Saved' : 'Send'}
              </button>
            </div>
          </div>

          <div className="flex flex-wrap items-center justify-between gap-2 pt-2">
            {onNeverRemind ? (
              <button
                type="button"
                onClick={() => {
                  onNeverRemind();
                  onClose();
                }}
                className="ios-stretch rounded-xl px-3 py-2 text-xs font-semibold text-slate-500 hover:bg-black/5 dark:hover:bg-white/5"
              >
                Hide reminders
              </button>
            ) : <span />}

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleShare}
                className="ios-stretch inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-rose-500 to-pink-600 px-4 py-2 text-xs font-semibold text-white shadow-sm"
              >
                {shared ? <Check className="h-3.5 w-3.5" /> : <Share2 className="h-3.5 w-3.5" />}
                {shared ? 'Copied Invite!' : 'Share Pro Notes'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export const SupportBanner: React.FC<{
  onOpenSupport: () => void;
  onDismiss: () => void;
}> = ({ onOpenSupport, onDismiss }) => (
  <div className="mx-6 mt-3 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-rose-500/25 bg-gradient-to-r from-rose-500/10 via-pink-500/10 to-violet-500/10 px-4 py-2.5 shadow-xs animate-stagger-in">
    <div className="flex items-center gap-3 min-w-0">
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-rose-500 text-white shadow-sm">
        <Heart className="h-4 w-4 fill-white" />
      </div>
      <div className="min-w-0">
        <div className="text-xs font-bold text-slate-800 dark:text-slate-100">
          Enjoying Pro Notes v10?
        </div>
        <div className="truncate text-[11px] text-slate-600 dark:text-slate-300">
          100% free &amp; ad-free. Share a kind word or invite a study partner!
        </div>
      </div>
    </div>
    <div className="flex items-center gap-2">
      <button
        type="button"
        onClick={onOpenSupport}
        className="ios-stretch rounded-xl bg-rose-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-rose-700"
      >
        Open Support Note
      </button>
      <button
        type="button"
        onClick={onDismiss}
        className="ios-stretch rounded-lg p-1 text-slate-400 hover:bg-black/5 hover:text-slate-700 dark:hover:bg-white/10"
        title="Dismiss"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  </div>
);
