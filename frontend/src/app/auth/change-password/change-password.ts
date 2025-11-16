import { Component, signal, DestroyRef, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormControl, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Auth, ChangePasswordRequest } from '../auth';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

interface ChangePasswordForm {
  currentPassword: FormControl<string>;
  newPassword: FormControl<string>;
  confirmPassword: FormControl<string>;
}

@Component({
  selector: 'app-change-password',
  imports: [ReactiveFormsModule, CommonModule, RouterModule],
  templateUrl: './change-password.html',
  styleUrl: './change-password.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChangePasswordComponent {
  private readonly MIN_PASSWORD_LENGTH = 8;
  private readonly PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/;
  private readonly REDIRECT_DELAY_MS = 2000;

  private fb = inject(FormBuilder);
  private authService = inject(Auth);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  changePasswordForm: FormGroup<ChangePasswordForm>;
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  showCurrentPassword = signal(false);
  showNewPassword = signal(false);
  showConfirmPassword = signal(false);

  constructor() {
    this.changePasswordForm = this.fb.group<ChangePasswordForm>({
      currentPassword: this.fb.control('', {
        nonNullable: true,
        validators: [Validators.required]
      }),
      newPassword: this.fb.control('', {
        nonNullable: true,
        validators: [
          Validators.required,
          Validators.minLength(this.MIN_PASSWORD_LENGTH),
          Validators.maxLength(100),
          Validators.pattern(this.PASSWORD_PATTERN),
          this.notSameAsCurrentPasswordValidator()
        ]
      }),
      confirmPassword: this.fb.control('', {
        nonNullable: true,
        validators: [Validators.required]
      })
    }, {
      validators: this.passwordMatchValidator
    });

    // Re-validate confirm password when new password changes
    this.changePasswordForm.controls.newPassword.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.changePasswordForm.controls.confirmPassword.updateValueAndValidity();
      });

    // Re-validate new password when current password changes
    this.changePasswordForm.controls.currentPassword.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.changePasswordForm.controls.newPassword.updateValueAndValidity();
      });
  }

  onSubmit(): void {
    if (this.changePasswordForm.invalid) {
      this.changePasswordForm.markAllAsTouched();
      return;
    }

    const formValue = this.changePasswordForm.getRawValue();
    const request: ChangePasswordRequest = {
      currentPassword: formValue.currentPassword,
      newPassword: formValue.newPassword
    };

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.changePasswordForm.disable();

    this.authService.changePassword(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          this.successMessage.set('Password changed successfully! Redirecting...');
          this.changePasswordForm.reset();

          setTimeout(() => {
            this.router.navigate(['/chart']).catch(err => {
              console.error('Navigation failed:', err);
              this.errorMessage.set('Navigation failed. Please try again.');
              this.successMessage.set(null);
              this.changePasswordForm.enable();
            });
          }, this.REDIRECT_DELAY_MS);
        },
        error: (error: HttpErrorResponse) => {
          this.isLoading.set(false);
          this.changePasswordForm.enable();
          this.handleError(error);
        }
      });
  }

  togglePasswordVisibility(field: 'current' | 'new' | 'confirm'): void {
    if (field === 'current') {
      this.showCurrentPassword.update(value => !value);
    } else if (field === 'new') {
      this.showNewPassword.update(value => !value);
    } else {
      this.showConfirmPassword.update(value => !value);
    }
  }

  get currentPassword(): FormControl<string> {
    return this.changePasswordForm.controls.currentPassword;
  }

  get newPassword(): FormControl<string> {
    return this.changePasswordForm.controls.newPassword;
  }

  get confirmPassword(): FormControl<string> {
    return this.changePasswordForm.controls.confirmPassword;
  }

  private notSameAsCurrentPasswordValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const currentPassword = this.changePasswordForm?.get('currentPassword')?.value;
      const newPassword = control.value;

      if (currentPassword && newPassword && currentPassword === newPassword) {
        return { sameAsCurrentPassword: true };
      }
      return null;
    };
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const newPassword = control.get('newPassword')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if (!newPassword || !confirmPassword) {
      return null;
    }

    return newPassword === confirmPassword ? null : { passwordMismatch: true };
  }

  private handleError(error: HttpErrorResponse): void {
    if (error.status === 401) {
      const errorBody = error.error;
      // Check if it's token expiration vs wrong current password
      if (errorBody && typeof errorBody === 'object' && errorBody.message) {
        const message = errorBody.message.toLowerCase();
        if (message.includes('token') || message.includes('expired') || message.includes('unauthorized')) {
          this.errorMessage.set('Your session has expired. Please sign in again.');
          setTimeout(() => {
            this.authService.signOut();
            this.router.navigate(['/signin']);
          }, this.REDIRECT_DELAY_MS);
          return;
        }
      }
      this.errorMessage.set('Current password is incorrect');
    } else if (error.status === 400) {
      const errorBody = error.error;
      if (errorBody && typeof errorBody === 'object' && errorBody.message) {
        this.errorMessage.set(errorBody.message);
      } else {
        this.errorMessage.set('Invalid password format. Password must be 8-100 characters with uppercase, lowercase, and digit.');
      }
    } else if (error.status === 0) {
      this.errorMessage.set('Unable to connect to server. Please try again later.');
    } else {
      this.errorMessage.set('An error occurred. Please try again.');
    }
    console.error('Change password error:', error);
  }
}
