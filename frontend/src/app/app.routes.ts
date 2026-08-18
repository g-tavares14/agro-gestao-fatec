import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/landing/landing').then((m) => m.Landing),
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'cadastro',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
  },
  {
    path: 'auth/callback',
    loadComponent: () => import('./features/auth/callback/callback').then((m) => m.Callback),
  },
  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/app-shell/app-shell').then((m) => m.AppShell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
        data: { title: 'Painel' },
      },
      {
        path: 'propriedades',
        loadComponent: () =>
          import('./features/properties/properties-page').then((m) => m.PropertiesPage),
        data: { title: 'Propriedades' },
      },
      {
        path: 'culturas/nova',
        loadComponent: () => import('./features/crops/crop-form').then((m) => m.CropForm),
        data: { title: 'Nova cultura' },
      },
      {
        path: 'culturas/pdf',
        loadComponent: () => import('./features/crops/crop-pdf').then((m) => m.CropPdf),
        data: { title: 'Importar PDF' },
      },
      {
        path: 'culturas/:id/custos',
        loadComponent: () => import('./features/crops/crop-costs').then((m) => m.CropCosts),
        data: { title: 'Custos de produção' },
      },
      {
        path: 'culturas/:id',
        loadComponent: () => import('./features/crops/crop-detail').then((m) => m.CropDetail),
        data: { title: 'Cultura' },
      },
      {
        path: 'culturas',
        loadComponent: () => import('./features/crops/crops-list').then((m) => m.CropsList),
        data: { title: 'Culturas' },
      },
      {
        path: 'diario',
        loadComponent: () =>
          import('./features/field-logs/field-logs-page').then((m) => m.FieldLogsPage),
        data: { title: 'Diário de campo' },
      },
      {
        path: 'financeiro',
        loadComponent: () => import('./features/finance/finance-page').then((m) => m.FinancePage),
        data: { title: 'Financeiro' },
      },
      {
        path: 'documentos',
        loadComponent: () =>
          import('./features/documents/documents-page').then((m) => m.DocumentsPage),
        data: { title: 'Documentos' },
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
