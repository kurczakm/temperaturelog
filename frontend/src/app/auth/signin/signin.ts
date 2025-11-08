import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Auth } from '../auth';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-signin',
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './signin.html',
  styleUrl: './signin.scss',
})
export class Signin {
  signInForm: FormGroup;
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal(false);

  constructor(
    private fb: FormBuilder,
    private authService: Auth,
    private router: Router
  ) {
    this.signInForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  onSubmit(): void {
    if (this.signInForm.invalid) {
      this.signInForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.signInForm.disable();

    this.authService.signIn(this.signInForm.value).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.signInForm.enable();
        console.log('Sign in successful', response);
        // Navigate to dashboard or home page
        // this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.isLoading.set(false);
        this.signInForm.enable();
        if (error.status === 401) {
          this.errorMessage.set('Invalid username or password');
        } else if (error.status === 0) {
          this.errorMessage.set('Unable to connect to server. Please try again later.');
        } else {
          this.errorMessage.set('An error occurred. Please try again.');
        }
        console.error('Sign in error:', error);
      }
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword.update(value => !value);
  }

  get username() {
    return this.signInForm.get('username');
  }

  get password() {
    return this.signInForm.get('password');
  }
}
