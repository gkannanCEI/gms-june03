import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, FormGroup } from '@angular/forms';
import { QuestionRenderDTO, ChildQuestionRenderDTO } from '../../../core/models';
import { QuestionHostDirective } from '../question-host.directive';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-radio-yes-no-question',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, QuestionHostDirective],
  template: `
    <div class="question-field">
      <fieldset style="border:none;padding:0;margin:0;">
        <legend class="question-label" style="float:none;width:auto;">
          {{ question.label }}
          <span *ngIf="question.required" class="question-required-mark">*</span>
        </legend>

        <div style="display:flex;gap:var(--spacing-4);margin-top:var(--spacing-2);">
          <label class="form-check" style="cursor:pointer;">
            <input type="radio" [formControl]="control" value="Yes"/>
            <span class="form-check-label">Yes</span>
          </label>
          <label class="form-check" style="cursor:pointer;">
            <input type="radio" [formControl]="control" value="No"/>
            <span class="form-check-label">No</span>
          </label>
        </div>

        <div *ngIf="question.displayConfig.helpText" class="question-help">
          {{ question.displayConfig!.helpText }}
        </div>

        <div *ngIf="visibleChildren.length > 0" class="child-questions">
          <div class="child-question-trigger">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none"
                 stroke="currentColor" stroke-width="2">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
            Follow-up {{ control.value === 'Yes' ? '(Yes selected)' : '(No selected)' }}
          </div>
          <div *ngFor="let child of visibleChildren"
               [class]="'col-span-' + child.question.displayConfig.columnSpan"
               style="margin-bottom:var(--spacing-4);">
            <div appQuestionHost
              [question]="toQuestion(child)"
              [control]="getChildControl(child.question.questionId)"
              [formGroup]="formGroup">
            </div>
          </div>
        </div>
      </fieldset>
    </div>
  `
})
export class RadioYesNoQuestionComponent implements OnInit, OnDestroy {
  @Input() question!:  QuestionRenderDTO;
  @Input() control!:   FormControl;
  @Input() formGroup!: FormGroup;

  private sub?: Subscription;

  ngOnInit(): void {
    this.sub = this.control.valueChanges.subscribe(val => {
      for (const child of (this.question.childQuestions ?? [])) {
        if (child.triggerValue !== val) {
          this.getChildControl(child.question.questionId).setValue('');
        }
      }
    });
  }

  ngOnDestroy(): void { this.sub?.unsubscribe(); }

  get visibleChildren(): ChildQuestionRenderDTO[] {
    const val = this.control.value;
    if (!val) return [];
    return (this.question.childQuestions ?? []).filter(c => c.triggerValue === val);
  }

  getChildControl(questionId: number): FormControl {
    const key  = 'q_' + questionId;
    let   ctrl = this.formGroup?.get(key) as FormControl;
    if (!ctrl) {
      ctrl = new FormControl('');
      this.formGroup?.addControl(key, ctrl);
    }
    return ctrl;
  }

  /** Map ChildQuestionRenderDTO to a QuestionRenderDTO-compatible object for the host directive */
  toQuestion(child: ChildQuestionRenderDTO): QuestionRenderDTO {
    return child.question;
  }
}
