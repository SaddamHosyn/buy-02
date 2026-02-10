import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { OrderDetailPage } from './order-detail';
import {
  OrderService,
  Order,
  OrderStatus,
} from '../../../core/services/order.service';
import { Auth } from '../../../core/services/auth';

// Shared test order fixture
function createMockOrder(overrides: Partial<Order> = {}): Order {
  return {
    id: 'order-1',
    orderNumber: 'ORD-20260209-001',
    buyerId: 'buyer-1',
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
        thumbnailMediaId: 'media-1',
      },
    ],
    statusHistory: [
      {
        previousStatus: '',
        newStatus: 'PENDING',
        changedAt: '2026-02-09T12:00:00Z',
        changedBy: 'buyer-1',
        changedByRole: 'CLIENT',
        reason: 'Order placed',
      },
      {
        previousStatus: 'PENDING',
        newStatus: 'CONFIRMED',
        changedAt: '2026-02-09T12:05:00Z',
        changedBy: 'seller-1',
        changedByRole: 'SELLER',
        reason: 'Order confirmed',
      },
    ],
    shippingAddress: {
      fullName: 'John Doe',
      addressLine1: '123 Main St',
      city: 'New York',
      postalCode: '10001',
      country: 'USA',
      phoneNumber: '+1-555-1234',
    },
    ...overrides,
  } as Order;
}

