import { User } from '../firebase';
import {
  AppSettings,
  CollabChatMessage,
  CollabItemDoc,
  CollabMember,
  CollabRoom,
  Flashcard,
  Folder,
  NotebookMeta,
  PaperStyle,
} from '../types/models';

export type Unsubscribe = () => void;

const COLLAB_COLORS = ['#3D6CF4', '#E11D48', '#0F7E5C', '#7C3AED', '#D97706', '#0EA5E9'];
const FREE_RELAY_BASE = 'https://ntfy.sh';

function memberColorForUid(uid: string): string {
  const hash = Array.from(uid).reduce((acc, ch) => (acc * 31 + ch.charCodeAt(0)) | 0, 7);
  return COLLAB_COLORS[Math.abs(hash) % COLLAB_COLORS.length];
}

function sanitizeForCloud<T>(val: T): T {
  return JSON.parse(
    JSON.stringify(val, (_key, v) => (v === undefined ? null : v))
  ) as T;
}

function compactNotebookForCloud(nb: NotebookMeta): NotebookMeta {
  const raw = JSON.stringify(nb);
  if (raw.length < 850_000) return sanitizeForCloud(nb);
  const trimmedPages = nb.pages.map((p) => ({
    ...p,
    cover: p.cover && p.cover.length > 180_000 ? null : p.cover,
    items: p.items.map((it) => {
      if (it.kind === 'image' && it.src.length > 220_000) {
        return { ...it, src: '' };
      }
      return it;
    }),
  }));
  return sanitizeForCloud({ ...nb, pages: trimmedPages });
}

function freeVaultKey(uid: string, suffix: string): string {
  return `pronotes_free_vault_${uid}_${suffix}`;
}

function freeRoomKey(roomId: string): string {
  return `pronotes_free_room_${roomId.trim().toUpperCase()}`;
}

function freeRoomTopic(roomId: string): string {
  const clean = roomId.trim().toUpperCase().replace(/[^A-Z0-9_-]/g, '');
  return `pronotes_v10_webroom_${clean}`;
}

async function postFreeRelay(topic: string, payload: unknown): Promise<void> {
  try {
    await fetch(`${FREE_RELAY_BASE}/${topic}`, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain;charset=UTF-8' },
      body: JSON.stringify(payload),
    });
  } catch {
    // Offline fallback uses localStorage + BroadcastChannel
  }
}

export async function syncUserProfileToCloud(user: User): Promise<void> {
  try {
    localStorage.setItem(
      freeVaultKey(user.uid, 'profile'),
      JSON.stringify({
        uid: user.uid,
        displayName: user.displayName || 'Pro Notes User',
        photoURL: user.photoURL || '',
        email: user.email || '',
        updatedAt: Date.now(),
      })
    );
  } catch {
    // ignore
  }
}

export async function pushSettingsToCloud(uid: string, settings: AppSettings): Promise<void> {
  const safeSettings = sanitizeForCloud({
    ...settings,
    customAiApiKey: '',
  });
  try {
    localStorage.setItem(freeVaultKey(uid, 'settings'), JSON.stringify(safeSettings));
  } catch {
    // ignore
  }
}

export async function pushLibraryListsToCloud(
  uid: string,
  folders: Folder[],
  flashcards: Flashcard[]
): Promise<void> {
  try {
    localStorage.setItem(freeVaultKey(uid, 'folders'), JSON.stringify(folders));
    localStorage.setItem(freeVaultKey(uid, 'flashcards'), JSON.stringify(flashcards));
  } catch {
    // ignore
  }
}

export async function pushNotebookToCloud(uid: string, nb: NotebookMeta): Promise<void> {
  const compacted = compactNotebookForCloud(nb);
  try {
    const existingRaw = localStorage.getItem(freeVaultKey(uid, 'notebooks'));
    const map: Record<string, NotebookMeta> = existingRaw ? JSON.parse(existingRaw) : {};
    map[nb.id] = compacted;
    localStorage.setItem(freeVaultKey(uid, 'notebooks'), JSON.stringify(map));
  } catch {
    // ignore
  }
}

export async function deleteNotebookFromCloud(uid: string, notebookId: string): Promise<void> {
  try {
    const existingRaw = localStorage.getItem(freeVaultKey(uid, 'notebooks'));
    if (existingRaw) {
      const map: Record<string, NotebookMeta> = JSON.parse(existingRaw);
      delete map[notebookId];
      localStorage.setItem(freeVaultKey(uid, 'notebooks'), JSON.stringify(map));
    }
  } catch {
    // ignore
  }
}

