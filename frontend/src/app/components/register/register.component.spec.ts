import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { of, throwError, BehaviorSubject } from 'rxjs';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../services/auth.service';
import { LoadingService } from '../../services/loading.service';
import { AuthResponse } from '../../models/user.model';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  let loadingService: jasmine.SpyObj<LoadingService>;
  let loadingSubject: BehaviorSubject<boolean>;

  beforeEach(async () => {
    const authSpy = jasmine.createSpyObj('AuthService', ['register', 'isAuthenticated']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const loadingSpy = jasmine.createSpyObj('LoadingService', ['getLoadingState', 'isLoadingFor']);
    
    loadingSubject = new BehaviorSubject<boolean>(false);

    await TestBed.configureTestingModule({
      declarations: [RegisterComponent],
      imports: [ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: routerSpy },
        { provide: LoadingService, useValue: loadingSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
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
});  it('s
hould initialize form with validators', () => {
    expect(component.registerForm).toBeDefined();
    expect(component.registerForm.get('username')?.hasError('required')).toBe(true);
    expect(component.registerForm.get('password')?.hasError('required')).toBe(true);
    expect(component.registerForm.get('confirmPassword')?.hasError('required')).toBe(true);
  });

  it('should redirect to chat if already authenticated', () => {
    authService.isAuthenticated.and.returnValue(true);
    
    component.ngOnInit();
    
    expect(router.navigate).toHaveBeenCalledWith(['/chat']);
  });

  it('should validate username length', () => {
    const usernameControl = component.registerForm.get('username');
    
    usernameControl?.setValue('ab');
    expect(usernameControl?.hasError('minlength')).toBe(true);
    
    usernameControl?.setValue('a'.repeat(25));
    expect(usernameControl?.hasError('maxlength')).toBe(true);
    
    usernameControl?.setValue('validuser');
    expect(usernameControl?.valid).toBe(true);
  });

  it('should validate password minimum length', () => {
    const passwordControl = component.registerForm.get('password');
    
    passwordControl?.setValue('12345');
    expect(passwordControl?.hasError('minlength')).toBe(true);
    
    passwordControl?.setValue('password123');
    expect(passwordControl?.valid).toBe(true);
  });

  describe('password matching', () => {
    it('should validate password match', () => {
      component.registerForm.patchValue({
        password: 'password123',
        confirmPassword: 'different'
      });
      
      expect(component.registerForm.get('confirmPassword')?.hasError('passwordMismatch')).toBe(true);
    });

    it('should pass validation when passwords match', () => {
      component.registerForm.patchValue({
        password: 'password123',
        confirmPassword: 'password123'
      });
      
      expect(component.registerForm.get('confirmPassword')?.hasError('passwordMismatch')).toBeFalsy();
    });

    it('should clear password mismatch error when passwords match', () => {
      const confirmPasswordControl = component.registerForm.get('confirmPassword');
      
      // First set mismatched passwords
      component.registerForm.patchValue({
        password: 'password123',
        confirmPassword: 'different'
      });
      expect(confirmPasswordControl?.hasError('passwordMismatch')).toBe(true);
      
      // Then fix the mismatch
      component.registerForm.patchValue({
        confirmPassword: 'password123'
      });
      expect(confirmPasswordControl?.hasError('passwordMismatch')).toBeFalsy();
    });
  });

  describe('onSubmit', () => {
    beforeEach(() => {
      component.registerForm.patchValue({
        username: 'testuser',
        password: 'password123',
        confirmPassword: 'password123'
      });
    });

    it('should not submit if form is invalid', () => {
      component.registerForm.patchValue({ username: '' });
      
      component.onSubmit();
      
      expect(authService.register).not.toHaveBeenCalled();
    });

    it('should not submit if loading', () => {
      component.loading = true;
      
      component.onSubmit();
      
      expect(authService.register).not.toHaveBeenCalled();
    });

    it('should submit valid form and show success message', (done) => {
      const mockResponse: AuthResponse = {
        user: { id: 1, username: 'testuser', status: 'ONLINE' as any },
        message: 'Registration successful',
        success: true
      };
      
      authService.register.and.returnValue(of(mockResponse));
      
      component.onSubmit();
      
      expect(authService.register).toHaveBeenCalledWith({
        username: 'testuser',
        password: 'password123'
      });
      expect(component.successMessage).toBe('Registration successful! Redirecting to login...');
      
      // Test navigation after timeout
      setTimeout(() => {
        expect(router.navigate).toHaveBeenCalledWith(['/login']);
        done();
      }, 2100);
    });

    it('should handle registration error', () => {
      const error = { status: 400, message: 'Username already exists' };
      authService.register.and.returnValue(throwError(() => error));
      
      component.onSubmit();
      
      expect(authService.register).toHaveBeenCalled();
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('should clear messages on submit', () => {
      component.errorMessage = 'Previous error';
      component.successMessage = 'Previous success';
      authService.register.and.returnValue(of({} as AuthResponse));
      
      component.onSubmit();
      
      expect(component.errorMessage).toBe('');
      expect(component.successMessage).toBe('');
    });
  });

  it('should navigate to login page', () => {
    component.navigateToLogin();
    
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should unsubscribe on destroy', () => {
    spyOn(component['subscription'], 'unsubscribe');
    
    component.ngOnDestroy();
    
    expect(component['subscription'].unsubscribe).toHaveBeenCalled();
  });
});