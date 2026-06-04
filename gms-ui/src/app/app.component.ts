import { Component, OnInit } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/auth/auth.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  template: `
    <ng-container *ngIf="!isLoginPage; else loginOutlet">
      <div class="page-shell">

        <!-- Sidebar -->
        <aside class="sidebar" [class.open]="sidebarOpen" role="navigation" aria-label="Main navigation">

          <div class="sidebar-logo">
            <div class="sidebar-logo-icon">G</div>
            <div class="sidebar-logo-text">
              <span class="sidebar-logo-title">GMS</span>
              <span class="sidebar-logo-subtitle">Grant Management</span>
            </div>
          </div>

          <nav class="sidebar-nav">

            <!-- Admin Navigation -->
            <ng-container *ngIf="isAdmin">
              <div class="sidebar-section-label">Administration</div>

              <a class="sidebar-link" routerLink="/admin/programs"
                 routerLinkActive="active" (click)="closeSidebar()">
                <svg class="sidebar-link-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="3" width="7" height="7" rx="1"/>
                  <rect x="14" y="3" width="7" height="7" rx="1"/>
                  <rect x="3" y="14" width="7" height="7" rx="1"/>
                  <rect x="14" y="14" width="7" height="7" rx="1"/>
                </svg>
                Programs
              </a>

              <a class="sidebar-link" routerLink="/admin/questions"
                 routerLinkActive="active" (click)="closeSidebar()">
                <svg class="sidebar-link-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
                  <line x1="12" y1="17" x2="12.01" y2="17"/>
                </svg>
                Question Library
              </a>

              <a class="sidebar-link" routerLink="/admin/pages"
                 routerLinkActive="active" (click)="closeSidebar()">
                <svg class="sidebar-link-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                </svg>
                Page Library
              </a>

              <a class="sidebar-link" routerLink="/admin/organizations"
                 routerLinkActive="active" (click)="closeSidebar()">
                <svg class="sidebar-link-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
                Organizations
              </a>
            </ng-container>

            <!-- Applicant Navigation -->
            <ng-container *ngIf="!isAdmin">
              <div class="sidebar-section-label">My Applications</div>

              <a class="sidebar-link" routerLink="/apply"
                 routerLinkActive="active"
                 [routerLinkActiveOptions]="{exact: true}"
                 (click)="closeSidebar()">
                <svg class="sidebar-link-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                  <polyline points="9 22 9 12 15 12 15 22"/>
                </svg>
                Browse Programs
              </a>
            </ng-container>

          </nav>

          <!-- User Footer -->
          <div class="sidebar-footer">
            <div class="sidebar-user">
              <div class="sidebar-user-avatar">{{ userInitial }}</div>
              <div class="sidebar-user-info">
                <div class="sidebar-user-name">{{ userName }}</div>
                <div class="sidebar-user-role">{{ userRoleLabel }}</div>
              </div>
            </div>
            <button class="sidebar-link" (click)="logout()" style="margin-top:4px;width:100%;text-align:left;">
              <svg class="sidebar-link-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
              Sign Out
            </button>
          </div>

        </aside>

        <!-- Main Content -->
        <div class="main-content">

          <header class="topbar">
            <button class="btn btn-ghost btn-icon"
                    style="display:none"
                    aria-label="Toggle navigation"
                    (click)="toggleSidebar()">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="3" y1="12" x2="21" y2="12"/>
                <line x1="3" y1="6" x2="21" y2="6"/>
                <line x1="3" y1="18" x2="21" y2="18"/>
              </svg>
            </button>

            <div class="topbar-breadcrumb">
              <span>{{ breadcrumbRoot }}</span>
              <ng-container *ngIf="breadcrumbCurrent">
                <svg class="topbar-breadcrumb-sep" width="14" height="14" viewBox="0 0 24 24"
                     fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 18 15 12 9 6"/>
                </svg>
                <span class="topbar-breadcrumb-current">{{ breadcrumbCurrent }}</span>
              </ng-container>
            </div>
          </header>

          <main class="page-content" id="main-content" tabindex="-1">
            <router-outlet></router-outlet>
          </main>

        </div>

        <!-- Mobile overlay -->
        <div *ngIf="sidebarOpen"
             style="position:fixed;inset:0;background:rgba(0,0,0,0.5);z-index:99"
             (click)="closeSidebar()">
        </div>

      </div>
    </ng-container>

    <ng-template #loginOutlet>
      <router-outlet></router-outlet>
    </ng-template>
  `,
  styles: [`
    @media (max-width: 1024px) {
      .topbar button[aria-label="Toggle navigation"] { display: flex !important; }
    }
  `]
})
export class AppComponent implements OnInit {
  isLoginPage       = false;
  sidebarOpen       = false;
  breadcrumbRoot    = 'GMS';
  breadcrumbCurrent = '';

  constructor(
    public  authService: AuthService,
    private router:      Router
  ) {}

  ngOnInit(): void {
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => {
        this.isLoginPage = e.urlAfterRedirects === '/login';
        this.updateBreadcrumb(e.urlAfterRedirects);
        this.closeSidebar();
      });
    this.isLoginPage = this.router.url === '/login';
  }

  get isAdmin(): boolean {
    return this.authService.currentUser?.roles?.includes('ADMIN') ?? false;
  }

  get userName(): string {
    return this.authService.currentUser?.username ?? 'User';
  }

  get userInitial(): string {
    return (this.authService.currentUser?.username ?? 'U')[0].toUpperCase();
  }

  get userRoleLabel(): string {
    const roles = this.authService.currentUser?.roles ?? [];
    if (roles.includes('ADMIN'))     return 'Administrator';
    if (roles.includes('APPLICANT')) return 'Applicant';
    return 'User';
  }

  toggleSidebar(): void { this.sidebarOpen = !this.sidebarOpen; }
  closeSidebar():  void { this.sidebarOpen = false; }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private updateBreadcrumb(url: string): void {
    const segments = url.split('/').filter(Boolean);
    if (!segments.length) { this.breadcrumbRoot = 'GMS'; this.breadcrumbCurrent = ''; return; }

    const map: Record<string, string> = {
      admin:         'Administration',
      programs:      'Programs',
      questions:     'Question Library',
      pages:         'Page Library',
      organizations: 'Organizations',
      applications:  'Applications',
      apply:         'My Applications',
      dashboard:     'Dashboard',
    };

    if (segments[0] === 'admin' && segments[1]) {
      this.breadcrumbRoot    = 'Administration';
      this.breadcrumbCurrent = map[segments[1]] ?? segments[1];
    } else if (segments[0] === 'apply') {
      this.breadcrumbRoot    = 'My Applications';
      this.breadcrumbCurrent = segments[1] ? (map[segments[1]] ?? segments[1]) : '';
    } else {
      this.breadcrumbRoot    = map[segments[0]] ?? segments[0];
      this.breadcrumbCurrent = '';
    }
  }
}
