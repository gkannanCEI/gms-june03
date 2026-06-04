import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl } from '@angular/forms';
import { QuestionRenderDTO } from '../../../core/models';

@Component({
  selector: 'app-readonly-question',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="question-field">
      <div class="question-label">{{ question.label }}</div>
      <div class="question-readonly-value">
        {{ displayValue }}
      </div>
    </div>
  `
})
export class ReadonlyQuestionComponent implements OnInit {
  @Input() question!:     QuestionRenderDTO;
  @Input() control!:      FormControl;
  @Input() currentValue?: string;

  displayValue = '—';

  ngOnInit(): void {
    const raw = this.currentValue ?? this.control?.value;
    if (raw === null || raw === undefined || raw === '') {
      this.displayValue = '—';
      return;
    }

    const strVal = String(raw);

    if (this.question.questionType === 'SELECT_ONE') {
      const opt = this.question.options?.find(o => o.optionValue === strVal);
      this.displayValue = opt?.optionLabel ?? strVal;
      return;
    }

    if (this.question.questionType === 'SELECT_MULTI') {
      try {
        const vals: string[] = JSON.parse(strVal);
        this.displayValue = vals.map(v => {
          if (v.startsWith('OTHER:')) return 'Other: ' + v.substring(6);
          return this.question.options?.find(o => o.optionValue === v)?.optionLabel ?? v;
        }).join(', ');
      } catch { this.displayValue = strVal; }
      return;
    }

    if (this.question.questionType === 'CHECKBOX') {
      this.displayValue = (strVal === 'true' || strVal === 'True') ? 'Yes' : 'No';
      return;
    }

    this.displayValue = strVal || '—';
  }
}
