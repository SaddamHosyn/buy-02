import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule } from '@angular/material/dialog';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { forkJoin } from 'rxjs';
import { MediaService, Media } from '../../../core/services/media.service';
import { Auth } from '../../../core/services/auth';
import { ProductService, Product } from '../../../core/services/product.service';
import { NotificationService } from '../../../core/services/notification.service';
import { DialogService } from '../../../shared/services/dialog.service';
import { ValidationPresets } from '../../../core/validators/file-upload.validator';

@Component({
  selector: 'app-media-manager',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatToolbarModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatTooltipModule,
    MatCheckboxModule,
    MatDialogModule
  ],
  templateUrl: './media-manager.html',
  styleUrl: './media-manager.css',
})
export class MediaManager implements OnInit {
    /**
     * Remove image from UI if it fails to load (404 or other error)
     */
    onImageError(mediaId: string): void {
      this.allMedia.update(media => media.filter(m => m.id !== mediaId));
      this.selectedMedia.update(selected => {
        const newSet = new Set(selected);
        newSet.delete(mediaId);
        return newSet;
      });
    }
  private readonly mediaService = inject(MediaService);
  private readonly productService = inject(ProductService);
  private readonly authService = inject(Auth);
  private readonly location = inject(Location);
  private readonly notification = inject(NotificationService);
  private readonly dialogService = inject(DialogService);
  
  // Signals for reactive state
  readonly allMedia = signal<Media[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly isUploading = signal<boolean>(false);
  readonly selectedMedia = signal<Set<string>>(new Set());
  readonly myProducts = signal<Product[]>([]);
  readonly selectedProductId = signal<string | undefined>(undefined);
  
  // Computed signals
  readonly currentUser = this.authService.currentUser;
  readonly hasSelectedMedia = computed(() => this.selectedMedia().size > 0);
  readonly selectedCount = computed(() => this.selectedMedia().size);
  readonly totalSize = computed(() => {
    return this.allMedia().reduce((sum, media) => sum + media.size, 0);
  });
  
  // Validation info from ValidationPresets
  readonly maxFileSize = ValidationPresets.PRODUCT_IMAGE.maxSize;
  readonly allowedTypes = ValidationPresets.PRODUCT_IMAGE.allowedTypes;
  readonly allowedExtensions = ValidationPresets.PRODUCT_IMAGE.allowedExtensions;
  
  ngOnInit(): void {
    this.loadMedia();
    this.loadMyProducts();
  }
  
  /**
   * Navigate back to previous page
   */
  goBack(): void {
    this.location.back();
  }
  
  /**
   * Load seller's products
   */
  loadMyProducts(): void {
    this.productService.getSellerProducts().subscribe({
      next: (products: Product[]) => {
        this.myProducts.set(products);
      },
      error: (error: any) => {
        console.error('Error loading products:', error);
      }
    });
  }
  
  /**
   * Load all media
   */
  loadMedia(): void {
    this.isLoading.set(true);
    this.mediaService.getAllMedia().subscribe({
      next: (media: Media[]) => {
        this.allMedia.set(media);
        this.isLoading.set(false);
      },
      error: (error: any) => {
        console.error('Error loading media:', error);
        this.isLoading.set(false);
        this.notification.error('Failed to load media');
      }
    });
  }
  
  /**
   * Handle file selection
   */
  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    
    if (!files || files.length === 0) {
      return;
    }
    
    const fileArray = Array.from(files);
    this.uploadFiles(fileArray);
    
    // Reset input
    input.value = '';
  }
  
  /**
   * Upload files
   */
  uploadFiles(files: File[]): void {
    this.isUploading.set(true);
    
    this.mediaService.uploadFiles(files).subscribe({
      next: (mediaList: Media[]) => {
        const productId = this.selectedProductId();
        
        // If a product is selected, associate the uploaded media with it
        if (productId) {
          this.associateMediaWithProduct(productId, mediaList);
        } else {
          this.isUploading.set(false);
          this.notification.fileUploadSuccess(mediaList.length);
          
          // Add uploaded media to the list
          this.allMedia.update(current => [...current, ...mediaList]);
        }
      },
      error: (error: any) => {
        console.error('Upload error:', error);
        this.isUploading.set(false);
        this.notification.error(error.message || 'Upload failed');
      }
    });
  }
  
