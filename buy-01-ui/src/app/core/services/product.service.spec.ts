import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ProductService, Product, PagedResponse } from './product.service';
import { Auth, User } from './auth';
import { environment } from '../../../environments/environment';

describe('ProductService', () => {
  let service: ProductService;
  let httpMock: HttpTestingController;
  let mockAuthService: jasmine.SpyObj<Auth>;

  const API_URL = environment.productsUrl;

  const mockProduct: Product = {
    id: 'prod-1',
    name: 'Test Product',
    description: 'A test product',
    price: 29.99,
    stock: 10,
    sellerId: 'seller-1',
    mediaIds: ['media-1'],
    imageUrls: ['http://example.com/img1.jpg'],
  };

  beforeEach(() => {
    mockAuthService = jasmine.createSpyObj('Auth', ['isAuthenticated'], {
      currentUser: signal({ id: 'user-1', name: 'John', email: 'john@test.com', role: 'SELLER' } as User),
    });

    TestBed.configureTestingModule({
      providers: [
        ProductService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Auth, useValue: mockAuthService },
      ],
    });

    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ==================== getAllProducts ====================

  describe('getAllProducts', () => {
    it('should fetch all products', () => {
      const mockProducts = [mockProduct];
      service.getAllProducts().subscribe((products) => {
        expect(products).toEqual(mockProducts);
      });

      const req = httpMock.expectOne(API_URL);
      expect(req.request.method).toBe('GET');
      req.flush(mockProducts);
    });

    it('should update products signal', () => {
      service.getAllProducts().subscribe();
      const req = httpMock.expectOne(API_URL);
      req.flush([mockProduct]);
      expect(service.products().length).toBe(1);
    });
  });

  // ==================== getProductById ====================

  describe('getProductById', () => {
    it('should fetch product by id', () => {
      service.getProductById('prod-1').subscribe((product) => {
        expect(product).toEqual(mockProduct);
      });

      const req = httpMock.expectOne(`${API_URL}/prod-1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockProduct);
    });
  });

  // ==================== getCategories ====================

  describe('getCategories', () => {
    it('should fetch categories', () => {
      const categories = ['Electronics', 'Clothing'];
      service.getCategories().subscribe((result) => {
        expect(result).toEqual(categories);
      });

      const req = httpMock.expectOne(`${API_URL}/categories`);
      expect(req.request.method).toBe('GET');
      req.flush(categories);
    });
  });

  // ==================== getSellerProducts ====================

  describe('getSellerProducts', () => {
    it('should filter products by current user sellerId', () => {
      const allProducts = [
        mockProduct,
        { ...mockProduct, id: 'prod-2', sellerId: 'other-seller' },
      ];

      service.getSellerProducts().subscribe((products) => {
        expect(products.length).toBe(0); // user-1 is not seller-1
      });

      const req = httpMock.expectOne(API_URL);
      req.flush(allProducts);
    });
  });

  // ==================== createProduct ====================

  describe('createProduct', () => {
    it('should create a product', () => {
      const request = { name: 'New Product', description: 'Desc', price: 19.99, quantity: 5 };
      service.createProduct(request).subscribe((product) => {
        expect(product.name).toBe('New Product');
      });

      const req = httpMock.expectOne(API_URL);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush({ ...mockProduct, name: 'New Product' });
    });
  });

  // ==================== updateProduct ====================

  describe('updateProduct', () => {
    it('should update a product', () => {
      const updates = { name: 'Updated Name' };
      service.updateProduct('prod-1', updates).subscribe((product) => {
        expect(product.name).toBe('Updated Name');
      });

      const req = httpMock.expectOne(`${API_URL}/prod-1`);
      expect(req.request.method).toBe('PUT');
      req.flush({ ...mockProduct, name: 'Updated Name' });
    });
  });

  // ==================== deleteProduct ====================

  describe('deleteProduct', () => {
    it('should delete a product', () => {
      service.deleteProduct('prod-1').subscribe();

      const req = httpMock.expectOne(`${API_URL}/prod-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  // ==================== associateMedia ====================

  describe('associateMedia', () => {
    it('should associate media with product', () => {
      service.associateMedia('prod-1', 'media-1').subscribe();

      const req = httpMock.expectOne(`${API_URL}/prod-1/media/media-1`);
      expect(req.request.method).toBe('POST');
      req.flush(mockProduct);
    });
  });

  // ==================== removeMediaFromProduct ====================

  describe('removeMediaFromProduct', () => {
    it('should remove media from product', () => {
      service.removeMediaFromProduct('prod-1', 'media-1').subscribe();

      const req = httpMock.expectOne(`${API_URL}/prod-1/remove-media/media-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  // ==================== searchProducts ====================

  describe('searchProducts', () => {
    it('should search products with keyword', () => {
      const mockResponse: PagedResponse<Product> = {
        products: [mockProduct],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      };

      service.searchProducts({ keyword: 'widget', page: 0, size: 10 }).subscribe((result) => {
        expect(result.products.length).toBe(1);
      });

      const req = httpMock.expectOne((request) => request.url === `${API_URL}/search`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('q')).toBe('widget');
      req.flush(mockResponse);
    });

    it('should handle sort parameter with direction', () => {
      service.searchProducts({ sort: 'price,desc' }).subscribe();

      const req = httpMock.expectOne((request) => request.url === `${API_URL}/search`);
      expect(req.request.params.get('sort')).toBe('price');
      expect(req.request.params.get('direction')).toBe('desc');
      req.flush({ products: [], page: 0, size: 10, totalElements: 0, totalPages: 0, first: true, last: true });
    });

    it('should handle price range parameters', () => {
      service.searchProducts({ minPrice: 10, maxPrice: 50 }).subscribe();

      const req = httpMock.expectOne((request) => request.url === `${API_URL}/search`);
      expect(req.request.params.get('minPrice')).toBe('10');
      expect(req.request.params.get('maxPrice')).toBe('50');
      req.flush({ products: [], page: 0, size: 10, totalElements: 0, totalPages: 0, first: true, last: true });
    });

    it('should handle category parameter', () => {
      service.searchProducts({ category: 'Electronics' }).subscribe();

      const req = httpMock.expectOne((request) => request.url === `${API_URL}/search`);
      expect(req.request.params.get('category')).toBe('Electronics');
      req.flush({ products: [], page: 0, size: 10, totalElements: 0, totalPages: 0, first: true, last: true });
    });
  });
});
