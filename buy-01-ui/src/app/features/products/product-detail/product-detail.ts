import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { ProductService, Product } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { Auth, User } from '../../../core/services/auth';
import { ImageLightbox } from '../../../shared/components/image-lightbox/image-lightbox';

import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatBadgeModule,
    MatSnackBarModule,
  ],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.css',
})
export class ProductDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);
  private readonly authService = inject(Auth);
  private readonly cartService = inject(CartService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);

  // Signals for reactive state
  readonly product = signal<Product | null>(null);
  readonly seller = signal<User | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string>('');
  readonly selectedImageIndex = signal<number>(0);
  readonly quantity = signal<number>(1);
  readonly isAddingToCart = signal<boolean>(false);

  // Computed signals
  readonly currentUser = this.authService.currentUser;
  readonly hasImages = computed(() => {
    const p = this.product();
    return p?.imageUrls && p.imageUrls.length > 0;
  });
  readonly selectedImage = computed(() => {
    const p = this.product();
    const index = this.selectedImageIndex();
    if (p?.imageUrls && p.imageUrls.length > 0) {
      return p.imageUrls[index];
    }
    return null;
  });
  readonly isOwnProduct = computed(() => {
    const p = this.product();
    const user = this.currentUser();
    return p?.sellerId === user?.id;
  });
  readonly totalPrice = computed(() => {
    const p = this.product();
    return p ? p.price * this.quantity() : 0;
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadProduct(id);
    } else {
      this.errorMessage.set('Product ID not found');
      this.isLoading.set(false);
    }
  }

  /**
   * Load product details
   */
  loadProduct(id: string): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.productService.getProductById(id).subscribe({
      next: (product) => {
        this.product.set(product);
        // Load seller information
        if (product.sellerId) {
          this.loadSeller(product.sellerId);
        } else {
          this.isLoading.set(false);
        }
      },
      error: (error) => {
        console.error('Error loading product:', error);
        this.errorMessage.set('Product not found');
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Load seller information
   */
  loadSeller(sellerId: string): void {
    this.http.get<User>(`${environment.usersUrl}/${sellerId}`).subscribe({
      next: (seller) => {
        this.seller.set(seller);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading seller:', error);
        // Still show product even if seller info fails
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Select image in gallery
   */
  selectImage(index: number): void {
    this.selectedImageIndex.set(index);
  }

  /**
   * Navigate to previous image
   */
  previousImage(): void {
    const p = this.product();
    if (!p?.imageUrls) return;

    const current = this.selectedImageIndex();
    const newIndex = current === 0 ? p.imageUrls.length - 1 : current - 1;
    this.selectedImageIndex.set(newIndex);
  }

  /**
   * Navigate to next image
   */
  nextImage(): void {
    const p = this.product();
    if (!p?.imageUrls) return;

    const current = this.selectedImageIndex();
    const newIndex = current === p.imageUrls.length - 1 ? 0 : current + 1;
    this.selectedImageIndex.set(newIndex);
  }

  /**
   * Increase quantity
   */
  increaseQuantity(): void {
    this.quantity.update((q) => q + 1);
  }

  /**
   * Decrease quantity
   */
  decreaseQuantity(): void {
    this.quantity.update((q) => (q > 1 ? q - 1 : 1));
  }

  /**
   * Add to cart
   */
  addToCart(): void {
    const p = this.product();
    if (!p) return;

    if (!this.authService.isAuthenticated()) {
      this.snackBar
        .open('Please login to add items to cart', 'Login', { duration: 3000 })
        .onAction()
        .subscribe(() => {
          this.router.navigate(['/auth/login']);
        });
      return;
    }

    this.isAddingToCart.set(true);

    this.cartService
      .addToCart({
        productId: p.id,
        quantity: this.quantity(),
        sellerId: p.sellerId || '',
        cachedProductName: p.name,
        cachedPrice: p.price,
      })
      .subscribe({
        next: () => {
          this.isAddingToCart.set(false);
          this.snackBar
            .open(`Added ${this.quantity()} x ${p.name} to cart!`, 'View Cart', { duration: 3000 })
            .onAction()
            .subscribe(() => {
              this.router.navigate(['/cart']);
            });
        },
        error: (error) => {
          console.error('Error adding to cart:', error);
          this.isAddingToCart.set(false);
          // Extract error message from backend if available
          const errorMessage = error.error?.message || 'Failed to add to cart. Please try again.';
          this.snackBar.open(errorMessage, 'Close', { duration: 4000 });
        },
      });
  }

  /**
   * Buy now - add to cart and go to checkout
   */
  buyNow(): void {
    const p = this.product();
    if (!p) return;

    if (!this.authService.isAuthenticated()) {
      this.snackBar
        .open('Please login to checkout', 'Login', { duration: 3000 })
        .onAction()
        .subscribe(() => {
          this.router.navigate(['/auth/login']);
        });
      return;
    }

    this.isAddingToCart.set(true);

    this.cartService
      .addToCart({
        productId: p.id,
        quantity: this.quantity(),
        sellerId: p.sellerId || '',
        cachedProductName: p.name,
        cachedPrice: p.price,
      })
      .subscribe({
        next: () => {
          this.isAddingToCart.set(false);
          this.router.navigate(['/checkout']);
        },
        error: (error) => {
          console.error('Error adding to cart:', error);
          this.isAddingToCart.set(false);
          // Extract error message from backend if available
          const errorMessage = error.error?.message || 'Failed to add to cart. Please try again.';
          this.snackBar.open(errorMessage, 'Close', { duration: 4000 });
        },
      });
  }

  /**
   * Edit product (for seller)
   */
  editProduct(): void {
    const p = this.product();
    if (!p) return;

    this.router.navigate(['/seller/product-form', p.id]);
  }

  /**
   * Go back to products list
   */
  goBack(): void {
    this.router.navigate(['/products']);
  }

  /**
   * Open image lightbox
   */
  openImageLightbox(index: number): void {
    const p = this.product();
    if (!p || !p.imageUrls || p.imageUrls.length === 0) return;

    this.dialog.open(ImageLightbox, {
      data: {
        images: p.imageUrls,
        initialIndex: index,
        title: p.name,
      },
      maxWidth: '100vw',
      maxHeight: '100vh',
      width: '100vw',
      height: '100vh',
      panelClass: 'lightbox-dialog',
    });
  }

  /**
   * Format date
   */
  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return 'N/A';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  }
}
