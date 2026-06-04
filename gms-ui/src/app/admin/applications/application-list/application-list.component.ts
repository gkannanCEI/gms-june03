import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-application-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">Applications</h1>
        <p class="page-subtitle">Review submitted applications for this round</p>
      </div>
    </div>

    <div class="card" style="margin-bottom:var(--spacing-5);">
      <div class="card-body" style="padding:var(--spacing-4) var(--spacing-5);">
        <div style="display:flex;align-items:center;gap:var(--spacing-4);flex-wrap:wrap;">
          <select class="form-control" [(ngModel)]="statusFilter" (ngModelChange)="load()"
                  style="margin:0;width:auto;">
            <option value="">All Statuses</option>
            <option value="DRAFT">Draft</option>
            <option value="SUBMITTED">Submitted</option>
            <option value="WITHDRAWN">Withdrawn</option>
          </select>
          <label class="form-check" style="margin:0;">
            <input type="checkbox" [(ngModel)]="eligibilityFilter" (ngModelChange)="load()"/>
            <span class="form-check-label">&#9888; Eligibility warnings only</span>
          </label>
        </div>
      </div>
    </div>

    <div *ngIf="loading" class="loading-overlay">
      <div class="spinner spinner-lg"></div>
    </div>

    <div *ngIf="!loading && applications.length === 0" class="card">
      <div class="empty-state">
        <div class="empty-state-icon">&#128203;</div>
        <div class="empty-state-title">No applications found</div>
        <div class="empty-state-desc">No applications match the current filters.</div>
      </div>
    </div>

    <div *ngIf="!loading && applications.length > 0" class="table-container">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>User</th>
            <th>Status</th>
            <th>Eligibility</th>
            <th>Created</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let a of applications"
              [style.background]="a.eligibilityWarning ? 'rgba(255,152,0,0.05)' : ''">
            <td class="td-muted" style="font-family:var(--font-mono);font-size:0.75rem;">#{{ a.id }}</td>
            <td>{{ a.userId }}</td>
            <td>
              <span [class]="statusBadge(a.status)">
                <span class="badge-dot"></span>{{ a.status }}
              </span>
            </td>
            <td>
              <span *ngIf="a.eligibilityWarning" class="badge badge-warning">&#9888; Ineligible</span>
            </td>
            <td class="td-muted">{{ a.createdAt | date:'MMM d, y' }}</td>
            <td>
              <div class="table-actions">
                <button *ngIf="a.status === 'SUBMITTED'" class="btn btn-secondary btn-sm" (click)="reopen(a.id)">Reopen</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `
})
export class ApplicationListComponent implements OnInit {
  applications:    any[] = [];
  loading          = true;
  statusFilter     = '';
  eligibilityFilter = false;
  private programId!: number;
  private roundId!:   number;
  private base = environment.apiBaseUrl;

  constructor(private route: ActivatedRoute, private http: HttpClient) {}

  ngOnInit(): void {
    this.programId = +this.route.snapshot.params['id'];
    this.roundId   = +this.route.snapshot.params['roundId'];
    this.load();
  }

  load(): void {
    this.loading = true;
    let url = `${this.base}/api/admin/programs/${this.programId}/rounds/${this.roundId}/applications?page=0&size=50`;
    if (this.statusFilter)      url += `&status=${this.statusFilter}`;
    if (this.eligibilityFilter) url += `&eligibilityWarning=true`;
    this.http.get<any>(url).subscribe({
      next: res => { this.applications = res.content ?? res; this.loading = false; },
      error: () => this.loading = false
    });
  }

  reopen(appId: number): void {
    this.http.put(`${this.base}/api/admin/applications/${appId}/reopen`, {}).subscribe(() => this.load());
  }

  statusBadge(s: string): string {
    const m: Record<string, string> = {
      DRAFT:      'badge badge-neutral',
      SUBMITTED:  'badge badge-success',
      WITHDRAWN:  'badge badge-warning'
    };
    return m[s] ?? 'badge badge-neutral';
  }
}
