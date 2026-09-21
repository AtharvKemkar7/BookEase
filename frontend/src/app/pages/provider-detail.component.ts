import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { describeError } from '../core/http-error';
import {
  AppointmentResponse,
  AvailabilitySlot,
  ProviderServiceResponse,
  ProviderSummaryResponse,
} from '../models';

@Component({
  selector: 'app-provider-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="container">
      <a class="btn ghost small back" routerLink="/">Back to discovery</a>

      @if (error()) {
        <div class="alert error">{{ error() }}</div>
      }

      @if (provider(); as p) {
        <div class="card header-card">
          <div>
            <span class="badge">{{ p.categoryName }}</span>
            <h1>{{ p.businessName }}</h1>
            <p class="muted">{{ p.description || 'No description provided.' }}</p>
            @if (p.city) {
              <p class="small muted">City: {{ p.city }}</p>
            }
          </div>
        </div>

        @if (booked(); as appointment) {
          <div class="card">
            <div class="alert success">
              Appointment confirmed for
              <strong>{{ appointment.startAt | date: 'full' }}</strong> with
              <strong>{{ appointment.providerName }}</strong>.
            </div>
            <p class="small muted">
              Reference #{{ appointment.id }}. Status: {{ appointment.status }}.
            </p>
            <a class="btn" routerLink="/appointments">Go to my appointments</a>
          </div>
        } @else {
          <div class="grid cols-2 booking">
            <section class="card">
              <h2>1. Choose a service</h2>
              @if (services().length === 0) {
                <div class="empty">This provider has no active services yet.</div>
              } @else {
                <div class="stack">
                  @for (service of services(); track service.id) {
                    <button
                      type="button"
                      class="service-option"
                      [class.selected]="selectedService()?.id === service.id"
                      (click)="selectService(service)"
                    >
                      <span>
                        <strong>{{ service.name }}</strong>
                        <span class="muted small block">
                          {{ service.durationMinutes }} min
                          @if (service.description) {
                            - {{ service.description }}
                          }
                        </span>
                      </span>
                      <span class="price">{{ service.price | number: '1.2-2' }}</span>
                    </button>
                  }
                </div>
              }
            </section>

            <section class="card">
              <h2>2. Pick a date</h2>
              <div class="field">
                <label for="date">Date</label>
                <input
                  id="date"
                  type="date"
                  [value]="date()"
                  (change)="onDateChange($event)"
                  [min]="today"
                />
              </div>

              @if (!selectedService()) {
                <div class="empty">Select a service to see available slots.</div>
              } @else if (loadingSlots()) {
                <div class="empty">Loading availability...</div>
              } @else if (slots().length === 0) {
                <div class="empty">No open slots on this date. Try another day.</div>
              } @else {
                <div class="slot-grid">
                  @for (slot of slots(); track slot.startAt) {
                    <button
                      type="button"
                      class="slot"
                      [class.selected]="selectedSlot() === slot.startAt"
                      (click)="selectedSlot.set(slot.startAt)"
                    >
                      {{ slot.startAt | date: 'shortTime' }}
                    </button>
                  }
                </div>
                <p class="small muted slots-note">
                  Times are shown in your local timezone. Availability is revalidated at booking time.
                </p>
              }
            </section>
          </div>

          <section class="card">
            <h2>3. Confirm</h2>
            @if (!auth.isAuthenticated()) {
              <div class="alert info">
                Please <a routerLink="/login">sign in</a> to complete your booking.
              </div>
            }

            @if (bookingError()) {
              <div class="alert error">{{ bookingError() }}</div>
            }

            <div class="confirm-grid">
              <div class="field">
                <label for="notes">Notes for the provider (optional)</label>
                <textarea
                  id="notes"
                  [(ngModel)]="notes"
                  maxlength="1000"
                  placeholder="Anything the provider should know?"
                ></textarea>
              </div>
              <div class="field">
                <label for="reminder">Reminder</label>
                <select id="reminder" [(ngModel)]="reminderOffset">
                  <option [ngValue]="null">No reminder</option>
                  <option [ngValue]="15">15 minutes before</option>
                  <option [ngValue]="60">1 hour before</option>
                  <option [ngValue]="1440">1 day before</option>
                </select>
              </div>
            </div>

            <div class="summary small muted">
              @if (selectedService(); as s) {
                <span>{{ s.name }} ({{ s.durationMinutes }} min)</span>
              }
              @if (selectedSlot()) {
                <span>on {{ selectedSlot() | date: 'full' }}</span>
              }
            </div>

            <button
              class="btn"
              type="button"
              [disabled]="!canBook() || booking()"
              (click)="book()"
            >
              {{ booking() ? 'Booking...' : 'Book appointment' }}
            </button>
          </section>
        }
      } @else if (!error()) {
        <div class="empty">Loading provider...</div>
      }
    </div>
  `,
  styles: [
    `
      .back {
        margin-bottom: 1rem;
      }

      .header-card {
        margin-bottom: 1rem;
      }

      .booking {
        margin-bottom: 1rem;
      }

      .service-option {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 1rem;
        width: 100%;
        text-align: left;
        background: var(--surface);
        border: 1px solid var(--border);
        border-radius: var(--radius-sm);
        padding: 0.7rem 0.85rem;
        cursor: pointer;
        font: inherit;
        color: inherit;
        transition: border-color 0.15s, background 0.15s;
      }

      .service-option:hover {
        border-color: var(--primary);
      }

      .service-option.selected {
        border-color: var(--primary);
        background: var(--primary-soft);
      }

      .block {
        display: block;
      }

      .price {
        font-weight: 700;
        white-space: nowrap;
      }

      .slots-note {
        margin-top: 0.75rem;
      }

      .confirm-grid {
        display: grid;
        grid-template-columns: 2fr 1fr;
        gap: 1rem;
      }

      .summary {
        display: flex;
        gap: 0.75rem;
        flex-wrap: wrap;
        margin: 0.5rem 0 1rem;
      }

      @media (max-width: 720px) {
        .confirm-grid {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class ProviderDetailComponent implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  readonly id = input.required<string>();

  protected readonly provider = signal<ProviderSummaryResponse | null>(null);
  protected readonly services = signal<ProviderServiceResponse[]>([]);
  protected readonly slots = signal<AvailabilitySlot[]>([]);
  protected readonly selectedService = signal<ProviderServiceResponse | null>(null);
  protected readonly selectedSlot = signal<string | null>(null);
  protected readonly loadingSlots = signal(false);
  protected readonly booking = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly bookingError = signal<string | null>(null);
  protected readonly booked = signal<AppointmentResponse | null>(null);

  protected readonly today = new Date().toISOString().slice(0, 10);
  protected readonly date = signal(this.today);
  protected readonly canBook = computed(
    () => this.auth.isAuthenticated() && this.selectedService() !== null && this.selectedSlot() !== null
  );

  protected notes = '';
  protected reminderOffset: number | null = null;

  ngOnInit(): void {
    const providerId = Number(this.id());
    this.api.getProvider(providerId).subscribe({
      next: (provider) => this.provider.set(provider),
      error: (err) => this.error.set(describeError(err)),
    });
    this.api.getProviderServices(providerId).subscribe({
      next: (services) => this.services.set(services.filter((s) => s.active)),
      error: () => this.services.set([]),
    });
  }

  protected selectService(service: ProviderServiceResponse): void {
    this.selectedService.set(service);
    this.selectedSlot.set(null);
    this.loadSlots();
  }

  protected onDateChange(event: Event): void {
    this.date.set((event.target as HTMLInputElement).value);
    this.selectedSlot.set(null);
    this.loadSlots();
  }

  protected book(): void {
    const service = this.selectedService();
    const startAt = this.selectedSlot();
    if (!service || !startAt) {
      return;
    }
    this.booking.set(true);
    this.bookingError.set(null);
    const idempotencyKey = crypto.randomUUID();

    this.api
      .createAppointment(
        {
          providerId: Number(this.id()),
          serviceId: service.id,
          startAt,
          notes: this.notes || undefined,
        },
        idempotencyKey
      )
      .subscribe({
        next: (appointment) => {
          this.booked.set(appointment);
          this.booking.set(false);
          if (this.reminderOffset) {
            this.createReminder(appointment, this.reminderOffset);
          }
        },
        error: (err) => {
          this.bookingError.set(describeError(err));
          this.booking.set(false);
          this.loadSlots();
        },
      });
  }

  private createReminder(appointment: AppointmentResponse, offsetMinutes: number): void {
    const reminderAt = new Date(
      new Date(appointment.startAt).getTime() - offsetMinutes * 60_000
    ).toISOString();
    this.api
      .createReminder({ appointmentId: appointment.id, reminderAt, channel: 'IN_APP' })
      .subscribe({ error: () => undefined });
  }

  private loadSlots(): void {
    const service = this.selectedService();
    if (!service) {
      return;
    }
    this.loadingSlots.set(true);
    this.api.getAvailability(Number(this.id()), service.id, this.date()).subscribe({
      next: (availability) => {
        this.slots.set(availability.slots);
        this.loadingSlots.set(false);
      },
      error: (err) => {
        this.error.set(describeError(err));
        this.slots.set([]);
        this.loadingSlots.set(false);
      },
    });
  }
}