export async function pullFullCloudLibrary(uid: string): Promise<{
  settingsPatch: Partial<AppSettings> | null;
  folders: Folder[];
  flashcards: Flashcard[];
  notebooks: NotebookMeta[];
}> {
  try {
    const sRaw = localStorage.getItem(freeVaultKey(uid, 'settings'));
    const fRaw = localStorage.getItem(freeVaultKey(uid, 'folders'));
    const fcRaw = localStorage.getItem(freeVaultKey(uid, 'flashcards'));
    const nbRaw = localStorage.getItem(freeVaultKey(uid, 'notebooks'));
    const nbMap: Record<string, NotebookMeta> = nbRaw ? JSON.parse(nbRaw) : {};
    return {
      settingsPatch: sRaw ? (JSON.parse(sRaw) as Partial<AppSettings>) : null,
      folders: fRaw ? (JSON.parse(fRaw) as Folder[]) : [],
      flashcards: fcRaw ? (JSON.parse(fcRaw) as Flashcard[]) : [],
      notebooks: Object.values(nbMap),
    };
  } catch {
    return { settingsPatch: null, folders: [], flashcards: [], notebooks: [] };
  }
}

// ================================================================
// Live Multi-User Collaborative Study Rooms (Free Relay + BroadcastChannel)
// ================================================================

interface FreeRoomState {
  room: CollabRoom;
  members: Record<string, CollabMember>;
  items: Record<string, CollabItemDoc>;
  messages: CollabChatMessage[];
}

function loadFreeRoomState(roomId: string, fallbackOwner = 'host', fallbackTitle = 'Study Jam Room'): FreeRoomState {
  const key = freeRoomKey(roomId);
  try {
    const raw = localStorage.getItem(key);
    if (raw) return JSON.parse(raw) as FreeRoomState;
  } catch {
    // ignore
  }
  const now = Date.now();
  return {
    room: {
      roomId,
      ownerId: fallbackOwner,
      title: fallbackTitle,
      paperStyle: 'DOTTED',
      paperColor: '#FFFFFF',
      createdAt: now,
      updatedAt: now,
    },
    members: {},
    items: {},
    messages: [],
  };
}

function saveFreeRoomState(roomId: string, state: FreeRoomState): void {
  try {
    localStorage.setItem(freeRoomKey(roomId), JSON.stringify(state));
    if (typeof BroadcastChannel !== 'undefined') {
      const bc = new BroadcastChannel(`pronotes_room_${roomId}`);
      bc.postMessage(state);
      bc.close();
    }
  } catch {
    // ignore
  }
}

export async function createCollabRoom(
  user: User,
  title: string,
  paperStyle: PaperStyle = 'DOTTED',
  paperColor = '#FFFFFF'
): Promise<CollabRoom> {
  const code = `STUDY-${Math.random().toString(36).slice(2, 7).toUpperCase()}`;
  const now = Date.now();
  const room: CollabRoom = {
    roomId: code,
    ownerId: user.uid,
    title: title.trim() || 'Study Jam Room',
    paperStyle,
    paperColor,
    createdAt: now,
    updatedAt: now,
  };
  const hostMember: CollabMember = {
    uid: user.uid,
    displayName: user.displayName || user.email?.split('@')[0] || 'Host',
    color: memberColorForUid(user.uid),
    cursorX: 200,
    cursorY: 200,
    activePageIdx: 0,
    lastSeen: now,
  };

  const state: FreeRoomState = {
    room,
    members: { [user.uid]: hostMember },
    items: {},
    messages: [],
  };
  saveFreeRoomState(code, state);
  void postFreeRelay(freeRoomTopic(code), { kind: 'member', member: hostMember, room });
  return room;
}

export async function joinCollabRoom(user: User, roomIdInput: string): Promise<CollabRoom> {
  const roomId = roomIdInput.trim().toUpperCase();
  const member: CollabMember = {
    uid: user.uid,
    displayName: user.displayName || user.email?.split('@')[0] || 'Scholar',
    color: memberColorForUid(user.uid),
    cursorX: 240,
    cursorY: 240,
    activePageIdx: 0,
    lastSeen: Date.now(),
  };

  const state = loadFreeRoomState(roomId, user.uid, `Study Room ${roomId}`);
  state.members[user.uid] = member;
  saveFreeRoomState(roomId, state);
  void postFreeRelay(freeRoomTopic(roomId), { kind: 'member', member, room: state.room });
  return state.room;
}

