import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-applicant-home',
  standalone: true,
  imports: [CommonModule, RouterLink, DecimalPipe],
  template: `
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">Welcome, {{ userName }}</h1>
        <p class="page-subtitle">Find and apply for available grant programs below</p>
      </div>
    </div>

    <!-- Loading -->
    <div *ngIf="loading" class="loading-overlay">
      <div class="spinner spinner-lg"></div>
      <span>Loading programs&hellip;</span>
    </div>

    <!-- Empty -->
    <div *ngIf="!loading && programs.length === 0" class="card">
      <div class="empty-state">
        <div class="empty-state-icon">&#128269;</div>
        <div class="empty-state-title">No open programs</div>
        <div class="empty-state-desc">
          There are no active grant programs available at this time. Check back later.
        </div>
      </div>
    </div>

    <!-- Program cards -->
    <div *ngIf="!loading && programs.length > 0"
         style="display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));
                gap:var(--spacing-5);">
      <div *ngFor="let p of programs" class="card"
           style="display:flex;flex-direction:column;">

        <div class="card-body" style="flex:1;">

          <!-- Icon + badge row -->
          <div style="display:flex;align-items:flex-start;
                      justify-content:space-between;
                      gap:var(--spacing-3);
                      margin-bottom:var(--spacing-4);">
            <div style="width:44px;height:44px;border-radius:var(--radius-md);
                        background:var(--color-primary-light);
                        display:flex;align-items:center;justify-content:center;
                        font-size:1.25rem;flex-shrink:0;">&#127963;</div>
            <span class="badge badge-success">
              <span class="badge-dot"></span>Open
            </span>
          </div>

          <!-- Title -->
          <h3 style="font-size:1.0625rem;font-weight:600;
                     margin-bottom:var(--spacing-2);">
            {{ p.programName }}
          </h3>

          <!-- Description -->
          <p style="font-size:var(--font-size-sm);color:var(--color-text-muted);
                    margin:0 0 var(--spacing-3);
                    display:-webkit-box;-webkit-line-clamp:3;
                    -webkit-box-orient:vertical;overflow:hidden;">
            {{ p.description || 'No description available.' }}
          </p>

          <!-- Goal -->
          <p *ngIf="p.goal"
             style="font-size:var(--font-size-sm);color:var(--color-text-secondary);
                    margin:0 0 var(--spacing-3);">
            <strong>Goal:</strong> {{ p.goal }}
          </p>

          <!-- Budget -->
          <div *ngIf="p.totalBudget"
               style="display:flex;align-items:center;gap:var(--spacing-2);">
            <span style="font-size:var(--font-size-sm);color:var(--color-text-muted);">
              Total Budget: <strong style="color:var(--color-text-primary);">
                &#36;{{ p.totalBudget | number:'1.0-0' }}
              </strong>
            </span>
          </div>

          <!-- Rounds list -->
          <div *ngIf="p.rounds?.length"
               style="margin-top:var(--spacing-4);
                      border-top:1px solid var(--color-border);
                      padding-top:var(--spacing-3);">
            <p style="font-size:var(--font-size-xs);font-weight:600;
                      text-transform:uppercase;letter-spacing:.05em;
                      color:var(--color-text-muted);margin:0 0 var(--spacing-2);">
              Open Rounds
            </p>
            <div *ngFor="let r of p.rounds"
                 style="display:flex;align-items:center;justify-content:space-between;
                        gap:var(--spacing-3);padding:var(--spacing-2) 0;
                        border-bottom:1px solid var(--color-border-light);">
              <div>
                <span style="font-size:var(--font-size-sm);font-weight:500;">
                  {{ r.roundName }}
                </span>
                <span *ngIf="r.endDate"
                      style="font-size:var(--font-size-xs);
                             color:var(--color-text-muted);
                             display:block;">
                  Closes {{ r.endDate | date:'mediumDate' }}
                </span>
              </div>
              <a [routerLink]="['/apply/programs', p.id, 'rounds', r.id, 'dashboard']"
                 class="btn btn-primary btn-sm"
                 style="white-space:nowrap;">
                Apply Now
              </a>
            </div>
          </div>

        </div><!-- /card-body -->
      </div><!-- /card -->
    </div><!-- /grid -->
  `
})
export class ApplicantHomeComponent implements OnInit {
  programs: any[] = [];
  loading = true;

  constructor(
    private api: ApiService,
    public authService: AuthService
  ) {}

  get userName(): string {
    return this.authService.currentUser?.username ?? 'there';
  }

  ngOnInit(): void {
    this.api.listActivePrograms().subscribe({
      next:  p  => { this.programs = p ?? []; this.loading = false; },
      error: () => { this.programs = [];      this.loading = false; }
    });
  }
}
