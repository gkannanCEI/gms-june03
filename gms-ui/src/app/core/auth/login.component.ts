import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

@Component({
  selector: 'gms-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-shell">
      <div class="login-card">
        <!-- Logo / Brand -->
        <div class="login-brand">
          <div class="brand-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"
                    stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <h1>Grant Management<br>System</h1>
        </div>

        <p class="login-subtitle">Sign in to your account to continue</p>

        <form (ngSubmit)="onSubmit()" class="login-form" novalidate>
          <div class="field">
            <label for="username">Username</label>
            <input
              id="username" type="text"
              [(ngModel)]="username" name="username"
              placeholder="Enter your username"
              autocomplete="username"
              required autofocus
              [class.input-error]="error">
          </div>

          <div class="field">
            <label for="password">Password</label>
            <div class="password-field">
              <input
                id="password" [type]="showPassword ? 'text' : 'password'"
                [(ngModel)]="password" name="password"
                placeholder="Enter your password"
                autocomplete="current-password"
                required
                [class.input-error]="error">
              <button type="button" class="toggle-pw" (click)="showPassword = !showPassword"
                      [attr.aria-label]="showPassword ? 'Hide password' : 'Show password'">
                <svg *ngIf="!showPassword" width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke="currentColor" stroke-width="2"/>
                  <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
                </svg>
                <svg *ngIf="showPassword" width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                  <line x1="1" y1="1" x2="23" y2="23" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
              </button>
            </div>
          </div>

          <div *ngIf="error" class="login-error" role="alert">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
              <line x1="12" y1="8" x2="12" y2="12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <line x1="12" y1="16" x2="12.01" y2="16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            Invalid username or password. Please try again.
          </div>

          <button type="submit" class="login-btn" [disabled]="loading || !username || !password">
            <span *ngIf="loading" class="spinner" aria-hidden="true"></span>
            {{ loading ? 'Signing in...' : 'Sign In' }}
          </button>
        </form>

        <div class="dev-hint">
          <div class="dev-hint-header">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
              <path d="M12 8v4M12 16h.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            Development accounts
          </div>
          <div class="dev-accounts">
            <div class="dev-account">
              <code>admin</code><span>/</span><code>password123</code>
              <span class="dev-role admin">Administrator</span>
            </div>
            <div class="dev-account">
              <code>applicant</code><span>/</span><code>password123</code>
              <span class="dev-role applicant">Applicant</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-shell {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #0d47a1 0%, #1565c0 50%, #1976d2 100%);
      padding: 1.5rem;
    }
    .login-card {
      background: #fff;
      border-radius: 16px;
      padding: 2.5rem;
      width: 100%;
      max-width: 420px;
      box-shadow: 0 20px 60px rgba(0,0,0,0.25), 0 4px 16px rgba(0,0,0,0.15);
    }
    .login-brand {
      display: flex;
      align-items: center;
      gap: 0.875rem;
      margin-bottom: 0.5rem;
    }
    .brand-icon {
      width: 48px; height: 48px;
      background: linear-gradient(135deg, #1565c0, #1976d2);
      border-radius: 12px;
      display: flex; align-items: center; justify-content: center;
      color: #fff;
      flex-shrink: 0;
      box-shadow: 0 4px 12px rgba(21,101,192,0.4);
    }
    .login-brand h1 {
      font-size: 1.15rem;
      font-weight: 700;
      color: #1a1a2e;
      line-height: 1.3;
      margin-bottom: 0;
    }
    .login-subtitle {
      color: #5a5a72;
      font-size: 0.875rem;
      margin-bottom: 1.75rem;
      margin-top: 0.25rem;
    }
    .login-form { display: flex; flex-direction: column; gap: 0; }
    .field { margin-bottom: 1.125rem; }
    .field label { display: block; font-size: 0.8125rem; font-weight: 600; color: #424242; margin-bottom: 0.375rem; }
    .field input {
      width: 100%;
      padding: 0.6rem 0.875rem;
      border: 1.5px solid #e0e0e0;
      border-radius: 8px;
      font-size: 0.9375rem;
      color: #1a1a2e;
      background: #fafafa;
      transition: border-color 150ms ease, box-shadow 150ms ease, background 150ms ease;
      outline: none;
    }
    .field input:focus {
      border-color: #1565c0;
      background: #fff;
      box-shadow: 0 0 0 3px rgba(21,101,192,0.12);
    }
    .field input.input-error { border-color: #c62828; box-shadow: 0 0 0 3px rgba(198,40,40,0.1); }
    .password-field { position: relative; }
    .password-field input { padding-right: 2.75rem; }
    .toggle-pw {
      position: absolute; right: 0.625rem; top: 50%; transform: translateY(-50%);
      background: none; border: none; color: #9e9e9e; cursor: pointer;
      padding: 0.25rem; border-radius: 4px;
      display: flex; align-items: center; justify-content: center;
    }
    .toggle-pw:hover { color: #424242; background: none; transform: translateY(-50%); box-shadow: none; }
    .login-error {
      display: flex; align-items: center; gap: 0.5rem;
      background: #ffebee; border: 1px solid #ef9a9a;
      color: #c62828; font-size: 0.8rem; font-weight: 500;
      padding: 0.6rem 0.875rem; border-radius: 6px; margin-bottom: 1rem;
    }
    .login-btn {
      width: 100%;
      padding: 0.75rem;
      background: linear-gradient(135deg, #1565c0, #1976d2);
      color: #fff;
      border: none;
      border-radius: 8px;
      font-size: 0.9375rem;
      font-weight: 600;
      cursor: pointer;
      margin-top: 0.25rem;
      transition: opacity 150ms ease, transform 150ms ease, box-shadow 150ms ease;
      box-shadow: 0 3px 10px rgba(21,101,192,0.35);
      display: flex; align-items: center; justify-content: center; gap: 0.5rem;
    }
    .login-btn:hover:not(:disabled) { opacity: 0.92; transform: translateY(-1px); box-shadow: 0 5px 14px rgba(21,101,192,0.4); }
    .login-btn:disabled { background: #e0e0e0; color: #9e9e9e; box-shadow: none; cursor: not-allowed; transform: none; }
    .spinner {
      width: 16px; height: 16px;
      border: 2px solid rgba(255,255,255,0.3);
      border-top-color: #fff;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
      display: inline-block;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .dev-hint {
      margin-top: 1.75rem;
      padding: 0.875rem 1rem;
      background: #f5f5f5;
      border-radius: 8px;
      border: 1px solid #e0e0e0;
    }
    .dev-hint-header {
      display: flex; align-items: center; gap: 0.375rem;
      font-size: 0.75rem; font-weight: 700; color: #757575;
      text-transform: uppercase; letter-spacing: 0.06em;
      margin-bottom: 0.625rem;
    }
    .dev-accounts { display: flex; flex-direction: column; gap: 0.375rem; }
    .dev-account {
      display: flex; align-items: center; gap: 0.375rem;
      font-size: 0.8125rem; color: #616161;
    }
    .dev-account code {
      background: #eeeeee; padding: 0.1rem 0.375rem;
      border-radius: 4px; font-family: monospace; font-size: 0.8rem;
    }
    .dev-role {
      margin-left: auto;
      font-size: 0.7rem; font-weight: 600; padding: 0.1rem 0.45rem;
      border-radius: 20px;
    }
    .dev-role.admin     { background: #e3f2fd; color: #1565c0; }
    .dev-role.applicant { background: #e8f5e9; color: #2e7d32; }
  `]
})
export class LoginComponent {
  username = '';
  password = '';
  showPassword = false;
  loading = false;
  error = false;

  constructor(private authService: AuthService, private router: Router) {
    if (this.authService.isAuthenticated) {
      this.navigateByRole();
    }
  }

  onSubmit() {
    this.loading = true;
    this.error = false;
    this.authService.login(this.username, this.password).subscribe(success => {
      this.loading = false;
      if (success) {
        this.navigateByRole();
      } else {
        this.error = true;
      }
    });
  }

  private navigateByRole(): void {
    const user = this.authService.currentUser;
    if (user && user.roles.includes('ADMIN')) {
      this.router.navigate(['/admin/programs']);
    } else {
      this.router.navigate(['/apply']);
    }
  }
}
