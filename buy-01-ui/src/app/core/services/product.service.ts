import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, map } from 'rxjs';
import { Auth } from './auth';
import { environment } from '../../../environments/environment';

export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  stock?: number;
  sellerId?: string;
  mediaIds?: string[];
  imageUrls?: string[];
  createdAt?: string;
  updatedAt?: string;
}

// DTO for creating/updating products (matches backend ProductRequest)
export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  quantity: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(Auth);
  
  // API URL from environment configuration
  private readonly API_URL = environment.productsUrl;
  
  // Signals for state management
  private readonly productsSignal = signal<Product[]>([]);
  readonly products = this.productsSignal.asReadonly();
  
  /**
   * Get all products (public)
   * Calls backend API: GET /api/products
   */
  getAllProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(this.API_URL).pipe(
      tap(products => this.productsSignal.set(products))
    );
  }
  
  /**
   * Get product by ID
   * Calls backend API: GET /api/products/{id}
   */
  getProductById(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.API_URL}/${id}`);
  }
  
  /**
   * Get seller's products (authenticated)
   * CSR APPROACH: Get all products, filter client-side by sellerId
   */
  getSellerProducts(): Observable<Product[]> {
    const currentUserId = this.authService.currentUser()?.id;
    
    if (!currentUserId) {
      throw new Error('User not authenticated');
    }
    
    // Get all products and filter by sellerId (client-side filtering - CSR!)
    return this.http.get<Product[]>(this.API_URL).pipe(
      map(products => products.filter(p => p.sellerId === currentUserId))
    );
  }
  
  /**
   * Create product (sellers only)
   * Calls backend API: POST /api/products
   */
  createProduct(productRequest: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.API_URL, productRequest);
  }
  
  /**
   * Update product (sellers only - own products)
   * Calls backend API: PUT /api/products/{id}
   */
  updateProduct(id: string, productRequest: Partial<ProductRequest>): Observable<Product> {
    return this.http.put<Product>(`${this.API_URL}/${id}`, productRequest);
  }
  
  /**
   * Delete product (sellers only - own products)
   * Calls backend API: DELETE /api/products/{id}
   */
  deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
  
  /**
   * Associate media with product
   * Calls backend API: POST /api/products/{productId}/media/{mediaId}
   */
  associateMedia(productId: string, mediaId: string): Observable<Product> {
    return this.http.post<Product>(`${this.API_URL}/${productId}/media/${mediaId}`, {});
  }




 /**
   * Remove media ID from product's mediaIds array
   * Calls backend API: DELETE /api/products/{productId}/remove-media/{mediaId}
   */
  removeMediaFromProduct(productId: string, mediaId: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${productId}/remove-media/${mediaId}`);
  }









}
