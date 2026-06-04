import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-program-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, DecimalPipe],
  template: `
    <div *ngIf="loading" class="loading-overlay">
      <div class="spinner spinner-lg"></div><span>Loading program&#8230;</span>
    </div>

    <ng-container *ngIf="!loading && program">

      <div class="page-header">
        <div class="page-header-left">
          <div style="display:flex;align-items:center;gap:var(--spacing-3);margin-bottom:var(--spacing-2);">
            <a routerLink="/admin/programs" class="btn btn-ghost btn-sm" style="padding:0.25rem 0.5rem;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="15 18 9 12 15 6"/>
              </svg>
              Programs
            </a>
          </div>
          <h1 class="page-title">{{ program.programName }}</h1>
          <div style="display:flex;align-items:center;gap:var(--spacing-3);margin-top:var(--spacing-2);">
            <span [class]="statusBadge(program.status)">
              <span class="badge-dot"></span>{{ program.status }}
            </span>
            <span *ngIf="program.totalBudget" class="text-sm text-muted">
              Budget: &#36;{{ program.totalBudget | number }}
            </span>
          </div>
        </div>
        <div class="page-actions">
          <a [routerLink]="['/admin/programs', program.id, 'edit']" class="btn btn-secondary">Edit Program</a>
          <button class="btn btn-primary" (click)="showRoundForm = true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            Add Round
          </button>
        </div>
      </div>

      <div *ngIf="program.description" class="card" style="margin-bottom:var(--spacing-6);">
        <div class="card-body" style="padding:var(--spacing-5);"><p style="margin:0;">{{ program.description }}</p></div>
      </div>

      <div *ngIf="showRoundForm" class="card" style="margin-bottom:var(--spacing-6);border-color:var(--color-primary-light);">
        <div class="card-header">
          <div class="card-title">New Round</div>
          <button class="btn btn-ghost btn-sm btn-icon" (click)="cancelRound()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="card-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Round Name <span class="form-label-required">*</span></label>
              <input type="text" class="form-control" [(ngModel)]="newRound.roundName" placeholder="e.g. 2025 Q1"/>
            </div>
            <div class="form-group">
              <label class="form-label">Start Date <span class="form-label-required">*</span></label>
              <input type="date" class="form-control" [(ngModel)]="newRound.startDate"/>
            </div>
            <div class="form-group">
              <label class="form-label">End Date <span class="form-label-required">*</span></label>
              <input type="date" class="form-control" [(ngModel)]="newRound.endDate"/>
            </div>
          </div>
          <div style="display:flex;gap:var(--spacing-3);justify-content:flex-end;">
            <button class="btn btn-secondary" (click)="cancelRound()">Cancel</button>
            <button class="btn btn-primary" (click)="createRound()" [disabled]="savingRound">
              <span *ngIf="savingRound" class="spinner spinner-sm"></span>
              {{ savingRound ? 'Creating&#8230;' : 'Create Round' }}
            </button>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <div>
            <div class="card-title">Rounds</div>
            <div class="card-subtitle">{{ rounds.length }} round{{ rounds.length !== 1 ? 's' : '' }} for this program</div>
          </div>
        </div>

        <div *ngIf="rounds.length === 0" class="empty-state">
          <div class="empty-state-icon">&#128197;</div>
          <div class="empty-state-title">No rounds yet</div>
          <div class="empty-state-desc">Add a round to start configuring application forms.</div>
          <button class="btn btn-primary" (click)="showRoundForm = true">Add First Round</button>
        </div>

        <div *ngIf="rounds.length > 0" class="table-container" style="border:none;border-radius:0;">
          <table>
            <thead>
              <tr>
                <th>Round Name</th>
                <th>Status</th>
                <th>Start Date</th>
                <th>End Date</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let r of rounds">
                <td style="font-weight:500;">{{ r.roundName }}</td>
                <td>
                  <span [class]="roundBadge(r.status)">
                    <span class="badge-dot"></span>{{ r.status }}
                  </span>
                </td>
                <td class="td-muted">{{ r.startDate }}</td>
                <td class="td-muted">{{ r.endDate }}</td>
                <td>
                  <div class="table-actions">
                    <a [routerLink]="['/admin/programs', programId, 'rounds', r.id]" class="btn btn-secondary btn-sm">Configure</a>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

    </ng-container>
  `
})
export class ProgramDetailComponent implements OnInit {
  program:      any  = null;
  rounds:       any[] = [];
  programId!:   number;
  loading       = true;
  showRoundForm = false;
  savingRound   = false;
  newRound      = { roundName: '', startDate: '', endDate: '' };

  constructor(private api: ApiService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.programId = +this.route.snapshot.params['id'];
    this.loadProgram();
  }

  loadProgram(): void {
    this.loading = true;
    forkJoin({
      program: this.api.getProgram(this.programId),
      rounds:  this.api.listRounds(this.programId)
    }).subscribe({
      next: ({ program, rounds }) => {
        this.program = program;
        this.rounds  = rounds ?? [];
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  createRound(): void {
    if (!this.newRound.roundName || !this.newRound.startDate || !this.newRound.endDate) return;
    this.savingRound = true;
    this.api.createRound(this.programId, this.newRound).subscribe({
      next: () => { this.savingRound = false; this.cancelRound(); this.loadProgram(); },
      error: () => this.savingRound = false
    });
  }

  cancelRound(): void {
    this.showRoundForm = false;
    this.newRound = { roundName: '', startDate: '', endDate: '' };
  }

  statusBadge(s: string): string {
    const m: Record<string, string> = {
      ACTIVE: 'badge badge-success', ARCHIVED: 'badge badge-warning'
    };
    return m[s] ?? 'badge badge-neutral';
  }

  roundBadge(s: string): string {
    const m: Record<string, string> = {
      ACTIVE: 'badge badge-success', DRAFT: 'badge badge-neutral',
      CLOSED: 'badge badge-info',    ARCHIVED: 'badge badge-warning'
    };
    return m[s] ?? 'badge badge-neutral';
  }
}
