import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-organization-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">Organizations</h1>
        <p class="page-subtitle">Manage organisations eligible to apply for grants</p>
      </div>
      <div class="page-actions">
        <button class="btn btn-primary" (click)="showForm = !showForm">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          {{ showForm ? 'Cancel' : 'Add Organization' }}
        </button>
      </div>
    </div>

    <div *ngIf="showForm" class="card" style="margin-bottom:var(--spacing-5);border-color:var(--color-primary-light);">
      <div class="card-header"><div class="card-title">New Organization</div></div>
      <div class="card-body">
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">Name <span class="form-label-required">*</span></label>
            <input type="text" class="form-control" [(ngModel)]="newOrg.name"
                   maxlength="255" placeholder="Organisation name"/>
          </div>
          <div class="form-group">
            <label class="form-label">Type <span class="form-label-required">*</span></label>
            <select class="form-control" [(ngModel)]="newOrg.organizationType">
              <option value="">Select type&#8230;</option>
              <option *ngFor="let t of orgTypes" [value]="t">{{ t }}</option>
            </select>
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">Description</label>
          <textarea class="form-control" [(ngModel)]="newOrg.description" rows="2"></textarea>
        </div>
        <div *ngIf="formError" class="alert alert-danger" style="margin-bottom:var(--spacing-3);">
          <span class="alert-icon">&#9888;</span><div>{{ formError }}</div>
        </div>
        <div style="display:flex;justify-content:flex-end;">
          <button class="btn btn-primary" (click)="save()">Save Organization</button>
        </div>
      </div>
    </div>

    <div *ngIf="loading" class="loading-overlay">
      <div class="spinner spinner-lg"></div>
    </div>

    <div *ngIf="!loading && organizations.length === 0" class="card">
      <div class="empty-state">
        <div class="empty-state-icon">&#127963;</div>
        <div class="empty-state-title">No organisations yet</div>
        <div class="empty-state-desc">Add organisations that can apply for grants.</div>
        <button class="btn btn-primary" (click)="showForm = true">Add Organisation</button>
      </div>
    </div>

    <div *ngIf="!loading && organizations.length > 0" class="table-container">
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Type</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let o of organizations">
            <td style="font-weight:600;">{{ o.name }}</td>
            <td>
              <span class="badge badge-info" style="font-size:0.7rem;">{{ o.organizationType }}</span>
            </td>
            <td>
              <span [class]="o.active ? 'badge badge-success' : 'badge badge-neutral'">
                <span class="badge-dot"></span>{{ o.active ? 'Active' : 'Inactive' }}
              </span>
            </td>
            <td>
              <div class="table-actions">
                <button *ngIf="o.active" class="btn btn-danger btn-sm" (click)="deactivate(o.id)">Deactivate</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `
})
export class OrganizationListComponent implements OnInit {
  organizations: any[] = [];
  loading   = true;
  showForm  = false;
  newOrg:   any = {};
  formError = '';
  orgTypes  = ['NONPROFIT','GOVERNMENT','BUSINESS','EDUCATIONAL','OTHER'];
  private base = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.http.get<any[]>(`${this.base}/api/admin/organizations`).subscribe({
      next: o  => { this.organizations = o; this.loading = false; },
      error: () => this.loading = false
    });
  }

  save(): void {
    this.formError = '';
    if (!this.newOrg.name || !this.newOrg.organizationType) {
      this.formError = 'Name and type are required';
      return;
    }
    this.http.post(`${this.base}/api/admin/organizations`, this.newOrg).subscribe({
      next: () => { this.showForm = false; this.newOrg = {}; this.load(); },
      error: (e: any) => this.formError = e.error?.message ?? 'Failed to save'
    });
  }

  deactivate(id: number): void {
    if (!confirm('Deactivate this organisation?')) return;
    this.http.delete(`${this.base}/api/admin/organizations/${id}`).subscribe(() => this.load());
  }
}
