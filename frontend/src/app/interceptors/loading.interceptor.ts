import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { LoadingService } from '../services/loading.service';

@Injectable()
export class LoadingInterceptor implements HttpInterceptor {
  private activeRequests = 0;

  constructor(private loadingService: LoadingService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Don't show loading for certain endpoints (like logout)
    const skipLoading = req.url.includes('/logout') || req.method === 'GET';
    
    if (!skipLoading) {
      this.activeRequests++;
      this.loadingService.setLoading(true);
    }

    return next.handle(req).pipe(
      finalize(() => {
        if (!skipLoading) {
          this.activeRequests--;
          if (this.activeRequests === 0) {
            this.loadingService.setLoading(false);
          }
        }
      })
    );
  }
}