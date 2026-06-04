import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-program-list',
  standalone: true,
  imports: [CommonModule, RouterLink, DecimalPipe],
  template: `
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">Programs</h1>
        <p class="page-subtitle">Manage grant programs and their application rounds</p>
      </div>
      <div class="page-actions">
        <a routerLink="/admin/programs/new" class="btn btn-primary">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          New Program
        </a>
      </div>
    </div>

    <div *ngIf="loading" class="loading-overlay">
      <div class="spinner spinner-lg"></div>
      <span>Loading programs&#8230;</span>
    </div>

    <div *ngIf="!loading && programs.length === 0" class="card">
      <div class="empty-state">
        <div class="empty-state-icon">&#128203;</div>
        <div class="empty-state-title">No programs yet</div>
        <div class="empty-state-desc">Create your first grant program to get started managing applications and rounds.</div>
        <a routerLink="/admin/programs/new" class="btn btn-primary">Create Program</a>
      </div>
    </div>

    <div *ngIf="!loading && programs.length > 0" class="table-container">
      <table>
        <thead>
          <tr>
            <th>Program</th>
            <th>Status</th>
            <th>Budget</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let p of programs">
            <td>
              <div style="font-weight:600;color:var(--color-text-primary);">{{ p.programName }}</div>
              <div *ngIf="p.description" class="text-xs text-muted"
                   style="margin-top:2px;max-width:320px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
                {{ p.description }}
              </div>
            </td>
            <td>
              <span [class]="statusBadge(p.status)">
                <span class="badge-dot"></span>{{ p.status }}
              </span>
            </td>
            <td class="td-muted">
              {{ p.totalBudget ? ('$' + (p.totalBudget | number)) : '&#8212;' }}
            </td>
            <td>
              <div class="table-actions">
                <a [routerLink]="['/admin/programs', p.id]" class="btn btn-secondary btn-sm">View</a>
                <a [routerLink]="['/admin/programs', p.id, 'edit']" class="btn btn-ghost btn-sm">Edit</a>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div *ngIf="totalPages > 1"
           style="display:flex;align-items:center;justify-content:space-between;
                  padding:var(--spacing-4) var(--spacing-5);
                  border-top:1px solid var(--color-border);
                  background:var(--color-neutral-50);">
        <span class="text-sm text-muted">Page {{ currentPage + 1 }} of {{ totalPages }}</span>
        <div style="display:flex;gap:var(--spacing-2);">
          <button class="btn btn-secondary btn-sm" [disabled]="currentPage === 0"
                  (click)="prevPage()">Previous</button>
          <button class="btn btn-secondary btn-sm" [disabled]="currentPage >= totalPages - 1"
                  (click)="nextPage()">Next</button>
        </div>
      </div>
    </div>
  `
})
export class ProgramListComponent implements OnInit {
  programs:    any[] = [];
  loading      = true;
  currentPage  = 0;
  totalPages   = 1;

  constructor(private api: ApiService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.api.listPrograms(this.currentPage).subscribe({
      next: res => {
        this.programs   = res.content ?? res;
        this.totalPages = res.totalPages ?? 1;
        this.loading    = false;
      },
      error: () => this.loading = false
    });
  }

  prevPage(): void { if (this.currentPage > 0) { this.currentPage--; this.load(); } }
  nextPage(): void { if (this.currentPage < this.totalPages - 1) { this.currentPage++; this.load(); } }

  statusBadge(status: string): string {
    const map: Record<string, string> = {
      ACTIVE:   'badge badge-success',
      DRAFT:    'badge badge-neutral',
      ARCHIVED: 'badge badge-warning',
    };
    return map[status] ?? 'badge badge-neutral';
  }
}
