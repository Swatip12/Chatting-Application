import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  duration?: number;
  timestamp: Date;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notifications = new BehaviorSubject<Notification[]>([]);
  private notificationIdCounter = 0;

  constructor() {}

  getNotifications(): Observable<Notification[]> {
    return this.notifications.asObservable();
  }

  showSuccess(message: string, duration: number = 5000): void {
    this.addNotification('success', message, duration);
  }

  showError(message: string, duration: number = 8000): void {
    this.addNotification('error', message, duration);
  }

  showWarning(message: string, duration: number = 6000): void {
    this.addNotification('warning', message, duration);
  }

  showInfo(message: string, duration: number = 5000): void {
    this.addNotification('info', message, duration);
  }

  private addNotification(type: Notification['type'], message: string, duration: number): void {
    const notification: Notification = {
      id: `notification-${++this.notificationIdCounter}`,
      type,
      message,
      duration,
      timestamp: new Date()
    };

    const currentNotifications = this.notifications.value;
    this.notifications.next([...currentNotifications, notification]);

    // Auto-remove notification after duration
    if (duration > 0) {
      setTimeout(() => {
        this.removeNotification(notification.id);
      }, duration);
    }
  }

  removeNotification(id: string): void {
    const currentNotifications = this.notifications.value;
    const filteredNotifications = currentNotifications.filter(n => n.id !== id);
    this.notifications.next(filteredNotifications);
  }

  clearAll(): void {
    this.notifications.next([]);
  }
}