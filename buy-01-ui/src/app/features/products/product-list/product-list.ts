import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
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
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Auth } from '../../../core/services/auth';
import { ProductService, Product, PagedResponse } from '../../../core/services/product.service';
import { ProductCardSkeleton } from '../../../shared/components/product-card-skeleton/product-card-skeleton';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    CommonModule,
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
    MatPaginatorModule,
    FormsModule,
    ReactiveFormsModule,
    ProductCardSkeleton,
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
  readonly categories = signal<string[]>([]);
  readonly isLoading = signal<boolean>(false);

  // Filtering & Pagination state
  keyword: string = '';
  sortBy: string = 'createdAt,desc';

  readonly totalElements = signal<number>(0);
  readonly pageSize = signal<number>(12);
  readonly pageIndex = signal<number>(0);

  // Search and filter signals
  readonly searchQuery = signal<string>('');
  readonly selectedCategory = signal<string>('all');
  readonly minPrice = signal<number>(0);
  readonly maxPrice = signal<number>(10000);

  // Computed filtered products
  readonly filteredProducts = computed(() => {
    let result = this.products();

    // Search filter
    const query = this.searchQuery().toLowerCase();
    if (query) {
      result = result.filter(
        (p) => p.name.toLowerCase().includes(query) || p.description.toLowerCase().includes(query),
      );
    }

    // Note: Category filter removed - Product interface doesn't include category field
    // You can add category to Product interface if needed

    // Price range filter
    result = result.filter((p) => p.price >= this.minPrice() && p.price <= this.maxPrice());

    return result;
  });

  ngOnInit(): void {
    this.loadProducts();
    this.loadCategories();
  }

  private loadCategories(): void {
    this.productService.getCategories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: (err) => console.error('Error loading categories:', err),
    });
  }

  /**
   * Load products from the API
   */
  private loadProducts(): void {
    this.isLoading.set(true);

    const category = this.selectedCategory();
    this.productService
      .searchProducts({
        keyword: this.keyword || undefined,
        category: category && category !== 'all' ? category : undefined,
        minPrice: this.minPrice() > 0 ? this.minPrice() : undefined,
        maxPrice: this.maxPrice() < 10000 ? this.maxPrice() : undefined,
        page: this.pageIndex(),
        size: this.pageSize(),
        sort: this.sortBy,
      })
      .subscribe({
        next: (response: PagedResponse<Product>) => {
          this.products.set(response.products);
          this.totalElements.set(response.totalElements);
          this.isLoading.set(false);
        },
        error: (error) => {
          console.error('Error loading products:', error);
          this.isLoading.set(false);
        },
      });
  }

  onSearch(): void {
    this.pageIndex.set(0);
    this.loadProducts();
  }

  onFilterChange(): void {
    this.pageIndex.set(0);
    this.loadProducts();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadProducts();
  }

  /**
   * Clear all filters
   */
  resetFilters(): void {
    this.searchQuery.set('');
    this.selectedCategory.set('all');
    this.minPrice.set(0);
    this.maxPrice.set(10000);
    this.keyword = '';
    this.pageIndex.set(0);
    this.loadProducts();
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
