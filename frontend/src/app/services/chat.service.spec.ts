import { TestBed } from '@angular/core/testing';
import { ChatService } from './chat.service';
import { AuthService } from './auth.service';
import { NotificationService } from './notification.service';
import { ConnectionStatusService } from './connection-status.service';
import { User, UserStatus } from '../models/user.model';
import { Message, MessageType } from '../models/message.model';
import { of } from 'rxjs';

// Mock STOMP Client
class MockStompClient {
  connected = false;
  onConnect: ((frame: any) => void) | null = null;
  onStompError: ((frame: any) => void) | null = null;
  onWebSocketError: ((error: any) => void) | null = null;
  onDisconnect: (() => void) | null = null;

  activate() {
    this.connected = true;
    if (this.onConnect) {
      this.onConnect({});
    }
  }

  deactivate() {
    this.connected = false;
    if (this.onDisconnect) {
      this.onDisconnect();
    }
  }

  subscribe(destination: string, callback: (message: any) => void) {
    return {
      unsubscribe: jasmine.createSpy('unsubscribe')
    };
  }

  publish(params: { destination: string; body: string }) {
    // Mock publish method
  }
}

describe('ChatService', () => {
  let service: ChatService;
  let authService: jasmine.SpyObj<AuthService>;
  let notificationService: jasmine.SpyObj<NotificationService>;
  let connectionStatusService: jasmine.SpyObj<ConnectionStatusService>;

  const mockUser: User = {
    id: 1,
    username: 'testuser',
    status: UserStatus.ONLINE
  };

  beforeEach(() => {
    const authSpy = jasmine.createSpyObj('AuthService', ['getCurrentUser', 'getUsers']);
    const notificationSpy = jasmine.createSpyObj('NotificationService', ['showSuccess', 'showError', 'showInfo']);
    const connectionStatusSpy = jasmine.createSpyObj('ConnectionStatusService', ['updateChatConnectionStatus']);

    TestBed.configureTestingModule({
      providers: [
        ChatService,
        { provide: AuthService, useValue: authSpy },
        { provide: NotificationService, useValue: notificationSpy },
        { provide: ConnectionStatusService, useValue: connectionStatusSpy }
      ]
    });

    service = TestBed.inject(ChatService);
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    notificationService = TestBed.inject(NotificationService) as jasmine.SpyObj<NotificationService>;
    connectionStatusService = TestBed.inject(ConnectionStatusService) as jasmine.SpyObj<ConnectionStatusService>;

    authService.getCurrentUser.and.returnValue(mockUser);
    authService.getUsers.and.returnValue(of([mockUser]));
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('connect', () => {
    it('should not connect when no authenticated user', () => {
      authService.getCurrentUser.and.returnValue(null);

      service.connect();

      expect(service.isConnected()).toBe(false);
    });

    it('should not connect when already connected', () => {
      service['stompClient'] = { connected: true } as any;

      service.connect();

      // Should not create new connection
      expect(service['stompClient'].connected).toBe(true);
    });

    it('should create STOMP client and connect', () => {
      spyOn(service as any, 'setupSubscriptions');
      spyOn(service as any, 'notifyUserJoined');

      // Mock the Client constructor
      const mockClient = new MockStompClient();
      spyOn(window as any, 'Client').and.returnValue(mockClient);

      service.connect();

      // Simulate successful connection
      if (mockClient.onConnect) {
        mockClient.onConnect({});
      }

      expect(service['setupSubscriptions']).toHaveBeenCalled();
      expect(service['notifyUserJoined']).toHaveBeenCalled();
      expect(notificationService.showSuccess).toHaveBeenCalledWith('Connected to chat server');
      expect(connectionStatusService.updateChatConnectionStatus).toHaveBeenCalledWith(true, 0);
    });
  });

  describe('sendMessage', () => {
    beforeEach(() => {
      service['stompClient'] = {
        connected: true,
        publish: jasmine.createSpy('publish')
      } as any;
    });

    it('should send private message successfully', () => {
      const message: Message = {
        sender: mockUser,
        receiver: { id: 2, username: 'receiver', status: UserStatus.ONLINE },
        content: 'Test message',
        type: MessageType.CHAT,
        timestamp: new Date()
      };

      spyOn(service['messageSubject'], 'next');
      spyOn(service['messageStatusSubject'], 'next');

      service.sendMessage(message);

      expect(service['stompClient'].publish).toHaveBeenCalledWith({
        destination: '/app/chat.sendMessage',
        body: jasmine.any(String)
      });
      expect(service['messageSubject'].next).toHaveBeenCalled();
      expect(service['messageStatusSubject'].next).toHaveBeenCalledWith({
        messageId: jasmine.any(String),
        status: 'sending'
      });
    });

    it('should send group message successfully', () => {
      const message: Message = {
        sender: mockUser,
        content: 'Group message',
        type: MessageType.CHAT,
        timestamp: new Date(),
        groupId: 'group1'
      };

      spyOn(service['messageSubject'], 'next');

      service.sendMessage(message);

      expect(service['stompClient'].publish).toHaveBeenCalledWith({
        destination: '/app/chat.sendGroupMessage',
        body: jasmine.any(String)
      });
      expect(service['messageSubject'].next).toHaveBeenCalled();
    });

    it('should handle error when not connected', () => {
      service['stompClient'] = { connected: false } as any;

      const message: Message = {
        sender: mockUser,
        content: 'Test message',
        type: MessageType.CHAT,
        timestamp: new Date()
      };

      service.sendMessage(message);

      expect(notificationService.showError).toHaveBeenCalledWith('Cannot send message: Not connected to chat server');
    });

    it('should handle send error', () => {
      service['stompClient'] = {
        connected: true,
        publish: jasmine.createSpy('publish').and.throwError('Network error')
      } as any;

      const message: Message = {
        sender: mockUser,
        content: 'Test message',
        type: MessageType.CHAT,
        timestamp: new Date()
      };

      spyOn(service['messageStatusSubject'], 'next');

      service.sendMessage(message);

      expect(notificationService.showError).toHaveBeenCalledWith('Failed to send message. Please try again.');
      expect(service['messageStatusSubject'].next).toHaveBeenCalledWith({
        messageId: jasmine.any(String),
        status: 'failed'
      });
    });
  });

  describe('subscribeToGroup', () => {
    it('should subscribe to group messages', () => {
      const mockSubscription = { unsubscribe: jasmine.createSpy('unsubscribe') };
      service['stompClient'] = {
        connected: true,
        subscribe: jasmine.createSpy('subscribe').and.returnValue(mockSubscription)
      } as any;

      service.subscribeToGroup('group1');

      expect(service['stompClient'].subscribe).toHaveBeenCalledWith(
        '/topic/group/group1',
        jasmine.any(Function)
      );
      expect(service['subscriptions']).toContain(mockSubscription);
    });

    it('should handle error when not connected', () => {
      service['stompClient'] = { connected: false } as any;

      service.subscribeToGroup('group1');

      // Should log error but not throw
      expect(service['stompClient']).toBeDefined();
    });
  });

  describe('disconnect', () => {
    beforeEach(() => {
      service['stompClient'] = {
        connected: true,
        publish: jasmine.createSpy('publish'),
        deactivate: jasmine.createSpy('deactivate')
      } as any;
      service['subscriptions'] = [
        { unsubscribe: jasmine.createSpy('unsubscribe') },
        { unsubscribe: jasmine.createSpy('unsubscribe') }
      ];
    });

    it('should disconnect and clean up', () => {
      spyOn(service['connectionStatusSubject'], 'next');

      service.disconnect();

      expect(service['stompClient'].publish).toHaveBeenCalledWith({
        destination: '/app/chat.removeUser',
        body: jasmine.any(String)
      });
      expect(service['stompClient'].deactivate).toHaveBeenCalled();
      expect(service['subscriptions'][0].unsubscribe).toHaveBeenCalled();
      expect(service['subscriptions'][1].unsubscribe).toHaveBeenCalled();
      expect(service['connectionStatusSubject'].next).toHaveBeenCalledWith(false);
      expect(service['stompClient']).toBeNull();
    });

    it('should handle disconnect when not connected', () => {
      service['stompClient'] = null;

      service.disconnect();

      // Should not throw error
      expect(service['stompClient']).toBeNull();
    });
  });

  describe('observables', () => {
    it('should provide message observable', (done) => {
      service.subscribeToMessages().subscribe(message => {
        expect(message).toBeDefined();
        done();
      });

      // Emit a test message
      const testMessage: Message = {
        sender: mockUser,
        content: 'Test',
        type: MessageType.CHAT,
        timestamp: new Date()
      };
      service['messageSubject'].next(testMessage);
    });

    it('should provide user status observable', (done) => {
      service.subscribeToUserStatus().subscribe(users => {
        expect(users).toEqual([mockUser]);
        done();
      });

      service['userStatusSubject'].next([mockUser]);
    });

    it('should provide connection status observable', (done) => {
      service.getConnectionStatus().subscribe(status => {
        expect(status).toBe(true);
        done();
      });

      service['connectionStatusSubject'].next(true);
    });

    it('should provide message status observable', (done) => {
      service.getMessageStatus().subscribe(status => {
        expect(status.status).toBe('sent');
        done();
      });

      service['messageStatusSubject'].next({ messageId: 'test', status: 'sent' });
    });
  });

  describe('isConnected', () => {
    it('should return true when connected', () => {
      service['stompClient'] = { connected: true } as any;

      expect(service.isConnected()).toBe(true);
    });

    it('should return false when not connected', () => {
      service['stompClient'] = { connected: false } as any;

      expect(service.isConnected()).toBe(false);
    });

    it('should return false when stompClient is null', () => {
      service['stompClient'] = null;

      expect(service.isConnected()).toBe(false);
    });
  });

  describe('sendGroupMessage', () => {
    it('should set groupId and call sendMessage', () => {
      spyOn(service, 'sendMessage');

      const message: Message = {
        sender: mockUser,
        content: 'Group message',
        type: MessageType.CHAT,
        timestamp: new Date()
      };

      service.sendGroupMessage(message, 'group1');

      expect(message.groupId).toBe('group1');
      expect(service.sendMessage).toHaveBeenCalledWith(message);
    });
  });
});