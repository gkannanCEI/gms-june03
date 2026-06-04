import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { Question } from '../../../core/models';

@Component({
  selector: 'app-question-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">{{ isEdit ? 'Edit Question' : 'New Question' }}</h1>
        <p class="page-subtitle">{{ isEdit ? 'Update question definition' : 'Create a new reusable question' }}</p>
      </div>
      <div class="page-actions">
        <a routerLink="/admin/questions" class="btn btn-secondary">Cancel</a>
      </div>
    </div>

    <div style="max-width:760px;">
      <div class="card">
        <div class="card-header"><div class="card-title">Question Details</div></div>
        <div class="card-body">

          <div *ngIf="error" class="alert alert-danger" style="margin-bottom:var(--spacing-4);">
            <span class="alert-icon">&#9888;</span><div>{{ error }}</div>
          </div>
          <div *ngIf="success" class="alert alert-success" style="margin-bottom:var(--spacing-4);">
            <span class="alert-icon">&#10003;</span><div>Question saved! Redirecting&#8230;</div>
          </div>

          <form (ngSubmit)="onSubmit()" novalidate>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label" for="qType">
                  Type <span class="form-label-required">*</span>
                </label>
                <select id="qType" class="form-control" [(ngModel)]="question.questionType" name="questionType">
                  <option value="">Select type&#8230;</option>
                  <option *ngFor="let t of questionTypes" [value]="t">{{ t }}</option>
                </select>
              </div>
              <div class="form-group" style="flex:2;">
                <label class="form-label" for="qLabel">
                  Label <span class="form-label-required">*</span>
                </label>
                <input id="qLabel" type="text" class="form-control"
                       [(ngModel)]="question.label" name="label"
                       maxlength="255" placeholder="Question label shown to applicants"/>
              </div>
            </div>

            <div *ngIf="question.questionType && question.questionType !== 'LABEL'">
              <div class="form-row">
                <div class="form-group">
                  <label class="form-label">Target Table</label>
                  <input type="text" class="form-control"
                         [(ngModel)]="question.targetTable" name="targetTable"
                         placeholder="e.g. gms_application_data"/>
                </div>
                <div class="form-group">
                  <label class="form-label">Target Column</label>
                  <input type="text" class="form-control"
                         [(ngModel)]="question.targetColumn" name="targetColumn"
                         placeholder="e.g. value_text"/>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label class="form-check">
                <input type="checkbox" [(ngModel)]="question.required" name="required"/>
                <span class="form-check-label">Required field</span>
              </label>
            </div>

            <div *ngIf="question.questionType === 'TEXT' || question.questionType === 'TEXT_AREA'"
                 class="form-group">
              <label class="form-label">Validation Regex</label>
              <input type="text" class="form-control"
                     [(ngModel)]="question.validationRegex" name="validationRegex"
                     placeholder="Optional regex pattern"/>
              <div class="form-hint">Leave empty for no regex validation</div>
            </div>

            <div *ngIf="question.questionType === 'ATTACHMENT'">
              <div class="form-row">
                <div class="form-group">
                  <label class="form-label">Allowed File Types</label>
                  <input type="text" class="form-control"
                         [(ngModel)]="question.allowedFileTypes" name="allowedFileTypes"
                         placeholder="pdf,docx,png"/>
                </div>
                <div class="form-group">
                  <label class="form-label">Max File Size (MB)</label>
                  <input type="number" class="form-control"
                         [(ngModel)]="question.maxFileSizeMb" name="maxFileSizeMb" min="1" max="100"/>
                </div>
              </div>
            </div>

            <div *ngIf="question.questionType === 'SELECT_ONE' || question.questionType === 'SELECT_MULTI'">
              <div style="display:flex;align-items:center;justify-content:space-between;
                          margin-bottom:var(--spacing-3);">
                <label class="form-label" style="margin:0;">Options</label>
                <button type="button" class="btn btn-ghost btn-sm" (click)="addOption()">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
                  </svg>
                  Add Option
                </button>
              </div>
              <div *ngFor="let opt of options; let i = index"
                   style="display:flex;gap:var(--spacing-3);align-items:center;margin-bottom:var(--spacing-2);">
                <span class="text-xs text-muted" style="width:20px;text-align:right;flex-shrink:0;font-weight:700;">{{ i + 1 }}</span>
                <input type="text" class="form-control" style="flex:2;margin:0;"
                       [(ngModel)]="opt.optionLabel" placeholder="Label"
                       [name]="'optLabel_' + i"/>
                <input type="text" class="form-control" style="flex:1;margin:0;font-family:var(--font-mono);font-size:0.8rem;"
                       [(ngModel)]="opt.optionValue" placeholder="Value"
                       [name]="'optValue_' + i"/>
                <button type="button" class="btn btn-ghost btn-sm btn-icon" (click)="removeOption(i)"
                        style="color:var(--color-danger);flex-shrink:0;">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
              </div>
            </div>

            <div *ngIf="question.questionType === 'RADIO_YES_NO'"
                 class="alert alert-info" style="margin-top:var(--spacing-3);">
              <span class="alert-icon">&#9432;</span>
              <div>Yes / No options are managed automatically by the system.</div>
            </div>

            <div style="display:flex;gap:var(--spacing-3);justify-content:flex-end;margin-top:var(--spacing-5);">
              <a routerLink="/admin/questions" class="btn btn-secondary">Cancel</a>
              <button type="submit" class="btn btn-primary" [disabled]="saving">
                <span *ngIf="saving" class="spinner spinner-sm"></span>
                {{ saving ? 'Saving&#8230;' : (isEdit ? 'Save Changes' : 'Create Question') }}
              </button>
            </div>

          </form>
        </div>
      </div>
    </div>
  `
})
export class QuestionFormComponent implements OnInit {
  question: Partial<Question> = {};
  options:  any[] = [];
  isEdit    = false;
  saving    = false;
  error     = '';
  success   = false;
  private questionId?: number;

  questionTypes = [
    'TEXT','TEXT_AREA','DATE','DECIMAL','WHOLE_NUMBER','CURRENCY',
    'PHONE','ZIP_CODE','ATTACHMENT','SELECT_ONE','SELECT_MULTI',
    'CHECKBOX','LABEL','RADIO_YES_NO'
  ];

  constructor(
    private route:  ActivatedRoute,
    private router: Router,
    private api:    ApiService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.params['id'];
    if (id) {
      this.isEdit     = true;
      this.questionId = +id;
      this.api.getQuestion(this.questionId).subscribe(q => {
        this.question = q;
        this.options  = q.options ?? [];
      });
    }
  }

  addOption(): void {
    this.options.push({ optionLabel: '', optionValue: '', displayOrder: this.options.length + 1, isOtherOption: false });
  }

  removeOption(index: number): void { this.options.splice(index, 1); }

  onSubmit(): void {
    if (!this.question.questionType || !this.question.label?.trim()) {
      this.error = 'Type and label are required';
      return;
    }
    this.saving = true; this.error = ''; this.success = false;
    const payload: any = { ...this.question };
    if (this.question.questionType === 'SELECT_ONE' || this.question.questionType === 'SELECT_MULTI') {
      payload.options = this.options;
    }
    const obs = this.isEdit
      ? this.api.updateQuestion(this.questionId!, payload)
      : this.api.createQuestion(payload);
    obs.subscribe({
      next: () => {
        this.saving  = false;
        this.success = true;
        setTimeout(() => this.router.navigate(['/admin/questions']), 1000);
      },
      error: (err: any) => {
        this.saving = false;
        this.error  = err.error?.message ?? 'Failed to save question';
      }
    });
  }
}
