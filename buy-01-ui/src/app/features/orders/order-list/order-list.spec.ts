import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { OrderListPage } from './order-list';
import {
  OrderService,
  Order,
  OrderStatus,
} from '../../../core/services/order.service';
import { Auth } from '../../../core/services/auth';

function createMockOrder(overrides: Partial<Order> = {}): Order {
  return {
    id: 'order-1',
    orderNumber: 'ORD-20260209-001',
    buyerId: 'user-1',
    buyerName: 'John Doe',
    buyerEmail: 'john@example.com',
    status: OrderStatus.CONFIRMED,
    totalAmount: 99.99,
    subtotal: 99.99,
    shippingCost: 0,
    taxAmount: 0,
    discountAmount: 0,
    paymentMethod: 'PAY_ON_DELIVERY',
    createdAt: '2026-02-09T12:00:00Z',
    items: [
      {
        productId: 'prod-1',
        productName: 'Widget A',
        productDescription: 'A useful widget',
        quantity: 2,
        price: 49.995,
        priceAtPurchase: 49.995,
        subtotal: 99.99,
        sellerId: 'seller-1',
        sellerName: 'Test Seller',
      },
    ],
    statusHistory: [],
    shippingAddress: {
      fullName: 'John Doe',
      addressLine1: '123 Main St',
      city: 'New York',
      postalCode: '10001',
      country: 'USA',
    },
    ...overrides,
  } as Order;
}

