import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-question-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">Question Library</h1>
        <p class="page-subtitle">Reusable questions that can be assigned to any program round</p>
      </div>
      <div class="page-actions">
        <a routerLink="/admin/questions/new" class="btn btn-primary">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          New Question
        </a>
      </div>
    </div>

    <div class="card" style="margin-bottom:var(--spacing-5);">
      <div class="card-body" style="padding:var(--spacing-4) var(--spacing-5);">
        <div style="display:flex;align-items:center;gap:var(--spacing-4);flex-wrap:wrap;">
          <div style="flex:1;min-width:200px;">
            <input type="text" class="form-control" placeholder="Search questions&#8230;"
                   [(ngModel)]="searchTerm" (ngModelChange)="onSearchChange()"
                   style="margin:0;"/>
          </div>
          <div>
            <select class="form-control" [(ngModel)]="filterType" (ngModelChange)="onFilterChange()"
                    style="margin:0;width:auto;">
              <option value="">All Types</option>
              <option *ngFor="let t of questionTypes" [value]="t.value">{{ t.label }}</option>
            </select>
          </div>
        </div>
      </div>
    </div>

    <div *ngIf="loading" class="loading-overlay">
      <div class="spinner spinner-lg"></div><span>Loading questions&#8230;</span>
    </div>

    <div *ngIf="!loading && questions.length === 0" class="card">
      <div class="empty-state">
        <div class="empty-state-icon">&#10067;</div>
        <div class="empty-state-title">No questions found</div>
        <div class="empty-state-desc">
          {{ searchTerm || filterType ? 'Try adjusting your filters.' : 'Create your first question to add to the library.' }}
        </div>
        <a *ngIf="!searchTerm && !filterType" routerLink="/admin/questions/new" class="btn btn-primary">Create Question</a>
      </div>
    </div>

    <div *ngIf="!loading && questions.length > 0" class="table-container">
      <table>
        <thead>
          <tr>
            <th>Label</th>
            <th>Type</th>
            <th>Target</th>
            <th>Required</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let q of questions">
            <td style="max-width:340px;">
              <div style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-weight:500;">{{ q.label }}</div>
            </td>
            <td>
              <span [class]="typeBadge(q.questionType)" style="font-size:0.6875rem;">{{ q.questionType }}</span>
            </td>
            <td class="td-muted" style="font-family:var(--font-mono);font-size:0.75rem;">
              {{ q.targetTable }}.{{ q.targetColumn }}
            </td>
            <td>
              <span *ngIf="q.required" class="badge badge-danger">Required</span>
              <span *ngIf="!q.required" class="text-muted text-xs">Optional</span>
            </td>
            <td>
              <span [class]="q.active ? 'badge badge-success' : 'badge badge-neutral'">
                <span class="badge-dot"></span>{{ q.active ? 'Active' : 'Inactive' }}
              </span>
            </td>
            <td>
              <div class="table-actions">
                <a [routerLink]="['/admin/questions', q.id, 'edit']" class="btn btn-secondary btn-sm">Edit</a>
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
          <button class="btn btn-secondary btn-sm" [disabled]="currentPage === 0" (click)="prevPage()">Previous</button>
          <button class="btn btn-secondary btn-sm" [disabled]="currentPage >= totalPages - 1" (click)="nextPage()">Next</button>
        </div>
      </div>
    </div>
  `
})
export class QuestionListComponent implements OnInit {
  questions:   any[] = [];
  loading      = true;
  searchTerm   = '';
  filterType   = '';
  currentPage  = 0;
  totalPages   = 1;

  questionTypes = [
    { value: 'TEXT',         label: 'Text' },
    { value: 'TEXT_AREA',    label: 'Text Area' },
    { value: 'DATE',         label: 'Date' },
    { value: 'DECIMAL',      label: 'Decimal' },
    { value: 'WHOLE_NUMBER', label: 'Whole Number' },
    { value: 'CURRENCY',     label: 'Currency' },
    { value: 'PHONE',        label: 'Phone' },
    { value: 'ZIP_CODE',     label: 'ZIP Code' },
    { value: 'SELECT_ONE',   label: 'Select One' },
    { value: 'SELECT_MULTI', label: 'Select Multi' },
    { value: 'RADIO_YES_NO', label: 'Radio Yes/No' },
    { value: 'CHECKBOX',     label: 'Checkbox' },
    { value: 'ATTACHMENT',   label: 'Attachment' },
    { value: 'LABEL',        label: 'Label' },
  ];

  private searchTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(private api: ApiService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.api.listQuestions(this.currentPage, 20, this.filterType || undefined, this.searchTerm || undefined).subscribe({
      next: res => {
        this.questions  = res.content ?? res;
        this.totalPages = res.totalPages ?? 1;
        this.loading    = false;
      },
      error: () => this.loading = false
    });
  }

  onSearchChange(): void {
    if (this.searchTimeout) clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => { this.currentPage = 0; this.load(); }, 350);
  }

  onFilterChange(): void { this.currentPage = 0; this.load(); }
  prevPage(): void { if (this.currentPage > 0) { this.currentPage--; this.load(); } }
  nextPage(): void { if (this.currentPage < this.totalPages - 1) { this.currentPage++; this.load(); } }

  typeBadge(type: string): string {
    const map: Record<string, string> = {
      TEXT: 'badge badge-neutral',      TEXT_AREA: 'badge badge-neutral',
      DATE: 'badge badge-info',         SELECT_ONE: 'badge badge-primary',
      SELECT_MULTI: 'badge badge-primary', RADIO_YES_NO: 'badge badge-primary',
      CHECKBOX: 'badge badge-primary',  ATTACHMENT: 'badge badge-warning',
      LABEL: 'badge badge-neutral',     CURRENCY: 'badge badge-success',
      DECIMAL: 'badge badge-neutral',   WHOLE_NUMBER: 'badge badge-neutral',
      PHONE: 'badge badge-neutral',     ZIP_CODE: 'badge badge-neutral',
    };
    return map[type] ?? 'badge badge-neutral';
  }
}
