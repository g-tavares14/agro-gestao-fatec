import { persistToken, readToken, clearToken, TOKEN_KEY } from './token-storage';

describe('token-storage', () => {
  afterEach(() => {
    clearToken();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('persists the token in memory and sessionStorage only', () => {
    persistToken('abc.def');
    expect(readToken()).toBe('abc.def');
    expect(sessionStorage.getItem(TOKEN_KEY)).toBe('abc.def');
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
  });

  it('reads from sessionStorage when memory is empty', () => {
    sessionStorage.setItem(TOKEN_KEY, 'from-session');
    expect(readToken()).toBe('from-session');
  });

  it('does not read leftover localStorage tokens', () => {
    localStorage.setItem(TOKEN_KEY, 'legacy');
    expect(readToken()).toBeNull();
  });

  it('clears memory and sessionStorage', () => {
    persistToken('abc.def');
    localStorage.setItem(TOKEN_KEY, 'legacy');
    clearToken();
    expect(readToken()).toBeNull();
    expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull();
  });
});
