import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, throwError, of, map } from 'rxjs';
import { Auth } from './auth';
import { environment } from '../../../environments/environment';

export interface CartItem {
  productId: string;
  quantity: number;
  sellerId: string;
  addedAt?: string;
  updatedAt?: string;
  cachedProductName?: string;
  cachedPrice?: number;
}

export interface Cart {
  id?: string;
  userId: string;
  status: 'ACTIVE' | 'PURCHASED' | 'ABANDONED' | 'MERGED';
  items: CartItem[];
  totalItems: number;
  cachedSubtotal: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AddToCartRequest {
  productId: string;
  quantity: number;
  sellerId: string;
  cachedProductName?: string;
  cachedPrice?: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(Auth);

  private readonly API_URL = `${environment.apiUrl}/cart`;

  // Signals for reactive state
  private readonly cartSignal = signal<Cart | null>(null);
  private readonly loadingSignal = signal<boolean>(false);

  readonly cart = this.cartSignal.asReadonly();
  readonly isLoading = this.loadingSignal.asReadonly();

  // Computed values
  readonly itemCount = computed(() => this.cartSignal()?.totalItems ?? 0);
  readonly subtotal = computed(() => this.cartSignal()?.cachedSubtotal ?? 0);
  readonly isEmpty = computed(() => !this.cartSignal()?.items?.length);

  /**
   * Get current user's cart
   */
  getCart(): Observable<Cart> {
    this.loadingSignal.set(true);
    return this.http.get<Cart>(`${this.API_URL}`).pipe(
      tap((cart) => {
        this.cartSignal.set(cart);
        this.loadingSignal.set(false);
      }),
      catchError((error) => {
        this.loadingSignal.set(false);
        if (error.status === 404) {
          // No cart exists yet - return empty cart
          const emptyCart: Cart = {
            userId: this.authService.currentUser()?.id || '',
            status: 'ACTIVE',
            items: [],
            totalItems: 0,
            cachedSubtotal: 0,
          };
          this.cartSignal.set(emptyCart);
          return of(emptyCart);
        }
        return throwError(() => error);
      }),
    );
  }

  /**
   * Add item to cart with optimistic UI update
   */
  addToCart(request: AddToCartRequest): Observable<Cart> {
    // Optimistic update
    const currentCart = this.cartSignal();
    const optimisticCart = this.createOptimisticCart(currentCart, 'add', request);
    this.cartSignal.set(optimisticCart);

    this.loadingSignal.set(true);
    return this.http.post<Cart>(`${this.API_URL}/items`, request).pipe(
      tap((cart) => {
        this.cartSignal.set(cart);
        this.loadingSignal.set(false);
      }),
      catchError((error) => {
        // Revert optimistic update on error
        this.cartSignal.set(currentCart);
        this.loadingSignal.set(false);
        return throwError(() => error);
      }),
    );
  }

  /**
   * Update item quantity with optimistic UI update
   */
  updateItemQuantity(productId: string, quantity: number): Observable<Cart> {
    // Optimistic update
    const currentCart = this.cartSignal();
    const optimisticCart = this.createOptimisticCart(currentCart, 'update', {
      productId,
      quantity,
    });
    this.cartSignal.set(optimisticCart);

    this.loadingSignal.set(true);
    return this.http.patch<Cart>(`${this.API_URL}/items/${productId}`, { quantity }).pipe(
      tap((cart) => {
        this.cartSignal.set(cart);
        this.loadingSignal.set(false);
      }),
      catchError((error) => {
        // Revert optimistic update on error
        this.cartSignal.set(currentCart);
        this.loadingSignal.set(false);
        return throwError(() => error);
      }),
    );
  }

  /**
   * Remove item from cart with optimistic UI update
   */
  removeItem(productId: string): Observable<Cart> {
    // Optimistic update
    const currentCart = this.cartSignal();
    const optimisticCart = this.createOptimisticCart(currentCart, 'remove', { productId });
    this.cartSignal.set(optimisticCart);

    this.loadingSignal.set(true);
    return this.http.delete<Cart>(`${this.API_URL}/items/${productId}`).pipe(
      tap((cart) => {
        this.cartSignal.set(cart);
        this.loadingSignal.set(false);
      }),
      catchError((error) => {
        // Revert optimistic update on error
        this.cartSignal.set(currentCart);
        this.loadingSignal.set(false);
        return throwError(() => error);
      }),
    );
  }

  /**
   * Clear entire cart with optimistic UI update
   */
  clearCart(): Observable<void> {
    // Optimistic update
    const currentCart = this.cartSignal();
    const emptyCart: Cart = {
      userId: currentCart?.userId || '',
      status: 'ACTIVE',
      items: [],
      totalItems: 0,
      cachedSubtotal: 0,
    };
    this.cartSignal.set(emptyCart);

    this.loadingSignal.set(true);
    return this.http.delete<void>(`${this.API_URL}`).pipe(
      tap(() => {
        this.loadingSignal.set(false);
      }),
      catchError((error) => {
        // Revert optimistic update on error
        this.cartSignal.set(currentCart);
        this.loadingSignal.set(false);
        return throwError(() => error);
      }),
    );
  }

  /**
   * Create optimistic cart state for UI updates
   */
  private createOptimisticCart(
    currentCart: Cart | null,
    action: 'add' | 'update' | 'remove',
    data: any,
  ): Cart {
    if (!currentCart) {
      return {
        userId: this.authService.currentUser()?.id || '',
        status: 'ACTIVE',
        items: [],
        totalItems: 0,
        cachedSubtotal: 0,
      };
    }

    const cart = { ...currentCart, items: [...currentCart.items] };

    switch (action) {
      case 'add':
        const existingItemIndex = cart.items.findIndex((i) => i.productId === data.productId);
        if (existingItemIndex >= 0) {
          cart.items[existingItemIndex] = {
            ...cart.items[existingItemIndex],
            quantity: cart.items[existingItemIndex].quantity + data.quantity,
          };
        } else {
          cart.items.push({
            productId: data.productId,
            quantity: data.quantity,
            sellerId: data.sellerId,
            cachedProductName: data.cachedProductName,
            cachedPrice: data.cachedPrice,
            addedAt: new Date().toISOString(),
          });
        }
        break;

      case 'update':
        const updateIndex = cart.items.findIndex((i) => i.productId === data.productId);
        if (updateIndex >= 0) {
          cart.items[updateIndex] = {
            ...cart.items[updateIndex],
            quantity: data.quantity,
            updatedAt: new Date().toISOString(),
          };
        }
        break;

      case 'remove':
        cart.items = cart.items.filter((i) => i.productId !== data.productId);
        break;
    }

    // Recalculate totals
    cart.totalItems = cart.items.reduce((sum, item) => sum + item.quantity, 0);
    cart.cachedSubtotal = cart.items.reduce(
      (sum, item) => sum + (item.cachedPrice || 0) * item.quantity,
      0,
    );

    return cart;
  }

  /**
   * Update local cart state (for optimistic updates)
   */
  updateLocalCart(cart: Cart): void {
    this.cartSignal.set(cart);
  }

  /**
   * Reset cart state (e.g., after checkout)
   */
  resetCart(): void {
    this.cartSignal.set(null);
  }
}