  /**
   * Associate uploaded media with a product
   */
  private associateMediaWithProduct(productId: string, mediaList: Media[]): void {
    const mediaIds = mediaList.map(m => m.id);
    
    const associationRequests = mediaIds.map(mediaId =>
      this.productService.associateMedia(productId, mediaId)
    );
    
    forkJoin(associationRequests).subscribe({
      next: () => {
        this.isUploading.set(false);
        this.notification.success(
          `${mediaList.length} image(s) uploaded and linked to product`,
          3000
        );
        
        const updatedMedia = mediaList.map(m => ({...m, productId}));
        this.allMedia.update(current => [...current, ...updatedMedia]);
        
        this.selectedProductId.set(undefined);
      },
      error: (error: any) => {
        console.error('Error associating media:', error);
        this.isUploading.set(false);
        
        this.allMedia.update(current => [...current, ...mediaList]);
        this.notification.warning(
          `Images uploaded but failed to link to product: ${error.message}`,
          4000
        );
      }
    });
  }
  
  /**
   * Toggle media selection
   */
  toggleSelection(mediaId: string): void {
    this.selectedMedia.update(selected => {
      const newSet = new Set(selected);
      if (newSet.has(mediaId)) {
        newSet.delete(mediaId);
      } else {
        newSet.add(mediaId);
      }
      return newSet;
    });
  }
  
  /**
   * Select all media
   */
  selectAll(): void {
    const allIds = this.allMedia().map(m => m.id);
    this.selectedMedia.set(new Set(allIds));
  }
  
  /**
   * Deselect all media
   */
  deselectAll(): void {
    this.selectedMedia.set(new Set());
  }
  
  /**
   * Delete single media
   */
  deleteMedia(mediaId: string): void {
    this.dialogService.confirmDelete('this image').subscribe(confirmed => {
      if (!confirmed) {
        return;
      }
      
      this.mediaService.deleteMedia(mediaId).subscribe({
        next: () => {
          this.notification.success('Image deleted successfully', 2000);
          this.allMedia.update(media => media.filter(m => m.id !== mediaId));
          this.selectedMedia.update(selected => {
            const newSet = new Set(selected);
            newSet.delete(mediaId);
            return newSet;
          });
        },
        error: (error) => {
          console.error('Error deleting media:', error);
          this.notification.error('Failed to delete image');
        }
      });
    });
  }
  
  /**
   * Delete selected media
   */
  deleteSelected(): void {
    const count = this.selectedMedia().size;
    
    this.dialogService.confirm({
      title: 'Delete Multiple Images',
      message: `Are you sure you want to delete ${count} selected image(s)? This action cannot be undone.`,
      confirmText: 'Delete All',
      cancelText: 'Cancel',
      type: 'danger',
      icon: 'delete_forever'
    }).subscribe(confirmed => {
      if (!confirmed) {
        return;
      }
      
      const ids = Array.from(this.selectedMedia());
      
      this.mediaService.deleteMediaFiles(ids).subscribe({
        next: () => {
          this.notification.success(`${count} image(s) deleted successfully`, 2000);
          this.allMedia.update(media => media.filter(m => !ids.includes(m.id)));
          this.deselectAll();
        },
        error: (error: any) => {
          console.error('Error deleting media:', error);
          this.notification.error('Failed to delete images');
        }
      });
    });
  }
  
  /**
   * Copy image URL to clipboard
   */
  copyUrl(url: string): void {
    navigator.clipboard.writeText(url).then(() => {
      this.notification.success('URL copied to clipboard', 2000);
    }).catch(() => {
      this.notification.error('Failed to copy URL', 2000);
    });
  }
  
  /**
   * Format file size
   */
  formatSize(bytes: number): string {
    return this.mediaService.formatFileSize(bytes);
  }
  
  /**
   * Format date
   */
  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
  
  /**
   * Check if media is selected
   */
  isSelected(mediaId: string): boolean {
    return this.selectedMedia().has(mediaId);
  }
  
  /**
   * Get product name by ID (returns null if no product)
   */
  getProductName(productId?: string): string | null {
    if (!productId) return null;
    const product = this.myProducts().find(p => p.id === productId);
    return product?.name || null;
  }
}
