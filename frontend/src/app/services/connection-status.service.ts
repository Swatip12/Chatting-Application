import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, fromEvent, merge } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

export interface ConnectionStatus {
  online: boolean;
  chatConnected: boolean;
  lastConnected?: Date;
  reconnectAttempts: number;
}

@Injectable({
  providedIn: 'root'
})
export class ConnectionStatusService {
  private connectionStatusSubject = new BehaviorSubject<ConnectionStatus>({
    online: navigator.onLine,
    chatConnected: false,
    reconnectAttempts: 0
  });

  constructor() {
    this.initializeNetworkStatusMonitoring();
  }

  getConnectionStatus(): Observable<ConnectionStatus> {
    return this.connectionStatusSubject.asObservable();
  }

  updateChatConnectionStatus(connected: boolean, reconnectAttempts: number = 0): void {
    const currentStatus = this.connectionStatusSubject.value;
    this.connectionStatusSubject.next({
      ...currentStatus,
      chatConnected: connected,
      lastConnected: connected ? new Date() : currentStatus.lastConnected,
      reconnectAttempts
    });
  }

  isOnline(): boolean {
    return this.connectionStatusSubject.value.online;
  }

  isChatConnected(): boolean {
    return this.connectionStatusSubject.value.chatConnected;
  }

  private initializeNetworkStatusMonitoring(): void {
    // Monitor network connectivity
    const online$ = fromEvent(window, 'online').pipe(map(() => true));
    const offline$ = fromEvent(window, 'offline').pipe(map(() => false));
    
    merge(online$, offline$)
      .pipe(startWith(navigator.onLine))
      .subscribe(online => {
        const currentStatus = this.connectionStatusSubject.value;
        this.connectionStatusSubject.next({
          ...currentStatus,
          online
        });
      });
  }
}