import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
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
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatToolbarModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSliderModule,
    MatPaginatorModule,
    FormsModule,
    ReactiveFormsModule,
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
  readonly categories = signal<string[]>([]);
  readonly isLoading = signal<boolean>(false);

  // Filtering & Pagination state
  keyword: string = '';
  selectedCategory: string = '';
  minPrice: number = 0;
  maxPrice: number = 2000;
  sortBy: string = 'createdAt,desc';

  readonly totalElements = signal<number>(0);
  readonly pageSize = signal<number>(12);
  readonly pageIndex = signal<number>(0);

  ngOnInit(): void {
    this.loadProducts();
    this.loadCategories();
  }

  private loadCategories(): void {
    this.productService.getCategories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: (err) => console.error('Error loading categories:', err)
    });
  }

  loadProducts(): void {
    this.isLoading.set(true);

    this.productService.searchProducts({
      keyword: this.keyword || undefined,
      category: this.selectedCategory || undefined,
      minPrice: this.minPrice,
      maxPrice: this.maxPrice,
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: this.sortBy
    }).subscribe({
      next: (response: PagedResponse<Product>) => {
        this.products.set(response.content);
        this.totalElements.set(response.totalElements);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading products:', error);
        this.isLoading.set(false);
      }
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

  resetFilters(): void {
    this.keyword = '';
    this.selectedCategory = '';
    this.minPrice = 0;
    this.maxPrice = 2000;
    this.sortBy = 'createdAt,desc';
    this.pageIndex.set(0);
    this.loadProducts();
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

