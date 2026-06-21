import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page, ProductDto, CategoryDto, AccountTier } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private http = inject(HttpClient);
  private apiUrl = '/api/catalog';

  getProducts(search?: string, categoryId?: string, page = 0, size = 20): Observable<Page<ProductDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (search) {
      params = params.set('search', search);
    }
    if (categoryId) {
      params = params.set('categoryId', categoryId);
    }
    return this.http.get<Page<ProductDto>>(`${this.apiUrl}/products`, { params });
  }

  getProductBySku(sku: string): Observable<ProductDto> {
    return this.http.get<ProductDto>(`${this.apiUrl}/products/${sku}`);
  }

  getTierPrice(sku: string, tier: AccountTier): Observable<ProductDto> {
    return this.http.get<ProductDto>(`${this.apiUrl}/products/${sku}/price`, {
      params: { tier },
    });
  }

  getCategories(): Observable<CategoryDto[]> {
    return this.http.get<CategoryDto[]>(`${this.apiUrl}/categories`);
  }
}
