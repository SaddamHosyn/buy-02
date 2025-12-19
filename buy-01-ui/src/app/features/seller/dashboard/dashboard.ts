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
    MatTooltipModule
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private readonly authService = inject(Auth);
  private readonly productService = inject(ProductService);
  private readonly router = inject<Router>(Router);
  private readonly dialogService = inject(DialogService);
  
  // Signals for reactive state
  readonly currentUser = this.authService.currentUser;
  readonly myProducts = signal<Product[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string>('');
  
  // Computed signals for stats
  readonly totalProducts = computed(() => this.myProducts().length);
  readonly totalRevenue = computed(() => {
    return this.myProducts().reduce((sum, product) => sum + product.price, 0);
  });
  readonly avgPrice = computed(() => {
    const total = this.totalProducts();
    return total > 0 ? (this.totalRevenue() / total) : 0;
  });
  
  // Table columns
  readonly displayedColumns = ['image', 'name', 'price', 'stock', 'createdAt', 'actions'];
  
  ngOnInit(): void {
    this.loadMyProducts();
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
