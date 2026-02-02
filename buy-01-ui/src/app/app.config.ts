import { ApplicationConfig, provideZoneChangeDetection, ErrorHandler } from '@angular/core';
import { provideRouter, withComponentInputBinding, withViewTransitions } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth-interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { GlobalErrorHandler } from './core/handlers/global-error.handler';

/**
 * Modern Angular application configuration using standalone APIs
 * - Pure CSR (Client-Side Rendering)
 * - Functional interceptors
 * - Global error handling
 * - Component input binding
 * - View transitions for smooth navigation
 */
export const appConfig: ApplicationConfig = {
  providers: [
    // Zone.js configuration with event coalescing for better performance
    provideZoneChangeDetection({ eventCoalescing: true }),

    // Router with modern features
    provideRouter(
      routes,
      withComponentInputBinding(), // Bind route params to component inputs
      withViewTransitions() // Smooth page transitions
    ),

    // HTTP Client with fetch API and interceptors
    provideHttpClient(
      withFetch(), // Use modern Fetch API instead of XMLHttpRequest
      withInterceptors([
        authInterceptor, // Add auth token to requests
        errorInterceptor // Handle HTTP errors globally
      ])
    ),

    // Angular Material animations
    provideAnimationsAsync(),

    // Charts configuration
    provideCharts(withDefaultRegisterables()),

    // Global error handler for unhandled exceptions
    { provide: ErrorHandler, useClass: GlobalErrorHandler }
  ]
};

