import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { User, UserStatus } from '../../models/user.model';
import { ChatService } from '../../services/chat.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.css']
})
export class UserListComponent implements OnInit, OnDestroy {
  @Input() users: User[] = [];
  @Input() selectedUser: User | null = null;
  @Input() currentUser: User | null = null;
  @Output() userSelected = new EventEmitter<User>();

  private subscriptions: Subscription[] = [];
  connectionStatus = false;

  constructor(
    private chatService: ChatService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.subscribeToUserUpdates();
    this.subscribeToConnectionStatus();
    this.loadUsers();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private subscribeToUserUpdates(): void {
    const userStatusSub = this.chatService.subscribeToUserStatus().subscribe({
      next: (users: User[]) => {
        this.users = users.filter(user => user.id !== this.currentUser?.id);
      },
      error: (error: any) => {
        console.error('Error receiving user status updates:', error);
      }
    });

    this.subscriptions.push(userStatusSub);
  }

  private subscribeToConnectionStatus(): void {
    const connectionSub = this.chatService.getConnectionStatus().subscribe({
      next: (status: boolean) => {
        this.connectionStatus = status;
      }
    });

    this.subscriptions.push(connectionSub);
  }

  private loadUsers(): void {
    this.authService.getUsers().subscribe({
      next: (users: User[]) => {
        this.users = users.filter(user => user.id !== this.currentUser?.id);
      },
      error: (error: any) => {
        console.error('Error loading users:', error);
      }
    });
  }

  selectUser(user: User): void {
    this.userSelected.emit(user);
  }

  isOnline(user: User): boolean {
    return user.status === UserStatus.ONLINE;
  }

  getStatusClass(user: User): string {
    return this.isOnline(user) ? 'user-status-online' : 'user-status-offline';
  }

  getStatusIcon(user: User): string {
    return this.isOnline(user) ? 'fas fa-circle' : 'far fa-circle';
  }

  isSelected(user: User): boolean {
    return this.selectedUser?.id === user.id;
  }

  getUserInitial(username: string): string {
    return username.charAt(0).toUpperCase();
  }

  getLastSeenText(user: User): string {
    if (this.isOnline(user)) {
      return 'Online';
    }

    if (!user.lastSeen) {
      return 'Offline';
    }

    const lastSeen = new Date(user.lastSeen);
    const now = new Date();
    const diffInMinutes = Math.floor((now.getTime() - lastSeen.getTime()) / (1000 * 60));

    if (diffInMinutes < 1) {
      return 'Just now';
    } else if (diffInMinutes < 60) {
      return `${diffInMinutes}m ago`;
    } else if (diffInMinutes < 1440) { // 24 hours
      const hours = Math.floor(diffInMinutes / 60);
      return `${hours}h ago`;
    } else {
      const days = Math.floor(diffInMinutes / 1440);
      return `${days}d ago`;
    }
  }

  refreshUsers(): void {
    this.loadUsers();
  }

  getOnlineUsersCount(): number {
    return this.users.filter(user => this.isOnline(user)).length;
  }

  getTotalUsersCount(): number {
    return this.users.length;
  }

  trackByUserId(index: number, user: User): number {
    return user.id;
  }
}