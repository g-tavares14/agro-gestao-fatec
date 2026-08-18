import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const PUBLIC_API_PREFIXES = [
  '/api/auth/google-enabled',
  '/api/auth/login',
  '/api/auth/register',
  '/api/auth/oauth/exchange',
];

function isPublicApi(url: string): boolean {
  return PUBLIC_API_PREFIXES.some((prefix) => url.startsWith(prefix));
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const isApi = req.url.startsWith('/api');

  const authorized =
    token && isApi
      ? req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`,
          },
        })
      : req;

  return next(authorized).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && isApi && !isPublicApi(req.url)) {
        auth.logout('/login');
      }
      return throwError(() => error);
    }),
  );
};
