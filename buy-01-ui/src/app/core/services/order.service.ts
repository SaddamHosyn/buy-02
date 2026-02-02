import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { Auth } from './auth';
import { environment } from '../../../environments/environment';

export interface ShippingAddress {
  fullName: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  postalCode: string;
  country: string;
  phoneNumber?: string;
}

export interface OrderItem {
  productId: string;
  productName: string;
  productDescription?: string;
  priceAtPurchase: number;
  quantity: number;
  subtotal: number;
  sellerId: string;
  sellerName?: string;
  thumbnailMediaId?: string;
}

export interface OrderStatusHistory {
  previousStatus: string | null;
  newStatus: string;
  changedAt: string;
  changedBy: string;
  changedByRole: string;
  reason?: string;
}

export interface Order {
  id: string;
  orderNumber: string;
  buyerId: string;
  buyerName: string;
  buyerEmail: string;
  items: OrderItem[];
  sellerIds: string[];
  subtotal: number;
  shippingCost: number;
  taxAmount: number;
  discountAmount: number;
  totalAmount: number;
  paymentMethod: string;
  paymentStatus: string;
  shippingAddress: ShippingAddress;
  deliveryNotes?: string;
  status: OrderStatus;
  statusHistory: OrderStatusHistory[];
  estimatedDeliveryDate?: string;
  actualDeliveryDate?: string;
  createdAt: string;
  updatedAt?: string;
  originalOrderId?: string;
  isRemoved: boolean;
}

export type OrderStatus = 
  | 'PENDING' 
  | 'CONFIRMED' 
  | 'PROCESSING' 
  | 'SHIPPED' 
  | 'DELIVERED' 
  | 'CANCELLED' 
  | 'RETURNED';

export interface CheckoutRequest {
  shippingAddress: ShippingAddress;
  deliveryNotes?: string;
}

export interface OrderSearchParams {
  status?: OrderStatus;
  startDate?: string;
  endDate?: string;
  search?: string;
  page?: number;
  size?: number;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(Auth);
  
  private readonly API_URL = `${environment.apiUrl}/orders`;
  
  // Signals for reactive state
  private readonly ordersSignal = signal<Order[]>([]);
  private readonly loadingSignal = signal<boolean>(false);
  private readonly currentOrderSignal = signal<Order | null>(null);
  
  readonly orders = this.ordersSignal.asReadonly();
  readonly isLoading = this.loadingSignal.asReadonly();
  readonly currentOrder = this.currentOrderSignal.asReadonly();
  
  /**
   * Checkout - Create order from cart (Pay on Delivery)
   */
  checkout(request: CheckoutRequest): Observable<Order> {
    this.loadingSignal.set(true);
    return this.http.post<Order>(`${this.API_URL}/checkout`, request).pipe(
      tap(order => {
        this.currentOrderSignal.set(order);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.loadingSignal.set(false);
        return throwError(() => error);
      })
    );
  }
  
  /**
   * Get orders for current user (buyer)
   */
  getMyOrders(params?: OrderSearchParams): Observable<Order[]> {
    this.loadingSignal.set(true);
    const httpParams = this.buildHttpParams(params);
    
    return this.http.get<Order[]>(`${this.API_URL}/my-orders`, { params: httpParams }).pipe(
      tap(orders => {
        this.ordersSignal.set(orders);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.loadingSignal.set(false);
        return throwError(() => error);
      })
    );
  }
  
  /**
   * Get orders for seller (orders containing seller's products)
   */
  getSellerOrders(params?: OrderSearchParams): Observable<Order[]> {
    this.loadingSignal.set(true);
    const httpParams = this.buildHttpParams(params);
    
    return this.http.get<Order[]>(`${this.API_URL}/seller-orders`, { params: httpParams }).pipe(
      tap(orders => {
        this.ordersSignal.set(orders);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.loadingSignal.set(false);
        return throwError(() => error);
      })
    );
  }
  
