import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap, finalize } from 'rxjs/operators';
import { User, LoginRequest, RegisterRequest, AuthResponse } from '../models/user.model';
import { Message } from '../models/message.model';
import { LoadingService } from './loading.service';
import { NotificationService } from './notification.service';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private apiUrl = 'http://localhost:8082/api';
    private currentUserSubject = new BehaviorSubject<User | null>(null);
    public currentUser$ = this.currentUserSubject.asObservable();

    constructor(
        private http: HttpClient,
        private loadingService: LoadingService,
        private notificationService: NotificationService
    ) {
        // Load user from localStorage on service initialization
        const storedUser = localStorage.getItem('currentUser');
        if (storedUser) {
            this.currentUserSubject.next(JSON.parse(storedUser));
        }
    }

    register(userData: RegisterRequest): Observable<AuthResponse> {
        this.loadingService.setLoadingFor('register', true);
        return this.http.post<AuthResponse>(`${this.apiUrl}/register`, userData)
            .pipe(
                tap((response: AuthResponse) => {
                    if (response.success) {
                        this.notificationService.showSuccess('Registration successful! Please log in.');
                    }
                }),
                finalize(() => {
                    this.loadingService.setLoadingFor('register', false);
                })
            );
    }

    login(credentials: LoginRequest): Observable<AuthResponse> {
        this.loadingService.setLoadingFor('login', true);
        return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials)
            .pipe(
                tap((response: AuthResponse) => {
                    if (response.user) {
                        // Store user in localStorage
                        localStorage.setItem('currentUser', JSON.stringify(response.user));
                        this.currentUserSubject.next(response.user);
                        this.notificationService.showSuccess(`Welcome back, ${response.user.username}!`);
                    }
                }),
                finalize(() => {
                    this.loadingService.setLoadingFor('login', false);
                })
            );
    }

    logout(): void {
        this.loadingService.setLoadingFor('logout', true);
        // Call logout endpoint
        this.http.post(`${this.apiUrl}/logout`, {}).subscribe({
            next: () => {
                this.clearUserData();
                this.notificationService.showInfo('You have been logged out successfully.');
            },
            error: () => {
                // Clear data even if logout request fails
                this.clearUserData();
                this.notificationService.showInfo('You have been logged out.');
            },
            complete: () => {
                this.loadingService.setLoadingFor('logout', false);
            }
        });
    }

    private clearUserData(): void {
        localStorage.removeItem('currentUser');
        this.currentUserSubject.next(null);
    }

    getCurrentUser(): User | null {
        return this.currentUserSubject.value;
    }

    isAuthenticated(): boolean {
        return this.getCurrentUser() !== null;
    }

    getUsers(): Observable<User[]> {
        return this.http.get<User[]>(`${this.apiUrl}/users`);
    }

    getOnlineUsers(): Observable<User[]> {
        return this.http.get<User[]>(`${this.apiUrl}/users/online`);
    }

    updateUserStatus(status: string): Observable<any> {
        return this.http.put(`${this.apiUrl}/users/status`, { status });
    }

    getMessageHistory(userId: number): Observable<Message[]> {
        return this.http.get<Message[]>(`${this.apiUrl}/messages/${userId}`);
    }

    getGroupMessageHistory(groupId: string): Observable<Message[]> {
        return this.http.get<Message[]>(`${this.apiUrl}/messages/group/${groupId}`);
    }

    // HTTP interceptor helper methods
    getAuthHeaders(): HttpHeaders {
        const user = this.getCurrentUser();
        if (user) {
            return new HttpHeaders({
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${user.id}` // Simple user ID based auth for demo
            });
        }
        return new HttpHeaders({
            'Content-Type': 'application/json'
        });
    }
}