import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  private loadingSubject = new BehaviorSubject<boolean>(false);
  private loadingStates = new Map<string, boolean>();

  constructor() {}

  /**
   * Get global loading state
   */
  getLoadingState(): Observable<boolean> {
    return this.loadingSubject.asObservable();
  }

  /**
   * Set global loading state
   */
  setLoading(loading: boolean): void {
    this.loadingSubject.next(loading);
  }

  /**
   * Set loading state for a specific operation
   */
  setLoadingFor(key: string, loading: boolean): void {
    this.loadingStates.set(key, loading);
    this.updateGlobalLoadingState();
  }

  /**
   * Get loading state for a specific operation
   */
  isLoadingFor(key: string): boolean {
    return this.loadingStates.get(key) || false;
  }

  /**
   * Clear all loading states
   */
  clearAll(): void {
    this.loadingStates.clear();
    this.loadingSubject.next(false);
  }

  private updateGlobalLoadingState(): void {
    const hasAnyLoading = Array.from(this.loadingStates.values()).some(loading => loading);
    this.loadingSubject.next(hasAnyLoading);
  }
}