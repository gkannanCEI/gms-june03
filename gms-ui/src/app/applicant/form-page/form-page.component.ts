import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { QuestionHostDirective } from '../../shared/question-components/question-host.directive';
import { PageRenderDTO, QuestionRenderDTO } from '../../core/models';

interface Section {
  title: string | null;
  questions: QuestionRenderDTO[];
}

@Component({
  selector: 'app-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, QuestionHostDirective],
  template: `
    <div *ngIf="loading" class="loading-overlay">
      <div class="spinner spinner-lg"></div><span>Loading form&#8230;</span>
    </div>

    <ng-container *ngIf="!loading && pageData">

      <div style="margin-bottom:var(--spacing-6);">
        <a [routerLink]="backLink" class="btn btn-ghost btn-sm"
           style="margin-bottom:var(--spacing-4);padding:0.25rem 0.5rem;">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
          Back to Dashboard
        </a>
        <h1 class="page-title">{{ pageData.pageName }}</h1>
        <p *ngIf="pageData.pageDescription" class="page-subtitle">{{ pageData.pageDescription }}</p>
      </div>

      <div *ngIf="saveError" class="alert alert-danger" style="margin-bottom:var(--spacing-6);" role="alert">
        <span class="alert-icon">&#9888;</span>
        <div>
          <div class="alert-title">Could not save answers</div>
          <ul *ngIf="fieldErrors.length"
              style="margin:var(--spacing-2) 0 0 var(--spacing-4);">
            <li *ngFor="let e of fieldErrors" style="font-size:var(--font-size-sm);">{{ e.message }}</li>
          </ul>
          <span *ngIf="!fieldErrors.length">An unexpected error occurred. Please try again.</span>
        </div>
      </div>

      <div *ngIf="saveSuccess" class="alert alert-success" style="margin-bottom:var(--spacing-6);" role="status">
        <span class="alert-icon">&#10003;</span>
        <div><div class="alert-title">Answers saved successfully</div></div>
      </div>

      <form [formGroup]="formGroup" (ngSubmit)="onSubmit()" novalidate>

        <div *ngIf="ungroupedQuestions.length > 0" class="card" style="margin-bottom:var(--spacing-6);">
          <div class="card-body">
            <div class="question-grid">
              <div *ngFor="let q of ungroupedQuestions"
                   [class]="'col-span-' + q.displayConfig.columnSpan">

                <div appQuestionHost
                  [question]="q"
                  [control]="getControl(q.questionId)"
                  [appId]="appId"
                  [formGroup]="formGroup">
                </div>
              </div>
            </div>
          </div>
        </div>

        <div *ngFor="let section of groupedSections" class="section-group">
          <div *ngIf="section.title" class="section-group-header">
            <div class="section-group-title">{{ section.title }}</div>
          </div>
          <div class="section-group-body">
            <div class="question-grid">
              <div *ngFor="let q of section.questions"
                   [class]="'col-span-' + q.displayConfig.columnSpan">

                <div appQuestionHost
                  [question]="q"
                  [control]="getControl(q.questionId)"
                  [appId]="appId"
                  [formGroup]="formGroup">
                </div>
              </div>
            </div>
          </div>
        </div>

        <div style="display:flex;align-items:center;justify-content:space-between;
                    padding:var(--spacing-5) var(--spacing-6);
                    background:var(--color-bg-card);
                    border:1px solid var(--color-border);
                    border-radius:var(--radius-lg);
                    margin-top:var(--spacing-6);">
          <a [routerLink]="backLink" class="btn btn-secondary">Back</a>
          <button type="submit" class="btn btn-primary" [disabled]="submitting">
            <span *ngIf="submitting" class="spinner spinner-sm"></span>
            {{ submitting ? 'Saving&#8230;' : 'Save &amp; Continue' }}
          </button>
        </div>

      </form>

    </ng-container>

    <div *ngIf="!loading && !pageData" class="card">
      <div class="empty-state">
        <div class="empty-state-icon">&#9888;</div>
        <div class="empty-state-title">Page not found</div>
        <div class="empty-state-desc">This form page could not be loaded.</div>
        <a [routerLink]="backLink" class="btn btn-secondary">Back to Dashboard</a>
      </div>
    </div>
  `
})
export class FormPageComponent implements OnInit {
  pageData:   PageRenderDTO | null = null;
  formGroup:  FormGroup = new FormGroup({});
  loading     = true;
  submitting  = false;
  saveError   = false;
  saveSuccess = false;
  fieldErrors: { questionId: number; message: string }[] = [];

  programId!: number;
  roundId!:   number;
  pageId!:    number;
  appId       = 0;

  constructor(
    private api:    ApiService,
    private route:  ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.programId = +this.route.snapshot.params['programId'];
    this.roundId   = +this.route.snapshot.params['roundId'];
    this.pageId    = +this.route.snapshot.params['pageId'];
    this.loadApplication();
  }

  get backLink(): string[] {
    return ['/apply/programs', String(this.programId), 'rounds', String(this.roundId), 'dashboard'];
  }

  loadApplication(): void {
    this.api.getMyApplication(this.programId, this.roundId).subscribe({
      next: app => { this.appId = app.id; this.loadPage(); },
      error: err => {
        if (err.status === 404) {
          this.api.createApplication(this.programId, this.roundId).subscribe({
            next: app => { this.appId = app.id; this.loadPage(); },
            error: () => this.loading = false
          });
        } else { this.loading = false; }
      }
    });
  }

