import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { RouterLink } from '@angular/router';
import { ProductDto, CategoryDto, AccountTier, Page } from '../../core/models/api.models';
import { CatalogService } from '../../core/services/catalog.service';
import { AuthService } from '../../core/services/auth.service';
import { ProductCardComponent } from './product-card.component';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
    MatSelectModule,
    MatCardModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    ProductCardComponent,
  ],
  template: `
    <div class="catalog-container">
      @if (lowStockProducts().length > 0) {
        <mat-card class="low-stock-widget">
          <mat-card-content>
            <div class="low-stock-header">
              <mat-icon color="warn">warning</mat-icon>
              <h3>Low Stock Alert</h3>
              <a mat-button routerLink="/inventory" class="view-all-link">View All</a>
            </div>
            <div class="low-stock-items">
              @for (p of lowStockProducts(); track p.sku) {
                <div class="low-stock-item">
                  <span class="item-sku">{{ p.sku }}</span>
                  <span class="item-name">{{ p.name }}</span>
                  <span class="item-stock">{{ p.inventoryLevel - p.reservedInventory }} available</span>
                </div>
              }
            </div>
          </mat-card-content>
        </mat-card>
      }

      <!-- Toolbar: Search + Filters + Sort -->
      <div class="catalog-toolbar">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Search parts by name, SKU, or description</mat-label>
          <mat-icon matPrefix>search</mat-icon>
          <input matInput [value]="search()" (input)="onSearchInput($event)" />
        </mat-form-field>

        <mat-chip-listbox
          class="category-chips"
          [multiple]="false"
          [value]="selectedCategoryId()"
          (selectionChange)="onCategoryChange($event)"
        >
          <mat-chip-option value="">All</mat-chip-option>
          @for (category of categories(); track category.id) {
            <mat-chip-option [value]="category.id">{{ category.name }}</mat-chip-option>
          }
        </mat-chip-listbox>

        <mat-form-field appearance="outline" class="sort-field">
          <mat-label>Sort by</mat-label>
          <mat-select [value]="sortOption()" (selectionChange)="onSortChange($event.value)">
            <mat-option value="name">Name</mat-option>
            <mat-option value="price">Price</mat-option>
            <mat-option value="sku">SKU</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      <!-- Loading State -->
      @if (loading()) {
        <div class="loading-state">
          <mat-spinner diameter="40"></mat-spinner>
          <p>Loading catalog...</p>
        </div>
      }

      <!-- Error State -->
      @if (error()) {
        <mat-card class="error-state">
          <mat-icon color="warn">error_outline</mat-icon>
          <p>{{ error() }}</p>
          <button mat-raised-button color="primary" (click)="reload()">Retry</button>
        </mat-card>
      }

      <!-- Product Grid -->
      @if (!loading() && !error()) {
        <div class="product-grid">
          @for (product of sortedProducts(); track product.sku) {
            <app-product-card [product]="product" [tier]="currentTier()" />
          } @empty {
            <div class="empty-state">
              <mat-icon>inventory_2</mat-icon>
              <h2>No parts found</h2>
              <p>Try adjusting your search or filter criteria to find the parts you need.</p>
            </div>
          }
        </div>

        <!-- Paginator -->
        @if (totalElements() > 0) {
          <mat-paginator
            [length]="totalElements()"
            [pageSize]="pageSize()"
            [pageIndex]="pageIndex()"
            [pageSizeOptions]="[10, 20, 50]"
            (page)="onPageChange($event)"
            showFirstLastButtons
          ></mat-paginator>
        }
      }
    </div>
  `,
  styles: [`
    .catalog-container {
      padding: 24px;
    }
    .low-stock-widget {
      margin-bottom: 24px;
      background-color: #FFF3E0;
    }
    .low-stock-widget mat-card-content {
      padding: 16px;
    }
    .low-stock-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
    }
    .low-stock-header h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 500;
      flex: 1;
    }
    .low-stock-header .view-all-link {
      font-size: 13px;
    }
    .low-stock-items {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .low-stock-item {
      display: flex;
      align-items: center;
      gap: 16px;
      font-size: 13px;
    }
    .low-stock-item .item-sku {
      font-weight: 500;
      min-width: 80px;
    }
    .low-stock-item .item-name {
      flex: 1;
      color: rgba(0,0,0,0.7);
    }
    .low-stock-item .item-stock {
      color: #D32F2F;
      font-weight: 500;
    }
    .catalog-toolbar {
      display: flex;
      flex-wrap: wrap;
      gap: 16px;
      align-items: flex-start;
      margin-bottom: 24px;
    }
    .search-field {
      flex: 1;
      min-width: 250px;
    }
    .category-chips {
      flex: 1;
      min-width: 200px;
    }
    .sort-field {
      width: 150px;
    }
    .product-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 24px;
    }
    .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 64px 0;
      gap: 16px;
      color: #757575;
    }
    .error-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px;
      text-align: center;
      gap: 16px;
    }
    .error-state mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
    }
    .empty-state {
      grid-column: 1 / -1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 64px 0;
      text-align: center;
      color: #757575;
    }
    .empty-state mat-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      margin-bottom: 16px;
      color: #BDBDBD;
    }
    .empty-state h2 {
      font-size: 20px;
      font-weight: 500;
      margin: 0 0 8px 0;
    }
    .empty-state p {
      font-size: 14px;
      margin: 0;
    }
    mat-paginator {
      margin-top: 24px;
    }
  `],
})
export class CatalogComponent implements OnInit {
  private catalogService = inject(CatalogService);
  private authService = inject(AuthService);

