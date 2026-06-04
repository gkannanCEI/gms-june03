import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { QuestionRenderDTO } from '../../../core/models';

@Component({
  selector: 'app-number-question',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="question-field">
      <label [for]="'q_' + question.questionId" class="question-label">
        {{ question.label }}
        <span *ngIf="question.required" class="question-required-mark">*</span>
      </label>
      <div style="position:relative;max-width:240px;">
        <span *ngIf="isCurrency" style="
          position:absolute;left:0.75rem;top:50%;transform:translateY(-50%);
          color:var(--color-text-muted);font-size:var(--font-size-sm);pointer-events:none;
        ">$</span>
        <input
          type="number"
          [id]="'q_' + question.questionId"
          class="form-control"
          [class.error]="control.invalid && control.touched"
          [formControl]="control"
          [step]="step"
          [min]="question.minValue || ''"
          [max]="question.maxValue || ''"
          [placeholder]="placeholder"
          [style.paddingLeft]="isCurrency ? '1.75rem' : '0.75rem'"
          inputmode="decimal"
        />
      </div>
      <div *ngIf="question.displayConfig.helpText" class="question-help">
        {{ question.displayConfig!.helpText }}
      </div>
      <div *ngIf="control.invalid && control.touched" class="question-error" role="alert">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        Please enter a valid {{ question.questionType === 'CURRENCY' ? 'amount' : 'number' }}
      </div>
    </div>
  `
})
export class NumberQuestionComponent {
  @Input() question!: QuestionRenderDTO;
  @Input() control!:  FormControl;

  get isCurrency(): boolean { return this.question.questionType === 'CURRENCY'; }
  get step():       string  { return this.question.questionType === 'WHOLE_NUMBER' ? '1' : '0.01'; }
  get placeholder(): string {
    switch (this.question.questionType) {
      case 'CURRENCY':     return '0.00';
      case 'WHOLE_NUMBER': return '0';
      default:             return '0.00';
    }
  }
}
