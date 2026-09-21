export type UserRole = 'USER' | 'PROVIDER' | 'ADMIN';
export type UserStatus = 'ACTIVE' | 'DISABLED';
export type ProviderStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED';
export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW';
export type ReminderChannel = 'IN_APP' | 'EMAIL';
export type ReminderStatus = 'PENDING' | 'SENT' | 'FAILED' | 'CANCELLED';
export type Weekday =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export interface UserResponse {
  id: number;
  name: string;
  email: string;
  phone?: string;
  role: UserRole;
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CategoryResponse {
  id: number;
  name: string;
  description?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProviderSummaryResponse {
  id: number;
  businessName: string;
  description?: string;
  city?: string;
  categoryId: number;
  categoryName: string;
}

export interface ProviderResponse {
  id: number;
  userId: number;
  businessName: string;
  description?: string;
  address?: string;
  city?: string;
  phone?: string;
  status: ProviderStatus;
  categoryId: number;
  categoryName: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProviderServiceResponse {
  id: number;
  providerId: number;
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ScheduleResponse {
  id: number;
  providerId: number;
  dayOfWeek: Weekday;
  startTime: string;
  endTime: string;
  active: boolean;
}

export interface BreakResponse {
  id: number;
  providerId: number;
  dayOfWeek: Weekday;
  startTime: string;
  endTime: string;
  active: boolean;
}

export interface AvailabilitySlot {
  startAt: string;
  endAt: string;
}

export interface AvailabilityResponse {
  providerId: number;
  serviceId: number;
  date: string;
  durationMinutes: number;
  slots: AvailabilitySlot[];
}

export interface AppointmentResponse {
  id: number;
  userId: number;
  providerId: number;
  providerName: string;
  serviceId: number;
  serviceName: string;
  startAt: string;
  endAt: string;
  status: AppointmentStatus;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ReminderResponse {
  id: number;
  appointmentId: number;
  userId: number;
  reminderAt: string;
  channel: ReminderChannel;
  status: ReminderStatus;
  sentAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ApiError {
  timestamp?: string;
  correlationId?: string;
  status: number;
  code: string;
  message: string;
  path?: string;
  details?: string[];
}

export interface CategoryRequest {
  name: string;
  description?: string;
}

export interface ProviderCreateRequest {
  categoryId: number;
  businessName: string;
  description?: string;
  address?: string;
  city?: string;
  phone?: string;
}

export interface ProviderServiceRequest {
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
}

export interface ScheduleRequest {
  dayOfWeek: Weekday;
  startTime: string;
  endTime: string;
}

export interface BreakRequest {
  dayOfWeek: Weekday;
  startTime: string;
  endTime: string;
}

export interface AppointmentCreateRequest {
  providerId: number;
  serviceId: number;
  startAt: string;
  notes?: string;
}

export interface AppointmentRescheduleRequest {
  startAt: string;
  notes?: string;
}

export interface ReminderCreateRequest {
  appointmentId: number;
  reminderAt: string;
  channel: ReminderChannel;
}
