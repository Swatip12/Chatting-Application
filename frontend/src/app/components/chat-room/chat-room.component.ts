import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ChatService } from '../../services/chat.service';
import { GroupService } from '../../services/group.service';
import { User } from '../../models/user.model';
import { Message, MessageType } from '../../models/message.model';
import { Group } from '../../models/group.model';
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
  groups: Group[] = [];
  messages: Message[] = [];
  newMessage = '';
  selectedUser: User | null = null;
  selectedGroup: Group | null = null;
  loading = false;
  connectionStatus = false;
  isTyping = false;
  
  // UI state
  activeTab: 'users' | 'groups' = 'users';
  showGroupCreate = false;
  
  private subscriptions: Subscription[] = [];
  private shouldScrollToBottom = false;

  constructor(
    private authService: AuthService,
    private chatService: ChatService,
    private groupService: GroupService,
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
    this.loadGroups();
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

  private loadGroups(): void {
    this.groupService.getUserGroups().subscribe({
      next: (groups) => {
        this.groups = groups;
      },
      error: (error: any) => {
        console.error('Error loading groups:', error);
      }
    });
  }

  onUserSelected(user: User): void {
    this.selectedUser = user;
    this.selectedGroup = null; // Clear group selection
    this.loadMessageHistory(user.id);
  }

  onGroupSelected(group: Group): void {
    this.selectedGroup = group;
    this.selectedUser = null; // Clear user selection
    this.loadGroupMessageHistory(group.id);
    // Subscribe to group messages
    this.chatService.subscribeToGroup(group.id);
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

  private loadGroupMessageHistory(groupId: string): void {
    this.loading = true;
    this.messages = []; // Clear current messages
    
    this.authService.getGroupMessageHistory(groupId).subscribe({
      next: (messages: Message[]) => {
        this.messages = messages;
        this.loading = false;
        this.shouldScrollToBottom = true;
      },
      error: (error: any) => {
        console.error('Error loading group message history:', error);
        this.loading = false;
      }
    });
  }

  sendMessage(): void {
    if (this.newMessage.trim() && this.currentUser) {
      const message: Message = {
        sender: this.currentUser,
        content: this.newMessage.trim(),
        type: MessageType.CHAT,
        timestamp: new Date()
      };

      if (this.selectedGroup) {
        // Send group message
        message.groupId = this.selectedGroup.id;
        this.chatService.sendGroupMessage(message, this.selectedGroup.id);
      } else if (this.selectedUser) {
        // Send private message
        message.receiver = this.selectedUser;
        this.chatService.sendMessage(message);
      }

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
    // System messages (JOIN/LEAVE) should be shown based on context
    if (message.type === MessageType.JOIN || message.type === MessageType.LEAVE) {
      // Show system messages for the current group or general chat
      if (this.selectedGroup && message.groupId === this.selectedGroup.id) {
        return true;
      }
      // Show general system messages when in private chat
      if (this.selectedUser && !message.groupId) {
        return true;
      }
      return false;
    }

    // Group messages
    if (message.groupId) {
      return this.selectedGroup?.id === message.groupId;
    }

    // Private messages
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

  // Group functionality methods
  switchToUsersTab(): void {
    this.activeTab = 'users';
    this.selectedGroup = null;
  }

  switchToGroupsTab(): void {
    this.activeTab = 'groups';
    this.selectedUser = null;
  }

  onCreateGroupClick(): void {
    this.showGroupCreate = true;
  }

  onGroupCreated(group: Group): void {
    this.showGroupCreate = false;
    this.groups.push(group);
    this.onGroupSelected(group);
  }

  onGroupCreateCancelled(): void {
    this.showGroupCreate = false;
  }

  getCurrentConversationName(): string {
    if (this.selectedGroup) {
      return this.selectedGroup.name;
    }
    if (this.selectedUser) {
      return this.selectedUser.username;
    }
    return 'Select a conversation';
  }

  isGroupConversation(): boolean {
    return !!this.selectedGroup;
  }

  canSendMessage(): boolean {
    return !!(this.selectedUser || this.selectedGroup);
  }
}