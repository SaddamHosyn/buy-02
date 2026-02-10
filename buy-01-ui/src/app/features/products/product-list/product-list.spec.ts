import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { ProductList } from './product-list';
import { ProductService, Product, PagedResponse } from '../../../core/services/product.service';
import { Auth } from '../../../core/services/auth';

function createMockProduct(overrides: Partial<Product> = {}): Product {
  return {
    id: 'prod-1',
    name: 'Test Product',
    description: 'A test product',
    price: 49.99,
    stock: 10,
    category: 'Electronics',
    sellerId: 'seller-1',
    imageUrls: [],
    createdAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

function createPagedResponse(products: Product[] = []): PagedResponse<Product> {
  return {
    products,
    page: 0,
    size: 12,
    totalElements: products.length,
    totalPages: 1,
    first: true,
    last: true,
  };
}

describe('ProductList', () => {
  let component: ProductList;
  let fixture: ComponentFixture<ProductList>;
  let router: Router;
  let mockProductService: jasmine.SpyObj<ProductService>;
  let mockAuthService: any;

  beforeEach(async () => {
    mockProductService = jasmine.createSpyObj('ProductService', [
      'searchProducts',
      'getCategories',
    ]);
    mockProductService.searchProducts.and.returnValue(of(createPagedResponse([createMockProduct()])));
    mockProductService.getCategories.and.returnValue(of([]));

    mockAuthService = jasmine.createSpyObj('Auth', ['isAuthenticated', 'isSeller', 'logout'], {
      currentUser: signal({ id: 'user-1', name: 'John', email: 'john@test.com', role: 'CLIENT' }),
    });
    mockAuthService.isAuthenticated.and.returnValue(true);
    mockAuthService.isSeller.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [ProductList, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ProductService, useValue: mockProductService },
        { provide: Auth, useValue: mockAuthService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductList);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ==================== ngOnInit ====================

  describe('ngOnInit', () => {
    it('should load products and categories on init', () => {
      component.ngOnInit();
      expect(mockProductService.searchProducts).toHaveBeenCalled();
      expect(mockProductService.getCategories).toHaveBeenCalled();
    });
  });

  // ==================== loadCategories ====================

  describe('loadCategories', () => {
    it('should set predefined categories when backend returns empty', () => {
      mockProductService.getCategories.and.returnValue(of([]));
      component.ngOnInit();
      expect(component.categories().length).toBeGreaterThan(0);
      expect(component.categories()).toContain('Electronics');
    });

    it('should merge backend categories with predefined ones', () => {
      mockProductService.getCategories.and.returnValue(of(['CustomCategory', 'Electronics']));
      component.ngOnInit();
      const cats = component.categories();
      expect(cats).toContain('Electronics');
      expect(cats).toContain('CustomCategory');
      // Should not have duplicates
      const elecCount = cats.filter(c => c === 'Electronics').length;
      expect(elecCount).toBe(1);
    });

    it('should handle null dbCategories gracefully', () => {
      mockProductService.getCategories.and.returnValue(of(null as any));
      component.ngOnInit();
      expect(component.categories().length).toBeGreaterThan(0);
    });

    it('should fallback to predefined categories on error', () => {
      mockProductService.getCategories.and.returnValue(throwError(() => new Error('fail')));
      component.ngOnInit();
      expect(component.categories().length).toBeGreaterThan(0);
      expect(component.categories()).toContain('Electronics');
    });
  });

  // ==================== loadProducts ====================

  describe('loadProducts', () => {
    it('should load products and set signal values', () => {
      const products = [createMockProduct()];
      mockProductService.searchProducts.and.returnValue(of(createPagedResponse(products)));
      component.ngOnInit();
      expect(component.products().length).toBe(1);
      expect(component.isLoading()).toBeFalse();
    });

    it('should handle error when loading products', () => {
      mockProductService.searchProducts.and.returnValue(throwError(() => new Error('fail')));
      spyOn(console, 'error');
      component.ngOnInit();
      expect(component.isLoading()).toBeFalse();
    });
  });

  // ==================== Search & Filters ====================

  describe('search and filters', () => {
    it('should reset pageIndex on search', () => {
      component.pageIndex.set(5);
      component.onSearch();
      expect(component.pageIndex()).toBe(0);
    });

    it('should reset pageIndex on filter change', () => {
      component.pageIndex.set(3);
      component.onFilterChange();
      expect(component.pageIndex()).toBe(0);
    });

    it('should update page on page change', () => {
      component.onPageChange({ pageIndex: 2, pageSize: 24, length: 100 });
      expect(component.pageIndex()).toBe(2);
      expect(component.pageSize()).toBe(24);
    });

    it('should reset all filters on resetFilters', () => {
      component.searchQuery.set('test');
      component.selectedCategory.set('Electronics');
      component.minPrice.set(50);
      component.maxPrice.set(500);
      component.keyword = 'test';
      component.pageIndex.set(3);
      component.resetFilters();
      expect(component.searchQuery()).toBe('');
      expect(component.selectedCategory()).toBe('all');
      expect(component.minPrice()).toBe(0);
      expect(component.maxPrice()).toBe(10000);
      expect(component.keyword).toBe('');
      expect(component.pageIndex()).toBe(0);
    });
  });

  // ==================== filteredProducts ====================

  describe('filteredProducts', () => {
    it('should filter by search query', () => {
      component.products.set([
        createMockProduct({ name: 'Laptop', description: 'Great laptop' }),
        createMockProduct({ id: 'prod-2', name: 'Phone', description: 'Nice phone' }),
      ]);
      component.searchQuery.set('laptop');
      expect(component.filteredProducts().length).toBe(1);
    });

    it('should filter by price range', () => {
      component.products.set([
        createMockProduct({ price: 10 }),
        createMockProduct({ id: 'prod-2', price: 500 }),
      ]);
      component.minPrice.set(100);
      component.maxPrice.set(1000);
      expect(component.filteredProducts().length).toBe(1);
    });
  });

  // ==================== Navigation ====================

  describe('navigation', () => {
    it('should navigate to product detail', () => {
      spyOn(router, 'navigate');
      component.viewProduct('prod-1');
      expect(router.navigate).toHaveBeenCalledWith(['/products', 'prod-1']);
    });

    it('should call authService.logout on logout', () => {
      component.logout();
      expect(mockAuthService.logout).toHaveBeenCalled();
    });
  });

  // ==================== Formatting ====================

  describe('formatPrice', () => {
    it('should format price with dollar sign', () => {
      expect(component.formatPrice(100)).toBe('$100');
    });
  });
});
