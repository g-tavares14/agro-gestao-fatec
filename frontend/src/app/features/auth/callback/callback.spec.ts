import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { apiUrl } from '../../../core/api/api';
import { AuthService } from '../../../core/auth/auth.service';
import { clearToken } from '../../../core/auth/token-storage';
import { Callback } from './callback';

describe('Callback', () => {
  let navigateByUrl: ReturnType<typeof vi.fn>;

  async function create(query: Record<string, string>): Promise<{
    fixture: ComponentFixture<Callback>;
    auth: AuthService;
    http: HttpTestingController;
  }> {
    navigateByUrl = vi.fn().mockResolvedValue(true);
    await TestBed.configureTestingModule({
      imports: [Callback],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(query) } },
        },
        { provide: Router, useValue: { navigateByUrl } },
      ],
    }).compileComponents();

    const auth = TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(Callback);
    fixture.detectChanges();
    return { fixture, auth, http };
  }

  afterEach(() => {
    clearToken();
    sessionStorage.clear();
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('exchanges the query code and navigates to /app', async () => {
    const { auth, fixture, http } = await create({ code: 'one-time' });

    const req = http.expectOne(apiUrl('/auth/oauth/exchange'));
    expect(req.request.body).toEqual({ code: 'one-time' });
    req.flush({
      token: 'jwt.from.google',
      user: { id: '33333333-3333-3333-3333-333333333333', name: 'Ana', email: 'ana@example.com' },
    });

    expect(auth.token()).toBe('jwt.from.google');
    expect(navigateByUrl).toHaveBeenCalledWith('/app', { replaceUrl: true });
    expect(fixture.componentInstance.error()).toBe(false);
  });

  it('ignores a JWT on the query string', async () => {
    const { auth, fixture, http } = await create({ token: 'jwt.from.google', access_token: 'alias.jwt' });

    http.expectNone(apiUrl('/auth/oauth/exchange'));
    expect(auth.isAuthenticated()).toBe(false);
    expect(navigateByUrl).not.toHaveBeenCalled();
    expect(fixture.componentInstance.error()).toBe(true);
  });

  it('shows an error when Google reports oauth_failed', async () => {
    const { fixture, http } = await create({ error: 'oauth_failed' });
    http.expectNone(apiUrl('/auth/oauth/exchange'));
    expect(navigateByUrl).not.toHaveBeenCalled();
    expect(fixture.componentInstance.error()).toBe(true);
    expect(fixture.componentInstance.errorDetail()).toContain('Google');
  });

  it('shows an error when the code exchange fails', async () => {
    const { fixture, http } = await create({ code: 'expired' });
    http.expectOne(apiUrl('/auth/oauth/exchange')).flush(
      { message: 'Código OAuth inválido ou expirado' },
      { status: 401, statusText: 'Unauthorized' },
    );
    expect(navigateByUrl).not.toHaveBeenCalled();
    expect(fixture.componentInstance.error()).toBe(true);
  });
});
