import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { describeError } from '../core/http-error';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="container narrow">
      <div class="card">
        <h1>Create your account</h1>
        <p class="muted small">
          New accounts are created with the USER role. Send a become-provider request after sign-in; you stay a USER until an admin approves it.
        </p>

        @if (error()) {
          <div class="alert error">{{ error() }}</div>
        }
        @if (created()) {
          <div class="alert success">
            Account created. You can now <a routerLink="/login">sign in</a>.
          </div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="field">
            <label for="name">Full name</label>
            <input id="name" formControlName="name" autocomplete="name" />
          </div>
          <div class="field">
            <label for="email">Email</label>
            <input id="email" type="email" formControlName="email" autocomplete="email" />
          </div>
          <div class="field">
            <label for="phone">Phone (optional)</label>
            <input id="phone" formControlName="phone" autocomplete="tel" />
          </div>
          <div class="field">
            <label for="password">Password</label>
            <input
              id="password"
              type="password"
              formControlName="password"
              autocomplete="new-password"
            />
            <span class="small muted">At least 8 characters.</span>
          </div>
          <button class="btn" type="submit" [disabled]="loading()">
            {{ loading() ? 'Creating...' : 'Create account' }}
          </button>
        </form>

        <p class="small muted mt">
          Already registered? <a routerLink="/login">Sign in</a>.
        </p>
      </div>
    </div>
  `,
  styles: [
    `
      .narrow {
        max-width: 460px;
      }
      .mt {
        margin-top: 1rem;
      }
    `,
  ],
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly created = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const value = this.form.getRawValue();
    this.auth
      .register({
        name: value.name,
        email: value.email,
        password: value.password,
        phone: value.phone || undefined,
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.created.set(true);
          this.form.reset({ name: '', email: '', phone: '', password: '' });
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(describeError(err));
        },
      });
  }
}
