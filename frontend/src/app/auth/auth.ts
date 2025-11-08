import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface SignInRequest {
  username: string;
  password: string;
}

export interface SignInResponse {
  token: string;
  username: string;
  role: string;
  expiresIn: number;
}

export interface AuthUser {
  username: string;
  role: string;
}

@Injectable({
  providedIn: 'root',
})
export class Auth {
  private readonly API_URL = 'http://localhost:8081/api/auth';
  private readonly TOKEN_KEY = 'jwt_token';

  currentUser = signal<AuthUser | null>(null);
  isAuthenticated = signal<boolean>(false);

  constructor(private http: HttpClient) {
    this.loadStoredAuth();
  }

  signIn(credentials: SignInRequest): Observable<SignInResponse> {
    return this.http.post<SignInResponse>(`${this.API_URL}/signin`, credentials).pipe(
      tap(response => {
        this.storeAuth(response);
      })
    );
  }

  signOut(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private storeAuth(response: SignInResponse): void {
    localStorage.setItem(this.TOKEN_KEY, response.token);
    localStorage.setItem('username', response.username);
    localStorage.setItem('role', response.role);
    this.currentUser.set({
      username: response.username,
      role: response.role
    });
    this.isAuthenticated.set(true);
  }

  private loadStoredAuth(): void {
    const token = localStorage.getItem(this.TOKEN_KEY);
    const username = localStorage.getItem('username');
    const role = localStorage.getItem('role');

    if (token && username && role) {
      this.currentUser.set({ username, role });
      this.isAuthenticated.set(true);
    }
  }
}