describe('OrderListPage', () => {
  let component: OrderListPage;
  let fixture: ComponentFixture<OrderListPage>;
  let router: Router;
  let mockSnackBar: jasmine.SpyObj<MatSnackBar>;
  let mockAuthService: jasmine.SpyObj<Auth>;

  const ordersSignal = signal<Order[]>([]);
  const loadingSignal = signal<boolean>(false);

  const mockOrderService = {
    ordersSignal: ordersSignal,
    isLoadingSignal: loadingSignal,
    getMyOrders: jasmine.createSpy('getMyOrders').and.returnValue(of([])),
    getSellerOrders: jasmine.createSpy('getSellerOrders').and.returnValue(of([])),
    cancelOrder: jasmine.createSpy('cancelOrder'),
    redoOrder: jasmine.createSpy('redoOrder'),
    canCancel: jasmine.createSpy('canCancel').and.returnValue(false),
    canRedo: jasmine.createSpy('canRedo').and.returnValue(false),
    getStatusLabel: jasmine.createSpy('getStatusLabel').and.callFake((s: OrderStatus) => s),
    getStatusColor: jasmine.createSpy('getStatusColor').and.returnValue('#000'),
  };

  beforeEach(async () => {
    mockOrderService.getMyOrders.calls.reset();
    mockOrderService.getSellerOrders.calls.reset();
    mockOrderService.cancelOrder.calls.reset();
    mockOrderService.redoOrder.calls.reset();

    mockAuthService = jasmine.createSpyObj('Auth', ['isAuthenticated', 'isSeller', 'logout'], {
      currentUser: signal({ id: 'user-1', name: 'John', email: 'john@test.com', role: 'CLIENT' }),
    });
    mockAuthService.isAuthenticated.and.returnValue(true);
    mockAuthService.isSeller.and.returnValue(false);

    mockSnackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [OrderListPage, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: OrderService, useValue: mockOrderService },
        { provide: Auth, useValue: mockAuthService },
        { provide: MatSnackBar, useValue: mockSnackBar },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OrderListPage);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    ordersSignal.set([]);
    loadingSignal.set(false);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ==================== ngOnInit ====================

  describe('ngOnInit', () => {
    it('should load buyer orders on init for client user', () => {
      component.ngOnInit();
      expect(mockOrderService.getMyOrders).toHaveBeenCalled();
    });

    it('should redirect to login when not authenticated', () => {
      mockAuthService.isAuthenticated.and.returnValue(false);
      spyOn(router, 'navigate');
      component.ngOnInit();
      expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
    });

    it('should default to seller view for seller users', () => {
      mockAuthService.isSeller.and.returnValue(true);
      component.ngOnInit();
      expect(component.viewMode()).toBe('seller');
      expect(mockOrderService.getSellerOrders).toHaveBeenCalled();
    });
  });

  // ==================== Load Orders ====================

  describe('loadOrders', () => {
    it('should load buyer orders by default', () => {
      component.viewMode.set('buyer');
      component.loadOrders();
      expect(mockOrderService.getMyOrders).toHaveBeenCalled();
    });

    it('should load seller orders when in seller mode', () => {
      component.viewMode.set('seller');
      component.loadOrders();
      expect(mockOrderService.getSellerOrders).toHaveBeenCalled();
    });

    it('should pass status filter to order params', () => {
      component.selectedStatus.set(OrderStatus.CONFIRMED);
      component.loadOrders();
      expect(mockOrderService.getMyOrders).toHaveBeenCalledWith(
        jasmine.objectContaining({ status: OrderStatus.CONFIRMED })
      );
    });

    it('should pass startDate filter when set', () => {
      const d = new Date('2026-01-01T00:00:00Z');
      component.startDate.set(d);
      component.loadOrders();
      expect(mockOrderService.getMyOrders).toHaveBeenCalledWith(
        jasmine.objectContaining({ startDate: d.toISOString() })
      );
    });

    it('should pass endDate filter when set', () => {
      const d = new Date('2026-12-31T00:00:00Z');
      component.endDate.set(d);
      component.loadOrders();
      expect(mockOrderService.getMyOrders).toHaveBeenCalledWith(
        jasmine.objectContaining({ endDate: d.toISOString() })
      );
    });

    it('should show snackbar on seller orders error', () => {
      const snack = (component as any).snackBar;
      spyOn(snack, 'open');
      component.viewMode.set('seller');
      mockOrderService.getSellerOrders.and.returnValue(throwError(() => new Error('fail')));
      component.loadOrders();
      expect(snack.open).toHaveBeenCalledWith('Failed to load orders', 'Close', jasmine.any(Object));
    });

    it('should show snackbar on buyer orders error', () => {
      const snack = (component as any).snackBar;
      spyOn(snack, 'open');
      component.viewMode.set('buyer');
      mockOrderService.getMyOrders.and.returnValue(throwError(() => new Error('fail')));
      component.loadOrders();
      expect(snack.open).toHaveBeenCalledWith('Failed to load orders', 'Close', jasmine.any(Object));
    });
  });

  // ==================== View Mode ====================

  describe('switchViewMode', () => {
    it('should switch to seller mode', () => {
      component.switchViewMode('seller');
      expect(component.viewMode()).toBe('seller');
    });

    it('should switch to buyer mode', () => {
      component.switchViewMode('buyer');
      expect(component.viewMode()).toBe('buyer');
    });
  });

  // ==================== Filters ====================

  describe('filters', () => {
    it('should apply filters and reload', () => {
      component.applyFilters();
      expect(mockOrderService.getMyOrders).toHaveBeenCalled();
    });

    it('should clear filters and reload', () => {
      component.searchQuery.set('test');
      component.selectedStatus.set(OrderStatus.CONFIRMED);
      component.clearFilters();
      expect(component.searchQuery()).toBe('');
      expect(component.selectedStatus()).toBe('');
    });
  });

  // ==================== Filtered Orders ====================

  describe('filteredOrders', () => {
    it('should filter orders by search query on order number', () => {
      ordersSignal.set([
        createMockOrder({ orderNumber: 'ORD-123' }),
        createMockOrder({ id: 'order-2', orderNumber: 'ORD-456' }),
      ]);
      component.searchQuery.set('123');
      expect(component.filteredOrders().length).toBe(1);
      expect(component.filteredOrders()[0].orderNumber).toBe('ORD-123');
    });

    it('should filter orders by product name', () => {
      ordersSignal.set([
        createMockOrder(),
        createMockOrder({
          id: 'order-2',
          orderNumber: 'ORD-999',
          items: [{ productId: 'p2', productName: 'Gadget', productDescription: '', quantity: 1, price: 10, priceAtPurchase: 10, subtotal: 10, sellerId: 's1' }],
        }),
      ]);
      component.searchQuery.set('gadget');
      expect(component.filteredOrders().length).toBe(1);
    });

    it('should return all orders when no search query', () => {
      ordersSignal.set([createMockOrder(), createMockOrder({ id: 'order-2', orderNumber: 'ORD-456' })]);
      component.searchQuery.set('');
      expect(component.filteredOrders().length).toBe(2);
    });

    it('should handle non-array orders gracefully', () => {
      ordersSignal.set(null as any);
      expect(component.filteredOrders()).toEqual([]);
    });

    it('should handle truthy non-array orders with console warning', () => {
      spyOn(console, 'warn');
      ordersSignal.set({ notAnArray: true } as any);
      expect(component.filteredOrders()).toEqual([]);
      expect(console.warn).toHaveBeenCalledWith('Orders is not an array:', jasmine.anything());
    });
  });

  // ==================== Navigation ====================

  describe('viewOrder', () => {
    it('should navigate to order detail', () => {
      spyOn(router, 'navigate');
      component.viewOrder('order-1');
      expect(router.navigate).toHaveBeenCalledWith(['/orders', 'order-1']);
    });
  });
});
