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
import { db, handleFirestoreError, OperationType, User } from '../firebase';
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
  // Respect Firestore 1MB document ceiling by compacting ultra-large embedded binary data URLs if present
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

export async function syncUserProfileToCloud(user: User): Promise<void> {
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
  const path = `users/${uid}/preferences/main`;
  try {
    // Strip sensitive local API key unless user keeps it locally
    const safeSettings = sanitizeForFirestore({
      ...settings,
      customAiApiKey: '',
    });
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
  const path = `users/${uid}/notebooks/${nb.id}`;
  try {
    const compacted = compactNotebookForCloud(nb);
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
// Live Multi-User Collaborative Study Rooms (Firestore onSnapshot)
// ================================================================

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
  try {
    await setDoc(doc(db, 'collabRooms', code), room);
    await setDoc(doc(db, 'collabRooms', code, 'members', user.uid), {
      uid: user.uid,
      displayName: user.displayName || user.email?.split('@')[0] || 'Host',
      color: memberColorForUid(user.uid),
      cursorX: 200,
      cursorY: 200,
      activePageIdx: 0,
      lastSeen: now,
    });
    return room;
  } catch (err) {
    handleFirestoreError(err, OperationType.CREATE, `collabRooms/${code}`);
  }
}

export async function joinCollabRoom(user: User, roomIdInput: string): Promise<CollabRoom> {
  const roomId = roomIdInput.trim().toUpperCase();
  const path = `collabRooms/${roomId}`;
  try {
    const snap = await getDoc(doc(db, 'collabRooms', roomId));
    if (!snap.exists()) {
      throw new Error(`Study room "${roomId}" was not found.`);
    }
    const room = snap.data() as CollabRoom;
    await setDoc(doc(db, 'collabRooms', roomId, 'members', user.uid), {
      uid: user.uid,
      displayName: user.displayName || user.email?.split('@')[0] || 'Scholar',
      color: memberColorForUid(user.uid),
      cursorX: 240,
      cursorY: 240,
      activePageIdx: 0,
      lastSeen: Date.now(),
    });
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
  const path = `collabRooms/${roomId}/items/${itemDoc.id}`;
  try {
    await setDoc(doc(db, 'collabRooms', roomId, 'items', itemDoc.id), {
      id: itemDoc.id,
      kind: itemDoc.kind,
      authorId: user.uid,
      pageIdx: itemDoc.pageIdx ?? 0,
      updatedAt: Date.now(),
      data: sanitizeForFirestore(itemDoc.data),
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.WRITE, path);
  }
}

export async function deleteCollabItem(roomId: string, itemId: string): Promise<void> {
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
  const path = `collabRooms/${roomId}/members/${user.uid}`;
  try {
    await setDoc(
      doc(db, 'collabRooms', roomId, 'members', user.uid),
      {
        uid: user.uid,
        displayName: user.displayName || user.email?.split('@')[0] || 'Scholar',
        color: memberColorForUid(user.uid),
        cursorX: Math.round(cursorX),
        cursorY: Math.round(cursorY),
        activePageIdx,
        lastSeen: Date.now(),
      },
      { merge: true }
    );
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
  const path = `collabRooms/${roomId}/messages/${id}`;
  try {
    await setDoc(doc(db, 'collabRooms', roomId, 'messages', id), {
      id,
      authorId: user.uid,
      authorName: user.displayName || user.email?.split('@')[0] || 'Scholar',
      text: clean,
      createdAt: Date.now(),
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.CREATE, path);
  }
}
