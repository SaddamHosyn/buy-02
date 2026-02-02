import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatBadgeModule } from '@angular/material/badge';
import { OrderService, Order, OrderStatus, OrderSearchParams } from '../../../core/services/order.service';
import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSnackBarModule,
    MatToolbarModule,
    MatTabsModule,
    MatBadgeModule
  ],
  templateUrl: './order-list.html',
  styleUrl: './order-list.css'
})
export class OrderListPage implements OnInit {
  private readonly router = inject(Router);
  private readonly orderService = inject(OrderService);
  private readonly snackBar = inject(MatSnackBar);
  readonly authService = inject(Auth);
  
  // Signals
  readonly orders = this.orderService.orders;
  readonly isLoading = this.orderService.isLoading;
  readonly searchQuery = signal<string>('');
  readonly selectedStatus = signal<OrderStatus | ''>('');
  readonly startDate = signal<Date | null>(null);
  readonly endDate = signal<Date | null>(null);
  
  // View mode: 'buyer' or 'seller'
  readonly viewMode = signal<'buyer' | 'seller'>('buyer');
  
  // Computed
  readonly filteredOrders = computed(() => {
    let result = this.orders();
    const query = this.searchQuery().toLowerCase();
    
    if (query) {
      result = result.filter(order => 
        order.orderNumber.toLowerCase().includes(query) ||
        order.items.some(item => item.productName.toLowerCase().includes(query))
      );
    }
    
    return result;
  });
  
  readonly isSeller = computed(() => this.authService.isSeller());
  
  // Status options for filter
  readonly statusOptions: { value: OrderStatus | ''; label: string }[] = [
    { value: '', label: 'All Statuses' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'CONFIRMED', label: 'Confirmed' },
    { value: 'PROCESSING', label: 'Processing' },
    { value: 'SHIPPED', label: 'Shipped' },
    { value: 'DELIVERED', label: 'Delivered' },
    { value: 'CANCELLED', label: 'Cancelled' },
    { value: 'RETURNED', label: 'Returned' }
  ];
  
  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/auth/login']);
      return;
    }
    
    // Default to seller view if user is a seller
    if (this.isSeller()) {
      this.viewMode.set('seller');
    }
    
    this.loadOrders();
  }
  
  /**
   * Load orders based on current view mode and filters
   */
  loadOrders(): void {
    const params: OrderSearchParams = {};
    
    if (this.selectedStatus()) {
      params.status = this.selectedStatus() as OrderStatus;
    }
    
    if (this.startDate()) {
      params.startDate = this.startDate()!.toISOString();
    }
    
    if (this.endDate()) {
      params.endDate = this.endDate()!.toISOString();
    }
    
    if (this.viewMode() === 'seller') {
      this.orderService.getSellerOrders(params).subscribe({
        error: (error) => {
          console.error('Error loading seller orders:', error);
          this.snackBar.open('Failed to load orders', 'Close', { duration: 3000 });
        }
      });
    } else {
      this.orderService.getMyOrders(params).subscribe({
        error: (error) => {
          console.error('Error loading orders:', error);
          this.snackBar.open('Failed to load orders', 'Close', { duration: 3000 });
        }
      });
    }
  }
  
  /**
   * Switch view mode
   */
  switchViewMode(mode: 'buyer' | 'seller'): void {
    this.viewMode.set(mode);
    this.loadOrders();
  }
  
  /**
   * Apply filters
   */
  applyFilters(): void {
    this.loadOrders();
  }
  
  /**
   * Clear filters
   */
  clearFilters(): void {
    this.searchQuery.set('');
    this.selectedStatus.set('');
    this.startDate.set(null);
    this.endDate.set(null);
    this.loadOrders();
  }
  
  /**
   * View order details
   */
  viewOrder(orderId: string): void {
    this.router.navigate(['/orders', orderId]);
  }
  
  /**
   * Cancel order
   */
  cancelOrder(order: Order, event: Event): void {
    event.stopPropagation();
    
    if (!confirm(`Are you sure you want to cancel order ${order.orderNumber}?`)) {
      return;
    }
    
    this.orderService.cancelOrder(order.id, 'Cancelled by user').subscribe({
      next: () => {
        this.snackBar.open('Order cancelled successfully', 'Close', { duration: 3000 });
        this.loadOrders();
      },
      error: (error) => {
        console.error('Error cancelling order:', error);
        this.snackBar.open('Failed to cancel order', 'Close', { duration: 3000 });
      }
    });
  }
  
  /**
   * Redo cancelled order
   */
  redoOrder(order: Order, event: Event): void {
    event.stopPropagation();
    
    if (!confirm(`Create a new order with the same items as ${order.orderNumber}?`)) {
      return;
    }
    
    this.orderService.redoOrder(order.id).subscribe({
      next: (newOrder) => {
        this.snackBar.open('New order created!', 'View', { duration: 5000 })
          .onAction().subscribe(() => {
            this.router.navigate(['/orders', newOrder.id]);
          });
        this.loadOrders();
      },
      error: (error) => {
        console.error('Error redoing order:', error);
        this.snackBar.open('Failed to redo order', 'Close', { duration: 3000 });
      }
    });
  }
  
  /**
   * Remove order from view
   */
  removeOrder(order: Order, event: Event): void {
    event.stopPropagation();
    
    if (!confirm(`Remove order ${order.orderNumber} from your list?`)) {
      return;
    }
    
    this.orderService.removeOrder(order.id).subscribe({
      next: () => {
        this.snackBar.open('Order removed', 'Close', { duration: 3000 });
      },
      error: (error) => {
        console.error('Error removing order:', error);
        this.snackBar.open('Failed to remove order', 'Close', { duration: 3000 });
      }
    });
  }
  
  /**
   * Check if order can be cancelled
   */
  canCancel(order: Order): boolean {
    return this.orderService.canCancel(order);
  }
  
  /**
   * Check if order can be redone
   */
  canRedo(order: Order): boolean {
    return this.orderService.canRedo(order);
  }
  
  /**
   * Get status label
   */
  getStatusLabel(status: OrderStatus): string {
    return this.orderService.getStatusLabel(status);
  }
  
  /**
   * Get status color
   */
  getStatusColor(status: OrderStatus): string {
    return this.orderService.getStatusColor(status);
  }
  
  /**
   * Format date
   */
  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
  
  /**
   * Navigate home
   */
  goHome(): void {
    this.router.navigate(['/products']);
  }
  
  /**
   * Logout
   */
  logout(): void {
    this.authService.logout();
  }
}
