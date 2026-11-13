import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  onSnapshot,
  orderBy,
  query,
  setDoc,
  Unsubscribe,
} from 'firebase/firestore';
import { db, handleFirestoreError, isFirebaseConfigured, OperationType, User } from '../firebase';
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

const COLLAB_COLORS = ['#3D6CF4', '#E11D48', '#0F7E5C', '#7C3AED', '#D97706', '#0EA5E9'];
const FREE_RELAY_BASE = 'https://ntfy.sh';

function memberColorForUid(uid: string): string {
  const hash = Array.from(uid).reduce((acc, ch) => (acc * 31 + ch.charCodeAt(0)) | 0, 7);
  return COLLAB_COLORS[Math.abs(hash) % COLLAB_COLORS.length];
}

function sanitizeForFirestore<T>(val: T): T {
  return JSON.parse(
    JSON.stringify(val, (_key, v) => (v === undefined ? null : v))
  ) as T;
}

function compactNotebookForCloud(nb: NotebookMeta): NotebookMeta {
  const raw = JSON.stringify(nb);
  if (raw.length < 850_000) return sanitizeForFirestore(nb);
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
  return sanitizeForFirestore({ ...nb, pages: trimmedPages });
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
  if (!isFirebaseConfigured) {
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
    return;
  }
  const path = `users/${user.uid}`;
  try {
    await setDoc(
      doc(db, 'users', user.uid),
      {
        uid: user.uid,
        displayName: user.displayName || 'Pro Notes User',
        photoURL: user.photoURL || '',
        email: user.email || '',
        updatedAt: Date.now(),
      },
      { merge: true }
    );
  } catch (err) {
    handleFirestoreError(err, OperationType.WRITE, path);
  }
}