  loadPage(): void {
    this.api.getRenderedPage(this.programId, this.roundId, this.pageId).subscribe({
      next: data => {
        this.pageData = data;
        this.buildForm(data);
        this.loadSavedAnswers(data);
      },
      error: () => this.loading = false
    });
  }

  buildForm(data: PageRenderDTO): void {
    const controls: Record<string, FormControl> = {};
    for (const q of data.questions) {
      if (q.displayConfig.readonly || q.questionType === 'LABEL') continue;
      const validators = q.required ? [Validators.required] : [];
      controls['q_' + q.questionId] = new FormControl(
        q.questionType === 'CHECKBOX' ? false : '', validators
      );
      for (const child of (q.childQuestions ?? [])) {
        controls['q_' + child.question.questionId] = new FormControl('');
      }
    }
    this.formGroup = new FormGroup(controls);
  }

  /**
   * Loads previously saved answers for all questions on this page and patches
   * the FormGroup so the applicant sees their prior entries when revisiting.
   *
   * Collects all top-level and child question IDs, calls GET /api/applications/{appId}/pages/{pageId}/answers,
   * then patches each control with the returned value.
   * CHECKBOX values are converted back to boolean (stored as 'true'/'false' strings).
   */
  loadSavedAnswers(data: PageRenderDTO): void {
    // Collect every question ID visible on this page (top-level + children)
    const questionIds: number[] = [];
    for (const q of data.questions) {
      if (q.displayConfig.readonly || q.questionType === 'LABEL') continue;
      questionIds.push(q.questionId);
      for (const child of (q.childQuestions ?? [])) {
        questionIds.push(child.question.questionId);
      }
    }

    if (questionIds.length === 0) {
      this.loading = false;
      return;
    }

    this.api.loadAnswers(this.appId, this.pageId, questionIds).subscribe({
      next: answers => {
        const patch: Record<string, unknown> = {};
        for (const answer of answers) {
          const key = 'q_' + answer.questionId;
          if (this.formGroup.contains(key)) {
            // CHECKBOX values are stored as 'true'/'false' strings — restore as boolean
            const q = this.findQuestion(answer.questionId, data);
            if (q?.questionType === 'CHECKBOX') {
              patch[key] = answer.value === 'true';
            } else {
              patch[key] = answer.value;
            }
          }
        }
        this.formGroup.patchValue(patch);
        this.loading = false;
      },
      error: () => {
        // Non-fatal: form still works, just shows blank values
        this.loading = false;
      }
    });
  }

  /**
   * Finds a QuestionRenderDTO by ID across top-level and child questions.
   */
  private findQuestion(questionId: number, data: PageRenderDTO): { questionType: string } | null {
    for (const q of data.questions) {
      if (q.questionId === questionId) return q;
      for (const child of (q.childQuestions ?? [])) {
        if (child.question.questionId === questionId) return child.question;
      }
    }
    return null;
  }

  get ungroupedQuestions(): QuestionRenderDTO[] {
    return (this.pageData?.questions ?? []).filter(q => !q.displayConfig?.sectionGroup);
  }

  get groupedSections(): Section[] {
    const questions = (this.pageData?.questions ?? []).filter(q => q.displayConfig?.sectionGroup);
    const sectionMap = new Map<string, QuestionRenderDTO[]>();
    for (const q of questions) {
      const key = q.displayConfig!.sectionGroup!;
      if (!sectionMap.has(key)) sectionMap.set(key, []);
      sectionMap.get(key)!.push(q);
    }
    return Array.from(sectionMap.entries()).map(([title, qs]) => ({ title, questions: qs }));
  }

  getControl(questionId: number): FormControl {
    const key   = 'q_' + questionId;
    const ctrl  = this.formGroup.get(key);
    if (!ctrl) {
      const fallback = new FormControl('');
      this.formGroup.addControl(key, fallback);
      return fallback as FormControl;
    }
    return ctrl as FormControl;
  }

  onSubmit(): void {
    if (this.submitting || !this.appId) return;
    this.submitting  = true;
    this.saveError   = false;
    this.saveSuccess = false;
    this.fieldErrors = [];

    const answers: { questionId: number; value: string }[] = [];

    for (const q of (this.pageData?.questions ?? [])) {
      if (q.displayConfig?.readonly || q.questionType === 'LABEL') continue;
      const val = this.getControl(q.questionId).value;
      answers.push({
        questionId: q.questionId,
        value: val === null || val === undefined ? '' : String(val)
      });
      for (const child of (q.childQuestions ?? [])) {
        const childVal = this.getControl(child.question.questionId).value;
        answers.push({
          questionId: child.question.questionId,
          value: childVal === null || childVal === undefined ? '' : String(childVal)
        });
      }
    }

    this.api.submitAnswers(this.appId, this.pageId, answers).subscribe({
      next: result => {
        this.submitting = false;
        if (result.success) {
          this.saveSuccess = true;
          setTimeout(() => this.router.navigate(this.backLink), 1200);
        } else {
          this.saveError   = true;
          this.fieldErrors = result.errors ?? [];
        }
      },
      error: () => { this.submitting = false; this.saveError = true; }
    });
  }
}
