import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AppointmentCreateRequest,
  AppointmentRescheduleRequest,
  AppointmentResponse,
  AvailabilityResponse,
  BreakRequest,
  BreakResponse,
  CategoryRequest,
  CategoryResponse,
  PageResponse,
  ProviderCreateRequest,
  ProviderResponse,
  ProviderServiceRequest,
  ProviderServiceResponse,
  ProviderSummaryResponse,
  ReminderCreateRequest,
  ReminderResponse,
  ScheduleRequest,
  ScheduleResponse,
} from '../models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1';

  listCategories(): Observable<CategoryResponse[]> {
    return this.http.get<CategoryResponse[]>(`${this.base}/categories`);
  }

  createCategory(body: CategoryRequest): Observable<CategoryResponse> {
    return this.http.post<CategoryResponse>(`${this.base}/categories`, body);
  }

  updateCategory(id: number, body: CategoryRequest): Observable<CategoryResponse> {
    return this.http.put<CategoryResponse>(`${this.base}/categories/${id}`, body);
  }

  deactivateCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/categories/${id}`);
  }

  searchProviders(filters: {
    categoryId?: number | null;
    city?: string | null;
    name?: string | null;
    page?: number;
    size?: number;
  }): Observable<PageResponse<ProviderSummaryResponse>> {
    let params = new HttpParams();
    if (filters.categoryId) {
      params = params.set('categoryId', filters.categoryId);
    }
    if (filters.city) {
      params = params.set('city', filters.city);
    }
    if (filters.name) {
      params = params.set('name', filters.name);
    }
    params = params.set('page', filters.page ?? 0).set('size', filters.size ?? 20);
    return this.http.get<PageResponse<ProviderSummaryResponse>>(`${this.base}/providers`, { params });
  }

  getProvider(providerId: number): Observable<ProviderSummaryResponse> {
    return this.http.get<ProviderSummaryResponse>(`${this.base}/providers/${providerId}`);
  }

  getProviderServices(providerId: number): Observable<ProviderServiceResponse[]> {
    return this.http.get<ProviderServiceResponse[]>(`${this.base}/providers/${providerId}/services`);
  }

  getAvailability(
    providerId: number,
    serviceId: number,
    date: string
  ): Observable<AvailabilityResponse> {
    const params = new HttpParams().set('serviceId', serviceId).set('date', date);
    return this.http.get<AvailabilityResponse>(
      `${this.base}/providers/${providerId}/availability`,
      { params }
    );
  }

  createAppointment(
    body: AppointmentCreateRequest,
    idempotencyKey?: string
  ): Observable<AppointmentResponse> {
    const options = idempotencyKey ? { headers: { 'Idempotency-Key': idempotencyKey } } : {};
    return this.http.post<AppointmentResponse>(`${this.base}/appointments`, body, options);
  }

  listMyAppointments(page = 0, size = 20): Observable<PageResponse<AppointmentResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AppointmentResponse>>(`${this.base}/appointments/me`, {
      params,
    });
  }

  getAppointment(id: number): Observable<AppointmentResponse> {
    return this.http.get<AppointmentResponse>(`${this.base}/appointments/${id}`);
  }

  cancelAppointment(id: number): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(`${this.base}/appointments/${id}/cancel`, {});
  }

  rescheduleAppointment(
    id: number,
    body: AppointmentRescheduleRequest
  ): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(`${this.base}/appointments/${id}/reschedule`, body);
  }

  confirmAppointment(id: number): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(`${this.base}/appointments/${id}/confirm`, {});
  }

  completeAppointment(id: number): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(`${this.base}/appointments/${id}/complete`, {});
  }

  markAppointmentNoShow(id: number): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(`${this.base}/appointments/${id}/no-show`, {});
  }

  createReminder(body: ReminderCreateRequest): Observable<ReminderResponse> {
    return this.http.post<ReminderResponse>(`${this.base}/reminders`, body);
  }

  listMyReminders(page = 0, size = 50): Observable<PageResponse<ReminderResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<ReminderResponse>>(`${this.base}/reminders/me`, { params });
  }

  cancelReminder(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/reminders/${id}`);
  }

  getMyProviderProfile(): Observable<ProviderResponse> {
    return this.http.get<ProviderResponse>(`${this.base}/providers/me`);
  }

  createProviderProfile(body: ProviderCreateRequest): Observable<ProviderResponse> {
    return this.http.post<ProviderResponse>(`${this.base}/providers`, body);
  }

  updateProviderProfile(body: ProviderCreateRequest): Observable<ProviderResponse> {
    return this.http.put<ProviderResponse>(`${this.base}/providers/me`, body);
  }

  listMyServices(): Observable<ProviderServiceResponse[]> {
    return this.http.get<ProviderServiceResponse[]>(`${this.base}/providers/me/services`);
  }

  createService(body: ProviderServiceRequest): Observable<ProviderServiceResponse> {
    return this.http.post<ProviderServiceResponse>(`${this.base}/providers/me/services`, body);
  }

  updateService(
    serviceId: number,
    body: ProviderServiceRequest
  ): Observable<ProviderServiceResponse> {
    return this.http.put<ProviderServiceResponse>(
      `${this.base}/providers/me/services/${serviceId}`,
      body
    );
  }

  deactivateService(serviceId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/providers/me/services/${serviceId}`);
  }

  listMySchedules(): Observable<ScheduleResponse[]> {
    return this.http.get<ScheduleResponse[]>(`${this.base}/providers/me/schedules`);
  }

  createSchedule(body: ScheduleRequest): Observable<ScheduleResponse> {
    return this.http.post<ScheduleResponse>(`${this.base}/providers/me/schedules`, body);
  }

  updateSchedule(scheduleId: number, body: ScheduleRequest): Observable<ScheduleResponse> {
    return this.http.put<ScheduleResponse>(
      `${this.base}/providers/me/schedules/${scheduleId}`,
      body
    );
  }

  deactivateSchedule(scheduleId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/providers/me/schedules/${scheduleId}`);
  }

  listMyBreaks(): Observable<BreakResponse[]> {
    return this.http.get<BreakResponse[]>(`${this.base}/providers/me/breaks`);
  }

  createBreak(body: BreakRequest): Observable<BreakResponse> {
    return this.http.post<BreakResponse>(`${this.base}/providers/me/breaks`, body);
  }

  updateBreak(breakId: number, body: BreakRequest): Observable<BreakResponse> {
    return this.http.put<BreakResponse>(`${this.base}/providers/me/breaks/${breakId}`, body);
  }

  deactivateBreak(breakId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/providers/me/breaks/${breakId}`);
  }

  listPendingProviders(): Observable<ProviderResponse[]> {
    return this.http.get<ProviderResponse[]>(`${this.base}/admin/providers/pending`);
  }

  approveProvider(providerId: number): Observable<ProviderResponse> {
    return this.http.post<ProviderResponse>(
      `${this.base}/admin/providers/${providerId}/approve`,
      {}
    );
  }

  rejectProvider(providerId: number, reason?: string): Observable<ProviderResponse> {
    return this.http.post<ProviderResponse>(`${this.base}/admin/providers/${providerId}/reject`, {
      reason,
    });
  }
}
