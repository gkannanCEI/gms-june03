import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-round-page-config',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div *ngIf="loading" class="loading-overlay">
      <div class="spinner spinner-lg"></div><span>Loading configuration&#8230;</span>
    </div>

    <ng-container *ngIf="!loading">

      <div class="page-header">
        <div class="page-header-left">
          <div style="display:flex;align-items:center;gap:var(--spacing-3);margin-bottom:var(--spacing-2);">
            <a [routerLink]="['/admin/programs', programId, 'rounds', roundId]" class="btn btn-ghost btn-sm"
               style="padding:0.25rem 0.5rem;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="15 18 9 12 15 6"/>
              </svg>
              Round
            </a>
          </div>
          <h1 class="page-title">Configure: {{ pageName }}</h1>
          <p class="page-subtitle">Manage questions and display settings for this page within the round</p>
        </div>
        <div class="page-actions">
          <button class="btn btn-secondary" (click)="loadPreview()">&#128064; Preview</button>
          <button class="btn btn-primary" (click)="showAssign = !showAssign">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            Assign Question
          </button>
        </div>
      </div>

      <!-- Assign Question -->
      <div *ngIf="showAssign" class="card" style="margin-bottom:var(--spacing-5);border-color:var(--color-primary-light);">
        <div class="card-header">
          <div class="card-title">Assign Question</div>
          <button class="btn btn-ghost btn-sm btn-icon" (click)="showAssign = false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="card-body">
          <div class="form-group">
            <label class="form-label">Select Question</label>
            <select class="form-control" [(ngModel)]="selectedQuestionId">
              <option value="">Choose a question&#8230;</option>
              <option *ngFor="let q of availableQuestions" [value]="q.id">
                {{ q.label }} ({{ q.questionType }})
              </option>
            </select>
          </div>
          <div *ngIf="assignError" class="alert alert-danger" style="margin-bottom:var(--spacing-3);">
            <span class="alert-icon">&#9888;</span><div>{{ assignError }}</div>
          </div>
          <div style="display:flex;gap:var(--spacing-3);justify-content:flex-end;">
            <button class="btn btn-secondary" (click)="showAssign = false">Cancel</button>
            <button class="btn btn-primary" (click)="assignQuestion()">
              Assign
            </button>
          </div>
        </div>
      </div>

      <!-- Preview -->
      <div *ngIf="preview" class="card" style="margin-bottom:var(--spacing-5);border:2px dashed var(--color-primary-light);">
        <div class="card-header">
          <div class="card-title">&#128064; Live Preview: {{ preview.pageName }}</div>
          <button class="btn btn-ghost btn-sm" (click)="preview = null">Close</button>
        </div>
        <div class="card-body">
          <div *ngFor="let q of preview.questions" style="padding:var(--spacing-3) 0;border-bottom:1px solid var(--color-border);">
            <div style="font-weight:500;">{{ q.label }}
              <span *ngIf="q.required" style="color:var(--color-danger);">&nbsp;*</span>
              <span class="badge badge-neutral" style="font-size:0.625rem;margin-left:var(--spacing-2);">{{ q.questionType }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Questions -->
      <div *ngIf="questions.length === 0" class="card">
        <div class="empty-state">
          <div class="empty-state-icon">&#10067;</div>
          <div class="empty-state-title">No questions assigned</div>
          <div class="empty-state-desc">Assign questions to build this form page.</div>
          <button class="btn btn-primary" (click)="showAssign = true">Assign Question</button>
        </div>
      </div>

      <div *ngFor="let rpq of questions; let i = index" class="card" style="margin-bottom:var(--spacing-4);">
        <div class="card-header">
          <div style="display:flex;align-items:center;gap:var(--spacing-3);">
            <span style="width:28px;height:28px;border-radius:50%;background:var(--color-primary-light);
                         color:var(--color-primary);display:flex;align-items:center;justify-content:center;
                         font-size:0.75rem;font-weight:700;flex-shrink:0;">
              {{ rpq.displayOrder }}
            </span>
            <div>
              <div style="font-weight:600;">{{ getLabel(rpq) }}</div>
              <div style="display:flex;gap:var(--spacing-2);margin-top:2px;">
                <span class="badge badge-neutral" style="font-size:0.625rem;">{{ rpq.question?.questionType }}</span>
                <span *ngIf="rpq.excluded" class="badge badge-danger" style="font-size:0.625rem;">Excluded</span>
                <span *ngIf="rpq.readonly" class="badge badge-warning" style="font-size:0.625rem;">Readonly</span>
                <span *ngIf="rpq.labelOverride" class="badge badge-info" style="font-size:0.625rem;">Override</span>
              </div>
            </div>
          </div>
          <div style="display:flex;gap:var(--spacing-2);">
            <button class="btn btn-secondary btn-sm" (click)="rpq._editing = !rpq._editing">
              {{ rpq._editing ? 'Close' : 'Configure' }}
            </button>
            <button class="btn btn-danger btn-sm" (click)="removeQuestion(rpq)">Remove</button>
          </div>
        </div>

        <div *ngIf="rpq._editing" class="card-body" style="border-top:1px solid var(--color-border);">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Display Order</label>
              <input type="number" class="form-control" [(ngModel)]="rpq.displayOrder" min="1"/>
            </div>
            <div class="form-group">
              <label class="form-label">Column Span (1&#8211;12)</label>
              <input type="number" class="form-control" [(ngModel)]="rpq.columnSpan" min="1" max="12"/>
            </div>
            <div class="form-group">
              <label class="form-label">Label Position</label>
              <select class="form-control" [(ngModel)]="rpq.labelPosition">
                <option>ABOVE</option><option>LEFT</option><option>HIDDEN</option>
              </select>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Label Override</label>
              <input type="text" class="form-control" [(ngModel)]="rpq.labelOverride" maxlength="255"/>
            </div>
            <div class="form-group">
              <label class="form-label">Section Group</label>
              <input type="text" class="form-control" [(ngModel)]="rpq.sectionGroup" maxlength="100"/>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">Help Text</label>
            <input type="text" class="form-control" [(ngModel)]="rpq.helpText" maxlength="500"/>
          </div>
          <div style="display:flex;gap:var(--spacing-6);margin-bottom:var(--spacing-4);">
            <label class="form-check">
              <input type="checkbox" [(ngModel)]="rpq.excluded"/>
              <span class="form-check-label">Excluded from this round</span>
            </label>
            <label class="form-check">
              <input type="checkbox" [(ngModel)]="rpq.readonly"/>
              <span class="form-check-label">Readonly</span>
            </label>
          </div>
          <div style="display:flex;justify-content:flex-end;">
            <button class="btn btn-primary btn-sm" (click)="saveConfig(rpq)">
              <span *ngIf="rpq._saving" class="spinner spinner-sm"></span>
              {{ rpq._saving ? 'Saving&#8230;' : 'Save Configuration' }}
            </button>
          </div>
          <div *ngIf="rpq._saveSuccess" class="alert alert-success" style="margin-top:var(--spacing-3);">
            <span class="alert-icon">&#10003;</span>
            <div>Configuration saved</div>
          </div>
        </div>
      </div>

    </ng-container>
  `
})
export class RoundPageConfigComponent implements OnInit {
  questions:          any[] = [];
  availableQuestions: any[] = [];
  pageName          = '';
  preview:          any = null;
  loading           = true;
  showAssign        = false;
  selectedQuestionId = '';
  assignError       = '';

  programId!: number;
  roundId!:   number;
  pageId!:    number;
  private base = environment.apiBaseUrl;

  constructor(
    private route: ActivatedRoute,
    private http:  HttpClient,
    private api:   ApiService
  ) {}

  ngOnInit(): void {
    this.programId = +this.route.snapshot.params['id'];
    this.roundId   = +this.route.snapshot.params['roundId'];
    this.pageId    = +this.route.snapshot.params['pageId'];
    this.loadPage();
    this.loadQuestions();
    this.loadAvailableQuestions();
  }

  loadPage(): void {
    this.http.get<any[]>(
      `${this.base}/api/admin/programs/${this.programId}/rounds/${this.roundId}/pages`
    ).subscribe({
      next: pages => {
        const rp = pages.find((p: any) => (p.page?.id ?? p.pageId) === this.pageId);
        if (rp) this.pageName = rp.page?.pageName ?? '';
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  loadQuestions(): void {
    this.http.get<any[]>(
      `${this.base}/api/admin/programs/${this.programId}/rounds/${this.roundId}/pages/${this.pageId}/questions`
    ).subscribe({
      next: qs => {
        this.questions = (qs ?? []).map((q: any) => ({
          ...q, _editing: false, _saving: false, _saveSuccess: false
        }));
      },
      error: () => {}
    });
  }

  loadAvailableQuestions(): void {
    this.api.listQuestions(0, 200).subscribe((res: any) => {
      this.availableQuestions = res.content ?? res;
    });
  }

  assignQuestion(): void {
    this.assignError = '';
    if (!this.selectedQuestionId) { this.assignError = 'Please select a question'; return; }
    this.http.post(
      `${this.base}/api/admin/programs/${this.programId}/rounds/${this.roundId}/pages/${this.pageId}/questions`,
      { question: { id: +this.selectedQuestionId }, displayOrder: this.questions.length + 1 }
    ).subscribe({
      next: () => {
        this.showAssign          = false;
        this.selectedQuestionId  = '';
        this.loadQuestions();
      },
      error: (e: any) => this.assignError = e.error?.message ?? 'Failed to assign question'
    });
  }

  removeQuestion(rpq: any): void {
    const qId = rpq.question?.id ?? rpq.questionId;
    if (!confirm('Remove this question from the page?')) return;
    this.http.delete(
      `${this.base}/api/admin/programs/${this.programId}/rounds/${this.roundId}/pages/${this.pageId}/questions/${qId}`
    ).subscribe(() => this.loadQuestions());
  }

  saveConfig(rpq: any): void {
    const qId   = rpq.question?.id ?? rpq.questionId;
    rpq._saving = true;
    const body  = {
      displayOrder:     rpq.displayOrder,
      labelOverride:    rpq.labelOverride  || null,
      requiredOverride: rpq.requiredOverride ?? null,
      excluded:         rpq.excluded  || false,
      readonly:         rpq.readonly  || false,
      columnSpan:       rpq.columnSpan || 12,
      labelPosition:    rpq.labelPosition || 'ABOVE',
      helpText:         rpq.helpText   || null,
      sectionGroup:     rpq.sectionGroup || null
    };
    this.http.put(
      `${this.base}/api/admin/programs/${this.programId}/rounds/${this.roundId}/pages/${this.pageId}/questions/${qId}`,
      body
    ).subscribe({
      next: () => {
        rpq._saving      = false;
        rpq._saveSuccess = true;
        setTimeout(() => rpq._saveSuccess = false, 2500);
      },
      error: () => rpq._saving = false
    });
  }

  loadPreview(): void {
    this.http.get<any>(
      `${this.base}/api/admin/programs/${this.programId}/rounds/${this.roundId}/pages/${this.pageId}/preview`
    ).subscribe(p => this.preview = p);
  }

  getLabel(rpq: any): string {
    return rpq.labelOverride?.trim() || rpq.question?.label || '';
  }
}
