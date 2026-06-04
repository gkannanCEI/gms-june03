import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./core/auth/login.component').then(m => m.LoginComponent)
  },
  { path: '', redirectTo: '/apply', pathMatch: 'full' },

  // Admin routes
  {
    path: 'admin',
    canActivate: [authGuard],
    children: [
      {
        path: 'questions',
        loadComponent: () => import('./admin/questions/question-list/question-list.component')
          .then(m => m.QuestionListComponent)
      },
      {
        path: 'questions/new',
        loadComponent: () => import('./admin/questions/question-form/question-form.component')
          .then(m => m.QuestionFormComponent)
      },
      {
        path: 'questions/:id/edit',
        loadComponent: () => import('./admin/questions/question-form/question-form.component')
          .then(m => m.QuestionFormComponent)
      },
      {
        path: 'programs',
        loadComponent: () => import('./admin/programs/program-list/program-list.component')
          .then(m => m.ProgramListComponent)
      },
      {
        path: 'programs/new',
        loadComponent: () => import('./admin/programs/program-form/program-form.component')
          .then(m => m.ProgramFormComponent)
      },
      {
        path: 'programs/:id/edit',
        loadComponent: () => import('./admin/programs/program-form/program-form.component')
          .then(m => m.ProgramFormComponent)
      },
      {
        path: 'programs/:id',
        loadComponent: () => import('./admin/programs/program-detail/program-detail.component')
          .then(m => m.ProgramDetailComponent)
      },
      {
        path: 'programs/:id/rounds/:roundId',
        loadComponent: () => import('./admin/programs/round-detail/round-detail.component')
          .then(m => m.RoundDetailComponent)
      },
      {
        path: 'programs/:id/rounds/:roundId/pages/:pageId',
        loadComponent: () => import('./admin/programs/round-page-config/round-page-config.component')
          .then(m => m.RoundPageConfigComponent)
      },
      {
        path: 'programs/:id/rounds/:roundId/applications',
        loadComponent: () => import('./admin/applications/application-list/application-list.component')
          .then(m => m.ApplicationListComponent)
      },
      {
        path: 'pages',
        loadComponent: () => import('./admin/pages/page-list/page-list.component')
          .then(m => m.PageListComponent)
      },
      {
        path: 'pages/new',
        loadComponent: () => import('./admin/pages/page-form/page-form.component')
          .then(m => m.PageFormComponent)
      },
      {
        path: 'pages/:id/edit',
        loadComponent: () => import('./admin/pages/page-form/page-form.component')
          .then(m => m.PageFormComponent)
      },
      {
        path: 'organizations',
        loadComponent: () => import('./admin/organizations/organization-list/organization-list.component')
          .then(m => m.OrganizationListComponent)
      },
    ]
  },

  // Applicant routes
  {
    path: 'apply',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./applicant/home/applicant-home.component')
          .then(m => m.ApplicantHomeComponent)
      },
      {
        path: 'programs/:programId/rounds/:roundId',
        children: [
          {
            path: 'dashboard',
            loadComponent: () => import('./applicant/dashboard/dashboard.component')
              .then(m => m.DashboardComponent)
          },
          {
            path: 'pages/:pageId',
            loadComponent: () => import('./applicant/form-page/form-page.component')
              .then(m => m.FormPageComponent)
          }
        ]
      }
    ]
  },

  { path: '**', redirectTo: '/login' }
];
