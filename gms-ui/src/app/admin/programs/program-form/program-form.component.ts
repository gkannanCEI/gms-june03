import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-program-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">{{ isEdit ? 'Edit Program' : 'New Program' }}</h1>
        <p class="page-subtitle">{{ isEdit ? 'Update program details' : 'Create a new grant program' }}</p>
      </div>
      <div class="page-actions">
        <a routerLink="/admin/programs" class="btn btn-secondary">Cancel</a>
      </div>
    </div>

    <div *ngIf="loading" class="loading-overlay"><div class="spinner spinner-lg"></div></div>

    <div *ngIf="!loading" style="max-width:720px;">
      <div class="card">
        <div class="card-header">
          <div>
            <div class="card-title">Program Details</div>
            <div class="card-subtitle">Basic information about this grant program</div>
          </div>
        </div>
        <div class="card-body">

          <div *ngIf="errorMessage" class="alert alert-danger" style="margin-bottom:1.5rem;">
            <span class="alert-icon">⚠</span>
            <div>{{ errorMessage }}</div>
          </div>

          <form (ngSubmit)="onSubmit()" #f="ngForm" novalidate>

            <div class="form-group">
              <label class="form-label" for="name">
                Program Name <span class="form-label-required">*</span>
              </label>
              <input id="name" name="programName" type="text" class="form-control"
                     [(ngModel)]="program.programName" required
                     placeholder="e.g. Community Innovation Grant 2025"/>
            </div>

            <div class="form-group">
              <label class="form-label" for="desc">Description</label>
              <textarea id="desc" name="description" class="form-control"
                        [(ngModel)]="program.description" rows="4"
                        placeholder="Describe the purpose and goals of this program…"></textarea>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label" for="budget">Total Budget</label>
                <input id="budget" name="totalBudget" type="number" class="form-control"
                       [(ngModel)]="program.totalBudget" min="0"
                       placeholder="0.00"/>
                <div class="form-hint">Enter total available funds in dollars</div>
              </div>

              <div class="form-group" *ngIf="isEdit">
                <label class="form-label" for="status">Status</label>
                <select id="status" name="status" class="form-control"
                        [(ngModel)]="program.status">
                  <option value="DRAFT">Draft</option>
                  <option value="ACTIVE">Active</option>
                  <option value="ARCHIVED">Archived</option>
                </select>
              </div>
            </div>

            <div style="display:flex;gap:var(--spacing-3);justify-content:flex-end;margin-top:var(--spacing-4);">
              <a routerLink="/admin/programs" class="btn btn-secondary">Cancel</a>
              <button type="submit" class="btn btn-primary" [disabled]="saving || f.invalid">
                <span *ngIf="saving" class="spinner spinner-sm"></span>
                {{ saving ? 'Saving…' : (isEdit ? 'Save Changes' : 'Create Program') }}
              </button>
            </div>

          </form>
        </div>
      </div>
    </div>
  `
})
export class ProgramFormComponent implements OnInit {
  program: any = { programName: '', description: '', totalBudget: null };
  isEdit = false;
  loading = false;
  saving  = false;
  errorMessage = '';

  constructor(
    private api: ApiService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.params['id'];
    if (id && id !== 'new') {
      this.isEdit  = true;
      this.loading = true;
      this.api.getProgram(+id).subscribe({
        next: p => { this.program = p; this.loading = false; },
        error: () => this.loading = false
      });
    }
  }

  onSubmit(): void {
    this.saving = true;
    this.errorMessage = '';
    const action = this.isEdit
      ? this.api.updateProgram(this.program.id, this.program)
      : this.api.createProgram(this.program);

    action.subscribe({
      next: () => this.router.navigate(['/admin/programs']),
      error: err => {
        this.saving = false;
        this.errorMessage = err?.error?.message ?? 'Failed to save program. Please try again.';
      }
    });
  }
}
