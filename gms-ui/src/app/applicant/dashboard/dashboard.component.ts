import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, Router, NavigationEnd } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div *ngIf="loading" class="loading-overlay">
      <div class="spinner spinner-lg"></div><span>Loading your application&#8230;</span>
    </div>

    <ng-container *ngIf="!loading">

      <div *ngIf="eligibilityWarning" class="alert alert-warning" style="margin-bottom:var(--spacing-6);">
        <span class="alert-icon">&#9888;</span>
        <div>
          <div class="alert-title">Eligibility Issue Detected</div>
          Your organisation may not meet the eligibility requirements for this program.
          Please review requirements before submitting. Submission is disabled until resolved.
        </div>
      </div>

      <div class="page-header">
        <div class="page-header-left">
          <h1 class="page-title">Application Dashboard</h1>
          <p class="page-subtitle">Complete all required sections to submit your application</p>
        </div>
        <div class="page-actions">
          <button class="btn btn-primary"
                  [disabled]="eligibilityWarning || !allComplete"
                  (click)="submitApplication()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            Submit Application
          </button>
        </div>
      </div>

      <div class="stats-grid" style="margin-bottom:var(--spacing-6);">
        <div class="stat-card">
          <div class="stat-label">Total Sections</div>
          <div class="stat-value">{{ pages.length }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Completed</div>
          <div class="stat-value" style="color:var(--color-success);">{{ completedCount }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Remaining</div>
          <div class="stat-value" style="color:var(--color-warning);">{{ pages.length - completedCount }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Progress</div>
          <div class="stat-value">{{ progressPct }}%</div>
          <div style="margin-top:var(--spacing-2);height:4px;background:var(--color-neutral-200);border-radius:var(--radius-full);">
            <div [style.width]="progressPct + '%'"
                 style="height:100%;background:var(--color-primary);border-radius:var(--radius-full);transition:width 0.4s ease;"></div>
          </div>
        </div>
      </div>

      <div *ngIf="pages.length === 0" class="card">
        <div class="empty-state">
          <div class="empty-state-icon">&#128203;</div>
          <div class="empty-state-title">No sections configured</div>
          <div class="empty-state-desc">This round has no form pages set up yet. Contact the administrator.</div>
        </div>
      </div>

      <div *ngIf="pages.length > 0" class="card">
        <div class="card-header">
          <div class="card-title">Application Sections</div>
        </div>
        <div>
          <div *ngFor="let p of pages; let i = index"
               style="display:flex;align-items:center;gap:var(--spacing-4);
                      padding:var(--spacing-4) var(--spacing-6);"
               [style.borderBottom]="i < pages.length - 1 ? '1px solid var(--color-border)' : 'none'">

            <div style="width:36px;height:36px;border-radius:50%;flex-shrink:0;
                        display:flex;align-items:center;justify-content:center;
                        font-size:0.8125rem;font-weight:700;"
                 [style.background]="p.completed ? 'var(--color-success-light)' : 'var(--color-neutral-100)'"
                 [style.color]="p.completed ? 'var(--color-success)' : 'var(--color-text-muted)'"
                 [style.border]="p.completed ? '2px solid var(--color-success-border)' : '2px solid var(--color-border)'">
              <svg *ngIf="p.completed" width="14" height="14" viewBox="0 0 24 24"
                   fill="none" stroke="currentColor" stroke-width="3">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
              <span *ngIf="!p.completed">{{ i + 1 }}</span>
            </div>

            <div style="flex:1;min-width:0;">
              <div style="font-weight:600;font-size:var(--font-size-md);">{{ p.pageName }}</div>
            </div>

            <div style="display:flex;align-items:center;gap:var(--spacing-3);flex-shrink:0;">
              <span *ngIf="p.completed" class="badge badge-success">
                <span class="badge-dot"></span>Complete
              </span>
              <span *ngIf="!p.completed" class="badge badge-neutral">
                <span class="badge-dot"></span>Incomplete
              </span>
              <a [routerLink]="['/apply/programs', programId, 'rounds', roundId, 'pages', p.pageId]"
                 class="btn btn-secondary btn-sm">
                {{ p.completed ? 'Review' : 'Start' }}
              </a>
            </div>
          </div>
        </div>
      </div>

    </ng-container>
  `
})
export class DashboardComponent implements OnInit {
  pages:      any[] = [];
  loading     = true;
  programId!: number;
  roundId!:   number;
  appId!:     number;
  eligibilityWarning = false;

  constructor(
    private api: ApiService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.programId = +this.route.snapshot.params['programId'];
    this.roundId   = +this.route.snapshot.params['roundId'];
    this.loadApplication();

    // Refresh completion status every time the user navigates back to the dashboard
    // from a form page (e.g. after saving answers on a page).
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => {
        if (e.url?.includes('/dashboard') && this.appId) {
          this.loadPages();
        }
      });
  }

  loadApplication(): void {
    this.api.getMyApplication(this.programId, this.roundId).subscribe({
      next: app => {
        this.appId              = app.id;
        this.eligibilityWarning = app.eligibilityWarning;
        this.loadPages();
      },
      error: err => {
        if (err.status === 404) {
          this.api.createApplication(this.programId, this.roundId).subscribe({
            next: app => { this.appId = app.id; this.loadPages(); },
            error: () => this.loading = false
          });
        } else {
          this.loading = false;
        }
      }
    });
  }

  loadPages(): void {
    // Pass appId so the backend skips the extra getMyApplication() lookup.
    // appId may be 0/NaN briefly during first load — backend falls back safely.
    this.api.getPageList(this.programId, this.roundId, this.appId || undefined).subscribe({
      next: p  => { this.pages = p; this.loading = false; },
      error: () => this.loading = false
    });
  }

  get completedCount(): number { return this.pages.filter(p => p.completed).length; }
  get allComplete():    boolean { return this.pages.length > 0 && this.pages.every(p => p.completed); }
  get progressPct():   number {
    if (!this.pages.length) return 0;
    return Math.round((this.completedCount / this.pages.length) * 100);
  }

  submitApplication(): void {
    if (!this.appId) return;
    this.api.updateApplicationStatus(this.appId, 'SUBMITTED').subscribe({
      next: () => this.router.navigate(['/apply']),
      error: (err: any) => alert(err?.error?.message ?? 'Could not submit application.')
    });
  }
}