export function subscribeCollabRoom(
  roomId: string,
  callbacks: {
    onMembers: (members: CollabMember[]) => void;
    onItems: (items: CollabItemDoc[]) => void;
    onMessages: (msgs: CollabChatMessage[]) => void;
  }
): Unsubscribe {
  const emitState = (st: FreeRoomState) => {
    callbacks.onMembers(Object.values(st.members));
    callbacks.onItems(Object.values(st.items).sort((a, b) => a.updatedAt - b.updatedAt));
    callbacks.onMessages([...st.messages].sort((a, b) => a.createdAt - b.createdAt));
  };

  emitState(loadFreeRoomState(roomId));

  const bc = typeof BroadcastChannel !== 'undefined' ? new BroadcastChannel(`pronotes_room_${roomId}`) : null;
  if (bc) {
    bc.onmessage = (ev) => {
      if (ev.data) emitState(ev.data as FreeRoomState);
    };
  }

  const timer = window.setInterval(async () => {
    try {
      const res = await fetch(`${FREE_RELAY_BASE}/${freeRoomTopic(roomId)}/json?poll=1&since=6h`);
      if (!res.ok) return;
      const text = await res.text();
      const st = loadFreeRoomState(roomId);
      let changed = false;
      for (const line of text.split('\n')) {
        if (!line.trim()) continue;
        const env = JSON.parse(line);
        if (env.event !== 'message' || !env.message) continue;
        const pkt = JSON.parse(env.message);
        if (pkt.kind === 'member' && pkt.member?.uid) {
          st.members[pkt.member.uid] = pkt.member;
          changed = true;
        } else if (pkt.kind === 'item' && pkt.item?.id) {
          st.items[pkt.item.id] = pkt.item;
          changed = true;
        } else if (pkt.kind === 'delete_item' && pkt.itemId) {
          delete st.items[pkt.itemId];
          changed = true;
        } else if (pkt.kind === 'chat' && pkt.msg?.id) {
          if (!st.messages.some((m) => m.id === pkt.msg.id)) {
            st.messages.push(pkt.msg);
            changed = true;
          }
        }
      }
      if (changed) {
        saveFreeRoomState(roomId, st);
        emitState(st);
      }
    } catch {
      // ignore network poll errors
    }
  }, 4500);

  return () => {
    window.clearInterval(timer);
    bc?.close();
  };
}

export async function upsertCollabItem(
  roomId: string,
  user: User,
  itemDoc: Omit<CollabItemDoc, 'authorId' | 'updatedAt'>
): Promise<void> {
  const fullDoc: CollabItemDoc = {
    id: itemDoc.id,
    kind: itemDoc.kind,
    authorId: user.uid,
    pageIdx: itemDoc.pageIdx ?? 0,
    updatedAt: Date.now(),
    data: sanitizeForCloud(itemDoc.data),
  };

  const st = loadFreeRoomState(roomId);
  st.items[fullDoc.id] = fullDoc;
  saveFreeRoomState(roomId, st);
  void postFreeRelay(freeRoomTopic(roomId), { kind: 'item', item: fullDoc });
}

export async function deleteCollabItem(roomId: string, itemId: string): Promise<void> {
  const st = loadFreeRoomState(roomId);
  delete st.items[itemId];
  saveFreeRoomState(roomId, st);
  void postFreeRelay(freeRoomTopic(roomId), { kind: 'delete_item', itemId });
}

export async function updateCollabCursor(
  roomId: string,
  user: User,
  cursorX: number,
  cursorY: number,
  activePageIdx = 0
): Promise<void> {
  const member: CollabMember = {
    uid: user.uid,
    displayName: user.displayName || user.email?.split('@')[0] || 'Scholar',
    color: memberColorForUid(user.uid),
    cursorX: Math.round(cursorX),
    cursorY: Math.round(cursorY),
    activePageIdx,
    lastSeen: Date.now(),
  };

  const st = loadFreeRoomState(roomId);
  st.members[user.uid] = member;
  saveFreeRoomState(roomId, st);
}

export async function sendCollabMessage(
  roomId: string,
  user: User,
  text: string
): Promise<void> {
  const clean = text.trim().slice(0, 1000);
  if (!clean) return;
  const id = `msg-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
  const msg: CollabChatMessage = {
    id,
    authorId: user.uid,
    authorName: user.displayName || user.email?.split('@')[0] || 'Scholar',
    text: clean,
    createdAt: Date.now(),
  };

  const st = loadFreeRoomState(roomId);
  st.messages.push(msg);
  saveFreeRoomState(roomId, st);
  void postFreeRelay(freeRoomTopic(roomId), { kind: 'chat', msg });
}
