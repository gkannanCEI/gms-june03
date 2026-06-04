import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { QuestionRenderDTO } from '../../../core/models';

@Component({
  selector: 'app-date-question',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="question-field">
      <label [for]="'q_' + question.questionId" class="question-label">
        {{ question.label }}
        <span *ngIf="question.required" class="question-required-mark">*</span>
      </label>
      <input
        type="date"
        [id]="'q_' + question.questionId"
        class="form-control"
        [class.error]="control.invalid && control.touched"
        [formControl]="control"
        [min]="question.minValue || ''"
        [max]="question.maxValue || ''"
        style="max-width:220px;"
      />
      <div *ngIf="question.minValue || question.maxValue" class="question-help">
        <ng-container *ngIf="question.minValue && question.maxValue">
          Date must be between {{ question.minValue }} and {{ question.maxValue }}
        </ng-container>
        <ng-container *ngIf="question.minValue && !question.maxValue">
          Date must be on or after {{ question.minValue }}
        </ng-container>
        <ng-container *ngIf="!question.minValue && question.maxValue">
          Date must be on or before {{ question.maxValue }}
        </ng-container>
      </div>
      <div *ngIf="control.invalid && control.touched" class="question-error" role="alert">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        Please enter a valid date
      </div>
    </div>
  `
})
export class DateQuestionComponent {
  @Input() question!: QuestionRenderDTO;
  @Input() control!:  FormControl;
}
