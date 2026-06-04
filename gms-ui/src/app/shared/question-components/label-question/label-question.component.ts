import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { QuestionRenderDTO } from '../../../core/models';

@Component({
  selector: 'app-label-question',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="
      padding: var(--spacing-4) var(--spacing-5);
      background: var(--color-info-light);
      border: 1px solid var(--color-info-border);
      border-left: 4px solid var(--color-info);
      border-radius: var(--radius-md);
      margin-bottom: var(--spacing-2);
    ">
      <p style="margin:0;font-size:var(--font-size-sm);color:var(--color-info);line-height:1.6;">
        {{ question.label }}
      </p>
    </div>
  `
})
export class LabelQuestionComponent {
  @Input() question!: QuestionRenderDTO;
}
