import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import {
  OrderService,
  OrderStatus,
} from '../../../core/services/order.service';
import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatToolbarModule,
    MatListModule,
  ],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.css',
})
export class OrderDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly orderService = inject(OrderService);
  private readonly snackBar = inject(MatSnackBar);
  readonly authService = inject(Auth);

  // Signals
  readonly order = this.orderService.currentOrderSignal;
  readonly isLoading = this.orderService.isLoadingSignal;
  readonly isActioning = signal<boolean>(false);

  // Status timeline steps - simplified to only show used statuses
  readonly statusSteps: OrderStatus[] = [
    OrderStatus.PENDING,
    OrderStatus.CONFIRMED,
  ];

  // Computed
  readonly currentStatusIndex = computed(() => {
    const order = this.order();
    if (!order) return -1;
    return this.statusSteps.indexOf(order.status);
  });

  readonly isCancelled = computed(() => this.order()?.status === OrderStatus.CANCELLED);
  readonly isReturned = computed(() => this.order()?.status === OrderStatus.RETURNED);

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/auth/login']);
      return;
    }

    const orderId = this.route.snapshot.paramMap.get('id');
    if (orderId) {
      this.loadOrder(orderId);
    } else {
      this.router.navigate(['/orders']);
    }
  }

  /**
   * Load order details
   */
  loadOrder(orderId: string): void {
    this.orderService.getOrderById(orderId).subscribe({
      error: (error) => {
        console.error('Error loading order:', error);
        this.snackBar.open('Order not found', 'Close', { duration: 3000 });
        this.router.navigate(['/orders']);
      },
    });
  }

  /**
   * Cancel order
   */
  cancelOrder(): void {
    const order = this.order();
    if (!order) return;

    if (!confirm(`Are you sure you want to cancel order ${order.orderNumber}?`)) {
      return;
    }

    this.isActioning.set(true);
    this.orderService.cancelOrder(order.id, 'Cancelled by user').subscribe({
      next: () => {
        this.isActioning.set(false);
        this.snackBar.open('Order cancelled successfully', 'Close', { duration: 3000 });
      },
      error: (error) => {
        console.error('Error cancelling order:', error);
        this.isActioning.set(false);
        this.snackBar.open('Failed to cancel order', 'Close', { duration: 3000 });
      },
    });
  }

  /**
   * Redo cancelled order - creates a new order
   */
  redoOrder(): void {
    const order = this.order();
    if (!order) return;

    if (!confirm('Create a new order with the same items?')) {
      return;
    }

    this.isActioning.set(true);
    this.orderService.redoOrder(order.id).subscribe({
      next: (newOrder) => {
        this.isActioning.set(false);
        this.snackBar
          .open('New order created!', 'View Order', { duration: 5000 })
          .onAction()
          .subscribe(() => this.router.navigate(['/orders', newOrder.id]));
        // Navigate to the new order
        this.router.navigate(['/orders', newOrder.id]);
      },
      error: (error) => {
        console.error('Error redoing order:', error);
        this.isActioning.set(false);
        const errorMsg = error.error?.message || error.error?.error || 'Failed to redo order';
        this.snackBar.open(errorMsg, 'Close', { duration: 5000 });
      },
    });
  }

  /**
   * Check if order can be cancelled
   */
  canCancel(): boolean {
    const order = this.order();
    return order ? this.orderService.canCancel(order) : false;
  }

  /**
   * Check if order can be redone
   */
  canRedo(): boolean {
    const order = this.order();
    return order ? this.orderService.canRedo(order) : false;
  }

  /**
   * Get status label
   */
  getStatusLabel(status: string): string {
    return this.orderService.getStatusLabel(status as OrderStatus);
  }

  /**
   * Get status icon
   */
  getStatusIcon(status: string): string {
    const icons: Record<OrderStatus, string> = {
      PENDING: 'pending',
      CONFIRMED: 'check_circle',
      PROCESSING: 'inventory',
      SHIPPED: 'local_shipping',
      DELIVERED: 'done_all',
      CANCELLED: 'cancel',
      RETURNED: 'undo',
    };
    return icons[status as OrderStatus] || 'help';
  }

  /**
   * Check if step is completed
   */
  isStepCompleted(stepStatus: OrderStatus): boolean {
    const currentIndex = this.currentStatusIndex();
    const stepIndex = this.statusSteps.indexOf(stepStatus);
    return stepIndex < currentIndex;
  }

  /**
   * Check if step is active
   */
  isStepActive(stepStatus: OrderStatus): boolean {
    return this.order()?.status === stepStatus;
  }

  /**
   * Format date
   */
  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  /**
   * Format short date
   */
  formatShortDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  /**
   * Back to orders
   */
  goBack(): void {
    this.router.navigate(['/orders']);
  }

  /**
   * Logout
   */
  logout(): void {
    this.authService.logout();
  }
}
