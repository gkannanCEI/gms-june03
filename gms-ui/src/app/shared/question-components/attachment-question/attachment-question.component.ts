import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { QuestionRenderDTO } from '../../../core/models';

@Component({
  selector: 'app-attachment-question',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="question-field">
      <div class="question-label">
        {{ question.label }}
        <span *ngIf="question.required" class="question-required-mark">*</span>
      </div>

      <div *ngIf="uploadedFile" class="file-item">
        <svg class="file-item-icon" width="16" height="16" viewBox="0 0 24 24"
             fill="none" stroke="currentColor" stroke-width="2">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
        </svg>
        <span class="file-item-name">{{ uploadedFile.originalFilename }}</span>
        <span [class]="scanBadge(uploadedFile.scanStatus)">
          <span class="badge-dot"></span>{{ uploadedFile.scanStatus }}
        </span>
      </div>

      <div *ngIf="!uploadedFile"
           class="file-upload-zone"
           (click)="fileInput.click()"
           (dragover)="$event.preventDefault()"
           (drop)="onDrop($event)">
        <div class="file-upload-icon">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
        </div>
        <div class="file-upload-text">
          <span class="file-upload-link">Click to upload</span> or drag and drop
        </div>
        <div class="file-upload-text" style="font-size:var(--font-size-xs);margin-top:var(--spacing-1);">
          PDF, DOC, DOCX, XLS, XLSX, PNG, JPG up to 10MB
        </div>
      </div>

      <input #fileInput type="file" style="display:none" (change)="onFileSelected($event)"/>

      <div *ngIf="uploading"
           style="display:flex;align-items:center;gap:var(--spacing-3);margin-top:var(--spacing-3);">
        <div class="spinner spinner-sm"></div>
        <span class="text-sm text-muted">Uploading&#8230;</span>
      </div>

      <div *ngIf="uploadError" class="question-error" style="margin-top:var(--spacing-2);">
        {{ uploadError }}
      </div>

      <div *ngIf="question.displayConfig.helpText" class="question-help">
        {{ question.displayConfig!.helpText }}
      </div>
    </div>
  `
})
export class AttachmentQuestionComponent {
  @Input() question!:     QuestionRenderDTO;
  @Input() control!:      FormControl;
  @Input() appId?:        number;
  @Input() currentValue?: string;

  uploadedFile: any = null;
  uploading    = false;
  uploadError  = '';

  constructor(private api: ApiService) {}

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.upload(file);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files?.[0];
    if (file) this.upload(file);
  }

  private upload(file: File): void {
    if (!this.appId) {
      this.uploadError = 'Cannot upload: application ID is missing.';
      return;
    }
    this.uploading   = true;
    this.uploadError = '';

    this.api.uploadFile(this.appId, this.question.questionId, file).subscribe({
      next: ref => {
        this.uploading    = false;
        this.uploadedFile = ref;
        this.control.setValue(ref.id);   // FileReferenceDTO.id (not .fileId)
      },
      error: (err: any) => {
        this.uploading   = false;
        this.uploadError = err?.error?.message ?? 'Upload failed. Please try again.';
      }
    });
  }

  scanBadge(status: string): string {
    const m: Record<string, string> = {
      CLEAN:    'badge badge-success',
      INFECTED: 'badge badge-danger',
      PENDING:  'badge badge-warning'
    };
    return m[status] ?? 'badge badge-neutral';
  }
}
