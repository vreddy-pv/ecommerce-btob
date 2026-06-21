import { Component, signal, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <div class="login-container">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon class="logo-icon">local_shipping</mat-icon>
            B2B Auto Parts
          </mat-card-title>
          <mat-card-subtitle>Sign in to your account</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email" placeholder="you@company.com" />
              <mat-icon matSuffix>email</mat-icon>
              @if (loginForm.controls.email.hasError('required') && loginForm.controls.email.touched) {
                <mat-error>Email is required</mat-error>
              }
              @if (loginForm.controls.email.hasError('email') && loginForm.controls.email.touched) {
                <mat-error>Please enter a valid email</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Password</mat-label>
              <input matInput formControlName="password" type="password" placeholder="Enter your password" />
              <mat-icon matSuffix>lock</mat-icon>
              @if (loginForm.controls.password.hasError('required') && loginForm.controls.password.touched) {
                <mat-error>Password is required</mat-error>
              }
              @if (loginForm.controls.password.hasError('minlength') && loginForm.controls.password.touched) {
                <mat-error>Password must be at least 6 characters</mat-error>
              }
            </mat-form-field>

            @if (errorMessage()) {
              <mat-error class="login-error">{{ errorMessage() }}</mat-error>
            }

            <button
              mat-raised-button
              color="primary"
              type="submit"
              class="full-width submit-btn"
              [disabled]="loginForm.invalid || loading()"
            >
              @if (loading()) {
                <ng-container><mat-icon class="spin">hourglass_empty</mat-icon></ng-container>
                Signing in...
              } @else {
                Sign In
              }
            </button>
          </form>
        </mat-card-content>

        <mat-card-actions align="end">
          <span class="register-link">
            Don't have an account?
            <a mat-button color="accent" routerLink="/register">Register</a>
          </span>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background-color: var(--md-sys-color-secondary, #F5F5F5);
    }
    .login-card {
      max-width: 420px;
      width: 100%;
      padding: 24px;
    }
    .logo-icon {
      margin-right: 8px;
      vertical-align: middle;
      color: var(--md-sys-color-primary, #1976D2);
    }
    .full-width { width: 100%; }
    .submit-btn { margin-top: 16px; height: 48px; font-size: 16px; }
    .login-error { display: block; margin-bottom: 16px; font-size: 14px; }
    .register-link { font-size: 14px; }
    .spin { animation: spin 1s linear infinite; }
    @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
  `],
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loading = signal(false);
  errorMessage = signal<string | null>(null);

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  onSubmit() {
    if (this.loginForm.invalid) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    const { email, password } = this.loginForm.value;
    this.authService.login(email!, password!).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/catalog']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.message || 'Invalid email or password');
      },
    });
  }
}
