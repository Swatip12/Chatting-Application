import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { LoadingService } from '../../services/loading.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit, OnDestroy {
  loginForm: FormGroup;
  loading = false;
  errorMessage = '';
  private subscription: Subscription = new Subscription();

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private loadingService: LoadingService
  ) {
    this.loginForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  ngOnInit(): void {
    // Redirect if already authenticated
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/chat']);
    }

    // Subscribe to loading state for login
    this.subscription.add(
      this.loadingService.getLoadingState().subscribe(loading => {
        this.loading = this.loadingService.isLoadingFor('login');
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  onSubmit(): void {
    if (this.loginForm.valid && !this.loading) {
      this.errorMessage = '';
      
      const credentials = this.loginForm.value;
      
      this.authService.login(credentials).subscribe({
        next: (response: any) => {
          this.router.navigate(['/chat']);
        },
        error: (error: any) => {
          // Error handling is now done by the interceptor
          // But we can still show component-specific errors if needed
          console.error('Login error:', error);
        }
      });
    }
  }

  navigateToRegister(): void {
    this.router.navigate(['/register']);
  }
}