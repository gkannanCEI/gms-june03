import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';

/**
 * HTTP interceptor that adds Basic auth headers when running in local auth mode.
 * Uses credentials from AuthService (set after login).
 * Redirects to login on 401 responses.
 */
@Injectable()
export class BasicAuthInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService, private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (environment.authMode !== 'local') {
      return next.handle(req);
    }

    // Only add auth header for API requests
    if (!req.url.includes('/api/')) {
      return next.handle(req);
    }

    const authHeader = this.authService.authHeader;
    let authReq = req;
    if (authHeader && !req.headers.has('Authorization')) {
      authReq = req.clone({
        setHeaders: { Authorization: authHeader }
      });
    }

    return next.handle(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          this.authService.logout();
          this.router.navigate(['/login']);
        }
        return throwError(() => error);
      })
    );
  }
}
