import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <div class="topbar-inner">
        <a class="brand" [routerLink]="auth.isAuthenticated() ? auth.homePath() : '/'">
          <span class="logo">BE</span>
          <span>BookEase</span>
        </a>

        <nav class="nav">
          @if (!auth.isAuthenticated() || auth.isUser()) {
            <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }"
              >Discover</a
            >
          }
          @if (auth.isUser()) {
            <a routerLink="/appointments" routerLinkActive="active">Appointments</a>
            <a routerLink="/reminders" routerLinkActive="active">Reminders</a>
            <a routerLink="/become-provider" routerLinkActive="active">Become a provider</a>
          }
          @if (auth.isProvider()) {
            <a routerLink="/provider/appointments" routerLinkActive="active">Appointments</a>
            <a routerLink="/provider/profile" routerLinkActive="active">Business</a>
            <a routerLink="/provider/services" routerLinkActive="active">Services</a>
            <a routerLink="/provider/availability" routerLinkActive="active">Availability</a>
          }
          @if (auth.isAdmin()) {
            <a routerLink="/admin/providers" routerLinkActive="active">Approvals</a>
            <a routerLink="/admin/categories" routerLinkActive="active">Categories</a>
          }
        </nav>

        <div class="account">
          @if (auth.isAuthenticated()) {
            <span class="who">
              <strong>{{ auth.user()?.name }}</strong>
              <span class="badge">{{ auth.role() }}</span>
            </span>
            <button class="btn secondary small" type="button" (click)="auth.logout()">
              Sign out
            </button>
          } @else {
            <a class="btn secondary small" routerLink="/login">Sign in</a>
            <a class="btn small" routerLink="/register">Create account</a>
          }
        </div>
      </div>
    </header>

    <main>
      <router-outlet />
    </main>

    <footer class="footer">
      <span>BookEase - service discovery and appointment booking</span>
      <span class="muted small">Powered by the BookEase Spring Boot REST API</span>
    </footer>
  `,
  styles: [
    `
      .topbar {
        background: var(--surface);
        border-bottom: 1px solid var(--border);
        position: sticky;
        top: 0;
        z-index: 20;
      }

      .topbar-inner {
        max-width: 1120px;
        margin: 0 auto;
        padding: 0.7rem 1.25rem;
        display: flex;
        align-items: center;
        gap: 1.25rem;
      }

      .brand {
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
        font-weight: 750;
        font-size: 1.1rem;
        color: var(--text);
      }

      .brand:hover {
        text-decoration: none;
      }

      .logo {
        display: grid;
        place-items: center;
        width: 32px;
        height: 32px;
        border-radius: 9px;
        background: var(--primary);
        color: #fff;
        font-size: 0.85rem;
        letter-spacing: 0.02em;
      }

      .nav {
        display: flex;
        gap: 0.25rem;
        flex-wrap: wrap;
        flex: 1;
      }

      .nav a {
        padding: 0.4rem 0.7rem;
        border-radius: var(--radius-sm);
        color: var(--text-muted);
        font-weight: 600;
        font-size: 0.9rem;
      }

      .nav a:hover {
        background: var(--surface-alt);
        color: var(--text);
        text-decoration: none;
      }

      .nav a.active {
        background: var(--primary-soft);
        color: var(--primary-dark);
      }

      .account {
        display: flex;
        align-items: center;
        gap: 0.6rem;
      }

      .who {
        display: flex;
        align-items: center;
        gap: 0.45rem;
        font-size: 0.9rem;
      }

      .footer {
        border-top: 1px solid var(--border);
        background: var(--surface);
        padding: 1rem 1.25rem;
        display: flex;
        justify-content: space-between;
        gap: 1rem;
        flex-wrap: wrap;
        font-size: 0.85rem;
        color: var(--text-muted);
      }
    `,
  ],
})
export class AppComponent implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    if (!this.auth.isAuthenticated()) {
      return;
    }
    this.auth.refreshIdentity().subscribe({
      next: () => {
        const url = this.router.url.split('?')[0];
        if (this.auth.isProvider() && (url === '/' || url.startsWith('/appointments') || url.startsWith('/reminders') || url.startsWith('/become-provider') || url.startsWith('/providers/'))) {
          void this.router.navigate([this.auth.homePath()]);
        } else if (this.auth.isAdmin() && url !== '/admin/providers' && url !== '/admin/categories') {
          void this.router.navigate([this.auth.homePath()]);
        }
      },
      error: () => undefined,
    });
  }
}