  /**
   * Get order by ID
   */
  getOrderById(orderId: string): Observable<Order> {
    this.loadingSignal.set(true);
    return this.http.get<Order>(`${this.API_URL}/${orderId}`).pipe(
      tap(order => {
        this.currentOrderSignal.set(order);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.loadingSignal.set(false);
        return throwError(() => error);
      })
    );
  }
  
  /**
   * Cancel an order (allowed for PENDING, CONFIRMED, PROCESSING)
   */
  cancelOrder(orderId: string, reason?: string): Observable<Order> {
    this.loadingSignal.set(true);
    return this.http.post<Order>(`${this.API_URL}/${orderId}/cancel`, { reason }).pipe(
      tap(order => {
        this.currentOrderSignal.set(order);
        this.updateOrderInList(order);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.loadingSignal.set(false);
        return throwError(() => error);
      })
    );
  }
  
  /**
   * Redo a cancelled order (creates new order with same items)
   */
  redoOrder(orderId: string): Observable<Order> {
    this.loadingSignal.set(true);
    return this.http.post<Order>(`${this.API_URL}/${orderId}/redo`, {}).pipe(
      tap(order => {
        this.currentOrderSignal.set(order);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.loadingSignal.set(false);
        return throwError(() => error);
      })
    );
  }
  
  /**
   * Remove order (soft delete - hides from user's view)
   */
  removeOrder(orderId: string): Observable<void> {
    this.loadingSignal.set(true);
    return this.http.delete<void>(`${this.API_URL}/${orderId}`).pipe(
      tap(() => {
        this.removeOrderFromList(orderId);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        this.loadingSignal.set(false);
        return throwError(() => error);
      })
    );
  }
  
  /**
   * Check if order can be cancelled based on status
   */
  canCancel(order: Order): boolean {
    return ['PENDING', 'CONFIRMED', 'PROCESSING'].includes(order.status);
  }
  
  /**
   * Check if order can be redone
   */
  canRedo(order: Order): boolean {
    return order.status === 'CANCELLED';
  }
  
  /**
   * Get status display label
   */
  getStatusLabel(status: OrderStatus): string {
    const labels: Record<OrderStatus, string> = {
      'PENDING': 'Pending',
      'CONFIRMED': 'Confirmed',
      'PROCESSING': 'Processing',
      'SHIPPED': 'Shipped',
      'DELIVERED': 'Delivered',
      'CANCELLED': 'Cancelled',
      'RETURNED': 'Returned'
    };
    return labels[status] || status;
  }
  
  /**
   * Get status color for styling
   */
  getStatusColor(status: OrderStatus): string {
    const colors: Record<OrderStatus, string> = {
      'PENDING': 'warn',
      'CONFIRMED': 'primary',
      'PROCESSING': 'primary',
      'SHIPPED': 'accent',
      'DELIVERED': 'primary',
      'CANCELLED': 'warn',
      'RETURNED': 'warn'
    };
    return colors[status] || 'primary';
  }
  
  // Helper methods
  private buildHttpParams(params?: OrderSearchParams): HttpParams {
    let httpParams = new HttpParams();
    if (params) {
      if (params.status) httpParams = httpParams.set('status', params.status);
      if (params.startDate) httpParams = httpParams.set('startDate', params.startDate);
      if (params.endDate) httpParams = httpParams.set('endDate', params.endDate);
      if (params.search) httpParams = httpParams.set('search', params.search);
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
    }
    return httpParams;
  }
  
  private updateOrderInList(updatedOrder: Order): void {
    const orders = this.ordersSignal();
    const index = orders.findIndex(o => o.id === updatedOrder.id);
    if (index !== -1) {
      const newOrders = [...orders];
      newOrders[index] = updatedOrder;
      this.ordersSignal.set(newOrders);
    }
  }
  
  private removeOrderFromList(orderId: string): void {
    const orders = this.ordersSignal().filter(o => o.id !== orderId);
    this.ordersSignal.set(orders);
  }
}
