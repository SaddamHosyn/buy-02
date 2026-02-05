import { Injectable, inject, signal, effect } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { Auth } from './auth';
import { environment } from '../../../environments/environment';

// ==================== Status Enums ====================

export enum OrderStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  PROCESSING = 'PROCESSING',
  SHIPPED = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED',
  RETURNED = 'RETURNED',
}

// ==================== Interfaces ====================

export interface ShippingAddress {
  fullName: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  postalCode: string;
  country: string;
  phoneNumber?: string;
}

export interface OrderStatusHistory {
  previousStatus: OrderStatus | null;
  newStatus: OrderStatus;
  changedAt: string;
  changedBy: string;
  changedByRole: string;
  reason?: string;
}

export interface OrderItem {
  productId: string;
  items?: any;
  productName: string;
  productDescription: string;
  quantity: number;
  price: number;
  priceAtPurchase: number;
  subtotal: number;
  sellerId: string;
  sellerName?: string;
  thumbnailMediaId?: string;
}

export interface Order {
  id: string;
  orderNumber: string;
  items: OrderItem[];
  totalAmount: number;
  subtotal: number;
  shippingCost: number;
  taxAmount: number;
  discountAmount: number;
  status: OrderStatus;
  statusHistory: OrderStatusHistory[];
  shippingAddress: ShippingAddress;
  createdAt: string;
  buyerId: string;
  buyerName: string;
  buyerEmail?: string;
  paymentMethod?: string;
  paymentStatus?: string;
  deliveryNotes?: string;
  estimatedDeliveryDate?: string;
  actualDeliveryDate?: string;
}

export interface OrderSearchParams {
  keyword?: string;
  status?: OrderStatus;
  dateFrom?: string;
  dateTo?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

export interface BuyerStats {
  userId: string;
  totalSpent: number;
  totalOrders: number;
  pendingOrders: number;
  deliveredOrders: number;
  cancelledOrders: number;
  topProductsByAmount: ProductStat[];
  mostBoughtProducts: ProductStat[];
  averageOrderValue: number;
}

export interface ProductStat {
  productId: string;
  productName: string;
  quantity: number;
  totalAmount: number;
}

export interface SellerStats {
  sellerId: string;
  totalEarned: number;
  totalOrders: number;
  pendingOrders: number;
  deliveredOrders: number;
  cancelledOrders: number;
  totalProductsSold: number;
  bestSellingByAmount: ProductStat[];
  bestSellingByQuantity: ProductStat[];
  averageOrderValue: number;
}

// ==================== Service ====================

@Injectable({
  providedIn: 'root',
})
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(Auth);

  // API URL from environment configuration
  private readonly API_URL = environment.ordersUrl;
  private readonly PROFILE_URL = environment.profileUrl;

  // State management with signals (for template use)
  readonly currentOrderSignal = signal<Order | null>(null);
  readonly ordersSignal = signal<Order[]>([]);
  readonly isLoadingSignal = signal<boolean>(false);

  // Legacy Observable-based state (for backward compatibility)
  private currentOrderSubject = new BehaviorSubject<Order | null>(null);
  public currentOrder = this.currentOrderSubject.asObservable();

  private ordersSubject = new BehaviorSubject<Order[]>([]);
  public orders = this.ordersSubject.asObservable();

  private isLoadingSubject = new BehaviorSubject<boolean>(false);
  public isLoading = this.isLoadingSubject.asObservable();

  constructor() {
    // Sync signal changes to subject for legacy observers
    effect(() => {
      this.currentOrderSubject.next(this.currentOrderSignal());
      this.ordersSubject.next(this.ordersSignal());
      this.isLoadingSubject.next(this.isLoadingSignal());
    });
  }

  /**
   * Helper to get common headers (e.g. User ID)
   */
  private getHeaders(): HttpHeaders {
    const userId = this.authService.currentUser()?.id || '';
    return new HttpHeaders({
      'X-User-Id': userId,
    });
  }

  // ==================== Buyer Operations ====================

  /**
   * Create order from cart (checkout)
   */
  checkout(request: {
    shippingAddress: ShippingAddress;
    deliveryNotes?: string;
    paymentMethod: string;
  }): Observable<Order> {
    const userId = this.authService.currentUser()?.id || '';
    const fullRequest = { ...request, userId };

    return this.http
      .post<Order>(`${this.API_URL}/checkout`, fullRequest, {
        headers: this.getHeaders(),
      })
      .pipe(tap((order) => this.currentOrderSignal.set(order)));
  }

  /**
   * Get current order details
   */
  getOrderById(id: string): Observable<Order> {
    this.isLoadingSignal.set(true);
    return this.http
      .get<Order>(`${this.API_URL}/${id}`, {
        headers: this.getHeaders(),
      })
      .pipe(
        tap((order) => {
          this.currentOrderSignal.set(order);
          this.isLoadingSignal.set(false);
        }),
      );
  }

