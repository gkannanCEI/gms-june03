import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { QuestionRenderDTO } from '../../../core/models';

@Component({
  selector: 'app-select-one-question',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="question-field">
      <label [for]="'q_' + question.questionId" class="question-label">
        {{ question.label }}
        <span *ngIf="question.required" class="question-required-mark">*</span>
      </label>
      <select
        [id]="'q_' + question.questionId"
        class="form-control"
        [class.error]="control.invalid && control.touched"
        [formControl]="control"
        style="max-width:400px;"
      >
        <option value="">— Select an option —</option>
        <option *ngFor="let opt of question.options" [value]="opt.optionValue">
          {{ opt.optionLabel }}
        </option>
      </select>
      <div *ngIf="question.displayConfig.helpText" class="question-help">
        {{ question.displayConfig!.helpText }}
      </div>
      <div *ngIf="control.invalid && control.touched" class="question-error" role="alert">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        Please select an option
      </div>
    </div>
  `
})
export class SelectOneQuestionComponent {
  @Input() question!: QuestionRenderDTO;
  @Input() control!:  FormControl;
}
