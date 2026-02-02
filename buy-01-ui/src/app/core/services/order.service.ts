import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Auth } from './auth';

export interface Order {
  id: string;
  orderNumber: string;
  items: OrderItem[];
  totalAmount: number;
  status: 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  createdAt: string;
  buyerId: string;
}

export interface OrderItem {
  productId: string;
  items?: any;
  productName: string;
  quantity: number;
  price: number;
  subtotal: number;
  sellerId: string;
}

export interface BuyerStats {
  totalSpent: number;
  totalOrders: number;
  mostBoughtProducts: { name: string; count: number }[];
  topCategories: { name: string; percentage: number }[];
}

export interface SellerStats {
  totalRevenue: number;
  totalUnitsSold: number;
  bestSellingProducts: { name: string; revenue: number; units: number }[];
  revenueByMonth: { month: string; amount: number }[];
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(Auth);

  // API URL - connects to API Gateway
  private readonly API_URL = '/api/orders';

  constructor() { }

  /**
   * Helper to get common headers (e.g. User ID)
   * In a real app with proper JWT auth in gateway, X-User-Id is injected by the gateway.
   * However, for direct service-to-service or if gateway expects client to send it (for testing),
   * we include it here.
   */
  private getHeaders(): HttpHeaders {
    // Current user ID from auth service state
    const userId = this.authService.currentUser()?.id || '';
    return new HttpHeaders({
      'X-User-Id': userId
    });
  }

  /**
   * Get stats for the Buyer Dashboard
   */
  getBuyerStats(): Observable<BuyerStats> {
    return this.http.get<BuyerStats>(`${this.API_URL}/stats/buyer`, {
      headers: this.getHeaders()
    });
  }

  /**
   * Get stats for the Seller Dashboard
   */
  getSellerStats(): Observable<SellerStats> {
    return this.http.get<SellerStats>(`${this.API_URL}/stats/seller`, {
      headers: this.getHeaders()
    });
  }

  /**
   * Get User orders
   */
  getMyOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.API_URL}/my-orders`, {
      headers: this.getHeaders()
    });
  }
}
