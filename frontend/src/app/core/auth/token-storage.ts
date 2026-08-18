export const TOKEN_KEY = 'agrogestao.token';

let memoryToken: string | null = null;

export function persistToken(token: string): void {
  memoryToken = token;
  try {
    sessionStorage.setItem(TOKEN_KEY, token);
  } catch {
    /* Safari private / partitioned storage */
  }
}

export function readToken(): string | null {
  if (memoryToken) {
    return memoryToken;
  }
  try {
    return sessionStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function clearToken(): void {
  memoryToken = null;
  try {
    sessionStorage.removeItem(TOKEN_KEY);
  } catch {
    /* ignore */
  }
}
