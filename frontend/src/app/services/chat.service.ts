import { Injectable } from '@angular/core';
import { Client, StompSubscription } from '@stomp/stompjs';
import * as SockJS from 'sockjs-client';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { Message, ChatMessage, UserStatusMessage, MessageType } from '../models/message.model';
import { User, UserStatus } from '../models/user.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private stompClient: Client | null = null;
  private messageSubject = new Subject<Message>();
  private userStatusSubject = new BehaviorSubject<User[]>([]);
  private connectionStatusSubject = new BehaviorSubject<boolean>(false);
  
  private subscriptions: StompSubscription[] = [];
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 5000; // 5 seconds

  constructor(private authService: AuthService) {}

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
      webSocketFactory: () => new SockJS('http://localhost:8080/ws/chat'),
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
      this.reconnectAttempts = 0;
      this.setupSubscriptions();
      this.notifyUserJoined();
    };

    // Connection error callback
    this.stompClient.onStompError = (frame: any) => {
      console.error('STOMP error:', frame);
      this.connectionStatusSubject.next(false);
      this.handleReconnection();
    };

    // WebSocket error callback
    this.stompClient.onWebSocketError = (error: any) => {
      console.error('WebSocket error:', error);
      this.connectionStatusSubject.next(false);
      this.handleReconnection();
    };

    // Disconnect callback
    this.stompClient.onDisconnect = () => {
      console.log('Disconnected from WebSocket');
      this.connectionStatusSubject.next(false);
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
      return;
    }

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
    this.messageSubject.next(message);
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
      
      setTimeout(() => {
        if (!this.stompClient?.connected) {
          this.connect();
        }
      }, this.reconnectInterval * this.reconnectAttempts);
    } else {
      console.error('Max reconnection attempts reached. Please refresh the page.');
    }
  }

  isConnected(): boolean {
    return this.stompClient?.connected || false;
  }
}