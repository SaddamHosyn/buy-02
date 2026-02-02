import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Auth } from '../../../core/services/auth';
import { ProductService, Product } from '../../../core/services/product.service';
import { ProductCardSkeleton } from '../../../shared/components/product-card-skeleton/product-card-skeleton';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatToolbarModule,
    MatIconModule,
    MatTooltipModule,
    ProductCardSkeleton
  ],
  templateUrl: './product-list.html',
  styleUrl: './product-list.css',
})
export class ProductList implements OnInit {
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);
  readonly authService = inject(Auth);
  
  // Signals for reactive state
  readonly products = signal<Product[]>([]);
  readonly isLoading = signal<boolean>(false);
  
  ngOnInit(): void {
    this.loadProducts();
  }
  
  /**
   * Load products from JSON Server API
   */
  private loadProducts(): void {
    this.isLoading.set(true);
    
    this.productService.getAllProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading products:', error);
        this.isLoading.set(false);
      }
    });
  }
  
  /**
   * Navigate to product details
   */
  viewProduct(productId: string): void {
    this.router.navigate(['/products', productId]);
  }
  
  /**
   * Handle logout
   */
  logout(): void {
    this.authService.logout();
  }
  
  /**
   * Navigate to seller dashboard
   */
  goToDashboard(): void {
    this.router.navigate(['/seller/dashboard']);
  }
}

