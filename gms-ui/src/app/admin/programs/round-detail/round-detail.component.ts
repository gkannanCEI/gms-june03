import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Page } from '../../../core/models';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-round-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div *ngIf="loading" class="loading-overlay">
      <div class="spinner spinner-lg"></div><span>Loading round&#8230;</span>
    </div>

    <ng-container *ngIf="!loading && round">

      <div class="page-header">
        <div class="page-header-left">
          <div style="display:flex;align-items:center;gap:var(--spacing-3);margin-bottom:var(--spacing-2);">
            <a [routerLink]="['/admin/programs', programId]" class="btn btn-ghost btn-sm"
               style="padding:0.25rem 0.5rem;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="15 18 9 12 15 6"/>
              </svg>
              Program
            </a>
          </div>
          <h1 class="page-title">{{ round.roundName }}</h1>
          <div style="display:flex;align-items:center;gap:var(--spacing-3);margin-top:var(--spacing-2);">
            <span [class]="statusBadge(round.status)">
              <span class="badge-dot"></span>{{ round.status }}
            </span>
            <span class="text-sm text-muted">{{ round.startDate }} &#8212; {{ round.endDate }}</span>
          </div>
        </div>
        <div class="page-actions">
          <button class="btn btn-primary" (click)="showAssignPage = !showAssignPage">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            Assign Page
          </button>
        </div>
      </div>

      <!-- Assign Page Form -->
      <div *ngIf="showAssignPage" class="card" style="margin-bottom:var(--spacing-6);border-color:var(--color-primary-light);">
        <div class="card-header">
          <div class="card-title">Assign Page to Round</div>
          <button class="btn btn-ghost btn-sm btn-icon" (click)="showAssignPage = false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="card-body">
          <div class="form-row">
            <div class="form-group" style="flex:2;">
              <label class="form-label">Page <span class="form-label-required">*</span></label>
              <select class="form-control" [(ngModel)]="selectedPageId">
                <option value="">Select a page&#8230;</option>
                <option *ngFor="let p of availablePages" [value]="p.id">{{ p.pageName }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Display Order</label>
              <input type="number" class="form-control" [(ngModel)]="pageDisplayOrder" min="1"/>
            </div>
          </div>
          <div *ngIf="assignError" class="alert alert-danger" style="margin-bottom:var(--spacing-3);">
            <span class="alert-icon">&#9888;</span><div>{{ assignError }}</div>
          </div>
          <div style="display:flex;gap:var(--spacing-3);justify-content:flex-end;">
            <button class="btn btn-secondary" (click)="showAssignPage = false">Cancel</button>
            <button class="btn btn-primary" (click)="assignPage()" [disabled]="assigning">
              <span *ngIf="assigning" class="spinner spinner-sm"></span>
              {{ assigning ? 'Assigning&#8230;' : 'Assign Page' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Pages Table -->
      <div class="card">
        <div class="card-header">
          <div>
            <div class="card-title">Assigned Pages</div>
            <div class="card-subtitle">{{ roundPages.length }} page{{ roundPages.length !== 1 ? 's' : '' }} in this round</div>
          </div>
        </div>

        <div *ngIf="roundPages.length === 0" class="empty-state">
          <div class="empty-state-icon">&#128196;</div>
          <div class="empty-state-title">No pages assigned</div>
          <div class="empty-state-desc">Assign pages to this round to build the application form.</div>
          <button class="btn btn-primary" (click)="showAssignPage = true">Assign First Page</button>
        </div>

        <div *ngIf="roundPages.length > 0" class="table-container" style="border:none;border-radius:0;">
          <table>
            <thead>
              <tr>
                <th>Order</th>
                <th>Page Name</th>
                <th>Path</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let rp of roundPages">
                <td class="td-muted" style="width:60px;font-weight:600;">{{ rp.displayOrder }}</td>
                <td style="font-weight:500;">{{ rp.page?.pageName || rp.pageName || ('Page ' + (rp.page?.id || rp.pageId)) }}</td>
                <td class="td-muted">{{ rp.page?.path || '&#8212;' }}</td>
                <td>
                  <div class="table-actions">
                    <a [routerLink]="['/admin/programs', programId, 'rounds', roundId, 'pages', rp.page?.id || rp.pageId]"
                       class="btn btn-secondary btn-sm">Configure</a>
                    <button class="btn btn-danger btn-sm" (click)="removePage(rp)">Remove</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

    </ng-container>
  `
})
export class RoundDetailComponent implements OnInit {
  round:          any;
  roundPages:     any[] = [];
  availablePages: Page[] = [];
  loading          = true;
  showAssignPage   = false;
  assigning        = false;
  selectedPageId   = '';
  pageDisplayOrder = 1;
  assignError      = '';

  programId!: number;
  roundId!:   number;
  private base = environment.apiBaseUrl;

  constructor(
    private route:  ActivatedRoute,
    private http:   HttpClient,
    private api:    ApiService
  ) {}

  ngOnInit(): void {
    this.programId = +this.route.snapshot.params['id'];
    this.roundId   = +this.route.snapshot.params['roundId'];
    this.loadRound();
    this.loadRoundPages();
    this.loadAvailablePages();
  }

  loadRound(): void {
    this.http.get<any[]>(`${this.base}/api/admin/programs/${this.programId}/rounds`)
      .subscribe({
        next: rounds => {
          this.round   = rounds.find((r: any) => r.id === this.roundId);
          this.loading = false;
        },
        error: () => this.loading = false
      });
  }

  loadRoundPages(): void {
    this.http.get<any[]>(
      `${this.base}/api/admin/programs/${this.programId}/rounds/${this.roundId}/pages`
    ).subscribe({
      next: pages => {
        this.roundPages = pages;
        this.loadAvailablePages();
      },
      error: () => {}
    });
  }

  loadAvailablePages(): void {
    this.api.listPages(0, 100).subscribe((res: any) => {
      const all = res.content ?? res;
      const assigned = new Set(this.roundPages.map((rp: any) => rp.page?.id ?? rp.pageId));
      this.availablePages = all.filter((p: any) => !assigned.has(p.id));
    });
  }

  assignPage(): void {
    this.assignError = '';
    if (!this.selectedPageId) { this.assignError = 'Please select a page'; return; }
    this.assigning = true;
    this.http.post(
      `${this.base}/api/admin/programs/${this.programId}/rounds/${this.roundId}/pages`,
      { pageId: +this.selectedPageId, displayOrder: this.pageDisplayOrder }
    ).subscribe({
      next: () => {
        this.assigning      = false;
        this.showAssignPage = false;
        this.selectedPageId = '';
        this.loadRoundPages();
      },
      error: (e: any) => {
        this.assigning  = false;
        this.assignError = e.error?.message ?? 'Failed to assign page';
      }
    });
  }

  removePage(rp: any): void {
    const pageId = rp.page?.id ?? rp.pageId;
    if (!confirm('Remove this page from the round?')) return;
    this.http.delete(
      `${this.base}/api/admin/programs/${this.programId}/rounds/${this.roundId}/pages/${pageId}`
    ).subscribe(() => this.loadRoundPages());
  }

  statusBadge(s: string): string {
    const m: Record<string, string> = {
      ACTIVE:'badge badge-success', DRAFT:'badge badge-neutral',
      CLOSED:'badge badge-info',    ARCHIVED:'badge badge-warning'
    };
    return m[s] ?? 'badge badge-neutral';
  }
}
