import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { describeError } from '../core/http-error';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="container narrow">
      <div class="card">
        <h1>Sign in</h1>
        <p class="muted small">Sign in to your customer, provider, or admin workspace.</p>

        @if (error()) {
          <div class="alert error">{{ error() }}</div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="field">
            <label for="email">Email</label>
            <input id="email" type="email" formControlName="email" autocomplete="email" />
            @if (form.controls.email.invalid && form.controls.email.touched) {
              <span class="small error-text">Enter a valid email address.</span>
            }
          </div>
          <div class="field">
            <label for="password">Password</label>
            <input
              id="password"
              type="password"
              formControlName="password"
              autocomplete="current-password"
            />
            @if (form.controls.password.invalid && form.controls.password.touched) {
              <span class="small error-text">Password is required.</span>
            }
          </div>
          <button class="btn" type="submit" [disabled]="loading()">
            {{ loading() ? 'Signing in...' : 'Sign in' }}
          </button>
        </form>

        <p class="small muted mt">
          No account yet? <a routerLink="/register">Create one</a>.
        </p>
      </div>
    </div>
  `,
  styles: [
    `
      .narrow {
        max-width: 440px;
      }
      .error-text {
        color: var(--danger);
      }
      .mt {
        margin-top: 1rem;
      }
    `,
  ],
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigate([this.auth.homePath()]);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(describeError(err));
      },
    });
  }
}
