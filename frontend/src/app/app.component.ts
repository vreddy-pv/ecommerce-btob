import { Component, inject, computed } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from './core/services/auth.service';
import { TierBadgeComponent } from './shared/components/tier-badge.component';
import { toSignal } from '@angular/core/rxjs-interop';
import { map, shareReplay } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule,
    TierBadgeComponent,
  ],
  template: `
    @if (authService.isAuthenticated()) {
      <mat-sidenav-container class="app-container">
        <mat-sidenav
          #sidenav
          [mode]="sidenavMode()"
          [opened]="sidenavOpened()"
          class="app-sidenav"
          [class.collapsed]="sidenavMode() === 'side' && isSmall()"
        >
          <div class="sidenav-header">
            <mat-icon class="sidenav-logo">local_shipping</mat-icon>
            @if (!isSmall()) {
              <span class="sidenav-title">B2B Auto Parts</span>
            }
          </div>

          <mat-nav-list>
            <a mat-list-item routerLink="/catalog" routerLinkActive="active-link">
              <mat-icon matListItemIcon>inventory_2</mat-icon>
              @if (!isSmall()) {
                <span matListItemTitle>Catalog</span>
              }
            </a>
            <a mat-list-item routerLink="/orders" routerLinkActive="active-link">
              <mat-icon matListItemIcon>receipt_long</mat-icon>
              @if (!isSmall()) {
                <span matListItemTitle>Orders</span>
              }
            </a>
          </mat-nav-list>
        </mat-sidenav>

        <mat-sidenav-content>
          <mat-toolbar color="primary" class="app-toolbar">
            @if (isSmall()) {
              <button mat-icon-button (click)="sidenav.toggle()" aria-label="Toggle menu">
                <mat-icon>menu</mat-icon>
              </button>
            }

            <span class="toolbar-title">
              @if (!isSmall()) {
                B2B Auto Parts
              }
            </span>

            <span class="toolbar-spacer"></span>

            @if (authService.account(); as account) {
              <app-tier-badge [tier]="account.tier" />
              <button mat-icon-button [matMenuTriggerFor]="userMenu" aria-label="User menu">
                <mat-icon>account_circle</mat-icon>
              </button>
              <mat-menu #userMenu="matMenu">
                <div class="user-info" mat-menu-item disabled>
                  <mat-icon>email</mat-icon>
                  <span>{{ account.email }}</span>
                </div>
                <mat-divider></mat-divider>
                <button mat-menu-item (click)="authService.logout()">
                  <mat-icon>logout</mat-icon>
                  <span>Sign Out</span>
                </button>
              </mat-menu>
            }
          </mat-toolbar>

          <main class="app-content">
            <router-outlet />
          </main>
        </mat-sidenav-content>
      </mat-sidenav-container>
    } @else {
      <router-outlet />
    }
  `,
  styles: [`
    .app-container {
      height: 100vh;
    }

    .app-sidenav {
      width: 240px;
      background-color: var(--md-sys-color-surface-variant, #F5F5F5);
      border-right: 1px solid rgba(0, 0, 0, 0.06);
      transition: width 0.2s ease;

      &.collapsed {
        width: 64px;
      }
    }

    .sidenav-header {
      display: flex;
      align-items: center;
      padding: 16px;
      height: 64px;
      box-sizing: border-box;
    }

    .sidenav-logo {
      color: var(--md-sys-color-primary, #1976D2);
      font-size: 28px;
      width: 28px;
      height: 28px;
    }

    .sidenav-title {
      margin-left: 12px;
      font-size: 16px;
      font-weight: 500;
      white-space: nowrap;
    }

    .active-link {
      background-color: rgba(25, 118, 210, 0.08) !important;
      color: var(--md-sys-color-primary, #1976D2) !important;

      mat-icon {
        color: var(--md-sys-color-primary, #1976D2);
      }
    }

    .app-toolbar {
      position: sticky;
      top: 0;
      z-index: 100;
      height: 64px;
    }

    .toolbar-title {
      font-size: 18px;
      font-weight: 500;
      margin-left: 8px;
    }

    .toolbar-spacer {
      flex: 1 1 auto;
    }

    .app-content {
      padding: 24px;
      min-height: calc(100vh - 64px);
      background-color: var(--md-sys-color-background, #FFFFFF);
    }

    .user-info {
      opacity: 0.7;
      font-size: 13px;
    }
  `],
})
export class AppComponent {
  authService = inject(AuthService);
  private breakpointObserver = inject(BreakpointObserver);

  private isSmall$ = this.breakpointObserver
    .observe(['(max-width: 1199px)'])
    .pipe(
      map(result => result.matches),
      shareReplay(1),
    );

  isSmall = toSignal(this.isSmall$, { initialValue: false });

  sidenavMode = computed(() => this.isSmall() ? 'over' as const : 'side' as const);
  sidenavOpened = computed(() => !this.isSmall());
}
