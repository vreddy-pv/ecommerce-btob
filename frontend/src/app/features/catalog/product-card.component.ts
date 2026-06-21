import { Component, input, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { ProductDto, AccountTier } from '../../core/models/api.models';
import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatBadgeModule, MatSnackBarModule, MatIconModule],
  template: `
    <mat-card class="product-card">
      <div class="product-image">
        <mat-icon>build</mat-icon>
      </div>
      <mat-card-content>
        <h3 class="product-name">{{ product().name }}</h3>
        <p class="product-sku">SKU: {{ product().sku }}</p>
        <p class="product-description">{{ product().description }}</p>
        <div class="product-pricing">
          <span class="price">{{ tierPrice() | number:'1.2-2' }}</span>
          <span class="tier-label">{{ tier() }} price</span>
        </div>
        <div class="product-inventory" [class.low-stock]="isLowStock()">
          <span
            [matBadge]="isLowStock() ? 'Low stock' : null"
            matBadgeSize="small"
            matBadgeColor="warn"
          >
            {{ product().inventoryLevel }} in stock
          </span>
        </div>
      </mat-card-content>
      <mat-card-actions>
        <button mat-raised-button color="primary" (click)="addToOrder()">
          Add to Order
        </button>
      </mat-card-actions>
    </mat-card>
  `,
  styles: [`
    .product-card {
      display: flex;
      flex-direction: column;
      height: 100%;
    }
    .product-image {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 120px;
      background-color: #F5F5F5;
      color: #9E9E9E;
    }
    .product-image mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
    }
    mat-card-content {
      flex: 1;
      padding: 16px;
    }
    .product-name {
      font-size: 14px;
      font-weight: 500;
      margin: 0 0 4px 0;
      line-height: 1.3;
    }
    .product-sku {
      font-size: 12px;
      color: #757575;
      margin: 0 0 8px 0;
    }
    .product-description {
      font-size: 12px;
      color: #616161;
      margin: 0 0 12px 0;
      line-height: 1.4;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .product-pricing {
      display: flex;
      align-items: baseline;
      gap: 8px;
      margin-bottom: 8px;
    }
    .price {
      font-size: 14px;
      font-weight: 500;
      color: #1976D2;
    }
    .tier-label {
      font-size: 11px;
      color: #9E9E9E;
      text-transform: uppercase;
    }
    .product-inventory {
      font-size: 12px;
      color: #388E3C;
    }
    .product-inventory.low-stock {
      color: #D32F2F;
    }
    mat-card-actions {
      padding: 8px 16px 16px;
    }
    mat-card-actions button {
      width: 100%;
    }
  `],
})
export class ProductCardComponent {
  product = input.required<ProductDto>();
  tier = input.required<AccountTier>();

  private cartService = inject(CartService);
  private snackBar = inject(MatSnackBar);

  tierPrice = computed(() => {
    const p = this.product();
    const t = this.tier();
    const tierEntry = p.tierPricing?.find(tp => tp.tier === t);
    return tierEntry?.price ?? p.basePrice;
  });

  isLowStock = computed(() => this.product().inventoryLevel < 10);

  addToOrder(): void {
    const product = this.product();
    const price = this.tierPrice();
    this.cartService.add(product, 1, price);
    this.snackBar.open(`Added ${product.name} to order`, 'OK', {
      duration: 3000,
    });
  }
}
