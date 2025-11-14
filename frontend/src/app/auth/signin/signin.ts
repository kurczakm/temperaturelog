import { Component, signal, DestroyRef, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormControl } from '@angular/forms';
import { Auth, SignInRequest } from '../auth';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

interface SignInForm {
  username: FormControl<string>;
  password: FormControl<string>;
}

@Component({
  selector: 'app-signin',
  imports: [ReactiveFormsModule, CommonModule, RouterModule],
  templateUrl: './signin.html',
  styleUrl: './signin.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SigninComponent {
  private readonly MIN_USERNAME_LENGTH = 3;
  private readonly MIN_PASSWORD_LENGTH = 6;

  private fb = inject(FormBuilder);
  private authService = inject(Auth);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  signInForm: FormGroup<SignInForm>;
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal(false);

  constructor() {
    this.signInForm = this.fb.group<SignInForm>({
      username: this.fb.control('', {
        nonNullable: true,
        validators: [Validators.required, Validators.minLength(this.MIN_USERNAME_LENGTH)]
      }),
      password: this.fb.control('', {
        nonNullable: true,
        validators: [Validators.required, Validators.minLength(this.MIN_PASSWORD_LENGTH)]
      })
    });
  }

  onSubmit(): void {
    if (this.signInForm.invalid) {
      this.signInForm.markAllAsTouched();
      return;
    }

    const credentials: SignInRequest = {
      username: this.signInForm.value.username?.trim() || '',
      password: this.signInForm.value.password?.trim() || ''
    };

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.signInForm.disable();

    this.authService.signIn(credentials)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          this.signInForm.reset();
          this.router.navigate(['/chart']).catch(err => {
            console.error('Navigation failed:', err);
            this.errorMessage.set('Navigation failed. Please try again.');
            this.signInForm.enable();
          });
        },
        error: (error: HttpErrorResponse) => {
          this.isLoading.set(false);
          this.signInForm.enable();
          this.handleError(error);
        }
      });
  }

  togglePasswordVisibility(): void {
    this.showPassword.update(value => !value);
  }

  get username(): FormControl<string> {
    return this.signInForm.controls.username;
  }

  get password(): FormControl<string> {
    return this.signInForm.controls.password;
  }

  private handleError(error: HttpErrorResponse): void {
    if (error.status === 401) {
      this.errorMessage.set('Invalid username or password');
    } else if (error.status === 429) {
      this.errorMessage.set('Too many login attempts. Please try again later.');
    } else if (error.status === 0) {
      this.errorMessage.set('Unable to connect to server. Please try again later.');
    } else {
      this.errorMessage.set('An error occurred. Please try again.');
    }
    console.error('Sign in error:', error);
  }
}
