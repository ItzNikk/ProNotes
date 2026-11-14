import React, { useState } from 'react';
import {
  Check,
  Clock,
  Command,
  Copy,
  Download,
  Layers,
  Users,
  X,
} from 'lucide-react';
import {
  CollabChatMessage,
  CollabMember,
  CollabRoom,
} from '../types/models';
import {
  LiquidButton,
  LiquidGlassSurface,
  LiquidInput,
  LiquidModal,
} from './LiquidGlassSurface';

// ================================================================
// Live Multi-User Collaborative Study Room Modal
// ================================================================

export const CollabRoomModal: React.FC<{
  open: boolean;
  isSignedIn: boolean;
  activeRoom: CollabRoom | null;
  members: CollabMember[];
  messages: CollabChatMessage[];
  onSignInGoogle: () => Promise<void>;
  onCreateRoom: (title: string) => Promise<void>;
  onJoinRoom: (code: string) => Promise<void>;
  onLeaveRoom: () => void;
  onSendMessage: (text: string) => Promise<void>;
  onClose: () => void;
}> = ({
  open,
  isSignedIn,
  activeRoom,
  members,
  messages,
  onSignInGoogle,
  onCreateRoom,
  onJoinRoom,
  onLeaveRoom,
  onSendMessage,
  onClose,
}) => {
  const [roomTitle, setRoomTitle] = useState('Live Study Session');
  const [joinCode, setJoinCode] = useState('');
  const [chatText, setChatText] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [copiedCode, setCopiedCode] = useState(false);

  return (
    <LiquidModal open={open} onClose={onClose} maxWidthClass="max-w-lg">
      <div className="flex max-h-[88vh] flex-col">
        <div className="flex items-center justify-between border-b border-[#1F2421]/10 px-6 py-4 dark:border-[#EDE8DB]/10">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#24483F] text-[#F5F2E8] shadow-sm dark:bg-[#8CC1AC] dark:text-[#151E1B]">
              <Users className="h-4.5 w-4.5" />
            </div>
            <div>
              <h2 className="text-base font-bold tracking-tight">Live Collaborative Notes</h2>
              <p className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                Draw, annotate &amp; discuss together in real time
              </p>
            </div>
          </div>
          <LiquidButton onClick={onClose} className="!h-9 !w-9 !p-0">
            <X className="h-4 w-4" />
          </LiquidButton>
        </div>

        <div className="flex-1 space-y-4 overflow-y-auto p-6">
          {!activeRoom ? (
            <div className="space-y-4">
              {err && (
                <div className="rounded-2xl border border-[#9B3B3B]/30 bg-[#9B3B3B]/10 px-3.5 py-2 text-xs font-medium text-[#9B3B3B] dark:text-[#E58A8A]">
                  {err}
                </div>
              )}

              {!isSignedIn && (
                <LiquidGlassSurface variant="frost" radius={18} className="p-3.5">
                  <div className="flex items-center justify-between gap-3">
                    <div className="text-xs text-[#1F2421] dark:text-[#EDE8DB]">
                      <span className="font-bold text-[#24483F] dark:text-[#8CC1AC]">Instant Room Codes:</span>{' '}
                      Join or host with a room code below, or sign in with Google to sync your identity.
                    </div>
                    <LiquidButton
                      tint="#24483F"
                      onClick={() => void onSignInGoogle()}
                      className="shrink-0 !px-3.5 !py-1.5 !text-[11px]"
                    >
                      Google Sign-In
                    </LiquidButton>
                  </div>
                </LiquidGlassSurface>
              )}

              <LiquidGlassSurface variant="frost" radius={20} className="space-y-2.5 p-4">
                <div className="text-[11px] font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
                  Host a New Study Room
                </div>
                <div className="flex gap-2">
                  <LiquidInput
                    type="text"
                    value={roomTitle}
                    onChange={(e) => setRoomTitle(e.target.value)}
                    placeholder="Room name (e.g. Organic Chem Prep)"
                    wrapperClassName="flex-1"
                  />
                  <LiquidButton
                    tint="#24483F"
                    disabled={busy}
                    onClick={async () => {
                      setBusy(true);
                      setErr(null);
                      try {
                        await onCreateRoom(roomTitle);
                      } catch (e) {
                        setErr((e as Error).message);
                      } finally {
                        setBusy(false);
                      }
                    }}
                    className="!px-4 !py-2 !text-xs"
                  >
                    Create Room
                  </LiquidButton>
                </div>
              </LiquidGlassSurface>

              <LiquidGlassSurface variant="frost" radius={20} className="space-y-2.5 p-4">
                <div className="text-[11px] font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
                  Join Existing Room by Code
                </div>
                <div className="flex gap-2">
                  <LiquidInput
                    type="text"
                    value={joinCode}
                    onChange={(e) => setJoinCode(e.target.value.toUpperCase())}
                    placeholder="Enter code (e.g. BIO-101)"
                    className="font-mono-num uppercase"
                    wrapperClassName="flex-1"
                  />
                  <LiquidButton
                    tint="#2F6B4F"
                    disabled={busy || !joinCode.trim()}
                    onClick={async () => {
                      setBusy(true);
                      setErr(null);
                      try {
                        await onJoinRoom(joinCode);
                      } catch (e) {
                        setErr((e as Error).message);
                      } finally {
                        setBusy(false);
                      }
                    }}
                    className="!px-4 !py-2 !text-xs"
                  >
                    Join Room
                  </LiquidButton>
                </div>
                <div className="flex flex-wrap gap-1.5 pt-1">
                  {['BIO-101', 'CALC-202', 'PHYS-301', 'CHEM-LAB'].map((preset) => (
                    <LiquidButton
                      key={preset}
                      onClick={() => setJoinCode(preset)}
                      className="!px-2.5 !py-1 !text-[10px]"
                    >
                      #{preset}
                    </LiquidButton>
                  ))}
                </div>
              </LiquidGlassSurface>
            </div>
          ) : (
            <div className="space-y-4">
              <LiquidGlassSurface variant="frost" radius={20} className="p-3.5">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <div className="text-xs font-bold text-[#24483F] dark:text-[#8CC1AC]">
                      Connected: {activeRoom.title}
                    </div>
                    <div className="font-mono-num text-[11px] font-semibold text-[#5A5F58] dark:text-[#B1AC9F]">
                      Code: {activeRoom.roomId} · {members.length} active
                    </div>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <LiquidButton
                      onClick={() => {
                        navigator.clipboard?.writeText(activeRoom.roomId);
                        setCopiedCode(true);
                        window.setTimeout(() => setCopiedCode(false), 1600);
                      }}
                      className="!px-3 !py-1.5 !text-xs"
                    >
                      {copiedCode ? <Check className="h-3.5 w-3.5 text-emerald-600" /> : <Copy className="h-3.5 w-3.5" />}
                      {copiedCode ? 'Copied' : 'Copy Code'}
                    </LiquidButton>
                    <LiquidButton
                      onClick={onLeaveRoom}
                      className="!px-3 !py-1.5 !text-xs text-[#9B3B3B] dark:text-[#E58A8A]"
                    >
                      Leave
                    </LiquidButton>
                  </div>
                </div>
              </LiquidGlassSurface>

              <div className="flex flex-wrap gap-1.5">
                {members.map((m) => (
                  <LiquidGlassSurface
                    key={m.uid}
                    variant="frost"
                    radius={999}
                    className="inline-flex items-center gap-1.5 px-2.5 py-1 text-[11px] font-semibold"
                  >
                    <span className="h-2 w-2 rounded-full" style={{ backgroundColor: m.color }} />
                    {m.displayName}
                  </LiquidGlassSurface>
                ))}
              </div>

              <LiquidGlassSurface variant="frost" radius={20} className="flex h-52 flex-col overflow-hidden">
                <div className="flex-1 space-y-2 overflow-y-auto p-3 text-xs">
                  {messages.length === 0 ? (
                    <div className="py-10 text-center text-[#5A5F58] dark:text-[#B1AC9F]">
                      Everything drawn on the canvas syncs live! Say hello to your study group below.
                    </div>
                  ) : (
                    messages.map((msg) => (
                      <div
                        key={msg.id}
                        className="rounded-xl border border-[#1F2421]/8 bg-white/70 px-3 py-2 dark:border-[#EDE8DB]/10 dark:bg-white/6"
                      >
                        <span className="font-bold text-[#24483F] dark:text-[#8CC1AC]">{msg.authorName}: </span>
                        <span>{msg.text}</span>
                      </div>
                    ))
                  )}
                </div>
                <form
                  onSubmit={async (e) => {
                    e.preventDefault();
                    if (!chatText.trim()) return;
                    const txt = chatText;
                    setChatText('');
                    await onSendMessage(txt);
                  }}
                  className="flex gap-2 border-t border-[#1F2421]/8 p-2 dark:border-[#EDE8DB]/10"
                >
                  <LiquidInput
                    type="text"
                    value={chatText}
                    onChange={(e) => setChatText(e.target.value)}
                    placeholder="Message study group..."
                    wrapperClassName="flex-1 !py-1.5"
                  />
                  <LiquidButton tint="#24483F" onClick={() => undefined} className="!px-3.5 !py-1.5 !text-xs">
                    Send
                  </LiquidButton>
                </form>
              </LiquidGlassSurface>
            </div>
          )}
        </div>
      </div>
    </LiquidModal>
  );
};

