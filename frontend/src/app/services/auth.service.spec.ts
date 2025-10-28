import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { LoadingService } from './loading.service';
import { NotificationService } from './notification.service';
import { User, UserStatus, LoginRequest, RegisterRequest, AuthResponse } from '../models/user.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let loadingService: jasmine.SpyObj<LoadingService>;
  let notificationService: jasmine.SpyObj<NotificationService>;

  const mockUser: User = {
    id: 1,
    username: 'testuser',
    status: UserStatus.ONLINE,
    createdAt: new Date(),
    lastSeen: new Date()
  };

  beforeEach(() => {
    const loadingSpy = jasmine.createSpyObj('LoadingService', ['setLoadingFor']);
    const notificationSpy = jasmine.createSpyObj('NotificationService', ['showSuccess', 'showError', 'showInfo']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: LoadingService, useValue: loadingSpy },
        { provide: NotificationService, useValue: notificationSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    loadingService = TestBed.inject(LoadingService) as jasmine.SpyObj<LoadingService>;
    notificationService = TestBed.inject(NotificationService) as jasmine.SpyObj<NotificationService>;

    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('register', () => {
    it('should register user successfully', () => {
      const registerRequest: RegisterRequest = {
        username: 'testuser',
        password: 'password123'
      };

      const mockResponse: AuthResponse = {
        user: mockUser,
        message: 'Registration successful',
        success: true
      };

      service.register(registerRequest).subscribe(response => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne('http://localhost:8080/api/register');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(registerRequest);
      req.flush(mockResponse);

      expect(loadingService.setLoadingFor).toHaveBeenCalledWith('register', true);
      expect(loadingService.setLoadingFor).toHaveBeenCalledWith('register', false);
      expect(notificationService.showSuccess).toHaveBeenCalledWith('Registration successful! Please log in.');
    });

    it('should handle registration error', () => {
      const registerRequest: RegisterRequest = {
        username: 'testuser',
        password: 'password123'
      };

      service.register(registerRequest).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(400);
        }
      });

      const req = httpMock.expectOne('http://localhost:8080/api/register');
      req.flush('Username already exists', { status: 400, statusText: 'Bad Request' });

      expect(loadingService.setLoadingFor).toHaveBeenCalledWith('register', true);
      expect(loadingService.setLoadingFor).toHaveBeenCalledWith('register', false);
    });
  });

  describe('login', () => {
    it('should login user successfully', () => {
      const loginRequest: LoginRequest = {
        username: 'testuser',
        password: 'password123'
      };

      const mockResponse: AuthResponse = {
        user: mockUser,
        message: 'Login successful',
        success: true
      };

      service.login(loginRequest).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(service.getCurrentUser()).toEqual(mockUser);
        expect(service.isAuthenticated()).toBe(true);
      });

      const req = httpMock.expectOne('http://localhost:8080/api/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(loginRequest);
      req.flush(mockResponse);

      expect(loadingService.setLoadingFor).toHaveBeenCalledWith('login', true);
      expect(loadingService.setLoadingFor).toHaveBeenCalledWith('login', false);
      expect(notificationService.showSuccess).toHaveBeenCalledWith('Welcome back, testuser!');
      expect(localStorage.getItem('currentUser')).toBe(JSON.stringify(mockUser));
    });

    it('should handle login error', () => {
      const loginRequest: LoginRequest = {
        username: 'testuser',
        password: 'wrongpassword'
      };

      service.login(loginRequest).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(401);
        }
      });

      const req = httpMock.expectOne('http://localhost:8080/api/login');
      req.flush('Invalid credentials', { status: 401, statusText: 'Unauthorized' });

      expect(loadingService.setLoadingFor).toHaveBeenCalledWith('login', true);
      expect(loadingService.setLoadingFor).toHaveBeenCalledWith('login', false);
      expect(service.getCurrentUser()).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
    });
  });

  describe('logout', () => {
    beforeEach(() => {
      // Set up authenticated user
      localStorage.setItem('currentUser', JSON.stringify(mockUser));
      service['currentUserSubject'].next(mockUser);
    });

    it('should logout user successfully', () => {
      service.logout();

      const req = httpMock.expectOne('http://localhost:8080/api/logout');
      expect(req.request.method).toBe('POST');
      req.flush({});

      expect(loadingService.setLoadingFor).toHaveBeenCalledWith('logout', true);
      expect(loadingService.setLoadingFor).toHaveBeenCalledWith('logout', false);
      expect(notificationService.showInfo).toHaveBeenCalledWith('You have been logged out successfully.');
      expect(service.getCurrentUser()).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
      expect(localStorage.getItem('currentUser')).toBeNull();
    });

    it('should handle logout error gracefully', () => {
      service.logout();

      const req = httpMock.expectOne('http://localhost:8080/api/logout');
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });

      expect(loadingService.setLoadingFor).toHaveBeenCalledWith('logout', true);
      expect(loadingService.setLoadingFor).toHaveBeenCalledWith('logout', false);
      expect(notificationService.showInfo).toHaveBeenCalledWith('You have been logged out.');
      expect(service.getCurrentUser()).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
      expect(localStorage.getItem('currentUser')).toBeNull();
    });
  });

  describe('user management', () => {
    it('should get all users', () => {
      const mockUsers: User[] = [mockUser];

      service.getUsers().subscribe(users => {
        expect(users).toEqual(mockUsers);
      });

      const req = httpMock.expectOne('http://localhost:8080/api/users');
      expect(req.request.method).toBe('GET');
      req.flush(mockUsers);
    });

    it('should get online users', () => {
      const mockOnlineUsers: User[] = [mockUser];

      service.getOnlineUsers().subscribe(users => {
        expect(users).toEqual(mockOnlineUsers);
      });

      const req = httpMock.expectOne('http://localhost:8080/api/users/online');
      expect(req.request.method).toBe('GET');
      req.flush(mockOnlineUsers);
    });

    it('should update user status', () => {
      const status = 'ONLINE';

      service.updateUserStatus(status).subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne('http://localhost:8080/api/users/status');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ status });
      req.flush({ success: true });
    });
  });

  describe('message history', () => {
    it('should get message history for user', () => {
      const userId = 1;
      const mockMessages: any[] = [];

      service.getMessageHistory(userId).subscribe(messages => {
        expect(messages).toEqual(mockMessages);
      });

      const req = httpMock.expectOne(`http://localhost:8080/api/messages/${userId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockMessages);
    });

    it('should get group message history', () => {
      const groupId = 'group1';
      const mockMessages: any[] = [];

      service.getGroupMessageHistory(groupId).subscribe(messages => {
        expect(messages).toEqual(mockMessages);
      });

      const req = httpMock.expectOne(`http://localhost:8080/api/messages/group/${groupId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockMessages);
    });
  });

  describe('authentication state', () => {
    it('should initialize with user from localStorage', () => {
      localStorage.setItem('currentUser', JSON.stringify(mockUser));
      
      // Create new service instance to test initialization
      const newService = new AuthService(
        TestBed.inject(HttpClientTestingModule) as any,
        loadingService,
        notificationService
      );

      expect(newService.getCurrentUser()).toEqual(mockUser);
      expect(newService.isAuthenticated()).toBe(true);
    });

    it('should initialize without user when localStorage is empty', () => {
      expect(service.getCurrentUser()).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
    });

    it('should provide current user observable', (done) => {
      service.currentUser$.subscribe(user => {
        expect(user).toBeNull();
        done();
      });
    });
  });

  describe('getAuthHeaders', () => {
    it('should return headers with authorization when user is authenticated', () => {
      service['currentUserSubject'].next(mockUser);

      const headers = service.getAuthHeaders();
      
      expect(headers.get('Content-Type')).toBe('application/json');
      expect(headers.get('Authorization')).toBe('Bearer 1');
    });

    it('should return headers without authorization when user is not authenticated', () => {
      const headers = service.getAuthHeaders();
      
      expect(headers.get('Content-Type')).toBe('application/json');
      expect(headers.get('Authorization')).toBeNull();
    });
  });
});