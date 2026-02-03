import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Auth } from '../../../core/services/auth';
import { ProductService, Product } from '../../../core/services/product.service';
import { DialogService } from '../../../shared/services/dialog.service';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';
import { OrderService, SellerStats } from '../../../core/services/order.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatToolbarModule,
    MatTableModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    BaseChartDirective
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private readonly authService = inject(Auth);
  private readonly productService = inject(ProductService);
  private readonly orderService = inject(OrderService);
  private readonly router = inject<Router>(Router);
  private readonly dialogService = inject(DialogService);

  // Signals for reactive state
  readonly currentUser = this.authService.currentUser;
  readonly myProducts = signal<Product[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string>('');

  // Computed signals for stats
  readonly totalProducts = computed(() => this.myProducts().length);
  readonly totalInventoryValue = computed(() => {
    return this.myProducts().reduce((sum, product) => sum + (product.price * (product.stock || 0)), 0);
  });
  readonly avgPrice = computed(() => {
    const total = this.totalProducts();
    const inventoryVal = this.totalInventoryValue();
    return total > 0 ? (inventoryVal / total) : 0;
  });

  // Dashboard Stats
  readonly sellerStats = signal<SellerStats | null>(null);
  readonly statsLoading = signal<boolean>(false);

  // Charts Configuration
  public lineChartOptions: ChartConfiguration['options'] = {
    elements: {
      line: {
        tension: 0.5,
      },
    },
    scales: {
      y: {
        position: 'left',
      },
      y1: {
        position: 'right',
        grid: {
          color: 'rgba(255,0,0,0.3)',
        },
        display: false
      }
    },
    plugins: {
      legend: { display: true },
    }
  };
  public lineChartType: ChartType = 'line';

  public bestSellersChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    indexAxis: 'y',
    plugins: { legend: { display: true } }
  };
  public bestSellersChartType: ChartType = 'bar';

  // Chart Data Signals
  readonly revenueChartData = computed<ChartData<'line'> | undefined>(() => {
    const stats = this.sellerStats();
    if (!stats) return undefined;

    return {
      labels: stats.revenueByMonth.map(d => d.month),
      datasets: [
        {
          data: stats.revenueByMonth.map(d => d.amount),
          label: 'Revenue (€)',
          backgroundColor: 'rgba(148,159,177,0.2)',
          borderColor: 'rgba(148,159,177,1)',
          pointBackgroundColor: 'rgba(148,159,177,1)',
          pointBorderColor: '#fff',
          pointHoverBackgroundColor: '#fff',
          pointHoverBorderColor: 'rgba(148,159,177,0.8)',
          fill: 'origin',
        }
      ]
    };
  });

  readonly bestSellersChartData = computed<ChartData<'bar'> | undefined>(() => {
    const stats = this.sellerStats();
    if (!stats) return undefined;

    return {
      labels: stats.bestSellingProducts.map(p => p.name),
      datasets: [
        {
          data: stats.bestSellingProducts.map(p => p.units),
          label: 'Units Sold',
          backgroundColor: '#2196f3',
          borderColor: '#2196f3',
          barThickness: 20
        },
        {
          data: stats.bestSellingProducts.map(p => p.revenue),
          label: 'Revenue (€)',
          backgroundColor: '#4caf50',
          borderColor: '#4caf50',
          barThickness: 20
        }
      ]
    };
  });

  // Table columns
  readonly displayedColumns = ['image', 'name', 'price', 'stock', 'createdAt', 'actions'];

  ngOnInit(): void {
    this.loadMyProducts();
    this.loadDashboardStats();
  }

  loadDashboardStats(): void {
    this.statsLoading.set(true);
    this.orderService.getSellerStats().subscribe({
      next: (stats) => {
        this.sellerStats.set(stats);
        this.statsLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load seller stats', err);
        this.statsLoading.set(false);
      }
    });
  }

  /**
   * Navigate to home page (products list)
   */
  goBack(): void {
    this.router.navigate(['/products']);
  }

  /**
   * Logout user and redirect to login page
   */
  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }

  /**
   * Load seller's products
   */
  loadMyProducts(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    // getSellerProducts() automatically uses current authenticated user
    this.productService.getSellerProducts().subscribe({
      next: (products) => {
        this.myProducts.set(products);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading products:', error);
        this.errorMessage.set('Failed to load products');
        this.isLoading.set(false);
      }
    });
  }

  /**
   * Navigate to create product page
   */
  createProduct(): void {
    this.router.navigate(['/seller/product-form']);
  }

  /**
   * Navigate to edit product page
   */
  editProduct(productId: string): void {
    this.router.navigate(['/seller/product-form', productId]);
  }

  /**
   * Delete product
   */
  deleteProduct(productId: string): void {
    const product = this.myProducts().find(p => p.id === productId);
    const productName = product?.name || 'this product';

    this.dialogService.confirmDelete(productName).subscribe(confirmed => {
      if (!confirmed) {
        return;
      }

      this.productService.deleteProduct(productId).subscribe({
        next: () => {
          // Remove from local state
          this.myProducts.update(products =>
            products.filter(p => p.id !== productId)
          );
        },
        error: (error) => {
          console.error('Error deleting product:', error);
          this.errorMessage.set('Failed to delete product');
        }
      });
    });
  }

  /**
   * Format date for display
   */
  formatDate(dateStr: string | undefined): string {
    if (!dateStr) {
      return 'N/A';
    }
    const date = new Date(dateStr);
    // Check if date is valid
    if (isNaN(date.getTime())) {
      return 'Invalid Date';
    }
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }
}
