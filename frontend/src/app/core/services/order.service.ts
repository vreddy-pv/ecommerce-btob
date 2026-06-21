import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page, OrderResponse, CreateOrderRequest, OrderStatus } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private http = inject(HttpClient);
  private apiUrl = '/api/orders';

  /**
   * POST /api/orders with {accountId, items} body.
   * CRITICAL (Pitfall 2): backend does NOT extract accountId from JWT —
   * accountId MUST be passed in the request body from authService.account().accountId.
   */
  createOrder(
    accountId: string,
    items: { productSku: string; quantity: number }[],
  ): Observable<OrderResponse> {
    const body: CreateOrderRequest = { accountId, items };
    return this.http.post<OrderResponse>(this.apiUrl, body);
  }

  getOrders(accountId: string, page = 0, size = 10): Observable<Page<OrderResponse>> {
    const params = new HttpParams()
      .set('accountId', accountId)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<Page<OrderResponse>>(this.apiUrl, { params });
  }

  getOrder(id: string): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(`${this.apiUrl}/${id}`);
  }

  getOrderStatus(id: string): Observable<OrderStatus> {
    return this.http.get<OrderStatus>(`${this.apiUrl}/${id}/status`);
  }
}
