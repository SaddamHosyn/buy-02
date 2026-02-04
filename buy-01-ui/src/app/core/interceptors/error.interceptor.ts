import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { NotificationService } from '../services/notification.service';
import { Auth } from '../services/auth';

/**
 * HTTP Error Interceptor
 * Intercepts HTTP errors and handles them appropriately with consistent error handling
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(Auth);
  const notificationService = inject(NotificationService);
  
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An unexpected error occurred';
      let shouldShowNotification = true;

      // Handle specific HTTP error codes
      switch (error.status) {
        case 0:
          // Network error - no connection to server
          errorMessage = 'Cannot connect to server. Please check your internet connection.';
          notificationService.networkError();
          shouldShowNotification = false;
          break;

        case 400:
          // Bad Request - Validation errors
          errorMessage = extractErrorMessage(error) || 'Invalid request. Please check your input.';
          // Don't show notification for 400 - let components handle inline validation
          shouldShowNotification = false;
          break;

        case 401:
          // Unauthorized - logout and redirect to login
          errorMessage = 'Your session has expired. Please login again.';
          authService.logout();
          router.navigate(['/auth/login']);
          notificationService.authError();
          shouldShowNotification = false;
          break;
        
        case 403:
          // Forbidden - permission denied
          errorMessage = 'You do not have permission to perform this action.';
          notificationService.permissionError();
          shouldShowNotification = false;
          break;
        
        case 404:
          // Not found - show specific message if available
          errorMessage = extractErrorMessage(error) || 'The requested resource was not found.';
          if (error.error?.message) {
            notificationService.error(error.error.message);
            shouldShowNotification = false;
          }
          break;

        case 409:
          // Conflict - duplicate or state conflict
          errorMessage = extractErrorMessage(error) || 'This action conflicts with existing data.';
          break;

        case 422:
          // Unprocessable Entity - Business logic error
          errorMessage = extractErrorMessage(error) || 'Unable to process your request.';
          break;

        case 429:
          // Too Many Requests
          errorMessage = 'Too many requests. Please try again later.';
          break;
        
        case 500:
          // Internal Server Error
          errorMessage = 'Server error occurred. Please try again later.';
          console.error('Server error:', error);
          break;

        case 502:
        case 503:
        case 504:
          // Bad Gateway, Service Unavailable, Gateway Timeout
          errorMessage = 'Service temporarily unavailable. Please try again later.';
          break;

        default:
          // Other errors - log for debugging
          errorMessage = extractErrorMessage(error) || `Error ${error.status}: ${error.statusText}`;
          console.error('Unhandled HTTP error:', error);
      }

      // Show notification if needed
      if (shouldShowNotification) {
        notificationService.error(errorMessage);
      }

      // Return structured error object
      return throwError(() => ({
        status: error.status,
        message: errorMessage,
        originalError: error,
        details: extractErrorDetails(error)
      }));
    })
  );
};

/**
 * Extract error message from various error response formats
 */
function extractErrorMessage(error: HttpErrorResponse): string | null {
  if (error.error) {
    // Format: { message: "..." }
    if (typeof error.error.message === 'string') {
      return error.error.message;
    }
    
    // Format: { error: "...", reason: "..." }
    if (typeof error.error.error === 'string') {
      const msg = error.error.error;
      if (error.error.reason) {
        return `${msg}: ${error.error.reason}`;
      }
      return msg;
    }
    
    // Format: { reason: "..." }
    if (typeof error.error.reason === 'string') {
      return error.error.reason;
    }
    
    // If error is a string
    if (typeof error.error === 'string') {
      try {
        const parsed = JSON.parse(error.error);
        if (parsed.message) return parsed.message;
      } catch {
        return error.error;
      }
    }
  }
  
  return null;
}

/**
 * Extract detailed error information for debugging
 */
function extractErrorDetails(error: HttpErrorResponse): any {
  if (error.error) {
    // Return validation errors if present
    if (error.error.details) {
      return error.error.details;
    }
    
    // Return errors array if present
    if (Array.isArray(error.error.errors)) {
      return error.error.errors;
    }
    
    // Return fields errors if present
    if (error.error.fields) {
      return error.error.fields;
    }
  }
  
  return null;
}

