import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, UserResponse, UserRole } from '../models';

const TOKEN_KEY = 'bookease.token';
const USER_KEY = 'bookease.user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly tokenSignal = signal<string | null>(readStorage(TOKEN_KEY));
  private readonly userSignal = signal<UserResponse | null>(readUser());

  readonly token = this.tokenSignal.asReadonly();
  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.tokenSignal() !== null && this.userSignal() !== null);
  readonly role = computed<UserRole | null>(() => this.userSignal()?.role ?? null);
  readonly isAdmin = computed(() => this.role() === 'ADMIN');
  readonly isProvider = computed(() => this.role() === 'PROVIDER');
  readonly isUser = computed(() => this.role() === 'USER');

  homePath(): string {
    switch (this.role()) {
      case 'ADMIN':
        return '/admin/providers';
      case 'PROVIDER':
        return '/provider/appointments';
      default:
        return '/';
    }
  }

  register(payload: {
    name: string;
    email: string;
    password: string;
    phone?: string;
  }): Observable<UserResponse> {
    return this.http.post<UserResponse>('/api/v1/auth/register', payload);
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/v1/auth/login', { email, password })
      .pipe(tap((response) => this.persist(response)));
  }

  refreshIdentity(): Observable<UserResponse> {
    return this.http
      .get<UserResponse>('/api/v1/auth/me')
      .pipe(tap((user) => this.persistUser(user)));
  }

  logout(redirect = true): void {
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    if (redirect) {
      void this.router.navigate(['/login']);
    }
  }

  private persist(response: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, response.accessToken);
    this.tokenSignal.set(response.accessToken);
    this.persistUser(response.user);
  }

  private persistUser(user: UserResponse): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.userSignal.set(user);
  }
}

function readStorage(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function readUser(): UserResponse | null {
  const raw = readStorage(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as UserResponse;
  } catch {
    return null;
  }
}
