import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, FormControl } from '@angular/forms';
import { QuestionRenderDTO } from '../../../core/models';

@Component({
  selector: 'app-select-multi-question',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="question-field">
      <fieldset style="border:none;padding:0;margin:0;">
        <legend class="question-label" style="float:none;width:auto;">
          {{ question.label }}
          <span *ngIf="question.required" class="question-required-mark">*</span>
        </legend>

        <div style="display:flex;flex-direction:column;gap:var(--spacing-2);margin-top:var(--spacing-2);">
          <label *ngFor="let opt of question.options" class="form-check">
            <input
              type="checkbox"
              [checked]="isSelected(opt.optionValue)"
              (change)="toggleOption(opt.optionValue)"
            />
            <span class="form-check-label">{{ opt.optionLabel }}</span>
          </label>

          <!-- Other option -->
          <div *ngIf="hasOtherOption">
            <label class="form-check">
              <input type="checkbox" [checked]="otherSelected" (change)="toggleOther()"/>
              <span class="form-check-label">Other</span>
            </label>
            <input *ngIf="otherSelected"
                   type="text"
                   class="form-control"
                   [value]="otherText"
                   (input)="onOtherTextChange($event)"
                   placeholder="Please specify…"
                   style="margin-top:var(--spacing-2);max-width:360px;"/>
          </div>
        </div>

        <div *ngIf="question.displayConfig.helpText" class="question-help">
          {{ question.displayConfig!.helpText }}
        </div>
      </fieldset>
    </div>
  `
})
export class SelectMultiQuestionComponent implements OnInit {
  @Input() question!: QuestionRenderDTO;
  @Input() control!:  FormControl;

  selectedValues: string[] = [];
  otherText = '';

  get hasOtherOption(): boolean {
    return this.question.options?.some(o => o.optionValue === 'OTHER') ?? false;
  }

  get otherSelected(): boolean {
    return this.selectedValues.some(v => v.startsWith('OTHER:') || v === 'OTHER');
  }

  ngOnInit(): void {
    try {
      const raw = this.control.value || '[]';
      this.selectedValues = Array.isArray(raw) ? raw : JSON.parse(raw);
    } catch {
      console.warn('SelectMulti: could not parse stored value, resetting to empty');
      this.selectedValues = [];
    }
    const otherEntry = this.selectedValues.find(v => v.startsWith('OTHER:'));
    if (otherEntry) this.otherText = otherEntry.substring(6);
  }

  isSelected(value: string): boolean {
    if (value === 'OTHER') return this.otherSelected;
    return this.selectedValues.includes(value);
  }

  toggleOption(value: string): void {
    const idx = this.selectedValues.indexOf(value);
    if (idx > -1) this.selectedValues.splice(idx, 1);
    else          this.selectedValues.push(value);
    this.updateControl();
  }

  toggleOther(): void {
    const idx = this.selectedValues.findIndex(v => v.startsWith('OTHER:') || v === 'OTHER');
    if (idx > -1) { this.selectedValues.splice(idx, 1); this.otherText = ''; }
    else          { this.selectedValues.push('OTHER:'); }
    this.updateControl();
  }

  onOtherTextChange(event: Event): void {
    this.otherText = (event.target as HTMLInputElement).value;
    const idx = this.selectedValues.findIndex(v => v.startsWith('OTHER:') || v === 'OTHER');
    if (idx > -1) this.selectedValues[idx] = 'OTHER:' + this.otherText;
    else          this.selectedValues.push('OTHER:' + this.otherText);
    this.updateControl();
  }

  private updateControl(): void {
    this.control.setValue(JSON.stringify([...this.selectedValues]));
    this.control.markAsDirty();
  }
}
