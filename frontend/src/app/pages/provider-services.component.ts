import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { describeError } from '../core/http-error';
import { ProviderServiceResponse } from '../models';

@Component({
  selector: 'app-provider-services',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="container">
      <h1>My services</h1>

      @if (error()) {
        <div class="alert error">{{ error() }}</div>
      }
      @if (success()) {
        <div class="alert success">{{ success() }}</div>
      }

      <div class="grid cols-2">
        <section class="card">
          <h2>Service offerings</h2>
          @if (loading()) {
            <div class="empty">Loading services...</div>
          } @else if (services().length === 0) {
            <div class="empty">No services yet. Add your first offering.</div>
          } @else {
            <div class="stack">
              @for (service of services(); track service.id) {
                <div class="list-item">
                  <div>
                    <strong>{{ service.name }}</strong>
                    <span class="badge" [class]="service.active ? 'ACTIVE' : 'DISABLED'">
                      {{ service.active ? 'ACTIVE' : 'INACTIVE' }}
                    </span>
                    <div class="small muted">
                      {{ service.durationMinutes }} min &middot; {{ service.price | number: '1.2-2' }}
                    </div>
                    @if (service.description) {
                      <div class="small muted">{{ service.description }}</div>
                    }
                  </div>
                  <div class="row">
                    <button class="btn ghost small" type="button" (click)="edit(service)">Edit</button>
                    @if (service.active) {
                      <button
                        class="btn ghost small"
                        type="button"
                        (click)="deactivate(service.id)"
                      >
                        Deactivate
                      </button>
                    }
                  </div>
                </div>
              }
            </div>
          }
        </section>

        <section class="card">
          <h2>{{ editingId() ? 'Edit service' : 'Add service' }}</h2>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="field">
              <label for="name">Name</label>
              <input id="name" formControlName="name" />
            </div>
            <div class="field">
              <label for="description">Description</label>
              <textarea id="description" formControlName="description" maxlength="1000"></textarea>
            </div>
            <div class="field">
              <label for="durationMinutes">Duration (minutes)</label>
              <input id="durationMinutes" type="number" formControlName="durationMinutes" min="5" max="1440" />
            </div>
            <div class="field">
              <label for="price">Price</label>
              <input id="price" type="number" formControlName="price" min="0" step="0.01" />
            </div>
            <div class="row">
              <button class="btn" type="submit" [disabled]="saving()">
                {{ saving() ? 'Saving...' : editingId() ? 'Save changes' : 'Add service' }}
              </button>
              @if (editingId()) {
                <button class="btn secondary" type="button" (click)="resetForm()">Cancel</button>
              }
            </div>
          </form>
        </section>
      </div>
    </div>
  `,
})
export class ProviderServicesComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly services = signal<ProviderServiceResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly editingId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);

  protected readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.maxLength(1000)]],
    durationMinutes: [60, [Validators.required, Validators.min(5), Validators.max(1440)]],
    price: [0, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.api.listMyServices().subscribe({
      next: (services) => {
        this.services.set(services);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(describeError(err));
        this.loading.set(false);
      },
    });
  }

  protected edit(service: ProviderServiceResponse): void {
    this.editingId.set(service.id);
    this.form.patchValue({
      name: service.name,
      description: service.description ?? '',
      durationMinutes: service.durationMinutes,
      price: service.price,
    });
  }

  protected resetForm(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', description: '', durationMinutes: 60, price: 0 });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const body = {
      name: value.name ?? '',
      description: value.description || undefined,
      durationMinutes: Number(value.durationMinutes),
      price: Number(value.price),
    };
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    const id = this.editingId();
    const request = id ? this.api.updateService(id, body) : this.api.createService(body);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.success.set(id ? 'Service updated.' : 'Service added.');
        this.resetForm();
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(describeError(err));
      },
    });
  }

  protected deactivate(id: number): void {
    if (!confirm('Deactivate this service? It will no longer be bookable.')) {
      return;
    }
    this.api.deactivateService(id).subscribe({
      next: () => {
        this.success.set('Service deactivated.');
        this.load();
      },
      error: (err) => this.error.set(describeError(err)),
    });
  }
}
