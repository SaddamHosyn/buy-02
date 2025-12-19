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
import { MatDialog } from '@angular/material/dialog';
import { ProductService, Product } from '../../../core/services/product.service';
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
    MatBadgeModule
  ],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.css',
})
export class ProductDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);
  private readonly authService = inject(Auth);
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);
  
  // Signals for reactive state
  readonly product = signal<Product | null>(null);
  readonly seller = signal<User | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string>('');
  readonly selectedImageIndex = signal<number>(0);
  readonly quantity = signal<number>(1);
  
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
      }
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
      }
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
    this.quantity.update(q => q + 1);
  }
  
  /**
   * Decrease quantity
   */
  decreaseQuantity(): void {
    this.quantity.update(q => q > 1 ? q - 1 : 1);
  }
  
  /**
   * Add to cart (placeholder - will implement later)
   */
  addToCart(): void {
    const p = this.product();
    if (!p) return;
    
    console.log('Add to cart:', {
      product: p,
      quantity: this.quantity()
    });
    
    alert(`Added ${this.quantity()} x ${p.name} to cart!\n(Cart feature coming soon)`);
  }
  
  /**
   * Buy now (placeholder - will implement later)
   */
  buyNow(): void {
    const p = this.product();
    if (!p) return;
    
    console.log('Buy now:', {
      product: p,
      quantity: this.quantity()
    });
    
    alert(`Proceed to checkout for ${this.quantity()} x ${p.name}\n(Checkout feature coming soon)`);
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
        title: p.name
      },
      maxWidth: '100vw',
      maxHeight: '100vh',
      width: '100vw',
      height: '100vh',
      panelClass: 'lightbox-dialog'
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
      day: 'numeric'
    });
  }
}
