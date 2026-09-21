import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { describeError } from '../core/http-error';
import { AppointmentResponse, AvailabilitySlot } from '../models';

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="container">
      <div class="row between">
        <h1>{{ auth.isProvider() ? 'Incoming appointments' : 'My appointments' }}</h1>
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
        <div class="empty">Loading appointments...</div>
      } @else if (appointments().length === 0) {
        <div class="empty">
          @if (auth.isProvider()) {
            No incoming appointments yet.
          } @else {
            You have no appointments yet. <a routerLink="/">Find a provider</a> to book one.
          }
        </div>
      } @else {
        <div class="stack">
          @for (appointment of appointments(); track appointment.id) {
            <article class="card appointment">
              <div class="row between">
                <div>
                  <h3>{{ appointment.serviceName }}</h3>
                  <p class="muted small">
                    {{ appointment.providerName }} &middot;
                    {{ appointment.startAt | date: 'full' }}
                  </p>
                  @if (appointment.notes) {
                    <p class="small muted">Notes: {{ appointment.notes }}</p>
                  }
                </div>
                <span class="badge" [class]="appointment.status">{{ appointment.status }}</span>
              </div>

              <div class="row actions">
                @if (isActive(appointment)) {
                  <button
                    class="btn secondary small"
                    type="button"
                    (click)="toggleReschedule(appointment)"
                  >
                    {{ rescheduleId() === appointment.id ? 'Close' : 'Reschedule' }}
                  </button>
                  @if (!auth.isProvider()) {
                    <button
                      class="btn secondary small"
                      type="button"
                      (click)="addReminder(appointment)"
                    >
                      Add reminder
                    </button>
                  }
                  @if (auth.isProvider() && appointment.status === 'PENDING') {
                    <button
                      class="btn small"
                      type="button"
                      (click)="confirm(appointment.id)"
                      [disabled]="busyId() === appointment.id"
                    >
                      Confirm
                    </button>
                  }
                  @if (auth.isProvider() && appointment.status === 'CONFIRMED') {
                    <button
                      class="btn small"
                      type="button"
                      (click)="complete(appointment.id)"
                      [disabled]="busyId() === appointment.id"
                    >
                      Complete
                    </button>
                  }
                  @if (auth.isProvider() && isActive(appointment)) {
                    <button
                      class="btn secondary small"
                      type="button"
                      (click)="noShow(appointment.id)"
                      [disabled]="busyId() === appointment.id"
                    >
                      No-show
                    </button>
                  }
                  <button
                    class="btn danger small"
                    type="button"
                    (click)="cancel(appointment.id)"
                    [disabled]="busyId() === appointment.id"
                  >
                    Cancel
                  </button>
                }
              </div>

              @if (rescheduleId() === appointment.id) {
                <div class="reschedule">
                  <div class="field">
                    <label [for]="'date-' + appointment.id">New date</label>
                    <input
                      [id]="'date-' + appointment.id"
                      type="date"
                      [(ngModel)]="rescheduleDate"
                      [min]="today"
                      (change)="loadSlots(appointment)"
                    />
                  </div>

                  @if (slotsLoading()) {
                    <div class="empty">Loading slots...</div>
                  } @else if (slots().length === 0) {
                    <div class="empty">No open slots on this date.</div>
                  } @else {
                    <div class="slot-grid">
                      @for (slot of slots(); track slot.startAt) {
                        <button
                          type="button"
                          class="slot"
                          [class.selected]="rescheduleSlot() === slot.startAt"
                          (click)="rescheduleSlot.set(slot.startAt)"
                        >
                          {{ slot.startAt | date: 'shortTime' }}
                        </button>
                      }
                    </div>
                    <button
                      class="btn small confirm"
                      type="button"
                      [disabled]="!rescheduleSlot()"
                      (click)="submitReschedule(appointment)"
                    >
                      Confirm new time
                    </button>
                  }
                </div>
              }
            </article>
          }
        </div>
      }
    </div>
  `,
  styles: [
    `
      .appointment h3 {
        margin: 0;
      }

      .appointment p {
        margin: 0.15rem 0 0;
      }

      .actions {
        margin-top: 0.75rem;
      }

      .reschedule {
        margin-top: 1rem;
        border-top: 1px dashed var(--border);
        padding-top: 1rem;
      }

      .confirm {
        margin-top: 0.85rem;
      }
    `,
  ],
})
export class MyAppointmentsComponent implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  protected readonly appointments = signal<AppointmentResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected readonly busyId = signal<number | null>(null);
  protected readonly rescheduleId = signal<number | null>(null);
  protected readonly slots = signal<AvailabilitySlot[]>([]);
  protected readonly slotsLoading = signal(false);
  protected readonly rescheduleSlot = signal<string | null>(null);

  protected readonly today = new Date().toISOString().slice(0, 10);
  protected rescheduleDate = this.today;

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listMyAppointments(0, 100).subscribe({
      next: (page) => {
        this.appointments.set(page.content);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(describeError(err));
        this.loading.set(false);
      },
    });
  }

  protected isActive(appointment: AppointmentResponse): boolean {
    return appointment.status === 'PENDING' || appointment.status === 'CONFIRMED';
  }

  protected cancel(id: number): void {
    if (!confirm('Cancel this appointment?')) {
      return;
    }
    this.busyId.set(id);
    this.success.set(null);
    this.error.set(null);
    this.api.cancelAppointment(id).subscribe({
      next: () => {
        this.busyId.set(null);
        this.success.set('Appointment cancelled.');
        this.load();
      },
      error: (err) => {
        this.busyId.set(null);
        this.error.set(describeError(err));
      },
    });
  }

  protected toggleReschedule(appointment: AppointmentResponse): void {
    if (this.rescheduleId() === appointment.id) {
      this.rescheduleId.set(null);
      return;
    }
    this.rescheduleId.set(appointment.id);
    this.rescheduleSlot.set(null);
    this.slots.set([]);
    this.rescheduleDate = appointment.startAt.slice(0, 10);
    this.loadSlots(appointment);
  }

  protected confirm(id: number): void {
    this.runAction(id, () => this.api.confirmAppointment(id), 'Appointment confirmed.');
  }

  protected complete(id: number): void {
    this.runAction(id, () => this.api.completeAppointment(id), 'Appointment completed.');
  }

  protected noShow(id: number): void {
    if (!confirm('Mark this appointment as no-show?')) {
      return;
    }
    this.runAction(id, () => this.api.markAppointmentNoShow(id), 'Appointment marked as no-show.');
  }

  private runAction(
    id: number,
    request: () => ReturnType<ApiService['confirmAppointment']>,
    message: string
  ): void {
    this.busyId.set(id);
    this.error.set(null);
    this.success.set(null);
    request().subscribe({
      next: () => {
        this.busyId.set(null);
        this.success.set(message);
        this.load();
      },
      error: (err) => {
        this.busyId.set(null);
        this.error.set(describeError(err));
      },
    });
  }

  protected loadSlots(appointment: AppointmentResponse): void {
    this.slotsLoading.set(true);
    this.rescheduleSlot.set(null);
    this.api.getAvailability(appointment.providerId, appointment.serviceId, this.rescheduleDate).subscribe({
      next: (availability) => {
        this.slots.set(availability.slots);
        this.slotsLoading.set(false);
      },
      error: (err) => {
        this.error.set(describeError(err));
        this.slots.set([]);
        this.slotsLoading.set(false);
      },
    });
  }

  protected submitReschedule(appointment: AppointmentResponse): void {
    const startAt = this.rescheduleSlot();
    if (!startAt) {
      return;
    }
    this.busyId.set(appointment.id);
    this.error.set(null);
    this.success.set(null);
    this.api
      .rescheduleAppointment(appointment.id, { startAt, notes: appointment.notes })
      .subscribe({
        next: () => {
          this.busyId.set(null);
          this.rescheduleId.set(null);
          this.success.set('Appointment rescheduled.');
          this.load();
        },
        error: (err) => {
          this.busyId.set(null);
          this.error.set(describeError(err));
        },
      });
  }

  protected addReminder(appointment: AppointmentResponse): void {
    const offset = prompt('Remind me how many minutes before the appointment?', '60');
    if (offset === null) {
      return;
    }
    const minutes = Number(offset);
    if (!Number.isFinite(minutes) || minutes <= 0) {
      this.error.set('Enter a positive number of minutes.');
      return;
    }
    const reminderAt = new Date(
      new Date(appointment.startAt).getTime() - minutes * 60_000
    ).toISOString();
    this.error.set(null);
    this.success.set(null);
    this.api.createReminder({ appointmentId: appointment.id, reminderAt, channel: 'IN_APP' }).subscribe({
      next: () => this.success.set('Reminder created.'),
      error: (err) => this.error.set(describeError(err)),
    });
  }
}
