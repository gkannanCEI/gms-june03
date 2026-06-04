import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { QuestionRenderDTO } from '../../../core/models';

@Component({
  selector: 'app-text-area-question',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="question-field">
      <label [for]="'q_' + question.questionId" [class]="labelClass">
        {{ question.label }}
        <span *ngIf="question.required" class="question-required-mark">*</span>
      </label>
      <textarea
        [id]="'q_' + question.questionId"
        class="form-control"
        [class.error]="control.invalid && control.touched"
        [formControl]="control"
        [rows]="5"
        [placeholder]="question.displayConfig.helpText || ''"
        [attr.aria-describedby]="question.displayConfig.helpText ? 'help-' + question.questionId : null"
      ></textarea>
      <div style="display:flex;align-items:center;justify-content:space-between;margin-top:var(--spacing-1);">
        <div *ngIf="question.displayConfig.helpText"
             [id]="'help-' + question.questionId"
             class="question-help">
          {{ question.displayConfig!.helpText }}
        </div>
        <div class="question-help" style="margin-left:auto;">
          {{ control.value?.length ?? 0 }} chars
        </div>
      </div>
      <div *ngIf="control.invalid && control.touched" class="question-error" role="alert">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        This field is required
      </div>
    </div>
  `
})
export class TextAreaQuestionComponent {
  @Input() question!: QuestionRenderDTO;
  @Input() control!:  FormControl;
  get labelClass(): string {
    return this.question.displayConfig.labelPosition === 'HIDDEN'
      ? 'question-label sr-only' : 'question-label';
  }
}
