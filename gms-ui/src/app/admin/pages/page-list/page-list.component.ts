import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-page-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">Page Library</h1>
        <p class="page-subtitle">Reusable form pages that can be assigned to program rounds</p>
      </div>
      <div class="page-actions">
        <a routerLink="/admin/pages/new" class="btn btn-primary">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          New Page
        </a>
      </div>
    </div>

    <div *ngIf="loading" class="loading-overlay">
      <div class="spinner spinner-lg"></div><span>Loading pages&#8230;</span>
    </div>

    <div *ngIf="!loading && pages.length === 0" class="card">
      <div class="empty-state">
        <div class="empty-state-icon">&#128196;</div>
        <div class="empty-state-title">No pages yet</div>
        <div class="empty-state-desc">Create reusable form pages to assign to your program rounds.</div>
        <a routerLink="/admin/pages/new" class="btn btn-primary">Create Page</a>
      </div>
    </div>

    <div *ngIf="!loading && pages.length > 0" class="table-container">
      <table>
        <thead>
          <tr>
            <th>Page Name</th>
            <th>Path</th>
            <th>Description</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let p of pages">
            <td style="font-weight:500;">{{ p.pageName }}</td>
            <td class="td-muted">{{ p.path || '&#8212;' }}</td>
            <td class="td-muted" style="max-width:400px;">
              <span style="display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
                {{ p.pageDescription || '&#8212;' }}
              </span>
            </td>
            <td>
              <span [class]="p.active ? 'badge badge-success' : 'badge badge-neutral'">
                <span class="badge-dot"></span>{{ p.active ? 'Active' : 'Inactive' }}
              </span>
            </td>
            <td>
              <div class="table-actions">
                <a [routerLink]="['/admin/pages', p.id, 'edit']" class="btn btn-secondary btn-sm">Edit</a>
                <button class="btn btn-danger btn-sm" (click)="deactivate(p)" [disabled]="!p.active">Deactivate</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `
})
export class PageListComponent implements OnInit {
  pages:   any[] = [];
  loading  = true;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.listPages().subscribe({
      next: res => { this.pages = res.content ?? res; this.loading = false; },
      error: () => this.loading = false
    });
  }

  deactivate(page: any): void {
    if (!confirm(`Deactivate "${page.pageName}"? This will remove it from future rounds.`)) return;
    this.api.updatePage(page.id, { active: false }).subscribe({
      next: () => page.active = false,
      error: (err: any) => alert(err?.error?.message ?? 'Could not deactivate page.')
    });
  }
}
