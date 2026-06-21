import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DatePipe, CurrencyPipe, SlicePipe } from '@angular/common';
import { StatusChipComponent } from '../../shared/components/status-chip.component';
import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';
import { OrderResponse } from '../../core/models/api.models';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    RouterLink,
    DatePipe,
    CurrencyPipe,
    SlicePipe,
    StatusChipComponent,
  ],
  template: `
    @if (loading()) {
      <div class="loading-state">
        <mat-spinner diameter="40"></mat-spinner>
        <p>Loading orders...</p>
      </div>
    } @else if (error()) {
      <div class="error-state">
        <mat-icon>error_outline</mat-icon>
        <p>{{ error() }}</p>
      </div>
    } @else if (orders().length === 0) {
      <div class="empty-state">
        <mat-icon class="empty-icon">shopping_bag</mat-icon>
        <p>No orders yet. Place your first order from the catalog.</p>
        <a mat-raised-button color="accent" routerLink="/catalog">Browse Catalog</a>
      </div>
    } @else {
      <div class="orders-container">
        <h2>Order History</h2>

        <table mat-table [dataSource]="orders()" class="orders-table">
          <ng-container matColumnDef="id">
            <th mat-header-cell *matHeaderCellDef scope="col">Order ID</th>
            <td mat-cell *matCellDef="let order">
              <a [routerLink]="['/orders', order.id]" class="order-link">{{ order.id | slice:0:8 }}</a>
            </td>
          </ng-container>

          <ng-container matColumnDef="date">
            <th mat-header-cell *matHeaderCellDef scope="col">Date Placed</th>
            <td mat-cell *matCellDef="let order">{{ order.createdAt | date:'medium' }}</td>
          </ng-container>

          <ng-container matColumnDef="items">
            <th mat-header-cell *matHeaderCellDef scope="col">Items</th>
            <td mat-cell *matCellDef="let order">{{ order.items.length }}</td>
          </ng-container>

          <ng-container matColumnDef="total">
            <th mat-header-cell *matHeaderCellDef scope="col">Total</th>
            <td mat-cell *matCellDef="let order">{{ order.totalAmount | currency }}</td>
          </ng-container>

          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef scope="col">Status</th>
            <td mat-cell *matCellDef="let order">
              <app-status-chip [status]="order.status" />
            </td>
          </ng-container>

          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef scope="col">Actions</th>
            <td mat-cell *matCellDef="let order">
              <a mat-icon-button [routerLink]="['/orders', order.id]" aria-label="View order details">
                <mat-icon>visibility</mat-icon>
              </a>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>

        <mat-paginator
          [length]="totalElements()"
          [pageSize]="size()"
          [pageIndex]="page()"
          [pageSizeOptions]="[5, 10, 20]"
          (page)="onPageChange($event)">
        </mat-paginator>
      </div>
    }

    <a mat-fab color="accent" routerLink="/catalog" class="fab-create" aria-label="Create Order">
      <mat-icon>add</mat-icon>
    </a>
  `,
  styles: [`
    .loading-state,
    .error-state,
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 64px 24px;
      text-align: center;

      mat-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
        color: rgba(0, 0, 0, 0.3);
        margin-bottom: 16px;
      }

      p {
        font-size: 14px;
        color: rgba(0, 0, 0, 0.6);
        margin: 0 0 16px 0;
      }

      .empty-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
      }
    }

    .orders-container {
      padding: 24px;
    }

    h2 {
      margin: 0 0 24px 0;
      font-size: 20px;
      font-weight: 500;
    }

    .orders-table {
      width: 100%;

      .order-link {
        color: #1976D2;
        text-decoration: none;
        font-weight: 500;

        &:hover {
          text-decoration: underline;
        }
      }
    }

    .fab-create {
      position: fixed;
      bottom: 24px;
      right: 24px;
    }
  `],
})
export class OrdersComponent implements OnInit {
  private orderService = inject(OrderService);
  private authService = inject(AuthService);
  private router = inject(Router);

  orders = signal<OrderResponse[]>([]);
  totalElements = signal(0);
  page = signal(0);
  size = signal(10);
  loading = signal(false);
  error = signal<string | null>(null);

  accountId = computed(() => this.authService.account()?.accountId);

  displayedColumns = ['id', 'date', 'items', 'total', 'status', 'actions'];

  ngOnInit(): void {
    if (!this.accountId()) {
      this.router.navigate(['/login']);
      return;
    }
    this.loadOrders(this.page(), this.size());
  }

  loadOrders(p: number, s: number): void {
    const accountId = this.accountId();
    if (!accountId) return;

    this.loading.set(true);
    this.orderService.getOrders(accountId, p, s).subscribe({
      next: (page) => {
        this.orders.set(page.content);
        this.totalElements.set(page.totalElements);
        this.page.set(page.number);
        this.size.set(page.size);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Something went wrong loading orders. Please try again or contact support.');
        this.loading.set(false);
      },
    });
  }

  onPageChange(event: PageEvent): void {
    this.loadOrders(event.pageIndex, event.pageSize);
  }
}