// ================================================================
// Quick Command Palette (Ctrl/Cmd+K)
// ================================================================

export interface CommandPaletteAction {
  id: string;
  label: string;
  hint: string;
  icon: 'collab' | 'timer' | 'export' | 'slides';
  run: () => void;
}

export const CommandPaletteModal: React.FC<{
  open: boolean;
  actions: CommandPaletteAction[];
  onClose: () => void;
}> = ({ open, actions, onClose }) => {
  const [q, setQ] = useState('');

  const filtered = actions.filter(
    (a) =>
      !q.trim() ||
      a.label.toLowerCase().includes(q.toLowerCase()) ||
      a.hint.toLowerCase().includes(q.toLowerCase())
  );

  return (
    <LiquidModal open={open} onClose={onClose} maxWidthClass="max-w-md">
      <div className="flex items-center gap-2 border-b border-[#1F2421]/10 px-4 py-3 dark:border-[#EDE8DB]/10">
        <Command className="h-4 w-4 text-[#24483F] dark:text-[#8CC1AC]" />
        <input
          autoFocus
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Run a Pro Notes action..."
          className="flex-1 bg-transparent text-sm outline-none placeholder:text-[#5A5F58] dark:placeholder:text-[#B1AC9F]"
        />
        <LiquidButton onClick={onClose} className="!px-2.5 !py-1 !text-[10px]">
          ESC
        </LiquidButton>
      </div>
      <div className="max-h-72 space-y-1 overflow-y-auto p-2">
        {filtered.map((a) => (
          <button
            key={a.id}
            type="button"
            onClick={() => {
              a.run();
              onClose();
            }}
            className="flex w-full items-center justify-between rounded-2xl px-3.5 py-2.5 text-left transition hover:bg-[#24483F]/10 dark:hover:bg-[#8CC1AC]/15"
          >
            <div className="flex items-center gap-2.5">
              {a.icon === 'collab' && <Users className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />}
              {a.icon === 'timer' && <Clock className="h-4 w-4 text-amber-600 dark:text-amber-400" />}
              {a.icon === 'export' && <Download className="h-4 w-4 text-[#24483F] dark:text-[#8CC1AC]" />}
              {a.icon === 'slides' && <Layers className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />}
              <span className="text-xs font-semibold">{a.label}</span>
            </div>
            <span className="text-[11px] text-[#5A5F58] dark:text-[#B1AC9F]">{a.hint}</span>
          </button>
        ))}
      </div>
    </LiquidModal>
  );
};
