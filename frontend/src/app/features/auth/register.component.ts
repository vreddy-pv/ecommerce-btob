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
  selector: 'app-register',
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
    <div class="register-container">
      <mat-card class="register-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon class="logo-icon">business</mat-icon>
            Create Account
          </mat-card-title>
          <mat-card-subtitle>Register your B2B account</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="registerForm" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Company Name</mat-label>
              <input matInput formControlName="companyName" placeholder="Your company name" />
              <mat-icon matSuffix>business</mat-icon>
              @if (registerForm.controls.companyName.hasError('required') && registerForm.controls.companyName.touched) {
                <mat-error>Company name is required</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email" placeholder="you@company.com" />
              <mat-icon matSuffix>email</mat-icon>
              @if (registerForm.controls.email.hasError('required') && registerForm.controls.email.touched) {
                <mat-error>Email is required</mat-error>
              }
              @if (registerForm.controls.email.hasError('email') && registerForm.controls.email.touched) {
                <mat-error>Please enter a valid email</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Password</mat-label>
              <input matInput formControlName="password" type="password" placeholder="At least 6 characters" />
              <mat-icon matSuffix>lock</mat-icon>
              @if (registerForm.controls.password.hasError('required') && registerForm.controls.password.touched) {
                <mat-error>Password is required</mat-error>
              }
              @if (registerForm.controls.password.hasError('minlength') && registerForm.controls.password.touched) {
                <mat-error>Password must be at least 6 characters</mat-error>
              }
            </mat-form-field>

            @if (errorMessage()) {
              <mat-error class="register-error">{{ errorMessage() }}</mat-error>
            }

            <button
              mat-raised-button
              color="primary"
              type="submit"
              class="full-width submit-btn"
              [disabled]="registerForm.invalid || loading()"
            >
              @if (loading()) {
                <ng-container><mat-icon class="spin">hourglass_empty</mat-icon></ng-container>
                Creating account...
              } @else {
                Create Account
              }
            </button>
          </form>
        </mat-card-content>

        <mat-card-actions align="end">
          <span class="login-link">
            Already have an account?
            <a mat-button color="accent" routerLink="/login">Sign In</a>
          </span>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .register-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background-color: var(--md-sys-color-secondary, #F5F5F5);
    }
    .register-card {
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
    .register-error { display: block; margin-bottom: 16px; font-size: 14px; }
    .login-link { font-size: 14px; }
    .spin { animation: spin 1s linear infinite; }
    @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
  `],
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loading = signal(false);
  errorMessage = signal<string | null>(null);

  registerForm = this.fb.group({
    companyName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  onSubmit() {
    if (this.registerForm.invalid) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    const { email, password, companyName } = this.registerForm.value;
    this.authService.register(email!, password!, companyName!).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/catalog']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.message || 'Registration failed. Please try again.');
      },
    });
  }
}
