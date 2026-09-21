import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { describeError } from '../core/http-error';
import { CategoryResponse, ProviderResponse } from '../models';

@Component({
  selector: 'app-provider-profile',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div class="container">
      <h1>{{ auth.isProvider() ? 'Business profile' : 'Become a provider' }}</h1>

      @if (error()) {
        <div class="alert error">{{ error() }}</div>
      }
      @if (success()) {
        <div class="alert success">{{ success() }}</div>
      }

      @if (profile(); as p) {
        <div class="card">
          <div class="row between">
            <div>
              <h2>{{ p.businessName }}</h2>
              <p class="muted small">
                {{ p.categoryName }}
                @if (p.city) {
                  &middot; {{ p.city }}
                }
              </p>
            </div>
            <span class="badge" [class]="p.status">{{ p.status }}</span>
          </div>
          @if (p.status === 'PENDING') {
            <div class="alert info">
              Your request is awaiting admin approval. You remain a USER until it is approved.
            </div>
          } @else if (p.status === 'REJECTED') {
            <div class="alert error">Your request was rejected. Update the details and save to resubmit.</div>
          }
        </div>
      } @else if (!loading()) {
        <div class="alert info">
          Send a request to offer services. An admin must approve it before you get a provider workspace.
        </div>
      }

      <div class="card">
        <h2>{{ profile() ? 'Update details' : 'Send request' }}</h2>
        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="grid cols-2">
            <div class="field">
              <label for="businessName">Business name</label>
              <input id="businessName" formControlName="businessName" />
            </div>
            <div class="field">
              <label for="categoryId">Category</label>
              <select id="categoryId" formControlName="categoryId">
                <option [ngValue]="null">Select a category</option>
                @for (category of categories(); track category.id) {
                  <option [ngValue]="category.id">{{ category.name }}</option>
                }
              </select>
            </div>
            <div class="field">
              <label for="city">City</label>
              <input id="city" formControlName="city" />
            </div>
            <div class="field">
              <label for="phone">Phone</label>
              <input id="phone" formControlName="phone" />
            </div>
          </div>
          <div class="field">
            <label for="address">Address</label>
            <input id="address" formControlName="address" />
          </div>
          <div class="field">
            <label for="description">Description</label>
            <textarea id="description" formControlName="description" maxlength="1000"></textarea>
          </div>
          <button class="btn" type="submit" [disabled]="saving()">
            {{ saving() ? 'Saving...' : creatingLabel() }}
          </button>
        </form>
      </div>
    </div>
  `,
})
export class ProviderProfileComponent implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  protected readonly profile = signal<ProviderResponse | null>(null);
  protected readonly categories = signal<CategoryResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);

  protected readonly form = this.fb.group({
    businessName: ['', [Validators.required, Validators.maxLength(150)]],
    categoryId: [null as number | null, [Validators.required]],
    city: ['', [Validators.maxLength(120)]],
    phone: ['', [Validators.maxLength(32)]],
    address: ['', [Validators.maxLength(255)]],
    description: ['', [Validators.maxLength(1000)]],
  });

  ngOnInit(): void {
    this.api.listCategories().subscribe({
      next: (categories) => this.categories.set(categories.filter((c) => c.active)),
      error: () => this.categories.set([]),
    });
    this.api.getMyProviderProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.patch(profile);
        this.loading.set(false);
      },
      error: () => {
        this.profile.set(null);
        this.loading.set(false);
      },
    });
  }

  protected creatingLabel(): string {
    if (this.profile()) {
      return this.auth.isProvider() ? 'Save profile' : 'Update request';
    }
    return 'Send request';
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const body = {
      categoryId: value.categoryId as number,
      businessName: value.businessName ?? '',
      city: value.city || undefined,
      phone: value.phone || undefined,
      address: value.address || undefined,
      description: value.description || undefined,
    };

    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    const creating = this.profile() === null;
    const request = creating ? this.api.createProviderProfile(body) : this.api.updateProviderProfile(body);
    request.subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.patch(profile);
        this.saving.set(false);
        this.success.set(
          creating
            ? 'Request sent. You stay a USER until an admin approves it.'
            : 'Profile saved.'
        );
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(describeError(err));
      },
    });
  }

  private patch(profile: ProviderResponse): void {
    this.form.patchValue({
      businessName: profile.businessName,
      categoryId: profile.categoryId,
      city: profile.city ?? '',
      phone: profile.phone ?? '',
      address: profile.address ?? '',
      description: profile.description ?? '',
    });
  }
}
