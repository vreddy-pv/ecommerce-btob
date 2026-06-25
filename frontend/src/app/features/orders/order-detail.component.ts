import { Component, inject, signal, OnInit, input } from '@angular/core';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { StatusChipComponent } from '../../shared/components/status-chip.component';
import { OrderService } from '../../core/services/order.service';
import { OrderResponse } from '../../core/models/api.models';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    RouterLink,
    DatePipe,
    CurrencyPipe,
    StatusChipComponent,
  ],
  template: `
    @if (loading()) {
      <div class="loading-state">
        <mat-spinner diameter="40"></mat-spinner>
        <p>Loading order...</p>
      </div>
    } @else if (error()) {
      <div class="error-state">
        <mat-icon>error_outline</mat-icon>
        <p>{{ error() }}</p>
        <a mat-raised-button routerLink="/orders">
          <mat-icon>arrow_back</mat-icon>
          Back to Orders
        </a>
      </div>
    } @else {
      @if (order(); as o) {
        <div class="order-detail">
          <div class="detail-header">
            <a mat-button routerLink="/orders" class="back-btn">
              <mat-icon>arrow_back</mat-icon>
              Back to Orders
            </a>
            <h2>Order Details</h2>
          </div>

          <div class="order-card">
            <div class="order-info">
              <div class="info-row">
                <span class="label">Order ID</span>
                <span class="value">{{ o.id }}</span>
              </div>
              <div class="info-row">
                <span class="label">Date Placed</span>
                <span class="value">{{ o.createdAt | date:'medium' }}</span>
              </div>
              <div class="info-row">
                <span class="label">Status</span>
                <app-status-chip [status]="o.status" />
              </div>
              @if (o.status === 'PENDING' || o.status === 'CONFIRMED') {
                <div class="info-row">
                  <span class="label"></span>
                  <button mat-raised-button color="warn" (click)="cancelOrder()" [disabled]="cancelling()">
                    {{ cancelling() ? 'Cancelling...' : 'Cancel Order' }}
                  </button>
                </div>
              }
              <div class="info-row">
                <span class="label">Total Amount</span>
                <span class="value total">{{ o.totalAmount | currency }}</span>
              </div>
              @if (o.creditUsed > 0) {
                <div class="info-row">
                  <span class="label">Credit Used</span>
                  <span class="value">{{ o.creditUsed | currency }}</span>
                </div>
              }
            </div>

            <h3>Line Items</h3>
            <table mat-table [dataSource]="o.items" class="items-table">
            <ng-container matColumnDef="sku">
              <th mat-header-cell *matHeaderCellDef scope="col">SKU</th>
              <td mat-cell *matCellDef="let item">{{ item.productSku }}</td>
            </ng-container>

            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef scope="col">Product Name</th>
              <td mat-cell *matCellDef="let item">{{ item.productName }}</td>
            </ng-container>

            <ng-container matColumnDef="quantity">
              <th mat-header-cell *matHeaderCellDef scope="col">Quantity</th>
              <td mat-cell *matCellDef="let item">{{ item.quantity }}</td>
            </ng-container>

            <ng-container matColumnDef="unitPrice">
              <th mat-header-cell *matHeaderCellDef scope="col">Unit Price</th>
              <td mat-cell *matCellDef="let item">{{ item.unitPrice | currency }}</td>
            </ng-container>

            <ng-container matColumnDef="lineTotal">
              <th mat-header-cell *matHeaderCellDef scope="col">Line Total</th>
              <td mat-cell *matCellDef="let item">{{ item.totalPrice | currency }}</td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="itemColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: itemColumns;"></tr>
          </table>
        </div>
      </div>
      }
    }
  `,
  styles: [`
    .loading-state,
    .error-state {
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
    }

    .detail-header {
      margin-bottom: 24px;

      .back-btn {
        margin-bottom: 8px;
      }

      h2 {
        margin: 0;
        font-size: 20px;
        font-weight: 500;
      }
    }

    .order-card {
      background: #FFFFFF;
      border-radius: 8px;
      padding: 24px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);

      h3 {
        font-size: 16px;
        font-weight: 500;
        margin: 24px 0 16px 0;
      }
    }

    .order-info {
      .info-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 0;
        border-bottom: 1px solid rgba(0, 0, 0, 0.06);

        &:last-child {
          border-bottom: none;
        }

        .label {
          font-size: 14px;
          color: rgba(0, 0, 0, 0.6);
        }

        .value {
          font-size: 14px;
          font-weight: 500;

          &.total {
            font-size: 18px;
            color: #1976D2;
          }
        }
      }
    }

    .items-table {
      width: 100%;
    }
  `],
})
export class OrderDetailComponent implements OnInit {
  private orderService = inject(OrderService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  order = signal<OrderResponse | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  cancelling = signal(false);

  itemColumns = ['sku', 'name', 'quantity', 'unitPrice', 'lineTotal'];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('Invalid order ID.');
      return;
    }
    this.loadOrder(id);
  }

  cancelOrder(): void {
    const o = this.order();
    if (!o) return;

    if (!confirm('Are you sure you want to cancel this order?')) return;

    this.cancelling.set(true);
    this.orderService.cancelOrder(o.id).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.cancelling.set(false);
        this.snackBar.open('Order cancelled successfully', 'OK', { duration: 3000 });
      },
      error: () => {
        this.cancelling.set(false);
        this.snackBar.open('Failed to cancel order. Please try again.', 'OK', { duration: 3000 });
      },
    });
  }

  private loadOrder(id: string): void {
    this.loading.set(true);
    this.orderService.getOrder(id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Something went wrong loading this order. Please try again or contact support.');
        this.loading.set(false);
      },
    });
  }
}