describe('OrderDetailPage', () => {
  let component: OrderDetailPage;
  let fixture: ComponentFixture<OrderDetailPage>;
  let mockAuthService: jasmine.SpyObj<Auth>;

  const orderSignal = signal<Order | null>(null);
  const loadingSignal = signal<boolean>(false);

  // Use a plain object mock to avoid readonly assignment issues on WritableSignal
  const mockOrderService = {
    currentOrderSignal: orderSignal,
    isLoadingSignal: loadingSignal,
    getOrderById: jasmine.createSpy('getOrderById').and.returnValue(of(createMockOrder())),
    cancelOrder: jasmine.createSpy('cancelOrder'),
    redoOrder: jasmine.createSpy('redoOrder'),
    canCancel: jasmine.createSpy('canCancel').and.returnValue(false),
    canRedo: jasmine.createSpy('canRedo').and.returnValue(false),
    getStatusLabel: jasmine.createSpy('getStatusLabel').and.callFake((status: OrderStatus) => {
      const labels: Record<string, string> = {
        PENDING: 'Pending',
        CONFIRMED: 'Confirmed',
        PROCESSING: 'Processing',
        SHIPPED: 'Shipped',
        DELIVERED: 'Delivered',
        CANCELLED: 'Cancelled',
        RETURNED: 'Returned',
      };
      return labels[status] || status;
    }),
    getStatusColor: jasmine.createSpy('getStatusColor').and.returnValue('#000'),
  };

  beforeEach(async () => {
    // Reset spies between tests
    mockOrderService.getOrderById.calls.reset();
    mockOrderService.cancelOrder.calls.reset();
    mockOrderService.redoOrder.calls.reset();
    mockOrderService.canCancel.calls.reset();
    mockOrderService.canRedo.calls.reset();
    mockOrderService.getStatusLabel.calls.reset();

    mockAuthService = jasmine.createSpyObj('Auth', ['isAuthenticated', 'logout'], {
      currentUser: signal({ id: 'buyer-1', name: 'John', role: 'CLIENT' }),
    });
    mockAuthService.isAuthenticated.and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [OrderDetailPage, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: OrderService, useValue: mockOrderService },
        { provide: Auth, useValue: mockAuthService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => 'order-1' } },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OrderDetailPage);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    orderSignal.set(null);
    loadingSignal.set(false);
  });

  // ==================== Component Creation ====================

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ==================== ngOnInit ====================

  describe('ngOnInit', () => {
    it('should load order on init when authenticated and id present', () => {
      mockOrderService.getOrderById.and.returnValue(of(createMockOrder()));
      component.ngOnInit();
      expect(mockOrderService.getOrderById).toHaveBeenCalledWith('order-1');
    });

    it('should redirect to login when not authenticated', () => {
      mockAuthService.isAuthenticated.and.returnValue(false);
      const router = TestBed.inject(ActivatedRoute);
      component.ngOnInit();
      expect(mockOrderService.getOrderById).not.toHaveBeenCalled();
    });

    it('should redirect to /orders when route has no id', () => {
      TestBed.resetTestingModule();
      mockAuthService = jasmine.createSpyObj('Auth', ['isAuthenticated', 'logout'], {
        currentUser: signal({ id: 'buyer-1', name: 'John', role: 'CLIENT' }),
      });
      mockAuthService.isAuthenticated.and.returnValue(true);
      TestBed.configureTestingModule({
        imports: [OrderDetailPage, NoopAnimationsModule],
        providers: [
          provideRouter([]),
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: OrderService, useValue: mockOrderService },
          { provide: Auth, useValue: mockAuthService },
          {
            provide: ActivatedRoute,
            useValue: { snapshot: { paramMap: { get: () => null } } },
          },
        ],
      }).compileComponents();
      const f = TestBed.createComponent(OrderDetailPage);
      const c = f.componentInstance;
      const r = TestBed.inject(Router);
      spyOn(r, 'navigate');
      c.ngOnInit();
      expect(r.navigate).toHaveBeenCalledWith(['/orders']);
    });
  });

  // ==================== Status Helpers ====================

  describe('status helpers', () => {
    it('should return correct status icon', () => {
      expect(component.getStatusIcon('PENDING')).toBe('pending');
      expect(component.getStatusIcon('CONFIRMED')).toBe('check_circle');
      expect(component.getStatusIcon('PROCESSING')).toBe('inventory');
      expect(component.getStatusIcon('SHIPPED')).toBe('local_shipping');
      expect(component.getStatusIcon('DELIVERED')).toBe('done_all');
      expect(component.getStatusIcon('CANCELLED')).toBe('cancel');
      expect(component.getStatusIcon('RETURNED')).toBe('undo');
      expect(component.getStatusIcon('UNKNOWN')).toBe('help');
    });

    it('should delegate getStatusLabel to OrderService', () => {
      component.getStatusLabel('CONFIRMED');
      expect(mockOrderService.getStatusLabel).toHaveBeenCalledWith(OrderStatus.CONFIRMED);
    });
  });

  // ==================== Timeline Steps ====================

  describe('timeline steps', () => {
    it('should have 2 status steps: PENDING and CONFIRMED', () => {
      expect(component.statusSteps).toEqual([
        OrderStatus.PENDING,
        OrderStatus.CONFIRMED,
      ]);
    });

    it('should compute currentStatusIndex correctly', () => {
      orderSignal.set(createMockOrder({ status: OrderStatus.CONFIRMED }));
      expect(component.currentStatusIndex()).toBe(1);
    });

    it('should return -1 for currentStatusIndex when no order', () => {
      orderSignal.set(null);
      expect(component.currentStatusIndex()).toBe(-1);
    });

    it('should mark step as completed when before current', () => {
      orderSignal.set(createMockOrder({ status: OrderStatus.CONFIRMED }));
      expect(component.isStepCompleted(OrderStatus.PENDING)).toBeTrue();
      expect(component.isStepCompleted(OrderStatus.CONFIRMED)).toBeFalse();
    });

    it('should mark step as active when it matches current status', () => {
      orderSignal.set(createMockOrder({ status: OrderStatus.SHIPPED }));
      expect(component.isStepActive(OrderStatus.SHIPPED)).toBeTrue();
      expect(component.isStepActive(OrderStatus.CONFIRMED)).toBeFalse();
    });
  });

  // ==================== Computed Signals ====================

  describe('computed signals', () => {
    it('should detect cancelled status', () => {
      orderSignal.set(createMockOrder({ status: OrderStatus.CANCELLED }));
      expect(component.isCancelled()).toBeTrue();
      expect(component.isReturned()).toBeFalse();
    });

    it('should detect returned status', () => {
      orderSignal.set(createMockOrder({ status: OrderStatus.RETURNED }));
      expect(component.isReturned()).toBeTrue();
      expect(component.isCancelled()).toBeFalse();
    });

    it('should not be cancelled or returned for active status', () => {
      orderSignal.set(createMockOrder({ status: OrderStatus.CONFIRMED }));
      expect(component.isCancelled()).toBeFalse();
      expect(component.isReturned()).toBeFalse();
    });
  });

  // ==================== cancelOrder ====================

  describe('cancelOrder', () => {
    beforeEach(() => {
      orderSignal.set(createMockOrder());
    });

    it('should cancel order when user confirms', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      mockOrderService.cancelOrder.and.returnValue(of(createMockOrder({ status: OrderStatus.CANCELLED })));

      component.cancelOrder();

      expect(mockOrderService.cancelOrder).toHaveBeenCalledWith('order-1', 'Cancelled by user');
    });

    it('should not cancel when user declines confirmation', () => {
      spyOn(window, 'confirm').and.returnValue(false);

      component.cancelOrder();

      expect(mockOrderService.cancelOrder).not.toHaveBeenCalled();
    });

    it('should do nothing when no order', () => {
      orderSignal.set(null);
      component.cancelOrder();
      expect(mockOrderService.cancelOrder).not.toHaveBeenCalled();
    });

    it('should handle cancel error', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      mockOrderService.cancelOrder.and.returnValue(
        throwError(() => new Error('Cancel failed'))
      );

      component.cancelOrder();

      expect(component.isActioning()).toBeFalse();
    });
  });

  // ==================== redoOrder ====================

  describe('redoOrder', () => {
    beforeEach(() => {
      orderSignal.set(createMockOrder({ status: OrderStatus.CANCELLED }));
    });

    it('should redo order when user confirms', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      const newOrder = createMockOrder({ id: 'order-2', status: OrderStatus.PENDING });
      mockOrderService.redoOrder.and.returnValue(of(newOrder));

      component.redoOrder();

      expect(mockOrderService.redoOrder).toHaveBeenCalledWith('order-1');
    });

    it('should not redo when user declines', () => {
      spyOn(window, 'confirm').and.returnValue(false);

      component.redoOrder();

      expect(mockOrderService.redoOrder).not.toHaveBeenCalled();
    });

    it('should do nothing when no order', () => {
      orderSignal.set(null);
      component.redoOrder();
      expect(mockOrderService.redoOrder).not.toHaveBeenCalled();
    });

    it('should handle redo error gracefully', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      mockOrderService.redoOrder.and.returnValue(
        throwError(() => ({ error: { message: 'Stock unavailable' } }))
      );

      component.redoOrder();

      expect(component.isActioning()).toBeFalse();
    });

    it('should fall back to error.error.error when no message', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      const snack = (component as any).snackBar;
      spyOn(snack, 'open').and.returnValue({ onAction: () => of(undefined) } as any);
      mockOrderService.redoOrder.and.returnValue(
        throwError(() => ({ error: { error: 'Bad request' } }))
      );
      component.redoOrder();
      expect(snack.open).toHaveBeenCalledWith('Bad request', 'Close', jasmine.any(Object));
    });

    it('should use default message when error has no details', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      const snack = (component as any).snackBar;
      spyOn(snack, 'open').and.returnValue({ onAction: () => of(undefined) } as any);
      mockOrderService.redoOrder.and.returnValue(
        throwError(() => ({ error: {} }))
      );
      component.redoOrder();
      expect(snack.open).toHaveBeenCalledWith('Failed to redo order', 'Close', jasmine.any(Object));
    });
  });

  // ==================== canCancel / canRedo ====================

  describe('action guards', () => {
    it('canCancel should delegate to OrderService', () => {
      orderSignal.set(createMockOrder());
      mockOrderService.canCancel.and.returnValue(true);

      expect(component.canCancel()).toBeTrue();
      expect(mockOrderService.canCancel).toHaveBeenCalled();
    });

    it('canCancel should return false when no order', () => {
      orderSignal.set(null);
      expect(component.canCancel()).toBeFalse();
    });

    it('canRedo should delegate to OrderService', () => {
      orderSignal.set(createMockOrder({ status: OrderStatus.CANCELLED }));
      mockOrderService.canRedo.and.returnValue(true);

      expect(component.canRedo()).toBeTrue();
      expect(mockOrderService.canRedo).toHaveBeenCalled();
    });

    it('canRedo should return false when no order', () => {
      orderSignal.set(null);
      expect(component.canRedo()).toBeFalse();
    });
  });

  // ==================== Date Formatting ====================

  describe('date formatting', () => {
    it('should format date string correctly', () => {
      const result = component.formatDate('2026-02-09T12:00:00Z');
      expect(result).toContain('2026');
      expect(result).toContain('February');
    });

    it('should format short date correctly', () => {
      const result = component.formatShortDate('2026-02-09T12:00:00Z');
      expect(result).toContain('Feb');
    });
  });

  // ==================== Navigation ====================

  describe('navigation', () => {
    it('should have goBack method', () => {
      expect(component.goBack).toBeDefined();
    });

    it('should have logout method that delegates to auth', () => {
      component.logout();
      expect(mockAuthService.logout).toHaveBeenCalled();
    });
  });
});
