import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { describeError } from '../core/http-error';
import { CategoryResponse, ProviderSummaryResponse } from '../models';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="hero">
      <div class="container">
        <h1>Find and book trusted local services</h1>
        <p class="muted">
          Browse approved providers, check real availability and book an appointment in seconds.
        </p>

        <div class="search card">
          <div class="field">
            <label for="name">Service or business name</label>
            <input
              id="name"
              [(ngModel)]="name"
              placeholder="e.g. Salon"
              (keyup.enter)="search()"
            />
          </div>
          <div class="field">
            <label for="city">City</label>
            <input id="city" [(ngModel)]="city" placeholder="e.g. Berlin" (keyup.enter)="search()" />
          </div>
          <div class="field">
            <label for="category">Category</label>
            <select id="category" [(ngModel)]="categoryId">
              <option [ngValue]="null">All categories</option>
              @for (category of categories(); track category.id) {
                <option [ngValue]="category.id">{{ category.name }}</option>
              }
            </select>
          </div>
          <button class="btn" type="button" (click)="search()" [disabled]="loading()">
            {{ loading() ? 'Searching...' : 'Search' }}
          </button>
        </div>
      </div>
    </section>

    <div class="container">
      @if (error()) {
        <div class="alert error">{{ error() }}</div>
      }

      <div class="row between results-header">
        <h2>Providers</h2>
        <span class="muted small">{{ total() }} result(s)</span>
      </div>

      @if (loading()) {
        <div class="empty">Loading providers...</div>
      } @else if (providers().length === 0) {
        <div class="empty">No providers match your search yet.</div>
      } @else {
        <div class="grid cols-3">
          @for (provider of providers(); track provider.id) {
            <article class="card provider-card">
              <span class="badge">{{ provider.categoryName }}</span>
              <h3>{{ provider.businessName }}</h3>
              <p class="muted small grow">
                {{ provider.description || 'No description provided.' }}
              </p>
              @if (provider.city) {
                <p class="small muted city">City: {{ provider.city }}</p>
              }
              <a class="btn small" [routerLink]="['/providers', provider.id]">
                View & book
              </a>
            </article>
          }
        </div>
      }
    </div>
  `,
  styles: [
    `
      .hero {
        background: linear-gradient(180deg, #ffffff 0%, #eef2f9 100%);
        border-bottom: 1px solid var(--border);
        padding: 2rem 0 1.5rem;
      }

      .search {
        display: grid;
        grid-template-columns: 2fr 1fr 1fr auto;
        gap: 0.75rem;
        align-items: end;
        margin-top: 1rem;
      }

      .search .field {
        margin-bottom: 0;
      }

      .search .btn {
        height: 42px;
      }

      .results-header {
        margin: 1.5rem 0 1rem;
      }

      .provider-card {
        display: flex;
        flex-direction: column;
        gap: 0.4rem;
      }

      .provider-card h3 {
        margin: 0;
      }

      .provider-card .grow {
        flex: 1;
      }

      .provider-card .city {
        margin: 0;
      }

      @media (max-width: 820px) {
        .search {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class HomeComponent implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly categories = signal<CategoryResponse[]>([]);
  protected readonly providers = signal<ProviderSummaryResponse[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected name = '';
  protected city = '';
  protected categoryId: number | null = null;

  ngOnInit(): void {
    this.api.listCategories().subscribe({
      next: (categories) => this.categories.set(categories.filter((c) => c.active)),
      error: () => this.categories.set([]),
    });
    this.search();
  }

  protected search(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api
      .searchProviders({
        name: this.name || null,
        city: this.city || null,
        categoryId: this.categoryId,
        size: 50,
      })
      .subscribe({
        next: (page) => {
          this.providers.set(page.content);
          this.total.set(page.totalElements);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(describeError(err));
          this.loading.set(false);
        },
      });
  }
}
