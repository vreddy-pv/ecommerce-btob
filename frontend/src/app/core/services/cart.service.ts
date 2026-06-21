import { Injectable, signal, computed } from '@angular/core';
import { ProductDto } from '../models/api.models';

export interface CartItem {
  product: ProductDto;
  quantity: number;
  unitPrice: number;  // tier-specific price
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private _items = signal<CartItem[]>([]);
  readonly items = this._items.asReadonly();
  readonly totalItems = computed(() => this._items().reduce((sum, i) => sum + i.quantity, 0));
  readonly totalAmount = computed(() => this._items().reduce((sum, i) => sum + i.unitPrice * i.quantity, 0));

  add(product: ProductDto, quantity: number, unitPrice: number) {
    const existing = this._items().find(i => i.product.sku === product.sku);
    if (existing) {
      this._items.update(items =>
        items.map(i => i.product.sku === product.sku
          ? { ...i, quantity: i.quantity + quantity }
          : i
        )
      );
    } else {
      this._items.update(items => [...items, { product, quantity, unitPrice }]);
    }
  }

  remove(sku: string) {
    this._items.update(items => items.filter(i => i.product.sku !== sku));
  }

  clear() {
    this._items.set([]);
  }
}
