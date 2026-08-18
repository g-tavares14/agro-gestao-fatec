import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { apiUrl } from '../api/api';
import { AuthService, TOKEN_KEY } from './auth.service';
import { clearToken } from './token-storage';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    clearToken();
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    clearToken();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('stores the token in sessionStorage under agrogestao.token', () => {
    service.login({ email: 'a@b.com', password: 'secret12' }).subscribe();
    const req = http.expectOne(apiUrl('/auth/login'));
    req.flush({
      token: 'abc.def',
      user: { id: '11111111-1111-1111-1111-111111111111', name: 'Ana', email: 'a@b.com' },
    });

    expect(sessionStorage.getItem(TOKEN_KEY)).toBe('abc.def');
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
    expect(service.token()).toBe('abc.def');
    expect(service.user()?.id).toBe('11111111-1111-1111-1111-111111111111');
    expect(service.isAuthenticated()).toBe(true);
  });

  it('exchanges an OAuth code for a session', () => {
    service.exchangeOAuth('one-time').subscribe();
    const req = http.expectOne(apiUrl('/auth/oauth/exchange'));
    expect(req.request.body).toEqual({ code: 'one-time' });
    req.flush({
      token: 'jwt.from.google',
      user: { id: '22222222-2222-2222-2222-222222222222', name: 'Ana', email: 'ana@example.com' },
    });

    expect(service.token()).toBe('jwt.from.google');
    expect(service.user()?.email).toBe('ana@example.com');
  });

  it('reads google-enabled as a boolean', () => {
    let enabled: boolean | undefined;
    service.googleEnabled().subscribe((value) => (enabled = value));
    http.expectOne(apiUrl('/auth/google-enabled')).flush({ enabled: true });
    expect(enabled).toBe(true);
  });

  it('clears the session on logout', () => {
    service.setToken('keep');
    service.logout();
    expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.user()).toBeNull();
  });
});
