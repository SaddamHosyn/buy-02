import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, ActivatedRoute, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { ProductDetail } from './product-detail';
import { ProductService, Product } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { Auth, User } from '../../../core/services/auth';

function createMockProduct(overrides: Partial<Product> = {}): Product {
  return {
    id: 'prod-1',
    name: 'Test Widget',
    description: 'A great widget',
    price: 29.99,
    stock: 10,
    sellerId: 'seller-1',
    mediaIds: ['media-1'],
    imageUrls: ['http://example.com/img1.jpg', 'http://example.com/img2.jpg'],
    ...overrides,
  };
}

describe('ProductDetail', () => {
  let component: ProductDetail;
  let fixture: ComponentFixture<ProductDetail>;
  let mockProductService: jasmine.SpyObj<ProductService>;
  let mockCartService: jasmine.SpyObj<CartService>;
  let mockAuthService: jasmine.SpyObj<Auth>;
  let mockSnackBar: jasmine.SpyObj<MatSnackBar>;
  let mockDialog: jasmine.SpyObj<MatDialog>;
  let router: Router;

  beforeEach(async () => {
    mockProductService = jasmine.createSpyObj('ProductService', [
      'getProductById',
      'getAllProducts',
    ]);

    mockCartService = jasmine.createSpyObj('CartService', ['addToCart']);

    mockAuthService = jasmine.createSpyObj('Auth', ['isAuthenticated', 'logout', 'isSeller'], {
      currentUser: signal({ id: 'user-1', name: 'John', email: 'john@test.com', role: 'CLIENT' } as User),
    });
    mockAuthService.isAuthenticated.and.returnValue(true);

    const snackBarRef = {
      onAction: () => of(undefined),
    };
    mockSnackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    mockSnackBar.open.and.returnValue(snackBarRef as any);

    mockDialog = jasmine.createSpyObj('MatDialog', ['open']);

    mockProductService.getProductById.and.returnValue(of(createMockProduct()));

    await TestBed.configureTestingModule({
      imports: [ProductDetail, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ProductService, useValue: mockProductService },
        { provide: CartService, useValue: mockCartService },
        { provide: Auth, useValue: mockAuthService },
        { provide: MatSnackBar, useValue: mockSnackBar },
        { provide: MatDialog, useValue: mockDialog },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: (key: string) => 'prod-1' } },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductDetail);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ==================== ngOnInit ====================

  describe('ngOnInit', () => {
    it('should load product on init when id is present', () => {
      component.ngOnInit();
      expect(mockProductService.getProductById).toHaveBeenCalledWith('prod-1');
    });

    it('should set error when product ID is not found', () => {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [ProductDetail, NoopAnimationsModule],
        providers: [
          provideRouter([]),
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: ProductService, useValue: mockProductService },
          { provide: CartService, useValue: mockCartService },
          { provide: Auth, useValue: mockAuthService },
          { provide: MatSnackBar, useValue: mockSnackBar },
          { provide: MatDialog, useValue: mockDialog },
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: { paramMap: { get: (key: string) => null } },
            },
          },
        ],
      }).compileComponents();

      const newFixture = TestBed.createComponent(ProductDetail);
      const newComponent = newFixture.componentInstance;
      newComponent.ngOnInit();
      expect(newComponent.errorMessage()).toBe('Product ID not found');
    });
  });

  // ==================== Image Gallery ====================

  describe('image gallery', () => {
    it('should select image at given index', () => {
      component.selectImage(1);
      expect(component.selectedImageIndex()).toBe(1);
    });

    it('should navigate to previous image', () => {
      component.product.set(createMockProduct());
      component.selectedImageIndex.set(1);
      component.previousImage();
      expect(component.selectedImageIndex()).toBe(0);
    });

    it('should wrap around to last image when at first', () => {
      component.product.set(createMockProduct());
      component.selectedImageIndex.set(0);
      component.previousImage();
      expect(component.selectedImageIndex()).toBe(1);
    });

    it('should navigate to next image', () => {
      component.product.set(createMockProduct());
      component.selectedImageIndex.set(0);
      component.nextImage();
      expect(component.selectedImageIndex()).toBe(1);
    });

    it('should wrap around to first image when at last', () => {
      component.product.set(createMockProduct());
      component.selectedImageIndex.set(1);
      component.nextImage();
      expect(component.selectedImageIndex()).toBe(0);
    });

    it('should not navigate if product has no images (previous)', () => {
      component.product.set(createMockProduct({ imageUrls: undefined }));
      component.previousImage();
      expect(component.selectedImageIndex()).toBe(0);
    });

    it('should not navigate if product has no images (next)', () => {
      component.product.set(createMockProduct({ imageUrls: undefined }));
      component.nextImage();
      expect(component.selectedImageIndex()).toBe(0);
    });
  });

  // ==================== Quantity ====================

  describe('quantity controls', () => {
    it('should increase quantity', () => {
      component.increaseQuantity();
      expect(component.quantity()).toBe(2);
    });

    it('should decrease quantity', () => {
      component.quantity.set(3);
      component.decreaseQuantity();
      expect(component.quantity()).toBe(2);
    });

    it('should not decrease quantity below 1', () => {
      component.quantity.set(1);
      component.decreaseQuantity();
      expect(component.quantity()).toBe(1);
    });
  });

  // ==================== Add to Cart ====================

  describe('addToCart', () => {
    it('should add product to cart successfully', () => {
      component.product.set(createMockProduct());
      mockCartService.addToCart.and.returnValue(of({} as any));
      component.addToCart();
      expect(mockCartService.addToCart).toHaveBeenCalled();
    });

    it('should not add to cart if not authenticated', () => {
      component.product.set(createMockProduct());
      // isAuthenticated is a computed signal - override it
      Object.defineProperty(component, 'authService', {
        value: { ...mockAuthService, isAuthenticated: () => false, currentUser: signal(null) },
      });
      component.addToCart();
      expect(mockCartService.addToCart).not.toHaveBeenCalled();
    });

    it('should handle error when adding to cart fails', () => {
      component.product.set(createMockProduct());
      mockCartService.addToCart.and.returnValue(throwError(() => new Error('Out of stock')));
      component.addToCart();
      expect(component.isAddingToCart()).toBeFalse();
    });

    it('should not add to cart if product is null', () => {
      component.product.set(null);
      component.addToCart();
      expect(mockCartService.addToCart).not.toHaveBeenCalled();
    });
  });

  // ==================== Buy Now ====================

  describe('buyNow', () => {
    it('should add to cart and navigate to checkout', () => {
      component.product.set(createMockProduct());
      mockCartService.addToCart.and.returnValue(of({} as any));
      spyOn(router, 'navigate');
      component.buyNow();
      expect(mockCartService.addToCart).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/checkout']);
    });

    it('should not buy if not authenticated', () => {
      component.product.set(createMockProduct());
      Object.defineProperty(component, 'authService', {
        value: { ...mockAuthService, isAuthenticated: () => false, currentUser: signal(null) },
      });
      component.buyNow();
      expect(mockCartService.addToCart).not.toHaveBeenCalled();
    });
  });

  // ==================== Navigation ====================

  describe('navigation', () => {
    it('should navigate to edit product for seller', () => {
      component.product.set(createMockProduct());
      spyOn(router, 'navigate');
      component.editProduct();
      expect(router.navigate).toHaveBeenCalledWith(['/seller/product-form', 'prod-1']);
    });

    it('should not navigate if product is null', () => {
      component.product.set(null);
      spyOn(router, 'navigate');
      component.editProduct();
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('should navigate back to products list', () => {
      spyOn(router, 'navigate');
      component.goBack();
      expect(router.navigate).toHaveBeenCalledWith(['/products']);
    });
  });

  // ==================== Image Lightbox ====================

  describe('openImageLightbox', () => {
    it('should open lightbox dialog', () => {
      const product = createMockProduct();
      component.product.set(product);
      component.openImageLightbox(0);
      expect(mockDialog.open).toHaveBeenCalled();
    });

    it('should not open lightbox if product has no images', () => {
      component.product.set(createMockProduct({ imageUrls: [] }));
      component.openImageLightbox(0);
      expect(mockDialog.open).not.toHaveBeenCalled();
    });

    it('should not open lightbox if product is null', () => {
      component.product.set(null);
      component.openImageLightbox(0);
      expect(mockDialog.open).not.toHaveBeenCalled();
    });
  });

  // ==================== Computed Signals ====================

  describe('computed signals', () => {
    it('should compute hasImages correctly', () => {
      component.product.set(createMockProduct());
      expect(component.hasImages()).toBeTrue();
    });

    it('should compute hasImages as false when no images', () => {
      component.product.set(createMockProduct({ imageUrls: [] }));
      expect(component.hasImages()).toBeFalse();
    });

    it('should compute selectedImage correctly', () => {
      component.product.set(createMockProduct());
      component.selectedImageIndex.set(0);
      expect(component.selectedImage()).toBe('http://example.com/img1.jpg');
    });

    it('should compute selectedImage as null when no images', () => {
      component.product.set(createMockProduct({ imageUrls: [] }));
      expect(component.selectedImage()).toBeNull();
    });

    it('should compute isOwnProduct correctly', () => {
      component.product.set(createMockProduct({ sellerId: 'user-1' }));
      expect(component.isOwnProduct()).toBeTrue();
    });

    it('should compute totalPrice correctly', () => {
      component.product.set(createMockProduct({ price: 10.0 }));
      component.quantity.set(3);
      expect(component.totalPrice()).toBe(30.0);
    });

    it('should compute totalPrice as 0 when no product', () => {
      component.product.set(null);
      expect(component.totalPrice()).toBe(0);
    });
  });

  // ==================== Format Date ====================

  describe('formatDate', () => {
    it('should format a valid date string', () => {
      const result = component.formatDate('2026-02-09T12:00:00Z');
      expect(result).toContain('2026');
    });

    it('should return N/A for undefined date', () => {
      expect(component.formatDate(undefined)).toBe('N/A');
    });
  });
});
