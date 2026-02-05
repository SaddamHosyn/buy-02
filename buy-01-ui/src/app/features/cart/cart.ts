import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatBadgeModule } from '@angular/material/badge';
import { CartService, Cart, CartItem } from '../../core/services/cart.service';
import { Auth } from '../../core/services/auth';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatInputModule,
    MatFormFieldModule,
    MatSnackBarModule,
    MatToolbarModule,
    MatBadgeModule
  ],
  templateUrl: './cart.html',
  styleUrl: './cart.css'
})
export class CartPage implements OnInit {
  private readonly router = inject(Router);
  private readonly cartService = inject(CartService);
  private readonly snackBar = inject(MatSnackBar);
  readonly authService = inject(Auth);
  
  // Signals
  readonly cart = this.cartService.cart;
  readonly isLoading = this.cartService.isLoading;
  readonly items = computed(() => this.cart()?.items ?? []);
  readonly subtotal = computed(() => this.cart()?.cachedSubtotal ?? 0);
  readonly totalItems = computed(() => this.cart()?.totalItems ?? 0);
  readonly isEmpty = computed(() => this.items().length === 0);
  
  // Track updating items
  readonly updatingItems = signal<Set<string>>(new Set());
  
  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/auth/login']);
      return;
    }
    this.loadCart();
  }
  
  /**
   * Load cart from server
   */
  loadCart(): void {
    this.cartService.getCart().subscribe({
      error: (error) => {
        console.error('Error loading cart:', error);
        this.snackBar.open('Failed to load cart', 'Close', { duration: 3000 });
      }
    });
  }
  
  /**
   * Update item quantity
   */
  updateQuantity(item: CartItem, newQuantity: number): void {
    if (newQuantity < 1) {
      this.removeItem(item);
      return;
    }
    
    this.setItemUpdating(item.productId, true);
    
    this.cartService.updateItemQuantity(item.productId, newQuantity).subscribe({
      next: () => {
        this.setItemUpdating(item.productId, false);
      },
      error: (error) => {
        console.error('Error updating quantity:', error);
        // Extract error message from backend if available
        const errorMessage = error.error?.message || 'Failed to update quantity';
        this.snackBar.open(errorMessage, 'Close', { duration: 3000 });
        this.setItemUpdating(item.productId, false);
      }
    });
  }
  
  /**
   * Increment item quantity
   */
  incrementQuantity(item: CartItem): void {
    this.updateQuantity(item, item.quantity + 1);
  }
  
  /**
   * Decrement item quantity
   */
  decrementQuantity(item: CartItem): void {
    if (item.quantity > 1) {
      this.updateQuantity(item, item.quantity - 1);
    } else {
      this.removeItem(item);
    }
  }
  
  /**
   * Remove item from cart
   */
  removeItem(item: CartItem): void {
    this.setItemUpdating(item.productId, true);
    
    this.cartService.removeItem(item.productId).subscribe({
      next: () => {
        this.snackBar.open('Item removed from cart', 'Close', { duration: 2000 });
        this.setItemUpdating(item.productId, false);
      },
      error: (error) => {
        console.error('Error removing item:', error);
        this.snackBar.open('Failed to remove item', 'Close', { duration: 3000 });
        this.setItemUpdating(item.productId, false);
      }
    });
  }
  
  /**
   * Clear entire cart
   */
  clearCart(): void {
    if (!confirm('Are you sure you want to clear your cart?')) {
      return;
    }
    
    this.cartService.clearCart().subscribe({
      next: () => {
        this.snackBar.open('Cart cleared', 'Close', { duration: 2000 });
      },
      error: (error) => {
        console.error('Error clearing cart:', error);
        this.snackBar.open('Failed to clear cart', 'Close', { duration: 3000 });
      }
    });
  }
  
  /**
   * Proceed to checkout
   */
  proceedToCheckout(): void {
    this.router.navigate(['/checkout']);
  }
  
  /**
   * Continue shopping
   */
  continueShopping(): void {
    this.router.navigate(['/products']);
  }
  
  /**
   * Navigate to product detail
   */
  viewProduct(productId: string): void {
    this.router.navigate(['/products', productId]);
  }
  
  /**
   * Check if item is being updated
   */
  isItemUpdating(productId: string): boolean {
    return this.updatingItems().has(productId);
  }
  
  /**
   * Calculate item subtotal
   */
  getItemSubtotal(item: CartItem): number {
    return (item.cachedPrice ?? 0) * item.quantity;
  }
  
  /**
   * Logout
   */
  logout(): void {
    this.authService.logout();
  }
  
  // Helper to track updating state
  private setItemUpdating(productId: string, isUpdating: boolean): void {
    const current = new Set(this.updatingItems());
    if (isUpdating) {
      current.add(productId);
    } else {
      current.delete(productId);
    }
    this.updatingItems.set(current);
  }
}
