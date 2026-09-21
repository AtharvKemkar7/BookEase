import { Routes } from '@angular/router';
import { authGuard, customerGuard, guestGuard, roleGuard } from './core/guards';

export const routes: Routes = [
  {
    path: '',
    canActivate: [customerGuard],
    loadComponent: () => import('./pages/home.component').then((m) => m.HomeComponent),
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'providers/:id',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./pages/provider-detail.component').then((m) => m.ProviderDetailComponent),
  },
  {
    path: 'appointments',
    canActivate: [roleGuard(['USER'])],
    loadComponent: () =>
      import('./pages/my-appointments.component').then((m) => m.MyAppointmentsComponent),
  },
  {
    path: 'reminders',
    canActivate: [roleGuard(['USER'])],
    loadComponent: () =>
      import('./pages/my-reminders.component').then((m) => m.MyRemindersComponent),
  },
  {
    path: 'become-provider',
    canActivate: [roleGuard(['USER'])],
    loadComponent: () =>
      import('./pages/provider-profile.component').then((m) => m.ProviderProfileComponent),
  },
  {
    path: 'provider/appointments',
    canActivate: [roleGuard(['PROVIDER'])],
    loadComponent: () =>
      import('./pages/my-appointments.component').then((m) => m.MyAppointmentsComponent),
  },
  {
    path: 'provider/profile',
    canActivate: [roleGuard(['PROVIDER'])],
    loadComponent: () =>
      import('./pages/provider-profile.component').then((m) => m.ProviderProfileComponent),
  },
  {
    path: 'provider/services',
    canActivate: [roleGuard(['PROVIDER'])],
    loadComponent: () =>
      import('./pages/provider-services.component').then((m) => m.ProviderServicesComponent),
  },
  {
    path: 'provider/availability',
    canActivate: [roleGuard(['PROVIDER'])],
    loadComponent: () =>
      import('./pages/provider-availability.component').then((m) => m.ProviderAvailabilityComponent),
  },
  {
    path: 'admin/categories',
    canActivate: [roleGuard(['ADMIN'])],
    loadComponent: () =>
      import('./pages/admin-categories.component').then((m) => m.AdminCategoriesComponent),
  },
  {
    path: 'admin/providers',
    canActivate: [roleGuard(['ADMIN'])],
    loadComponent: () =>
      import('./pages/admin-providers.component').then((m) => m.AdminProvidersComponent),
  },
  { path: '**', redirectTo: '' },
];
