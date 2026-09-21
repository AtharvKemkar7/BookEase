import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const PUBLIC_AUTH = ['/api/v1/auth/login', '/api/v1/auth/register'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const headers: Record<string, string> = {
    'X-Correlation-Id': crypto.randomUUID(),
  };
  if (token && req.url.startsWith('/api') && !PUBLIC_AUTH.some((path) => req.url.startsWith(path))) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return next(req.clone({ setHeaders: headers })).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && auth.isAuthenticated() && !req.url.includes('/auth/login')) {
        auth.logout();
      }
      return throwError(() => error);
    })
  );
};
