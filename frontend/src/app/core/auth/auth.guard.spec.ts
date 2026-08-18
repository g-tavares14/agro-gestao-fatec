import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UrlTree, provideRouter } from '@angular/router';
import { firstValueFrom, isObservable, Observable } from 'rxjs';
import { apiUrl } from '../api/api';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';
import { clearToken } from './token-storage';

describe('authGuard', () => {
  beforeEach(() => {
    clearToken();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
  });

  afterEach(() => {
    clearToken();
    sessionStorage.clear();
  });

  it('blocks unauthenticated users', () => {
    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
    expect(result instanceof UrlTree).toBe(true);
  });

  it('allows users after loading /me', async () => {
    TestBed.inject(AuthService).setToken('token');
    const http = TestBed.inject(HttpTestingController);
    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
    expect(isObservable(result)).toBe(true);
    const allowed = firstValueFrom(result as Observable<boolean | UrlTree>);
    http.expectOne(apiUrl('/auth/me')).flush({
      id: '11111111-1111-1111-1111-111111111111',
      name: 'Ana',
      email: 'a@b.com',
    });
    expect(await allowed).toBe(true);
  });

  it('logs out when /me returns 401', async () => {
    const auth = TestBed.inject(AuthService);
    auth.setToken('token');
    const http = TestBed.inject(HttpTestingController);
    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
    const decided = firstValueFrom(result as Observable<boolean | UrlTree>);
    http.expectOne(apiUrl('/auth/me')).flush(
      { message: 'Autenticação necessária' },
      { status: 401, statusText: 'Unauthorized' },
    );
    const value = await decided;
    expect(value instanceof UrlTree).toBe(true);
    expect(auth.isAuthenticated()).toBe(false);
  });
});
