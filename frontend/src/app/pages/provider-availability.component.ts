import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { ApiService } from '../core/api.service';
import { describeError } from '../core/http-error';
import { BreakResponse, ScheduleResponse, Weekday } from '../models';

@Component({
  selector: 'app-provider-availability',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="container">
      <h1>Availability</h1>
      <p class="muted small">
        Working hours and breaks are stored in UTC. Availability is computed from these windows minus existing
        appointments.
      </p>

      @if (error()) {
        <div class="alert error">{{ error() }}</div>
      }
      @if (success()) {
        <div class="alert success">{{ success() }}</div>
      }

      <div class="grid cols-2">
        <section class="card">
          <h2>Working schedules</h2>
          @if (schedules().length === 0) {
            <div class="empty">No working hours defined yet.</div>
          } @else {
            <table>
              <thead>
                <tr>
                  <th>Day</th>
                  <th>From</th>
                  <th>To</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (schedule of schedules(); track schedule.id) {
                  <tr>
                    <td>{{ schedule.dayOfWeek | titlecase }}</td>
                    <td>{{ schedule.startTime }}</td>
                    <td>{{ schedule.endTime }}</td>
                    <td class="right">
                      <button
                        class="btn ghost small"
                        type="button"
                        (click)="deactivateSchedule(schedule.id)"
                      >
                        Remove
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          }

          <form [formGroup]="scheduleForm" (ngSubmit)="addSchedule()" class="inline-form">
            <div class="field">
              <label for="scheduleDay">Day</label>
              <select id="scheduleDay" formControlName="dayOfWeek">
                @for (day of days; track day) {
                  <option [ngValue]="day">{{ day | titlecase }}</option>
                }
              </select>
            </div>
            <div class="field">
              <label for="scheduleStart">From</label>
              <input id="scheduleStart" type="time" formControlName="startTime" />
            </div>
            <div class="field">
              <label for="scheduleEnd">To</label>
              <input id="scheduleEnd" type="time" formControlName="endTime" />
            </div>
            <button class="btn small" type="submit" [disabled]="saving()">Add</button>
          </form>
        </section>

        <section class="card">
          <h2>Breaks</h2>
          @if (breaks().length === 0) {
            <div class="empty">No breaks defined yet.</div>
          } @else {
            <table>
              <thead>
                <tr>
                  <th>Day</th>
                  <th>From</th>
                  <th>To</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (brk of breaks(); track brk.id) {
                  <tr>
                    <td>{{ brk.dayOfWeek | titlecase }}</td>
                    <td>{{ brk.startTime }}</td>
                    <td>{{ brk.endTime }}</td>
                    <td class="right">
                      <button class="btn ghost small" type="button" (click)="deactivateBreak(brk.id)">
                        Remove
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          }

          <form [formGroup]="breakForm" (ngSubmit)="addBreak()" class="inline-form">
            <div class="field">
              <label for="breakDay">Day</label>
              <select id="breakDay" formControlName="dayOfWeek">
                @for (day of days; track day) {
                  <option [ngValue]="day">{{ day | titlecase }}</option>
                }
              </select>
            </div>
            <div class="field">
              <label for="breakStart">From</label>
              <input id="breakStart" type="time" formControlName="startTime" />
            </div>
            <div class="field">
              <label for="breakEnd">To</label>
              <input id="breakEnd" type="time" formControlName="endTime" />
            </div>
            <button class="btn small" type="submit" [disabled]="saving()">Add</button>
          </form>
        </section>
      </div>
    </div>
  `,
  styles: [
    `
      .right {
        text-align: right;
      }

      .inline-form {
        display: grid;
        grid-template-columns: 1.2fr 1fr 1fr auto;
        gap: 0.6rem;
        align-items: end;
        margin-top: 1rem;
        border-top: 1px dashed var(--border);
        padding-top: 1rem;
      }

      .inline-form .field {
        margin-bottom: 0;
      }

      @media (max-width: 620px) {
        .inline-form {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class ProviderAvailabilityComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly days: Weekday[] = [
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
    'SUNDAY',
  ];

  protected readonly schedules = signal<ScheduleResponse[]>([]);
  protected readonly breaks = signal<BreakResponse[]>([]);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);

  protected readonly scheduleForm = this.fb.nonNullable.group({
    dayOfWeek: ['MONDAY' as Weekday, [Validators.required]],
    startTime: ['09:00', [Validators.required]],
    endTime: ['17:00', [Validators.required]],
  });

  protected readonly breakForm = this.fb.nonNullable.group({
    dayOfWeek: ['MONDAY' as Weekday, [Validators.required]],
    startTime: ['12:00', [Validators.required]],
    endTime: ['13:00', [Validators.required]],
  });

  ngOnInit(): void {
    this.loadSchedules();
    this.loadBreaks();
  }

  protected addSchedule(): void {
    const value = this.scheduleForm.getRawValue();
    this.run(
      () => this.api.createSchedule({ ...value, startTime: seconds(value.startTime), endTime: seconds(value.endTime) }),
      'Working hours added.',
      () => this.loadSchedules()
    );
  }

  protected deactivateSchedule(id: number): void {
    this.run(() => this.api.deactivateSchedule(id), 'Working hours removed.', () => this.loadSchedules());
  }

  protected addBreak(): void {
    const value = this.breakForm.getRawValue();
    this.run(
      () => this.api.createBreak({ ...value, startTime: seconds(value.startTime), endTime: seconds(value.endTime) }),
      'Break added.',
      () => this.loadBreaks()
    );
  }

  protected deactivateBreak(id: number): void {
    this.run(() => this.api.deactivateBreak(id), 'Break removed.', () => this.loadBreaks());
  }

  private loadSchedules(): void {
    this.api.listMySchedules().subscribe({
      next: (schedules) => this.schedules.set(schedules.filter((s) => s.active)),
      error: (err) => this.error.set(describeError(err)),
    });
  }

  private loadBreaks(): void {
    this.api.listMyBreaks().subscribe({
      next: (breaks) => this.breaks.set(breaks.filter((b) => b.active)),
      error: (err) => this.error.set(describeError(err)),
    });
  }

  private run(request: () => Observable<unknown>, message: string, after: () => void): void {
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    request().subscribe({
      next: () => {
        this.saving.set(false);
        this.success.set(message);
        after();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.error.set(describeError(err));
      },
    });
  }
}

function seconds(value: string): string {
  return value.length === 5 ? `${value}:00` : value;
}
