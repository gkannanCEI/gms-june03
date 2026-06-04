import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { QuestionRenderDTO } from '../../../core/models';

@Component({
  selector: 'app-checkbox-question',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="question-field">
      <label class="form-check" style="align-items:flex-start;">
        <input
          type="checkbox"
          [formControl]="boolControl"
          style="margin-top:3px;"
          (change)="onValueChange()"
        />
        <span class="form-check-label">
          {{ question.label }}
          <span *ngIf="question.required" class="question-required-mark">*</span>
        </span>
      </label>
      <div *ngIf="question.displayConfig.helpText" class="question-help" style="margin-left:28px;">
        {{ question.displayConfig!.helpText }}
      </div>
    </div>
  `
})
export class CheckboxQuestionComponent {
  @Input() question!: QuestionRenderDTO;
  @Input() control!:  FormControl;

  get boolControl(): FormControl {
    // Ensure underlying control is boolean
    if (typeof this.control.value === 'string') {
      this.control.setValue(this.control.value === 'true', { emitEvent: false });
    }
    return this.control;
  }

  onValueChange(): void {
    // Sync string representation for submission
    this.control.setValue(this.boolControl.value ? 'true' : 'false');
  }
}
