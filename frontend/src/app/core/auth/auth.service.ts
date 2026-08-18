import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, map, of, retry, tap, timer } from 'rxjs';
import { apiUrl } from '../api/api';
import {
  AuthResponse,
  AuthUser,
  GoogleEnabledResponse,
  LoginPayload,
  RegisterPayload,
} from './auth.models';
import { clearToken, persistToken, readToken, TOKEN_KEY } from './token-storage';

export { TOKEN_KEY };

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly tokenSignal = signal<string | null>(readToken());
  private readonly userSignal = signal<AuthUser | null>(null);

  readonly token = this.tokenSignal.asReadonly();
  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.tokenSignal());

  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(apiUrl('/auth/login'), payload).pipe(
      tap((response) => this.persistSession(response)),
    );
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(apiUrl('/auth/register'), payload).pipe(
      tap((response) => this.persistSession(response)),
    );
  }

  exchangeOAuth(code: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(apiUrl('/auth/oauth/exchange'), { code }).pipe(
      tap((response) => this.persistSession(response)),
    );
  }

  me(): Observable<AuthUser> {
    return this.http.get<AuthUser>(apiUrl('/auth/me')).pipe(
      tap((user) => this.userSignal.set(user)),
    );
  }

  ensureSession(): Observable<boolean> {
    if (!this.tokenSignal()) {
      return of(false);
    }
    if (this.userSignal()) {
      return of(true);
    }
    return this.me().pipe(
      map(() => true),
      catchError(() => {
        this.clearSession();
        return of(false);
      }),
    );
  }

  googleEnabled(): Observable<boolean> {
    return this.http.get<GoogleEnabledResponse>(apiUrl('/auth/google-enabled')).pipe(
      retry({ count: 4, delay: (_, retryIndex) => timer(400 * (retryIndex + 1)) }),
      map((response) => response.enabled === true),
      catchError(() => of(false)),
    );
  }

  setToken(token: string): void {
    persistToken(token);
    this.tokenSignal.set(token);
  }

  logout(redirectTo: string = '/'): void {
    this.clearSession();
    void this.router.navigateByUrl(redirectTo);
  }

  private clearSession(): void {
    clearToken();
    this.tokenSignal.set(null);
    this.userSignal.set(null);
  }

  private persistSession(response: AuthResponse): void {
    this.setToken(response.token);
    if (response.user) {
      this.userSignal.set(response.user);
    }
  }
}