export async function pushSettingsToCloud(uid: string, settings: AppSettings): Promise<void> {
  const safeSettings = sanitizeForFirestore({
    ...settings,
    customAiApiKey: '',
  });
  if (!isFirebaseConfigured) {
    try {
      localStorage.setItem(freeVaultKey(uid, 'settings'), JSON.stringify(safeSettings));
    } catch {
      // ignore
    }
    return;
  }
  const path = `users/${uid}/preferences/main`;
  try {
    await setDoc(doc(db, 'users', uid, 'preferences', 'main'), {
      updatedAt: Date.now(),
      data: safeSettings,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.WRITE, path);
  }
}

export async function pushLibraryListsToCloud(
  uid: string,
  folders: Folder[],
  flashcards: Flashcard[]
): Promise<void> {
  if (!isFirebaseConfigured) {
    try {
      localStorage.setItem(freeVaultKey(uid, 'folders'), JSON.stringify(folders));
      localStorage.setItem(freeVaultKey(uid, 'flashcards'), JSON.stringify(flashcards));
    } catch {
      // ignore
    }
    return;
  }
  try {
    await setDoc(doc(db, 'users', uid, 'library', 'folders'), {
      updatedAt: Date.now(),
      items: sanitizeForFirestore(folders),
    });
    await setDoc(doc(db, 'users', uid, 'library', 'flashcards'), {
      updatedAt: Date.now(),
      items: sanitizeForFirestore(flashcards),
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.WRITE, `users/${uid}/library`);
  }
}

export async function pushNotebookToCloud(uid: string, nb: NotebookMeta): Promise<void> {
  const compacted = compactNotebookForCloud(nb);
  if (!isFirebaseConfigured) {
    try {
      const existingRaw = localStorage.getItem(freeVaultKey(uid, 'notebooks'));
      const map: Record<string, NotebookMeta> = existingRaw ? JSON.parse(existingRaw) : {};
      map[nb.id] = compacted;
      localStorage.setItem(freeVaultKey(uid, 'notebooks'), JSON.stringify(map));
    } catch {
      // ignore
    }
    return;
  }
  const path = `users/${uid}/notebooks/${nb.id}`;
  try {
    await setDoc(doc(db, 'users', uid, 'notebooks', nb.id), {
      id: nb.id,
      title: nb.title.slice(0, 280),
      folderId: nb.folderId || '',
      modified: nb.modified,
      updatedAt: Date.now(),
      deleted: nb.deletedAt !== null,
      payload: compacted,
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.WRITE, path);
  }
}

export async function deleteNotebookFromCloud(uid: string, notebookId: string): Promise<void> {
  if (!isFirebaseConfigured) {
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
    return;
  }
  const path = `users/${uid}/notebooks/${notebookId}`;
  try {
    await deleteDoc(doc(db, 'users', uid, 'notebooks', notebookId));
  } catch (err) {
    handleFirestoreError(err, OperationType.DELETE, path);
  }
}

export async function pullFullCloudLibrary(uid: string): Promise<{
  settingsPatch: Partial<AppSettings> | null;
  folders: Folder[];
  flashcards: Flashcard[];
  notebooks: NotebookMeta[];
}> {
  if (!isFirebaseConfigured) {
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

  let settingsPatch: Partial<AppSettings> | null = null;
  let folders: Folder[] = [];
  let flashcards: Flashcard[] = [];
  const notebooks: NotebookMeta[] = [];

  try {
    const prefSnap = await getDoc(doc(db, 'users', uid, 'preferences', 'main'));
    if (prefSnap.exists()) {
      const data = prefSnap.data()?.data;
      if (data && typeof data === 'object') {
        settingsPatch = data as Partial<AppSettings>;
      }
    }
  } catch (err) {
    handleFirestoreError(err, OperationType.GET, `users/${uid}/preferences/main`);
  }

  try {
    const foldersSnap = await getDoc(doc(db, 'users', uid, 'library', 'folders'));
    if (foldersSnap.exists()) {
      const items = foldersSnap.data()?.items;
      if (Array.isArray(items)) folders = items as Folder[];
    }
  } catch (err) {
    handleFirestoreError(err, OperationType.GET, `users/${uid}/library/folders`);
  }

  try {
    const fcSnap = await getDoc(doc(db, 'users', uid, 'library', 'flashcards'));
    if (fcSnap.exists()) {
      const items = fcSnap.data()?.items;
      if (Array.isArray(items)) flashcards = items as Flashcard[];
    }
  } catch (err) {
    handleFirestoreError(err, OperationType.GET, `users/${uid}/library/flashcards`);
  }

  try {
    const nbSnaps = await getDocs(collection(db, 'users', uid, 'notebooks'));
    nbSnaps.forEach((snap) => {
      const payload = snap.data()?.payload as NotebookMeta | undefined;
      if (payload && payload.id) {
        notebooks.push(payload);
      }
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.LIST, `users/${uid}/notebooks`);
  }

  return { settingsPatch, folders, flashcards, notebooks };
}

// ================================================================
// Live Multi-User Collaborative Study Rooms (Free Relay + Firestore)
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

  if (!isFirebaseConfigured) {
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

  try {
    await setDoc(doc(db, 'collabRooms', code), room);
    await setDoc(doc(db, 'collabRooms', code, 'members', user.uid), hostMember);
    return room;
  } catch (err) {
    handleFirestoreError(err, OperationType.CREATE, `collabRooms/${code}`);
  }
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

  if (!isFirebaseConfigured) {
    const state = loadFreeRoomState(roomId, user.uid, `Study Room ${roomId}`);
    state.members[user.uid] = member;
    saveFreeRoomState(roomId, state);
    void postFreeRelay(freeRoomTopic(roomId), { kind: 'member', member, room: state.room });
    return state.room;
  }

  const path = `collabRooms/${roomId}`;
  try {
    const snap = await getDoc(doc(db, 'collabRooms', roomId));
    if (!snap.exists()) {
      throw new Error(`Study room "${roomId}" was not found.`);
    }
    const room = snap.data() as CollabRoom;
    await setDoc(doc(db, 'collabRooms', roomId, 'members', user.uid), member);
    return room;
  } catch (err) {
    if (err instanceof Error && err.message.includes('was not found')) throw err;
    handleFirestoreError(err, OperationType.GET, path);
  }
}

export function subscribeCollabRoom(
  roomId: string,
  callbacks: {
    onMembers: (members: CollabMember[]) => void;
    onItems: (items: CollabItemDoc[]) => void;
    onMessages: (msgs: CollabChatMessage[]) => void;
  }
): Unsubscribe {
  if (!isFirebaseConfigured) {
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

  const unsubMembers = onSnapshot(
    collection(db, 'collabRooms', roomId, 'members'),
    (snap) => {
      const list: CollabMember[] = [];
      snap.forEach((d) => list.push(d.data() as CollabMember));
      callbacks.onMembers(list);
    },
    (err) => handleFirestoreError(err, OperationType.LIST, `collabRooms/${roomId}/members`)
  );

  const unsubItems = onSnapshot(
    collection(db, 'collabRooms', roomId, 'items'),
    (snap) => {
      const list: CollabItemDoc[] = [];
      snap.forEach((d) => list.push(d.data() as CollabItemDoc));
      list.sort((a, b) => a.updatedAt - b.updatedAt);
      callbacks.onItems(list);
    },
    (err) => handleFirestoreError(err, OperationType.LIST, `collabRooms/${roomId}/items`)
  );

  const unsubMessages = onSnapshot(
    query(collection(db, 'collabRooms', roomId, 'messages'), orderBy('createdAt', 'asc')),
    (snap) => {
      const list: CollabChatMessage[] = [];
      snap.forEach((d) => list.push(d.data() as CollabChatMessage));
      callbacks.onMessages(list);
    },
    (err) => handleFirestoreError(err, OperationType.LIST, `collabRooms/${roomId}/messages`)
  );

  return () => {
    unsubMembers();
    unsubItems();
    unsubMessages();
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
    data: sanitizeForFirestore(itemDoc.data),
  };

  if (!isFirebaseConfigured) {
    const st = loadFreeRoomState(roomId);
    st.items[fullDoc.id] = fullDoc;
    saveFreeRoomState(roomId, st);
    void postFreeRelay(freeRoomTopic(roomId), { kind: 'item', item: fullDoc });
    return;
  }

  const path = `collabRooms/${roomId}/items/${itemDoc.id}`;
  try {
    await setDoc(doc(db, 'collabRooms', roomId, 'items', itemDoc.id), fullDoc);
  } catch (err) {
    handleFirestoreError(err, OperationType.WRITE, path);
  }
}

export async function deleteCollabItem(roomId: string, itemId: string): Promise<void> {
  if (!isFirebaseConfigured) {
    const st = loadFreeRoomState(roomId);
    delete st.items[itemId];
    saveFreeRoomState(roomId, st);
    void postFreeRelay(freeRoomTopic(roomId), { kind: 'delete_item', itemId });
    return;
  }
  const path = `collabRooms/${roomId}/items/${itemId}`;
  try {
    await deleteDoc(doc(db, 'collabRooms', roomId, 'items', itemId));
  } catch (err) {
    handleFirestoreError(err, OperationType.DELETE, path);
  }
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

  if (!isFirebaseConfigured) {
    const st = loadFreeRoomState(roomId);
    st.members[user.uid] = member;
    saveFreeRoomState(roomId, st);
    return;
  }

  const path = `collabRooms/${roomId}/members/${user.uid}`;
  try {
    await setDoc(doc(db, 'collabRooms', roomId, 'members', user.uid), member, { merge: true });
  } catch (err) {
    handleFirestoreError(err, OperationType.UPDATE, path);
  }
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

  if (!isFirebaseConfigured) {
    const st = loadFreeRoomState(roomId);
    st.messages.push(msg);
    saveFreeRoomState(roomId, st);
    void postFreeRelay(freeRoomTopic(roomId), { kind: 'chat', msg });
    return;
  }

  const path = `collabRooms/${roomId}/messages/${id}`;
  try {
    await setDoc(doc(db, 'collabRooms', roomId, 'messages', id), msg);
  } catch (err) {
    handleFirestoreError(err, OperationType.CREATE, path);
  }
}
