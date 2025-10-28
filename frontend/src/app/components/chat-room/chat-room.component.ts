import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ChatService } from '../../services/chat.service';
import { User } from '../../models/user.model';
import { Message, MessageType } from '../../models/message.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat-room',
  templateUrl: './chat-room.component.html',
  styleUrls: ['./chat-room.component.css']
})
export class ChatRoomComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messageArea') private messageArea!: ElementRef;
  currentUser: User | null = null;
  users: User[] = [];
  messages: Message[] = [];
  newMessage = '';
  selectedUser: User | null = null;
  loading = false;
  connectionStatus = false;
  isTyping = false;
  
  private subscriptions: Subscription[] = [];
  private shouldScrollToBottom = false;

  constructor(
    private authService: AuthService,
    private chatService: ChatService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    if (!this.currentUser) {
      this.router.navigate(['/login']);
      return;
    }

    this.initializeChat();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.chatService.disconnect();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  private initializeChat(): void {
    // Connect to WebSocket
    this.chatService.connect();

    // Subscribe to messages
    const messagesSub = this.chatService.subscribeToMessages().subscribe({
      next: (message: Message) => {
        // Only add messages for current conversation or system messages
        if (this.isMessageForCurrentConversation(message)) {
          this.messages.push(message);
          this.shouldScrollToBottom = true;
        }
      },
      error: (error: any) => {
        console.error('Error receiving message:', error);
      }
    });

    // Subscribe to user status updates
    const userStatusSub = this.chatService.subscribeToUserStatus().subscribe({
      next: (users: User[]) => {
        this.users = users.filter(user => user.id !== this.currentUser?.id);
      },
      error: (error: any) => {
        console.error('Error receiving user status:', error);
      }
    });

    // Subscribe to connection status
    const connectionSub = this.chatService.getConnectionStatus().subscribe({
      next: (status: boolean) => {
        this.connectionStatus = status;
      }
    });

    this.subscriptions.push(messagesSub, userStatusSub, connectionSub);

    // Load initial data
    this.loadUsers();
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

  onUserSelected(user: User): void {
    this.selectedUser = user;
    this.loadMessageHistory(user.id);
  }

  private loadMessageHistory(userId: number): void {
    this.loading = true;
    this.messages = []; // Clear current messages
    
    this.authService.getMessageHistory(userId).subscribe({
      next: (messages: Message[]) => {
        this.messages = messages;
        this.loading = false;
        this.shouldScrollToBottom = true;
      },
      error: (error: any) => {
        console.error('Error loading message history:', error);
        this.loading = false;
      }
    });
  }

  sendMessage(): void {
    if (this.newMessage.trim() && this.selectedUser && this.currentUser) {
      const message: Message = {
        sender: this.currentUser,
        receiver: this.selectedUser,
        content: this.newMessage.trim(),
        type: MessageType.CHAT,
        timestamp: new Date()
      };

      this.chatService.sendMessage(message);
      this.newMessage = '';
      this.shouldScrollToBottom = true;
    }
  }

  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private scrollToBottom(): void {
    try {
      if (this.messageArea) {
        this.messageArea.nativeElement.scrollTop = this.messageArea.nativeElement.scrollHeight;
      }
    } catch (err) {
      console.error('Error scrolling to bottom:', err);
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  isOnline(user: User): boolean {
    return user.status === 'ONLINE';
  }

  getStatusClass(user: User): string {
    return this.isOnline(user) ? 'user-status-online' : 'user-status-offline';
  }

  private isMessageForCurrentConversation(message: Message): boolean {
    // System messages (JOIN/LEAVE) should always be shown
    if (message.type === MessageType.JOIN || message.type === MessageType.LEAVE) {
      return true;
    }

    // If no user is selected, don't show regular chat messages
    if (!this.selectedUser || !this.currentUser) {
      return false;
    }

    // Show messages between current user and selected user
    return (
      (message.sender.id === this.currentUser.id && message.receiver?.id === this.selectedUser.id) ||
      (message.sender.id === this.selectedUser.id && message.receiver?.id === this.currentUser.id)
    );
  }

  onMessageInput(): void {
    // Handle typing indicators here if needed
    this.isTyping = this.newMessage.trim().length > 0;
  }

  getConnectionStatusText(): string {
    return this.connectionStatus ? 'Connected' : 'Disconnected';
  }

  getConnectionStatusClass(): string {
    return this.connectionStatus ? 'text-success' : 'text-danger';
  }

  trackByMessageId(index: number, message: Message): number | string {
    return message.id || `${message.sender.id}-${message.timestamp.getTime()}`;
  }
}