import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSliderModule } from '@angular/material/slider';
import { Auth } from '../../../core/services/auth';
import { ProductService, Product } from '../../../core/services/product.service';
import { ProductCardSkeleton } from '../../../shared/components/product-card-skeleton/product-card-skeleton';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatToolbarModule,
    MatIconModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSliderModule,
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
  
  // Search and filter signals
  readonly searchQuery = signal<string>('');
  readonly selectedCategory = signal<string>('all');
  readonly minPrice = signal<number>(0);
  readonly maxPrice = signal<number>(10000);
  
  // Categories (you can load these dynamically if needed)
  readonly categories = signal<string[]>(['all', 'Electronics', 'Clothing', 'Books', 'Home', 'Sports', 'Toys']);
  
  // Computed filtered products
  readonly filteredProducts = computed(() => {
    let result = this.products();
    
    // Search filter
    const query = this.searchQuery().toLowerCase();
    if (query) {
      result = result.filter(p =>
        p.name.toLowerCase().includes(query) ||
        p.description.toLowerCase().includes(query)
      );
    }
    
    // Note: Category filter removed - Product interface doesn't include category field
    // You can add category to Product interface if needed
    
    // Price range filter
    result = result.filter(p =>
      p.price >= this.minPrice() && p.price <= this.maxPrice()
    );
    
    return result;
  });
  
  ngOnInit(): void {
    this.loadProducts();
  }
  
  /**
   * Load products from the API
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
   * Clear all filters
   */
  clearFilters(): void {
    this.searchQuery.set('');
    this.selectedCategory.set('all');
    this.minPrice.set(0);
    this.maxPrice.set(10000);
  }
  
  /**
   * Format price slider value
   */
  formatPrice(value: number): string {
    return `$${value}`;
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

