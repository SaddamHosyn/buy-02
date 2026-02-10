import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { ProductForm } from './product-form';
import { ProductService } from '../../../core/services/product.service';
import { MediaService } from '../../../core/services/media.service';
import { Auth } from '../../../core/services/auth';
import { DialogService } from '../../../shared/services/dialog.service';

describe('ProductForm', () => {
  let component: ProductForm;
  let fixture: ComponentFixture<ProductForm>;
  let router: Router;
  let mockProductService: jasmine.SpyObj<ProductService>;
  let mockMediaService: jasmine.SpyObj<MediaService>;
  let mockAuthService: any;
  let mockDialogService: jasmine.SpyObj<DialogService>;

  beforeEach(async () => {
    mockProductService = jasmine.createSpyObj('ProductService', [
      'getProductById',
      'createProduct',
      'updateProduct',
      'getCategories',
      'associateMedia',
      'removeMediaFromProduct',
    ]);
    mockProductService.getCategories.and.returnValue(of([]));
    mockProductService.getProductById.and.returnValue(of({
      id: 'prod-1',
      name: 'Test',
      description: 'Test product description for testing',
      price: 10,
      stock: 5,
      category: 'Electronics',
      imageUrls: [],
    } as any));

    mockMediaService = jasmine.createSpyObj('MediaService', ['uploadFiles']);
    mockMediaService.uploadFiles.and.returnValue(of([]));

    mockAuthService = jasmine.createSpyObj('Auth', ['isAuthenticated'], {
      currentUser: signal({ id: 'user-1', name: 'John', email: 'john@test.com', role: 'SELLER' }),
    });
    mockAuthService.isAuthenticated.and.returnValue(true);

    mockDialogService = jasmine.createSpyObj('DialogService', ['confirm', 'confirmDiscard']);
    mockDialogService.confirm.and.returnValue(of(true));
    mockDialogService.confirmDiscard.and.returnValue(of(true));

    await TestBed.configureTestingModule({
      imports: [ProductForm, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ProductService, useValue: mockProductService },
        { provide: MediaService, useValue: mockMediaService },
        { provide: Auth, useValue: mockAuthService },
        { provide: DialogService, useValue: mockDialogService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => null } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductForm);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ==================== ngOnInit ====================

  describe('ngOnInit', () => {
    it('should call loadCategories on init', () => {
      component.ngOnInit();
      expect(mockProductService.getCategories).toHaveBeenCalled();
    });

    it('should enter edit mode when route has id', () => {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [ProductForm, NoopAnimationsModule],
        providers: [
          provideRouter([]),
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: ProductService, useValue: mockProductService },
          { provide: MediaService, useValue: mockMediaService },
          { provide: Auth, useValue: mockAuthService },
          { provide: DialogService, useValue: mockDialogService },
          {
            provide: ActivatedRoute,
            useValue: { snapshot: { paramMap: { get: () => 'prod-1' } } },
          },
        ],
      }).compileComponents();
      const f = TestBed.createComponent(ProductForm);
      const c = f.componentInstance;
      c.ngOnInit();
      expect(c.isEditMode()).toBeTrue();
      expect(c.productId()).toBe('prod-1');
      expect(mockProductService.getProductById).toHaveBeenCalledWith('prod-1');
    });
  });

  // ==================== loadCategories ====================

  describe('loadCategories', () => {
    it('should set predefined categories when backend returns empty', () => {
      mockProductService.getCategories.and.returnValue(of([]));
      component.loadCategories();
      expect(component.availableCategories().length).toBeGreaterThan(0);
      expect(component.availableCategories()).toContain('Electronics');
    });

    it('should merge backend categories with predefined list', () => {
      mockProductService.getCategories.and.returnValue(of(['CustomCat', 'Electronics']));
      component.loadCategories();
      const cats = component.availableCategories();
      expect(cats).toContain('CustomCat');
      expect(cats).toContain('Electronics');
      // No duplicates
      expect(cats.filter(c => c === 'Electronics').length).toBe(1);
    });

    it('should handle null dbCategories', () => {
      mockProductService.getCategories.and.returnValue(of(null as any));
      component.loadCategories();
      expect(component.availableCategories().length).toBeGreaterThan(0);
    });

    it('should fallback to predefined categories on error', () => {
      spyOn(console, 'error');
      mockProductService.getCategories.and.returnValue(throwError(() => new Error('fail')));
      component.loadCategories();
      expect(component.availableCategories().length).toBeGreaterThan(0);
      expect(component.availableCategories()).toContain('Electronics');
    });
  });

  // ==================== Form validation ====================

  describe('form', () => {
    it('should have a form with required fields', () => {
      expect(component.productForm).toBeTruthy();
      expect(component.productForm.get('name')).toBeTruthy();
      expect(component.productForm.get('description')).toBeTruthy();
      expect(component.productForm.get('price')).toBeTruthy();
      expect(component.productForm.get('quantity')).toBeTruthy();
      expect(component.productForm.get('category')).toBeTruthy();
    });

    it('should be invalid when empty', () => {
      expect(component.productForm.valid).toBeFalse();
    });
  });

  // ==================== getErrorMessage ====================

  describe('getErrorMessage', () => {
    it('should return empty string when no errors', () => {
      component.productForm.get('name')!.setValue('Valid Name');
      expect(component.getErrorMessage('name')).toBe('');
    });

    it('should return quantity required error', () => {
      component.productForm.get('quantity')!.setValue(null);
      component.productForm.get('quantity')!.markAsTouched();
      expect(component.getErrorMessage('quantity')).toBe('Quantity is required');
    });

    it('should return quantity min error', () => {
      component.productForm.get('quantity')!.setValue(-1);
      component.productForm.get('quantity')!.markAsTouched();
      expect(component.getErrorMessage('quantity')).toBe('Quantity cannot be negative');
    });

    it('should return empty for unknown control', () => {
      expect(component.getErrorMessage('nonexistent')).toBe('');
    });
  });
});
