import { Component, inject, Inject } from '@angular/core';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CurrencyPipe } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';
import { CatalogService } from '../../core/services/catalog.service';
import { CartItem } from '../../core/services/cart.service';

@Component({
  selector: 'app-cart-sidebar',
  standalone: true,
  imports: [
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatDividerModule,
    CurrencyPipe,
  ],
  template: `
    <div class="cart-sidebar">
      <div class="cart-header">
        <h3>Shopping Cart</h3>
        <span class="cart-count">{{ cartService.totalItems() }} items</span>
      </div>

      @if (cartService.totalItems() === 0) {
        <div class="cart-empty">
          <mat-icon class="empty-icon">shopping_cart</mat-icon>
          <p>Your cart is empty. Add parts from the catalog.</p>
        </div>
      } @else {
        <mat-list class="cart-items">
          @for (item of cartService.items(); track item.product.sku) {
            <mat-list-item class="cart-item">
              <div class="item-details">
                <span class="item-name">{{ item.product.name }}</span>
                <span class="item-sku">SKU: {{ item.product.sku }}</span>
                <div class="item-pricing">
                  <span class="item-qty">Qty: {{ item.quantity }}</span>
                  <span class="item-unit-price">{{ item.unitPrice | currency }}</span>
                </div>
                <span class="item-line-total">Line Total: {{ item.unitPrice * item.quantity | currency }}</span>
              </div>
              <button mat-icon-button
                      aria-label="Remove item"
                      class="remove-btn"
                      (click)="cartService.remove(item.product.sku)">
                <mat-icon>delete_outline</mat-icon>
              </button>
            </mat-list-item>
          }
        </mat-list>

        <mat-divider></mat-divider>

        <div class="cart-footer">
          <div class="cart-total">
            <span>Total</span>
            <span class="total-amount">{{ cartService.totalAmount() | currency }}</span>
          </div>

          <div class="cart-actions">
            <button mat-stroked-button
                    color="warn"
                    class="clear-btn"
                    (click)="cartService.clear()">
              Clear Cart
            </button>
            <button mat-raised-button
                    color="accent"
                    class="place-order-btn"
                    [disabled]="cartService.totalItems() === 0"
                    (click)="openConfirmationDialog()">
              Place Order
            </button>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .cart-sidebar {
      display: flex;
      flex-direction: column;
      height: 100%;
      padding: 16px;
    }

    .cart-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 500;
      }

      .cart-count {
        font-size: 13px;
        color: rgba(0, 0, 0, 0.6);
      }
    }

    .cart-empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px 16px;
      text-align: center;

      .empty-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
        color: rgba(0, 0, 0, 0.3);
        margin-bottom: 16px;
      }

      p {
        color: rgba(0, 0, 0, 0.6);
        font-size: 14px;
        margin: 0;
      }
    }

    .cart-items {
      flex: 1;
      overflow-y: auto;
    }

    .cart-item {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      height: auto !important;
      padding: 8px 0;

      .item-details {
        display: flex;
        flex-direction: column;
        flex: 1;

        .item-name {
          font-size: 14px;
          font-weight: 500;
        }

        .item-sku {
          font-size: 12px;
          color: rgba(0, 0, 0, 0.5);
        }

        .item-pricing {
          display: flex;
          gap: 12px;
          font-size: 13px;
          margin-top: 4px;
        }

        .item-line-total {
          font-size: 13px;
          font-weight: 500;
          color: #1976D2;
          margin-top: 2px;
        }
      }

      .remove-btn {
        flex-shrink: 0;
      }
    }

    .cart-footer {
      padding-top: 16px;

      .cart-total {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        font-size: 16px;
        font-weight: 500;

        .total-amount {
          font-size: 20px;
          color: #1976D2;
        }
      }

      .cart-actions {
        display: flex;
        gap: 8px;

        .clear-btn {
          flex: 0 0 auto;
        }

        .place-order-btn {
          flex: 1;
        }
      }
    }
  `],
})
export class CartSidebarComponent {
  cartService = inject(CartService);
  private orderService = inject(OrderService);
  private authService = inject(AuthService);
  private catalogService = inject(CatalogService);
  private router = inject(Router);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  openConfirmationDialog(): void {
    const items = this.cartService.items();
    const stockChecks = items.map(item =>
      this.catalogService.getProductStock(item.product.sku)
    );

    forkJoin(stockChecks).subscribe({
      next: (stockResults) => {
        let hasError = false;
        for (let i = 0; i < items.length; i++) {
          const available = stockResults[i].available;
          if (available < items[i].quantity) {
            this.snackBar.open(
              `Insufficient stock for ${items[i].product.sku}: ${available} available, ${items[i].quantity} requested`,
              'OK', { duration: 5000 }
            );
            hasError = true;
          }
        }
        if (hasError) return;
        this.openConfirmDialog();
      },
      error: () => {
        this.openConfirmDialog();
      },
    });
  }

  private openConfirmDialog(): void {
    const dialogRef = this.dialog.open(CartConfirmationDialogComponent, {
      width: '480px',
      data: {
        items: this.cartService.items(),
        total: this.cartService.totalAmount(),
      },
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean | undefined) => {
      if (confirmed) {
        this.submitOrder();
      }
    });
  }

  private submitOrder(): void {
    const account = this.authService.account();
    if (!account) {
      this.router.navigate(['/login']);
      return;
    }

    const items = this.cartService.items().map(item => ({
      productSku: item.product.sku,
      quantity: item.quantity,
    }));

    this.orderService.createOrder(account.accountId, items).subscribe({
      next: () => {
        this.snackBar.open('Order placed successfully', 'Close', { duration: 3000 });
        this.cartService.clear();
        this.router.navigate(['/orders']);
      },
      error: (err) => {
        const message = err?.error?.message ?? 'Failed to place order. Please try again.';
        this.snackBar.open(message, 'Close', { duration: 5000 });
      },
    });
  }
}

@Component({
  selector: 'app-cart-confirmation-dialog',
  standalone: true,
  imports: [MatButtonModule, MatDialogModule, MatDividerModule, CurrencyPipe],
  template: `
    <h2 mat-dialog-title>Confirm Order</h2>
    <mat-dialog-content>
      <p>Are you sure you want to place this order?</p>
      <div class="order-summary">
        @for (item of data.items; track item.product.sku) {
          <div class="summary-item">
            <span>{{ item.product.name }} (x{{ item.quantity }})</span>
            <span>{{ item.unitPrice * item.quantity | currency }}</span>
          </div>
        }
        <mat-divider></mat-divider>
        <div class="summary-total">
          <strong>Total</strong>
          <strong>{{ data.total | currency }}</strong>
        </div>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="accent" [mat-dialog-close]="true">Confirm</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .order-summary {
      margin-top: 16px;

      .summary-item {
        display: flex;
        justify-content: space-between;
        padding: 8px 0;
        font-size: 14px;
      }

      .summary-total {
        display: flex;
        justify-content: space-between;
        padding: 12px 0;
        font-size: 16px;
      }
    }

    mat-divider {
      margin: 8px 0;
    }
  `],
})
export class CartConfirmationDialogComponent {
  data: { items: CartItem[]; total: number } = inject(MAT_DIALOG_DATA);
}
