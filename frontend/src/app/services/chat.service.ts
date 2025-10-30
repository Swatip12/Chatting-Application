import { Injectable } from '@angular/core';
import { Client, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { Message, ChatMessage, UserStatusMessage, MessageType } from '../models/message.model';
import { User, UserStatus } from '../models/user.model';
import { AuthService } from './auth.service';
import { NotificationService } from './notification.service';
import { ConnectionStatusService } from './connection-status.service';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private stompClient: Client | null = null;
  private messageSubject = new Subject<Message>();
  private userStatusSubject = new BehaviorSubject<User[]>([]);
  private connectionStatusSubject = new BehaviorSubject<boolean>(false);
  private messageStatusSubject = new Subject<{messageId: string, status: 'sending' | 'sent' | 'delivered' | 'failed'}>();
  
  private subscriptions: StompSubscription[] = [];
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 5000; // 5 seconds

  constructor(
    private authService: AuthService,
    private notificationService: NotificationService,
    private connectionStatusService: ConnectionStatusService
  ) {}

  connect(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) {
      console.error('Cannot connect: No authenticated user');
      return;
    }

    if (this.stompClient && this.stompClient.connected) {
      console.log('Already connected to WebSocket');
      return;
    }

    // Create STOMP client with SockJS
    this.stompClient = new Client({
      webSocketFactory: () => new (SockJS as any)('http://localhost:8082/ws/chat'),
      connectHeaders: {
        username: currentUser.username
      },
      debug: (str: string) => {
        console.log('STOMP Debug:', str);
      },
      reconnectDelay: this.reconnectInterval,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // Connection success callback
    this.stompClient.onConnect = (frame: any) => {
      console.log('Connected to WebSocket:', frame);
      this.connectionStatusSubject.next(true);
      this.connectionStatusService.updateChatConnectionStatus(true, 0);
      this.reconnectAttempts = 0;
      this.notificationService.showSuccess('Connected to chat server');
      this.setupSubscriptions();
      this.notifyUserJoined();
    };

    // Connection error callback
    this.stompClient.onStompError = (frame: any) => {
      console.error('STOMP error:', frame);
      this.connectionStatusSubject.next(false);
      this.connectionStatusService.updateChatConnectionStatus(false, this.reconnectAttempts);
      this.notificationService.showError('Chat connection error. Attempting to reconnect...');
      this.handleReconnection();
    };

    // WebSocket error callback
    this.stompClient.onWebSocketError = (error: any) => {
      console.error('WebSocket error:', error);
      this.connectionStatusSubject.next(false);
      this.connectionStatusService.updateChatConnectionStatus(false, this.reconnectAttempts);
      this.notificationService.showError('Connection to chat server lost. Attempting to reconnect...');
      this.handleReconnection();
    };

    // Disconnect callback
    this.stompClient.onDisconnect = () => {
      console.log('Disconnected from WebSocket');
      this.connectionStatusSubject.next(false);
      this.connectionStatusService.updateChatConnectionStatus(false, this.reconnectAttempts);
      this.clearSubscriptions();
    };

    // Activate the client
    this.stompClient.activate();
  }

  private setupSubscriptions(): void {
    if (!this.stompClient || !this.stompClient.connected) {
      return;
    }

    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) {
      return;
    }

    // Subscribe to private messages
    const privateMessageSub = this.stompClient.subscribe(
      `/user/${currentUser.username}/queue/messages`,
      (message: any) => {
        const chatMessage: ChatMessage = JSON.parse(message.body);
        this.handleIncomingMessage(chatMessage);
      }
    );

    // Subscribe to public messages (for group chats and notifications)
    const publicMessageSub = this.stompClient.subscribe(
      '/topic/public',
      (message: any) => {
        const chatMessage: ChatMessage = JSON.parse(message.body);
        this.handleIncomingMessage(chatMessage);
      }
    );

    // Subscribe to user status updates
    const userStatusSub = this.stompClient.subscribe(
      '/topic/users',
      (message: any) => {
        const statusMessage: UserStatusMessage = JSON.parse(message.body);
        this.handleUserStatusUpdate(statusMessage);
      }
    );

    this.subscriptions.push(privateMessageSub, publicMessageSub, userStatusSub);
  }

  /**
   * Subscribe to a specific group's messages
   */
  subscribeToGroup(groupId: string): void {
    if (!this.stompClient || !this.stompClient.connected) {
      console.error('Cannot subscribe to group: Not connected to WebSocket');
      return;
    }

    const groupSub = this.stompClient.subscribe(
      `/topic/group/${groupId}`,
      (message: any) => {
        const chatMessage: ChatMessage = JSON.parse(message.body);
        this.handleIncomingMessage(chatMessage);
      }
    );

    this.subscriptions.push(groupSub);
    console.log(`Subscribed to group: ${groupId}`);
  }

  /**
   * Unsubscribe from a specific group's messages
   */
  unsubscribeFromGroup(groupId: string): void {
    // Note: In a more sophisticated implementation, we would track subscriptions by group ID
    // For now, we rely on the automatic cleanup when disconnecting
    console.log(`Unsubscribed from group: ${groupId}`);
  }

  private handleIncomingMessage(chatMessage: ChatMessage): void {
    // Convert ChatMessage to Message format
    const message: Message = {
      sender: { 
        id: 0, // Will be populated from backend
        username: chatMessage.sender, 
        status: UserStatus.ONLINE 
      },
      receiver: chatMessage.receiver ? { 
        id: 0, 
        username: chatMessage.receiver, 
        status: UserStatus.ONLINE 
      } : undefined,
      content: chatMessage.content,
      type: chatMessage.type,
      timestamp: new Date(),
      groupId: chatMessage.groupId
    };

    this.messageSubject.next(message);
  }

  private handleUserStatusUpdate(statusMessage: UserStatusMessage): void {
    // Update user list based on status changes
    this.authService.getUsers().subscribe({
      next: (users: User[]) => {
        this.userStatusSubject.next(users);
      },
      error: (error: any) => {
        console.error('Error updating user list:', error);
      }
    });
  }

  private notifyUserJoined(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser || !this.stompClient) {
      return;
    }

    const joinMessage: ChatMessage = {
      sender: currentUser.username,
      content: `${currentUser.username} joined the chat`,
      type: MessageType.JOIN
    };

    this.stompClient.publish({
      destination: '/app/chat.addUser',
      body: JSON.stringify(joinMessage)
    });
  }

  sendMessage(message: Message): void {
    if (!this.stompClient || !this.stompClient.connected) {
      console.error('Cannot send message: Not connected to WebSocket');
      this.notificationService.showError('Cannot send message: Not connected to chat server');
      return;
    }

    try {
      const messageId = `msg-${Date.now()}-${Math.random()}`;
      
      // Emit sending status
      this.messageStatusSubject.next({messageId, status: 'sending'});

      const chatMessage: ChatMessage = {
        sender: message.sender.username,
        receiver: message.receiver?.username,
        content: message.content,
        type: message.type,
        groupId: message.groupId
      };

      const destination = message.groupId 
        ? '/app/chat.sendGroupMessage' 
        : '/app/chat.sendMessage';

      this.stompClient.publish({
        destination: destination,
        body: JSON.stringify(chatMessage)
      });

      // Add message to local message stream for immediate display
      const messageWithId = { ...message, id: messageId };
      this.messageSubject.next(messageWithId);
      
      // Emit sent status after a short delay (simulating server processing)
      setTimeout(() => {
        this.messageStatusSubject.next({messageId, status: 'sent'});
      }, 100);
      
    } catch (error) {
      console.error('Error sending message:', error);
      this.notificationService.showError('Failed to send message. Please try again.');
      // Emit failed status
      const messageId = `msg-${Date.now()}-${Math.random()}`;
      this.messageStatusSubject.next({messageId, status: 'failed'});
    }
  }

  sendGroupMessage(message: Message, groupId: string): void {
    message.groupId = groupId;
    this.sendMessage(message);
  }

  subscribeToMessages(): Observable<Message> {
    return this.messageSubject.asObservable();
  }

  subscribeToUserStatus(): Observable<User[]> {
    return this.userStatusSubject.asObservable();
  }

  getConnectionStatus(): Observable<boolean> {
    return this.connectionStatusSubject.asObservable();
  }

  getMessageStatus(): Observable<{messageId: string, status: 'sending' | 'sent' | 'delivered' | 'failed'}> {
    return this.messageStatusSubject.asObservable();
  }

  disconnect(): void {
    const currentUser = this.authService.getCurrentUser();
    
    if (currentUser && this.stompClient && this.stompClient.connected) {
      // Notify that user is leaving
      const leaveMessage: ChatMessage = {
        sender: currentUser.username,
        content: `${currentUser.username} left the chat`,
        type: MessageType.LEAVE
      };

      this.stompClient.publish({
        destination: '/app/chat.removeUser',
        body: JSON.stringify(leaveMessage)
      });
    }

    this.clearSubscriptions();
    
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
    
    this.connectionStatusSubject.next(false);
  }

  private clearSubscriptions(): void {
    this.subscriptions.forEach(sub => {
      if (sub) {
        sub.unsubscribe();
      }
    });
    this.subscriptions = [];
  }

  private handleReconnection(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      
      // Use exponential backoff for reconnection attempts
      const backoffDelay = this.reconnectInterval * Math.pow(2, this.reconnectAttempts - 1);
      
      setTimeout(() => {
        if (!this.stompClient?.connected) {
          this.connectionStatusService.updateChatConnectionStatus(false, this.reconnectAttempts);
          this.notificationService.showInfo(`Reconnecting to chat server... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
          this.connect();
        }
      }, Math.min(backoffDelay, 30000)); // Cap at 30 seconds
    } else {
      console.error('Max reconnection attempts reached. Please refresh the page.');
      this.notificationService.showError('Unable to reconnect to chat server. Please refresh the page.');
    }
  }

  isConnected(): boolean {
    return this.stompClient?.connected || false;
  }
}