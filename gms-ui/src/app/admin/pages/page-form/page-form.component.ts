import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { Page } from '../../../core/models';

@Component({
  selector: 'app-page-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">{{ isEdit ? 'Edit Page' : 'New Page' }}</h1>
        <p class="page-subtitle">{{ isEdit ? 'Update page details' : 'Create a new reusable form page' }}</p>
      </div>
      <div class="page-actions">
        <a routerLink="/admin/pages" class="btn btn-secondary">Cancel</a>
      </div>
    </div>

    <div style="max-width:640px;">
      <div class="card">
        <div class="card-header">
          <div class="card-title">Page Details</div>
        </div>
        <div class="card-body">

          <div *ngIf="error" class="alert alert-danger" style="margin-bottom:var(--spacing-4);">
            <span class="alert-icon">&#9888;</span><div>{{ error }}</div>
          </div>
          <div *ngIf="success" class="alert alert-success" style="margin-bottom:var(--spacing-4);">
            <span class="alert-icon">&#10003;</span><div>Page saved! Redirecting&#8230;</div>
          </div>

          <form (ngSubmit)="onSubmit()" novalidate>
            <div class="form-group">
              <label class="form-label" for="pageName">
                Page Name <span class="form-label-required">*</span>
              </label>
              <input id="pageName" type="text" class="form-control"
                     [(ngModel)]="page.pageName" name="pageName"
                     required maxlength="200"
                     placeholder="e.g. Applicant Information"/>
            </div>

            <div class="form-group">
              <label class="form-label" for="pageDescription">Description</label>
              <textarea id="pageDescription" class="form-control"
                        [(ngModel)]="page.pageDescription" name="pageDescription"
                        rows="4" maxlength="1000"
                        placeholder="Describe what this page collects&#8230;"></textarea>
            </div>

            <div style="display:flex;gap:var(--spacing-3);justify-content:flex-end;margin-top:var(--spacing-4);">
              <a routerLink="/admin/pages" class="btn btn-secondary">Cancel</a>
              <button type="submit" class="btn btn-primary" [disabled]="saving">
                <span *ngIf="saving" class="spinner spinner-sm"></span>
                {{ saving ? 'Saving&#8230;' : (isEdit ? 'Save Changes' : 'Create Page') }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `
})
export class PageFormComponent implements OnInit {
  page:   Partial<Page> = {};
  isEdit  = false;
  saving  = false;
  error   = '';
  success = false;
  private pageId?: number;

  constructor(
    private route:  ActivatedRoute,
    private router: Router,
    private api:    ApiService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.params['id'];
    if (id) { this.isEdit = true; this.pageId = +id; }
  }

  onSubmit(): void {
    if (!this.page.pageName?.trim()) { this.error = 'Page name is required'; return; }
    this.saving = true; this.error = ''; this.success = false;
    const obs = this.isEdit
      ? this.api.updatePage(this.pageId!, this.page)
      : this.api.createPage(this.page);
    obs.subscribe({
      next: () => {
        this.saving  = false;
        this.success = true;
        setTimeout(() => this.router.navigate(['/admin/pages']), 1000);
      },
      error: (err: any) => {
        this.saving = false;
        this.error  = err.error?.message ?? 'Failed to save page';
      }
    });
  }
}
