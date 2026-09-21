import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { describeError } from '../core/http-error';
import { CategoryResponse } from '../models';

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="container">
      <h1>Categories</h1>

      @if (error()) {
        <div class="alert error">{{ error() }}</div>
      }
      @if (success()) {
        <div class="alert success">{{ success() }}</div>
      }

      <div class="grid cols-2">
        <section class="card">
          <h2>All categories</h2>
          @if (loading()) {
            <div class="empty">Loading categories...</div>
          } @else if (categories().length === 0) {
            <div class="empty">No categories yet.</div>
          } @else {
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (category of categories(); track category.id) {
                  <tr>
                    <td>
                      <strong>{{ category.name }}</strong>
                      @if (category.description) {
                        <div class="small muted">{{ category.description }}</div>
                      }
                    </td>
                    <td>
                      <span class="badge" [class]="category.active ? 'ACTIVE' : 'DISABLED'">
                        {{ category.active ? 'ACTIVE' : 'INACTIVE' }}
                      </span>
                    </td>
                    <td class="right">
                      <button class="btn ghost small" type="button" (click)="edit(category)">Edit</button>
                      @if (category.active) {
                        <button class="btn ghost small" type="button" (click)="deactivate(category.id)">
                          Deactivate
                        </button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          }
        </section>

        <section class="card">
          <h2>{{ editingId() ? 'Edit category' : 'Create category' }}</h2>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="field">
              <label for="name">Name</label>
              <input id="name" formControlName="name" />
            </div>
            <div class="field">
              <label for="description">Description</label>
              <textarea id="description" formControlName="description" maxlength="500"></textarea>
            </div>
            <div class="row">
              <button class="btn" type="submit" [disabled]="saving()">
                {{ saving() ? 'Saving...' : editingId() ? 'Save changes' : 'Create' }}
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
  styles: [
    `
      .right {
        text-align: right;
        white-space: nowrap;
      }
    `,
  ],
})
export class AdminCategoriesComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly categories = signal<CategoryResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly editingId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.api.listCategories().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(describeError(err));
        this.loading.set(false);
      },
    });
  }

  protected edit(category: CategoryResponse): void {
    this.editingId.set(category.id);
    this.form.setValue({ name: category.name, description: category.description ?? '' });
  }

  protected resetForm(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', description: '' });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const body = { name: value.name, description: value.description || undefined };
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    const id = this.editingId();
    const request = id ? this.api.updateCategory(id, body) : this.api.createCategory(body);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.success.set(id ? 'Category updated.' : 'Category created.');
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
    if (!confirm('Deactivate this category? Providers keep their historical reference.')) {
      return;
    }
    this.api.deactivateCategory(id).subscribe({
      next: () => {
        this.success.set('Category deactivated.');
        this.load();
      },
      error: (err) => this.error.set(describeError(err)),
    });
  }
}
