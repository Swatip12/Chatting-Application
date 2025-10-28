import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, retry } from 'rxjs/operators';
import { NotificationService } from '../services/notification.service';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {

  constructor(
    private notificationService: NotificationService,
    private authService: AuthService,
    private router: Router
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      retry(1), // Retry failed requests once
      catchError((error: HttpErrorResponse) => {
        let errorMessage = 'An unexpected error occurred';
        
        if (error.error instanceof ErrorEvent) {
          // Client-side error
          errorMessage = `Client Error: ${error.error.message}`;
        } else {
          // Server-side error
          switch (error.status) {
            case 400:
              errorMessage = this.handleBadRequest(error);
              break;
            case 401:
              errorMessage = 'Invalid credentials or session expired';
              this.handleUnauthorized();
              break;
            case 403:
              errorMessage = 'You do not have permission to perform this action';
              break;
            case 404:
              errorMessage = 'The requested resource was not found';
              break;
            case 409:
              errorMessage = this.handleConflict(error);
              break;
            case 422:
              errorMessage = 'Invalid data provided';
              break;
            case 500:
              errorMessage = 'Server error. Please try again later';
              break;
            case 503:
              errorMessage = 'Service temporarily unavailable. Please try again later';
              break;
            case 0:
              errorMessage = 'Unable to connect to server. Please check your internet connection';
              break;
            default:
              errorMessage = `Server Error: ${error.status} - ${error.statusText}`;
          }
        }

        // Extract error message from API response if available
        if (error.error && error.error.message) {
          errorMessage = error.error.message;
        }

        // Show user-friendly notification
        this.notificationService.showError(errorMessage);

        // Log error for debugging
        console.error('HTTP Error:', {
          status: error.status,
          statusText: error.statusText,
          url: error.url,
          message: errorMessage,
          error: error.error
        });

        return throwError(() => error);
      })
    );
  }

  private handleBadRequest(error: HttpErrorResponse): string {
    if (error.error && error.error.data && typeof error.error.data === 'object') {
      // Handle validation errors
      const validationErrors = error.error.data;
      const errorMessages = Object.values(validationErrors).join(', ');
      return `Validation Error: ${errorMessages}`;
    }
    return error.error?.message || 'Invalid request data';
  }

  private handleConflict(error: HttpErrorResponse): string {
    return error.error?.message || 'Resource conflict occurred';
  }

  private handleUnauthorized(): void {
    // Clear user data and redirect to login
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}