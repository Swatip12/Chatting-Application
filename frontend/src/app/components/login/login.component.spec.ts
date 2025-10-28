import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { of, throwError, BehaviorSubject } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth.service';
import { LoadingService } from '../../services/loading.service';
import { AuthResponse } from '../../models/user.model';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  let loadingService: jasmine.SpyObj<LoadingService>;
  let loadingSubject: BehaviorSubject<boolean>;

  beforeEach(async () => {
    const authSpy = jasmine.createSpyObj('AuthService', ['login', 'isAuthenticated']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const loadingSpy = jasmine.createSpyObj('LoadingService', ['getLoadingState', 'isLoadingFor']);
    
    loadingSubject = new BehaviorSubject<boolean>(false);

    await TestBed.configureTestingModule({
      declarations: [LoginComponent],
      imports: [ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: routerSpy },
        { provide: LoadingService, useValue: loadingSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    loadingService = TestBed.inject(LoadingService) as jasmine.SpyObj<LoadingService>;

    loadingService.getLoadingState.and.returnValue(loadingSubject.asObservable());
    loadingService.isLoadingFor.and.returnValue(false);
    authService.isAuthenticated.and.returnValue(false);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with validators', () => {
    expect(component.loginForm).toBeDefined();
    expect(component.loginForm.get('username')?.hasError('required')).toBe(true);
    expect(component.loginForm.get('password')?.hasError('required')).toBe(true);
  });

  it('should redirect to chat if already authenticated', () => {
    authService.isAuthenticated.and.returnValue(true);
    
    component.ngOnInit();
    
    expect(router.navigate).toHaveBeenCalledWith(['/chat']);
  });

  it('should not redirect if not authenticated', () => {
    authService.isAuthenticated.and.returnValue(false);
    
    component.ngOnInit();
    
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should validate username minimum length', () => {
    const usernameControl = component.loginForm.get('username');
    
    usernameControl?.setValue('ab');
    
    expect(usernameControl?.hasError('minlength')).toBe(true);
    expect(component.loginForm.valid).toBe(false);
  });

  it('should validate password minimum length', () => {
    const passwordControl = component.loginForm.get('password');
    
    passwordControl?.setValue('12345');
    
    expect(passwordControl?.hasError('minlength')).toBe(true);
    expect(component.loginForm.valid).toBe(false);
  });

  it('should be valid with correct input', () => {
    component.loginForm.patchValue({
      username: 'testuser',
      password: 'password123'
    });
    
    expect(component.loginForm.valid).toBe(true);
  });

  describe('onSubmit', () => {
    beforeEach(() => {
      component.loginForm.patchValue({
        username: 'testuser',
        password: 'password123'
      });
    });

    it('should not submit if form is invalid', () => {
      component.loginForm.patchValue({ username: '' });
      
      component.onSubmit();
      
      expect(authService.login).not.toHaveBeenCalled();
    });

    it('should not submit if loading', () => {
      component.loading = true;
      
      component.onSubmit();
      
      expect(authService.login).not.toHaveBeenCalled();
    });

    it('should submit valid form and navigate on success', () => {
      const mockResponse: AuthResponse = {
        user: { id: 1, username: 'testuser', status: 'ONLINE' as any },
        message: 'Login successful',
        success: true
      };
      
      authService.login.and.returnValue(of(mockResponse));
      
      component.onSubmit();
      
      expect(authService.login).toHaveBeenCalledWith({
        username: 'testuser',
        password: 'password123'
      });
      expect(router.navigate).toHaveBeenCalledWith(['/chat']);
      expect(component.errorMessage).toBe('');
    });

    it('should handle login error', () => {
      const error = { status: 401, message: 'Invalid credentials' };
      authService.login.and.returnValue(throwError(() => error));
      
      component.onSubmit();
      
      expect(authService.login).toHaveBeenCalled();
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('should clear error message on submit', () => {
      component.errorMessage = 'Previous error';
      authService.login.and.returnValue(of({} as AuthResponse));
      
      component.onSubmit();
      
      expect(component.errorMessage).toBe('');
    });
  });

  it('should navigate to register page', () => {
    component.navigateToRegister();
    
    expect(router.navigate).toHaveBeenCalledWith(['/register']);
  });

  it('should subscribe to loading state', () => {
    loadingService.isLoadingFor.and.returnValue(true);
    
    component.ngOnInit();
    loadingSubject.next(true);
    
    expect(loadingService.isLoadingFor).toHaveBeenCalledWith('login');
  });

  it('should unsubscribe on destroy', () => {
    spyOn(component['subscription'], 'unsubscribe');
    
    component.ngOnDestroy();
    
    expect(component['subscription'].unsubscribe).toHaveBeenCalled();
  });

  describe('form interaction', () => {
    it('should update form values', () => {
      const compiled = fixture.nativeElement;
      const usernameInput = compiled.querySelector('input[formControlName="username"]');
      const passwordInput = compiled.querySelector('input[formControlName="password"]');
      
      if (usernameInput && passwordInput) {
        usernameInput.value = 'testuser';
        usernameInput.dispatchEvent(new Event('input'));
        
        passwordInput.value = 'password123';
        passwordInput.dispatchEvent(new Event('input'));
        
        fixture.detectChanges();
        
        expect(component.loginForm.get('username')?.value).toBe('testuser');
        expect(component.loginForm.get('password')?.value).toBe('password123');
      }
    });

    it('should show validation errors', () => {
      component.loginForm.get('username')?.markAsTouched();
      component.loginForm.get('password')?.markAsTouched();
      
      fixture.detectChanges();
      
      expect(component.loginForm.get('username')?.hasError('required')).toBe(true);
      expect(component.loginForm.get('password')?.hasError('required')).toBe(true);
    });
  });
});