import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { User, LoginRequest, RegisterRequest, AuthResponse } from '../models/user.model';
import { Message } from '../models/message.model';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private apiUrl = 'http://localhost:8080/api';
    private currentUserSubject = new BehaviorSubject<User | null>(null);
    public currentUser$ = this.currentUserSubject.asObservable();

    constructor(private http: HttpClient) {
        // Load user from localStorage on service initialization
        const storedUser = localStorage.getItem('currentUser');
        if (storedUser) {
            this.currentUserSubject.next(JSON.parse(storedUser));
        }
    }

    register(userData: RegisterRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.apiUrl}/register`, userData);
    }

    login(credentials: LoginRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials)
            .pipe(
                tap((response: AuthResponse) => {
                    if (response.user) {
                        // Store user in localStorage
                        localStorage.setItem('currentUser', JSON.stringify(response.user));
                        this.currentUserSubject.next(response.user);
                    }
                })
            );
    }

    logout(): void {
        // Call logout endpoint
        this.http.post(`${this.apiUrl}/logout`, {}).subscribe({
            next: () => {
                this.clearUserData();
            },
            error: () => {
                // Clear data even if logout request fails
                this.clearUserData();
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