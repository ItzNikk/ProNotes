export interface ProviderUserInfo {
  providerId: string;
  uid: string;
  displayName: string | null;
  email: string | null;
  phoneNumber: string | null;
  photoURL: string | null;
}

export interface User {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
  emailVerified: boolean;
  isAnonymous: boolean;
  tenantId?: string | null;
  providerData: ProviderUserInfo[];
}

export interface Auth {
  readonly currentUser: User | null;
}

export const isFirebaseConfigured = false;
export const db: null = null;

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

export const auth: Auth = {
  get currentUser() {
    return currentFreeUser;
  },
};

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
  const email = (customEmail || 'scholar@gmail.com').trim().toLowerCase();
  const displayName = (customName || email.split('@')[0] || 'Scholar').trim();
  const uid = `u_${await sha256Short(`pronotes_v10:${email}:`)}`;
  const freeUser: User = {
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
  };
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
  currentFreeUser = null;
  try {
    localStorage.removeItem(FREE_USER_STORAGE_KEY);
  } catch {
    // ignore
  }
  freeAuthListeners.forEach((cb) => cb(null));
}

export function onAuthStateChanged(_authObj: Auth, callback: AuthListener): () => void {
  freeAuthListeners.add(callback);
  queueMicrotask(() => callback(currentFreeUser));
  return () => {
    freeAuthListeners.delete(callback);
  };
}
