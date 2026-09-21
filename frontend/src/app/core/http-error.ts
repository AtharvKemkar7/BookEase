import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models';

export function describeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as ApiError | string | null;
    if (body && typeof body === 'object' && body.message) {
      const details = body.details?.length ? ` (${body.details.join(', ')})` : '';
      return `${body.message}${details}`;
    }
    if (typeof body === 'string' && body.trim()) {
      return body;
    }
    if (error.status === 0) {
      return 'Cannot reach the BookEase API. Is the backend running?';
    }
    return `Request failed with status ${error.status}`;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Unexpected error';
}
