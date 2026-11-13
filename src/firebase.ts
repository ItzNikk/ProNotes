import { initializeApp, FirebaseApp } from 'firebase/app';
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  signOut,
  onAuthStateChanged as fbOnAuthStateChanged,
  User,
  Auth,
} from 'firebase/auth';
import {
  getFirestore,
  Firestore,
} from 'firebase/firestore';
import firebaseConfig from '../firebase-applet-config.json';

export const isFirebaseConfigured = Boolean(
  firebaseConfig.apiKey && firebaseConfig.apiKey.trim().length > 0 &&
  firebaseConfig.projectId && firebaseConfig.projectId.trim().length > 0
);

let appInstance: FirebaseApp | null = null;
let dbInstance: Firestore | null = null;
let authInstance: Auth | null = null;

if (isFirebaseConfigured) {
  try {
    appInstance = initializeApp(firebaseConfig);
    dbInstance = getFirestore(appInstance, firebaseConfig.firestoreDatabaseId || '(default)');
    authInstance = getAuth(appInstance);
  } catch {
    appInstance = null;
    dbInstance = null;
    authInstance = null;
  }
}

export const db = dbInstance as Firestore;

const FREE_USER_STORAGE_KEY = 'pronotes_free_cloud_user_v10';
type AuthListener = (user: User | null) => void;
const freeAuthListeners = new Set<AuthListener>();

function loadFreeCloudUser(): User | null {
  try {
    const raw = localStorage.getItem(FREE_USER_STORAGE_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}

let currentFreeUser: User | null = loadFreeCloudUser();

export const auth = (authInstance ?? {
  get currentUser() {
    return currentFreeUser;
  },
}) as Auth;

const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({ prompt: 'select_account' });

export enum OperationType {
  CREATE = 'create',
  UPDATE = 'update',
  DELETE = 'delete',
  LIST = 'list',
  GET = 'get',
  WRITE = 'write',
}

export interface FirestoreErrorInfo {
  error: string;
  operationType: OperationType;
  path: string | null;
  authInfo: {
    userId: string | undefined;
    email: string | null | undefined;
    emailVerified: boolean | undefined;
    isAnonymous: boolean | undefined;
    tenantId: string | null | undefined;
    providerInfo: {
      providerId: string;
      displayName: string | null;
      email: string | null;
      photoUrl: string | null;
    }[];
  };
}

export function handleFirestoreError(
  error: unknown,
  operationType: OperationType,
  path: string | null
): never {
  const errInfo: FirestoreErrorInfo = {
    error: error instanceof Error ? error.message : String(error),
    authInfo: {
      userId: auth.currentUser?.uid,
      email: auth.currentUser?.email,
      emailVerified: auth.currentUser?.emailVerified,
      isAnonymous: auth.currentUser?.isAnonymous,
      tenantId: auth.currentUser?.tenantId,
      providerInfo:
        auth.currentUser?.providerData?.map((provider) => ({
          providerId: provider.providerId,
          displayName: provider.displayName,
          email: provider.email,
          photoUrl: provider.photoURL,
        })) || [],
    },
    operationType,
    path,
  };
  console.error('Cloud Sync Error:', JSON.stringify(errInfo));
  throw new Error(JSON.stringify(errInfo));
}

async function sha256Short(input: string): Promise<string> {
  try {
    const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(input));
    return Array.from(new Uint8Array(buf))
      .map((b) => b.toString(16).padStart(2, '0'))
      .join('')
      .slice(0, 22);
  } catch {
    return Array.from(input)
      .reduce((acc, c) => ((acc * 31 + c.charCodeAt(0)) >>> 0), 7)
      .toString(16);
  }
}

export async function signInWithGoogleAccount(customEmail?: string, customName?: string): Promise<User> {
  if (isFirebaseConfigured && authInstance) {
    const res = await signInWithPopup(authInstance, googleProvider);
    return res.user;
  }
  const email = (customEmail || 'scholar@gmail.com').trim().toLowerCase();
  const displayName = (customName || email.split('@')[0] || 'Scholar').trim();
  const uid = `u_${await sha256Short(`pronotes_v10:${email}:`)}`;
  const freeUser = {
    uid,
    email,
    displayName,
    photoURL: '',
    emailVerified: true,
    isAnonymous: false,
    providerData: [
      {
        providerId: 'google.com',
        uid,
        displayName,
        email,
        phoneNumber: null,
        photoURL: null,
      },
    ],
  } as unknown as User;
  currentFreeUser = freeUser;
  try {
    localStorage.setItem(FREE_USER_STORAGE_KEY, JSON.stringify(freeUser));
  } catch {
    // ignore storage quota errors
  }
  freeAuthListeners.forEach((cb) => cb(freeUser));
  return freeUser;
}

export async function signOutAccount(): Promise<void> {
  if (isFirebaseConfigured && authInstance) {
    await signOut(authInstance);
    return;
  }
  currentFreeUser = null;
  try {
    localStorage.removeItem(FREE_USER_STORAGE_KEY);
  } catch {
    // ignore
  }
  freeAuthListeners.forEach((cb) => cb(null));
}

export function onAuthStateChanged(authObj: Auth, callback: AuthListener): () => void {
  if (isFirebaseConfigured && authInstance) {
    return fbOnAuthStateChanged(authObj, callback);
  }
  freeAuthListeners.add(callback);
  queueMicrotask(() => callback(currentFreeUser));
  return () => {
    freeAuthListeners.delete(callback);
  };
}

export type { User };
