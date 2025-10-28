import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { UserListComponent } from './user-list.component';
import { ChatService } from '../../services/chat.service';
import { AuthService } from '../../services/auth.service';
import { User, UserStatus } from '../../models/user.model';

describe('UserListComponent', () => {
  let component: UserListComponent;
  let fixture: ComponentFixture<UserListComponent>;
  let chatService: jasmine.SpyObj<ChatService>;
  let authService: jasmine.SpyObj<AuthService>;
  let userStatusSubject: Subject<User[]>;
  let connectionStatusSubject: Subject<boolean>;

  const mockUsers: User[] = [
    { id: 1, username: 'user1', status: UserStatus.ONLINE, lastSeen: new Date() },
    { id: 2, username: 'user2', status: UserStatus.OFFLINE, lastSeen: new Date(Date.now() - 60000) },
    { id: 3, username: 'user3', status: UserStatus.ONLINE, lastSeen: new Date() }
  ];

  const currentUser: User = { id: 4, username: 'currentUser', status: UserStatus.ONLINE };

  beforeEach(async () => {
    const chatSpy = jasmine.createSpyObj('ChatService', ['subscribeToUserStatus', 'getConnectionStatus']);
    const authSpy = jasmine.createSpyObj('AuthService', ['getUsers']);
    
    userStatusSubject = new Subject<User[]>();
    connectionStatusSubject = new Subject<boolean>();

    await TestBed.configureTestingModule({
      declarations: [UserListComponent],
      providers: [
        { provide: ChatService, useValue: chatSpy },
        { provide: AuthService, useValue: authSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserListComponent);
    component = fixture.componentInstance;
    chatService = TestBed.inject(ChatService) as jasmine.SpyObj<ChatService>;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;

    chatService.subscribeToUserStatus.and.returnValue(userStatusSubject.asObservable());
    chatService.getConnectionStatus.and.returnValue(connectionStatusSubject.asObservable());
    authService.getUsers.and.returnValue(of(mockUsers));

    component.currentUser = currentUser;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize and load users', () => {
    component.ngOnInit();

    expect(chatService.subscribeToUserStatus).toHaveBeenCalled();
    expect(chatService.getConnectionStatus).toHaveBeenCalled();
    expect(authService.getUsers).toHaveBeenCalled();
    expect(component.users).toEqual(mockUsers);
  });

  it('should filter out current user from user list', () => {
    const usersWithCurrent = [...mockUsers, currentUser];
    authService.getUsers.and.returnValue(of(usersWithCurrent));

    component.ngOnInit();

    expect(component.users).toEqual(mockUsers);
    expect(component.users).not.toContain(currentUser);
  });

  it('should update users when receiving user status updates', () => {
    component.ngOnInit();

    const updatedUsers = [
      { id: 1, username: 'user1', status: UserStatus.OFFLINE, lastSeen: new Date() }
    ];

    userStatusSubject.next([...updatedUsers, currentUser]);

    expect(component.users).toEqual(updatedUsers);
  });

  it('should update connection status', () => {
    component.ngOnInit();

    connectionStatusSubject.next(true);
    expect(component.connectionStatus).toBe(true);

    connectionStatusSubject.next(false);
    expect(component.connectionStatus).toBe(false);
  });

  it('should emit user selection', () => {
    spyOn(component.userSelected, 'emit');
    const user = mockUsers[0];

    component.selectUser(user);

    expect(component.userSelected.emit).toHaveBeenCalledWith(user);
  });

  describe('user status methods', () => {
    it('should check if user is online', () => {
      expect(component.isOnline(mockUsers[0])).toBe(true);
      expect(component.isOnline(mockUsers[1])).toBe(false);
    });

    it('should return correct status class', () => {
      expect(component.getStatusClass(mockUsers[0])).toBe('user-status-online');
      expect(component.getStatusClass(mockUsers[1])).toBe('user-status-offline');
    });

    it('should return correct status icon', () => {
      expect(component.getStatusIcon(mockUsers[0])).toBe('fas fa-circle');
      expect(component.getStatusIcon(mockUsers[1])).toBe('far fa-circle');
    });
  });

  it('should check if user is selected', () => {
    component.selectedUser = mockUsers[0];

    expect(component.isSelected(mockUsers[0])).toBe(true);
    expect(component.isSelected(mockUsers[1])).toBe(false);
  });

  it('should get user initial', () => {
    expect(component.getUserInitial('testuser')).toBe('T');
    expect(component.getUserInitial('alice')).toBe('A');
  });

  describe('getLastSeenText', () => {
    it('should return "Online" for online users', () => {
      expect(component.getLastSeenText(mockUsers[0])).toBe('Online');
    });

    it('should return "Offline" for users without lastSeen', () => {
      const userWithoutLastSeen = { ...mockUsers[1], lastSeen: undefined };
      expect(component.getLastSeenText(userWithoutLastSeen)).toBe('Offline');
    });

    it('should return time-based text for offline users', () => {
      const now = new Date();
      const oneMinuteAgo = new Date(now.getTime() - 60000);
      const oneHourAgo = new Date(now.getTime() - 3600000);
      const oneDayAgo = new Date(now.getTime() - 86400000);

      const userOneMinuteAgo = { ...mockUsers[1], lastSeen: oneMinuteAgo };
      const userOneHourAgo = { ...mockUsers[1], lastSeen: oneHourAgo };
      const userOneDayAgo = { ...mockUsers[1], lastSeen: oneDayAgo };

      expect(component.getLastSeenText(userOneMinuteAgo)).toBe('1m ago');
      expect(component.getLastSeenText(userOneHourAgo)).toBe('1h ago');
      expect(component.getLastSeenText(userOneDayAgo)).toBe('1d ago');
    });
  });

  it('should refresh users', () => {
    component.refreshUsers();

    expect(authService.getUsers).toHaveBeenCalled();
  });

  it('should count online users', () => {
    component.users = mockUsers;

    expect(component.getOnlineUsersCount()).toBe(2);
  });

  it('should count total users', () => {
    component.users = mockUsers;

    expect(component.getTotalUsersCount()).toBe(3);
  });

  it('should track users by ID', () => {
    const user = mockUsers[0];

    expect(component.trackByUserId(0, user)).toBe(user.id);
  });

  it('should unsubscribe on destroy', () => {
    component.ngOnInit();
    
    const subscription = component['subscriptions'][0];
    spyOn(subscription, 'unsubscribe');

    component.ngOnDestroy();

    expect(subscription.unsubscribe).toHaveBeenCalled();
  });
});