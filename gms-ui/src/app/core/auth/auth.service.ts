import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface AuthUser {
  username: string;
  roles: string[];
}

/**
 * GAP-11: Replaced the fragile role-detection heuristic (which mapped HTTP 500
 * responses to ADMIN role) with a proper /api/me endpoint call.
 *
 * Login flow:
 *  1. POST credentials via HTTP Basic to /api/me.
 *  2. 200 response contains { userId, roles } — use roles directly.
 *  3. 401 → bad credentials → return false.
 *  4. Any other error → return false (no silent privilege escalation).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private currentUserSubject = new BehaviorSubject<AuthUser | null>(null);
  currentUser$ = this.currentUserSubject.asObservable();

  private credentials: { username: string; password: string } | null = null;

  constructor(private http: HttpClient) {
    // Restore from session storage on page reload
    const stored = sessionStorage.getItem('gms_auth');
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        this.credentials = parsed.credentials;
        this.currentUserSubject.next(parsed.user);
      } catch {
        sessionStorage.removeItem('gms_auth');
      }
    }
  }

  get isAuthenticated(): boolean {
    return this.currentUserSubject.value !== null;
  }

  get currentUser(): AuthUser | null {
    return this.currentUserSubject.value;
  }

  get authHeader(): string | null {
    if (!this.credentials) return null;
    return 'Basic ' + btoa(`${this.credentials.username}:${this.credentials.password}`);
  }

  /**
   * Authenticates by calling GET /api/me with the supplied credentials as HTTP Basic.
   * The server returns { userId, roles } for any authenticated user regardless of role,
   * so we can correctly set ADMIN vs APPLICANT without relying on error-code heuristics.
   */
  login(username: string, password: string): Observable<boolean> {
    const headers = { Authorization: 'Basic ' + btoa(`${username}:${password}`) };
    return this.http.get<{ userId: string; roles: string[] }>(
      `${environment.apiBaseUrl}/api/me`,
      { headers }
    ).pipe(
      map((profile) => {
        // Use actual roles from the server
        const roles = profile.roles && profile.roles.length > 0
          ? profile.roles
          : ['APPLICANT']; // Default to APPLICANT if roles array is empty
        this.setCredentials(username, password, roles);
        return true;
      }),
      catchError((err) => {
        // Only 401 means bad credentials; any other code is a genuine server error
        if (err.status === 401) {
          return of(false);
        }
        // For unexpected errors, do NOT grant access
        return of(false);
      })
    );
  }

  private setCredentials(username: string, password: string, roles: string[]) {
    this.credentials = { username, password };
    const user: AuthUser = { username, roles };
    this.currentUserSubject.next(user);
    sessionStorage.setItem('gms_auth', JSON.stringify({ credentials: this.credentials, user }));
  }

  logout(): void {
    this.credentials = null;
    this.currentUserSubject.next(null);
    sessionStorage.removeItem('gms_auth');
  }
}