  // State signals
  search = signal('');
  selectedCategoryId = signal('');
  sortOption = signal<'name' | 'price' | 'sku'>('name');
  products = signal<ProductDto[]>([]);
  categories = signal<CategoryDto[]>([]);
  pageIndex = signal(0);
  pageSize = signal(20);
  totalElements = signal(0);
  loading = signal(true);
  error = signal<string | null>(null);
  lowStockProducts = signal<ProductDto[]>([]);

  // Derived
  currentTier = computed<AccountTier>(() => this.authService.account()?.tier ?? 'STANDARD');

  sortedProducts = computed(() => {
    const items = [...this.products()];
    const sort = this.sortOption();
    switch (sort) {
      case 'name':
        return items.sort((a, b) => a.name.localeCompare(b.name));
      case 'price': {
        const tier = this.currentTier();
        return items.sort((a, b) => {
          const priceA = a.tierPricing?.find(tp => tp.tier === tier)?.price ?? a.basePrice;
          const priceB = b.tierPricing?.find(tp => tp.tier === tier)?.price ?? b.basePrice;
          return priceA - priceB;
        });
      }
      case 'sku':
        return items.sort((a, b) => a.sku.localeCompare(b.sku));
      default:
        return items;
    }
  });

  ngOnInit(): void {
    // Load categories
    this.catalogService.getCategories().subscribe({
      next: (cats) => this.categories.set(cats),
      error: () => {}, // silently fail - categories are optional
    });

    // Load low stock products
    this.catalogService.getLowStockProducts().subscribe({
      next: (items) => this.lowStockProducts.set(items),
      error: () => {},
    });

    // Load products
    this.loadProducts();
  }

  private loadProducts(): void {
    this.loading.set(true);
    this.error.set(null);

    const searchTerm = this.search() || undefined;
    const categoryId = this.selectedCategoryId() || undefined;
    const page = this.pageIndex();
    const size = this.pageSize();

    this.catalogService.getProducts(searchTerm, categoryId, page, size).subscribe({
      next: (page) => {
        this.products.set(page.content);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Something went wrong loading the catalog. Please try again or contact support.');
        this.loading.set(false);
      },
    });
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.search.set(value);
    this.pageIndex.set(0); // Reset to first page on new search
    this.loadProducts();
  }

  onCategoryChange(event: any): void {
    const value = event.value || '';
    this.selectedCategoryId.set(value);
    this.pageIndex.set(0);
    this.loadProducts();
  }

  onSortChange(value: 'name' | 'price' | 'sku'): void {
    this.sortOption.set(value);
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadProducts();
  }

  reload(): void {
    this.loadProducts();
  }
}
