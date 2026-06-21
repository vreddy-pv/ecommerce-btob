import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { AuthResponse, AccountTier } from '../models/api.models';

export interface AuthState {
  token: string;
  accountId: string;
  email: string;
  tier: AccountTier;
  expiresIn: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = '/api/auth';
  private storageKey = 'btob_auth';

  // Signal holds the full auth state (null = not logged in)
  private _auth = signal<AuthState | null>(this.loadFromStorage());
  readonly auth = this._auth.asReadonly();

  // Derived signals for convenience
  readonly token = computed(() => this._auth()?.token ?? null);
  readonly account = computed(() => {
    const a = this._auth();
    return a ? { accountId: a.accountId, email: a.email, tier: a.tier } : null;
  });
  readonly isAuthenticated = computed(() => this._auth() !== null);

  login(email: string, password: string) {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, { email, password }).pipe(
      tap((res) => this.setAuth(res)),
    );
  }

  register(email: string, password: string, companyName: string, tier: AccountTier = 'STANDARD') {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, { email, password, companyName, tier }).pipe(
      tap((res) => this.setAuth(res)),
    );
  }

  logout() {
    this._auth.set(null);
    localStorage.removeItem(this.storageKey);
    this.router.navigate(['/login']);
  }

  private setAuth(res: AuthResponse) {
    const state: AuthState = {
      token: res.token,
      accountId: res.accountId,
      email: res.email,
      tier: res.tier,
      expiresIn: res.expiresIn,
    };
    this._auth.set(state);
    localStorage.setItem(this.storageKey, JSON.stringify(state));
  }

  private loadFromStorage(): AuthState | null {
    const raw = localStorage.getItem(this.storageKey);
    return raw ? JSON.parse(raw) : null;
  }
}