  /**
   * Get buyer's orders
   */
  getMyOrders(params?: OrderSearchParams): Observable<Order[]> {
    this.isLoadingSignal.set(true);
    let queryParams = '';
    if (params) {
      const keys = Object.keys(params) as (keyof OrderSearchParams)[];
      queryParams = keys
        .filter((key) => params[key] !== undefined && params[key] !== null)
        .map((key) => `${key}=${encodeURIComponent(String(params[key]))}`)
        .join('&');
    }
    const url = queryParams
      ? `${this.API_URL}/my-orders?${queryParams}`
      : `${this.API_URL}/my-orders`;
    return this.http
      .get<Order[]>(url, {
        headers: this.getHeaders(),
      })
      .pipe(
        tap((orders) => {
          this.ordersSignal.set(orders);
          this.isLoadingSignal.set(false);
        }),
      );
  }

  /**
   * Cancel an order
   */
  cancelOrder(id: string, reason?: string): Observable<Order> {
    return this.http
      .patch<Order>(`${this.API_URL}/${id}/cancel`, { reason }, { headers: this.getHeaders() })
      .pipe(tap((order) => this.currentOrderSignal.set(order)));
  }

  /**
   * Redo a cancelled order (create new order with same items)
   */
  redoOrder(id: string): Observable<Order> {
    return this.http
      .post<Order>(`${this.API_URL}/${id}/redo`, {}, { headers: this.getHeaders() })
      .pipe(tap((order) => this.currentOrderSignal.set(order)));
  }

  /**
   * Check if order can be cancelled
   */
  canCancel(order: Order): boolean {
    return [OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING].includes(
      order.status,
    );
  }

  /**
   * Check if order can be redone
   */
  canRedo(order: Order): boolean {
    return order.status === OrderStatus.CANCELLED;
  }

  /**
   * Get status label
   */
  getStatusLabel(status: OrderStatus): string {
    const labels: { [key in OrderStatus]: string } = {
      [OrderStatus.PENDING]: 'Pending',
      [OrderStatus.CONFIRMED]: 'Confirmed',
      [OrderStatus.PROCESSING]: 'Processing',
      [OrderStatus.SHIPPED]: 'Shipped',
      [OrderStatus.DELIVERED]: 'Delivered',
      [OrderStatus.CANCELLED]: 'Cancelled',
      [OrderStatus.RETURNED]: 'Returned',
    };
    return labels[status] || status;
  }

  /**
   * Get status color for UI
   */
  getStatusColor(status: OrderStatus): string {
    const colors: { [key in OrderStatus]: string } = {
      [OrderStatus.PENDING]: '#ff9800',
      [OrderStatus.CONFIRMED]: '#2196f3',
      [OrderStatus.PROCESSING]: '#9c27b0',
      [OrderStatus.SHIPPED]: '#00bcd4',
      [OrderStatus.DELIVERED]: '#4caf50',
      [OrderStatus.CANCELLED]: '#f44336',
      [OrderStatus.RETURNED]: '#e91e63',
    };
    return colors[status] || '#757575';
  }

  /**
   * Get stats for the Buyer Dashboard
   */
  getBuyerStats(): Observable<BuyerStats> {
    return this.http.get<BuyerStats>(`${this.PROFILE_URL}/buyer/me`, {
      headers: this.getHeaders(),
    });
  }

  // ==================== Seller Operations ====================

  /**
   * Get seller's orders
   */
  getSellerOrders(params?: OrderSearchParams): Observable<Order[]> {
    this.isLoadingSignal.set(true);
    let queryParams = '';
    if (params) {
      const keys = Object.keys(params) as (keyof OrderSearchParams)[];
      queryParams = keys
        .filter((key) => params[key] !== undefined && params[key] !== null)
        .map((key) => `${key}=${encodeURIComponent(String(params[key]))}`)
        .join('&');
    }
    const url = queryParams ? `${this.API_URL}/seller?${queryParams}` : `${this.API_URL}/seller`;
    return this.http
      .get<Order[]>(url, {
        headers: this.getHeaders(),
      })
      .pipe(
        tap((orders) => {
          this.ordersSignal.set(orders);
          this.isLoadingSignal.set(false);
        }),
      );
  }

  /**
   * Get stats for the Seller Dashboard
   */
  getSellerStats(): Observable<SellerStats> {
    return this.http.get<SellerStats>(`${this.PROFILE_URL}/seller/me`, {
      headers: this.getHeaders(),
    });
  }

  /**
   * Remove/delete an order (soft delete)
   */
  removeOrder(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`, {
      headers: this.getHeaders(),
    });
  }
}
