import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { describeError } from '../core/http-error';
import { ReminderResponse } from '../models';

@Component({
  selector: 'app-my-reminders',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container">
      <div class="row between">
        <h1>My reminders</h1>
        <button class="btn secondary small" type="button" (click)="load()" [disabled]="loading()">
          Refresh
        </button>
      </div>

      @if (error()) {
        <div class="alert error">{{ error() }}</div>
      }

      @if (loading()) {
        <div class="empty">Loading reminders...</div>
      } @else if (reminders().length === 0) {
        <div class="empty">
          No reminders yet. Add one from your <a routerLink="/appointments">appointments</a>.
        </div>
      } @else {
        <div class="card">
          <table>
            <thead>
              <tr>
                <th>Appointment</th>
                <th>Remind at</th>
                <th>Channel</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (reminder of reminders(); track reminder.id) {
                <tr>
                  <td>#{{ reminder.appointmentId }}</td>
                  <td>{{ reminder.reminderAt | date: 'medium' }}</td>
                  <td>{{ reminder.channel }}</td>
                  <td>
                    <span class="badge" [class]="reminder.status">{{ reminder.status }}</span>
                  </td>
                  <td class="right">
                    @if (reminder.status === 'PENDING') {
                      <button
                        class="btn ghost small"
                        type="button"
                        (click)="cancel(reminder.id)"
                        [disabled]="busyId() === reminder.id"
                      >
                        Cancel
                      </button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .right {
        text-align: right;
      }
    `,
  ],
})
export class MyRemindersComponent implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly reminders = signal<ReminderResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly busyId = signal<number | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listMyReminders().subscribe({
      next: (page) => {
        this.reminders.set(page.content);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(describeError(err));
        this.loading.set(false);
      },
    });
  }

  protected cancel(id: number): void {
    this.busyId.set(id);
    this.api.cancelReminder(id).subscribe({
      next: () => {
        this.busyId.set(null);
        this.load();
      },
      error: (err) => {
        this.busyId.set(null);
        this.error.set(describeError(err));
      },
    });
  }
}
