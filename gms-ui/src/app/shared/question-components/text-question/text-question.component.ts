import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { QuestionRenderDTO } from '../../../core/models';

@Component({
  selector: 'app-text-question',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="question-field">
      <label [for]="fieldId" [class]="labelClass">
        {{ question.label }}
        <span *ngIf="question.required" class="question-required-mark" aria-label="required">*</span>
      </label>
      <input
        [id]="fieldId"
        [type]="inputType"
        class="form-control"
        [class.error]="control.invalid && control.touched"
        [formControl]="control"
        [placeholder]="placeholder"
        [attr.aria-describedby]="question.displayConfig.helpText ? 'help-' + question.questionId : null"
        [attr.inputmode]="inputMode"
        autocomplete="off"
      />
      <div *ngIf="question.displayConfig.helpText"
           [id]="'help-' + question.questionId"
           class="question-help">
        {{ question.displayConfig!.helpText }}
      </div>
      <div *ngIf="control.invalid && control.touched" class="question-error" role="alert">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        {{ errorMessage }}
      </div>
    </div>
  `
})
export class TextQuestionComponent {
  @Input() question!: QuestionRenderDTO;
  @Input() control!:  FormControl;

  get fieldId():   string { return 'q_' + this.question.questionId; }
  get labelClass(): string {
    const pos = this.question.displayConfig.labelPosition;
    return pos === 'HIDDEN' ? 'question-label sr-only' : 'question-label';
  }

  get inputType(): string {
    switch (this.question.questionType) {
      case 'PHONE':    return 'tel';
      case 'ZIP_CODE': return 'text';
      default:         return 'text';
    }
  }

  get inputMode(): string {
    if (this.question.questionType === 'PHONE') return 'tel';
    return 'text';
  }

  get placeholder(): string {
    switch (this.question.questionType) {
      case 'PHONE':    return '(555) 000-0000';
      case 'ZIP_CODE': return '00000';
      default:         return '';
    }
  }

  get errorMessage(): string {
    if (this.control.errors?.['required']) return 'This field is required';
    return 'Please enter a valid value';
  }
}
