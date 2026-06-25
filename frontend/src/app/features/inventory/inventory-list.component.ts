import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { CatalogService } from '../../core/services/catalog.service';
import { ProductDto } from '../../core/models/api.models';

@Component({
  selector: 'app-inventory-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    FormsModule,
  ],
  template: `
    <div class="inventory-container">
      <h2>Inventory Management</h2>

      @if (loading()) {
        <div class="loading-state">
          <mat-spinner diameter="40"></mat-spinner>
          <p>Loading inventory...</p>
        </div>
      } @else {
        <table mat-table [dataSource]="products()" class="inventory-table">
          <ng-container matColumnDef="sku">
            <th mat-header-cell *matHeaderCellDef scope="col">SKU</th>
            <td mat-cell *matCellDef="let p">{{ p.sku }}</td>
          </ng-container>

          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef scope="col">Name</th>
            <td mat-cell *matCellDef="let p">{{ p.name }}</td>
          </ng-container>

          <ng-container matColumnDef="inventoryLevel">
            <th mat-header-cell *matHeaderCellDef scope="col">On Hand</th>
            <td mat-cell *matCellDef="let p">{{ p.inventoryLevel }}</td>
          </ng-container>

          <ng-container matColumnDef="reservedInventory">
            <th mat-header-cell *matHeaderCellDef scope="col">Reserved</th>
            <td mat-cell *matCellDef="let p">{{ p.reservedInventory }}</td>
          </ng-container>

          <ng-container matColumnDef="available">
            <th mat-header-cell *matHeaderCellDef scope="col">Available</th>
            <td mat-cell *matCellDef="let p">{{ p.inventoryLevel - p.reservedInventory }}</td>
          </ng-container>

          <ng-container matColumnDef="reorderPoint">
            <th mat-header-cell *matHeaderCellDef scope="col">Reorder Point</th>
            <td mat-cell *matCellDef="let p">{{ p.reorderPoint }}</td>
          </ng-container>

          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef scope="col">Adjust</th>
            <td mat-cell *matCellDef="let p">
              <div class="adjust-row">
                <input matInput type="number" [(ngModel)]="deltas[p.sku]" class="delta-input"
                       placeholder="+/-" [value]="deltas[p.sku] || 0" />
                <button mat-raised-button color="primary" (click)="adjust(p.sku)"
                        [disabled]="adjusting.has(p.sku)">
                  {{ adjusting.has(p.sku) ? '...' : 'Adjust' }}
                </button>
              </div>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      }
    </div>
  `,
  styles: [`
    .inventory-container {
      padding: 24px;

      h2 {
        margin: 0 0 24px 0;
        font-size: 20px;
        font-weight: 500;
      }
    }

    .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 64px 24px;
    }

    .inventory-table {
      width: 100%;
    }

    .adjust-row {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .delta-input {
      width: 80px;
      padding: 4px 8px;
      border: 1px solid rgba(0, 0, 0, 0.12);
      border-radius: 4px;
      font-size: 14px;
    }
  `],
})
export class InventoryListComponent implements OnInit {
  private catalogService = inject(CatalogService);
  private snackBar = inject(MatSnackBar);

  products = signal<ProductDto[]>([]);
  loading = signal(false);
  deltas: Record<string, number> = {};
  adjusting = new Set<string>();

  displayedColumns = ['sku', 'name', 'inventoryLevel', 'reservedInventory', 'available', 'reorderPoint', 'actions'];

  ngOnInit(): void {
    this.loadProducts();
  }

  private loadProducts(): void {
    this.loading.set(true);
    this.catalogService.getProducts(undefined, undefined, 0, 100).subscribe({
      next: (page) => {
        this.products.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  adjust(sku: string): void {
    const delta = this.deltas[sku];
    if (delta === undefined || delta === 0) return;

    this.adjusting.add(sku);
    this.catalogService.adjustInventory(sku, delta).subscribe({
      next: () => {
        this.adjusting.delete(sku);
        this.deltas[sku] = 0;
        this.snackBar.open(`Inventory adjusted for ${sku}`, 'OK', { duration: 3000 });
        this.loadProducts();
      },
      error: () => {
        this.adjusting.delete(sku);
        this.snackBar.open(`Failed to adjust inventory for ${sku}`, 'OK', { duration: 3000 });
      },
    });
  }
}
