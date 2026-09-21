import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiService } from '../core/api.service';
import { describeError } from '../core/http-error';
import { ProviderResponse } from '../models';

@Component({
  selector: 'app-admin-providers',
  standalone: true,
  imports: [],
  template: `
    <div class="container">
      <div class="row between">
        <div>
          <h1>Provider approvals</h1>
          <p class="muted small">Approving a request promotes the applicant from USER to PROVIDER.</p>
        </div>
        <button class="btn secondary small" type="button" (click)="load()" [disabled]="loading()">
          Refresh
        </button>
      </div>

      @if (error()) {
        <div class="alert error">{{ error() }}</div>
      }
      @if (success()) {
        <div class="alert success">{{ success() }}</div>
      }

      @if (loading()) {
        <div class="empty">Loading pending providers...</div>
      } @else if (providers().length === 0) {
        <div class="empty">No providers are waiting for approval.</div>
      } @else {
        <div class="grid cols-2">
          @for (provider of providers(); track provider.id) {
            <article class="card">
              <div class="row between">
                <div>
                  <h3>{{ provider.businessName }}</h3>
                  <p class="muted small">
                    {{ provider.categoryName }}
                    @if (provider.city) {
                      &middot; {{ provider.city }}
                    }
                  </p>
                </div>
                <span class="badge" [class]="provider.status">{{ provider.status }}</span>
              </div>

              @if (provider.description) {
                <p class="small">{{ provider.description }}</p>
              }
              <p class="small muted">
                @if (provider.address) {
                  {{ provider.address }}
                }
                @if (provider.phone) {
                  &middot; {{ provider.phone }}
                }
              </p>

              <div class="row">
                <button
                  class="btn small"
                  type="button"
                  (click)="approve(provider.id)"
                  [disabled]="busyId() === provider.id"
                >
                  Approve
                </button>
                <button
                  class="btn danger small"
                  type="button"
                  (click)="reject(provider.id)"
                  [disabled]="busyId() === provider.id"
                >
                  Reject
                </button>
              </div>
            </article>
          }
        </div>
      }
    </div>
  `,
})
export class AdminProvidersComponent implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly providers = signal<ProviderResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly busyId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listPendingProviders().subscribe({
      next: (providers) => {
        this.providers.set(providers);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(describeError(err));
        this.loading.set(false);
      },
    });
  }

  protected approve(id: number): void {
    this.busyId.set(id);
    this.error.set(null);
    this.success.set(null);
    this.api.approveProvider(id).subscribe({
      next: () => {
        this.busyId.set(null);
        this.success.set('Provider approved and now visible in discovery.');
        this.load();
      },
      error: (err) => {
        this.busyId.set(null);
        this.error.set(describeError(err));
      },
    });
  }

  protected reject(id: number): void {
    const reason = prompt('Reason for rejection (optional):') ?? undefined;
    this.busyId.set(id);
    this.error.set(null);
    this.success.set(null);
    this.api.rejectProvider(id, reason || undefined).subscribe({
      next: () => {
        this.busyId.set(null);
        this.success.set('Provider rejected.');
        this.load();
      },
      error: (err) => {
        this.busyId.set(null);
        this.error.set(describeError(err));
      },
    });
  }
}
