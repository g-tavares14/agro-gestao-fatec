import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { UrlTree, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { guestGuard } from './guest.guard';
import { clearToken } from './token-storage';

describe('guestGuard', () => {
  beforeEach(() => {
    clearToken();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([])],
    });
  });

  afterEach(() => {
    clearToken();
    sessionStorage.clear();
  });

  it('allows visitors without a session', () => {
    const result = TestBed.runInInjectionContext(() => guestGuard({} as never, {} as never));
    expect(result).toBe(true);
  });

  it('redirects authenticated users to /app', () => {
    TestBed.inject(AuthService).setToken('token');
    const result = TestBed.runInInjectionContext(() => guestGuard({} as never, {} as never));
    expect(result instanceof UrlTree).toBe(true);
    expect((result as UrlTree).toString()).toContain('/app');
  });
});
