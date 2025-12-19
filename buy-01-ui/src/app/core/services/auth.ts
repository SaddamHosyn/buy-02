import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError, map, switchMap } from 'rxjs'; // Added 'map'
import { environment } from '../../../environments/environment';

export interface User {
  id: string;
  email: string;
  name: string;
  role: 'SELLER' | 'CLIENT' | 'ADMIN';
  avatarUrl?: string | null;
}

export interface UpdateProfileRequest {
  name?: string;
  password?: string;        // Current password (for verification)
  newPassword?: string;     // New password
  avatar?: string;       // Avatar (after upload)
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  role: 'SELLER' | 'CLIENT';
  avatarUrl?: string | null;
}

export interface AuthResponse {
  token: string;
  id: string;
  email: string;
  name: string;
  role: 'SELLER' | 'CLIENT' | 'ADMIN';
  avatarUrl?: string | null;
}

export interface RegisterResponse {
  id: string;
  email: string;
  name: string;
  role: 'SELLER' | 'CLIENT' | 'ADMIN';
  avatarUrl?: string | null;
}    

// Interface for the Media response
interface MediaResponse {
  id: string;
  url: string;
}

@Injectable({
  providedIn: 'root',
})
export class Auth {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  
  private readonly currentUserSignal = signal<User | null>(null);
  private readonly tokenSignal = signal<string | null>(null);
  private readonly loadingSignal = signal<boolean>(false);
  
  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly token = this.tokenSignal.asReadonly();
  readonly isLoading = this.loadingSignal.asReadonly();
  
  readonly isAuthenticated = computed(() => !!this.currentUserSignal());
  readonly isSeller = computed(() => this.currentUserSignal()?.role === 'SELLER');
  readonly isClient = computed(() => this.currentUserSignal()?.role === 'CLIENT');
  readonly isAdmin = computed(() => this.currentUserSignal()?.role === 'ADMIN');
  
  private readonly AUTH_URL = `${environment.authUrl}`; 
  private readonly USER_URL = `${environment.apiUrl}/users`; 
  
  constructor() {
    this.loadUserFromStorage();
  }
  
  private loadUserFromStorage(): void {
    try {
      const token = localStorage.getItem('auth_token');
      const userJson = localStorage.getItem('current_user');

      if (token && userJson) {
        const user = JSON.parse(userJson);
        this.tokenSignal.set(token);
        this.currentUserSignal.set(user);
      }
    } catch (error) {
      console.error('Error loading user from storage:', error);
      this.clearAuth();
    }
  }
  
  login(credentials: LoginRequest): Observable<AuthResponse> {
    this.loadingSignal.set(true);
    return this.http.post<AuthResponse>(`${this.AUTH_URL}/login`, credentials).pipe(
      tap((response: AuthResponse) => {
        const user: User = {
          id: response.id,
          email: response.email,
          name: response.name,
          role: response.role,
          avatarUrl: response.avatarUrl,
        };
        this.setAuth(user, response.token);
        this.loadingSignal.set(false);
      }),
      catchError((error: any) => {
        this.loadingSignal.set(false);
        return throwError(() => error);
      })
    );
  }

  /**
   * Register new user
   * Calls backend API: POST /api/auth/register
   */
  register(data: RegisterRequest): Observable<RegisterResponse> {
    this.loadingSignal.set(true);

    return this.http.post<RegisterResponse>(`${this.AUTH_URL}/register`, data).pipe(
      tap(() => {
        this.loadingSignal.set(false);
      }),
      catchError((error: any) => {
        this.loadingSignal.set(false);
        return throwError(() => error);
      })
    );
  }

  /**
   * Logout user
   */
  logout(): void {
    this.clearAuth();
    this.router.navigate(['/auth/login']);
  }

  // --- Profile Updates (Name, Password, Avatar) ---


  /**
   * Update Name
   */
  updateName(name: string, password?: string): Observable<User> {
    return this.updateProfile({ name, password });
  }
  /**
   * Change Password
   * FIXED: Added map(() => void 0) to correctly convert Observable<User> to Observable<void>
   */
  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.updateProfile({ 
      password: currentPassword, 
      newPassword: newPassword 
    }).pipe(
      map(() => void 0) // Explicitly return void to satisfy the type signature
    );
  }

   /**
   * Update Avatar
   * 1. Uploads file to Media Controller
   * 2. Updates User Profile with new avatar URL
   */
uploadAvatar(file: File): Observable<{ avatarUrl: string }> {
  const formData = new FormData();
  formData.append('file', file);
  
  return this.http.post<{ url: string }>(`${environment.apiUrl}/media/images`, formData).pipe(
    switchMap((response: { url: string }) => {
      // Wait for profile update to complete before returning
      return this.updateProfile({ avatar: response.url }).pipe(
        tap(() => {
          // Update the local user signal and localStorage
          this.updateUser({ avatarUrl: response.url });
        }),
        map(() => ({ avatarUrl: response.url }))
      );
    }),
    catchError(err => throwError(() => new Error('Avatar upload failed. ' + (err.message || ''))))
  );
}




  private setAuth(user: User, token: string): void {
    this.currentUserSignal.set(user);
    this.tokenSignal.set(token);
    localStorage.setItem('auth_token', token);
    localStorage.setItem('current_user', JSON.stringify(user));
  }

  private clearAuth(): void {
    this.currentUserSignal.set(null);
    this.tokenSignal.set(null);
    localStorage.removeItem('auth_token');
    localStorage.removeItem('current_user');
  }

  hasRole(role: User['role']): boolean {
    return this.currentUserSignal()?.role === role;
  }

  /**
   * Update user profile via API
   */
  updateProfile(updates: { name?: string; avatar?: string; password?: string; newPassword?: string }): Observable<User> {
    return this.http.put<User>(`${environment.usersUrl}/me`, updates).pipe(
      tap((updatedUser) => {
        // Update local state and storage
        this.updateUser(updatedUser);
      }),
      catchError((error) => {
        console.error('Profile update failed:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Update current user data (for local state updates)
   */
  updateUser(updates: Partial<User>): void {
    const currentUser = this.currentUserSignal();
    if (currentUser) {
      const updatedUser = { ...currentUser, ...updates };
      this.currentUserSignal.set(updatedUser);
      localStorage.setItem('current_user', JSON.stringify(updatedUser));
    }
  }
  
  getToken(): string | null {
    return this.tokenSignal();
  }
}
