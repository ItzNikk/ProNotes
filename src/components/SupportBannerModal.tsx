import React, { useState } from 'react';
import { Check, Heart, MessageSquareHeart, Share2, Sparkles, X } from 'lucide-react';
import { LiquidButton, LiquidGlassSurface, LiquidInput, LiquidModal } from './LiquidGlassSurface';

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
    const shareText =
      'Check out Pro Notes — free handwritten notes, mind maps, writable split view, and live collaborative study!';
    try {
      if (navigator.share) {
        await navigator.share({ title: 'Pro Notes', text: shareText });
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
    <LiquidModal open={open} onClose={onClose} maxWidthClass="max-w-md">
      <div className="relative border-b border-black/8 bg-gradient-to-br from-[#24483F]/14 via-emerald-500/8 to-transparent px-6 py-5 dark:border-white/10">
        <LiquidButton
          onClick={onClose}
          className="absolute right-4 top-4 !h-8 !w-8 !p-0"
          title="Close"
        >
          <X className="h-4 w-4" />
        </LiquidButton>
        <div className="flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-[#24483F] to-[#4E7A65] text-white shadow-md">
            <Heart className="h-5 w-5 fill-white" />
          </div>
          <div>
            <h2 className="text-base font-bold tracking-tight">Thank You for Studying with Pro Notes</h2>
            <p className="text-xs font-semibold text-[#24483F] dark:text-[#8CC1AC]">
              OpenLeaf &amp; Liquid Glass · 100% Free
            </p>
          </div>
        </div>
      </div>

      <div className="space-y-4 p-6 text-sm">
        <p className="leading-relaxed text-[#5A5F58] dark:text-[#B1AC9F]">
          Pro Notes has zero paywalls, no ads, offline notebooks, cloud Google Sync, and live study rooms built for students and creators.
        </p>

        <LiquidGlassSurface variant="frost" radius={18} className="space-y-1.5 p-3.5 text-xs">
          <div className="flex items-center gap-1.5 font-bold">
            <Sparkles className="h-3.5 w-3.5 text-[#24483F] dark:text-[#8CC1AC]" /> What is new
          </div>
          <p className="text-[#5A5F58] dark:text-[#B1AC9F]">
            OpenLeaf botanical design, optical Liquid Glass UI with customizable transparency, stretchable canvas items, writable Split Notes, Flashcards, Quiz &amp; Live Collab!
          </p>
        </LiquidGlassSurface>

        <div className="space-y-2">
          <label className="block text-xs font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
            Leave a note or feature wish
          </label>
          <div className="flex gap-2">
            <LiquidInput
              type="text"
              value={feedback}
              onChange={(e) => setFeedback(e.target.value)}
              placeholder="Tell us what helped you today..."
              wrapperClassName="flex-1"
            />
            <LiquidButton
              tint="#24483F"
              onClick={() => {
                if (!feedback.trim()) return;
                setFeedback('');
                setNoteSent(true);
                window.setTimeout(() => setNoteSent(false), 2200);
              }}
            >
              {noteSent ? <Check className="h-3.5 w-3.5" /> : <MessageSquareHeart className="h-3.5 w-3.5" />}
              {noteSent ? 'Saved' : 'Send'}
            </LiquidButton>
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-2 pt-2">
          {onNeverRemind ? (
            <LiquidButton
              onClick={() => {
                onNeverRemind();
                onClose();
              }}
              className="!text-xs"
            >
              Hide reminders
            </LiquidButton>
          ) : (
            <span />
          )}

          <LiquidButton onClick={handleShare} tint="#24483F">
            {shared ? <Check className="h-3.5 w-3.5" /> : <Share2 className="h-3.5 w-3.5" />}
            {shared ? 'Copied Invite!' : 'Share Pro Notes'}
          </LiquidButton>
        </div>
      </div>
    </LiquidModal>
  );
};

export const SupportBanner: React.FC<{
  onOpenSupport: () => void;
  onDismiss: () => void;
}> = ({ onOpenSupport, onDismiss }) => (
  <LiquidGlassSurface variant="frost" radius={18} className="mx-6 mt-3 px-4 py-2.5 animate-stagger-in">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div className="flex min-w-0 items-center gap-3">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-[#24483F] text-white shadow-sm">
          <Heart className="h-4 w-4 fill-white" />
        </div>
        <div className="min-w-0">
          <div className="text-xs font-bold">Enjoying Pro Notes?</div>
          <div className="truncate text-[11px] text-[#5A5F58] dark:text-[#B1AC9F]">
            100% free &amp; ad-free. Share a kind word or invite a study partner!
          </div>
        </div>
      </div>
      <div className="flex items-center gap-2">
        <LiquidButton onClick={onOpenSupport} tint="#24483F" className="!px-3 !py-1.5 !text-xs">
          Open Support Note
        </LiquidButton>
        <LiquidButton onClick={onDismiss} className="!h-8 !w-8 !p-0" title="Dismiss">
          <X className="h-4 w-4" />
        </LiquidButton>
      </div>
    </div>
  </LiquidGlassSurface>
);
