import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatStepperModule } from '@angular/material/stepper';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { CartService, Cart } from '../../core/services/cart.service';
import { OrderService, ShippingAddress } from '../../core/services/order.service';
import { Auth } from '../../core/services/auth';
import { FormValidationHelper } from '../../core/helpers/form-validation.helper';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatStepperModule,
    MatInputModule,
    MatFormFieldModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatToolbarModule
  ],
  templateUrl: './checkout.html',
  styleUrl: './checkout.css'
})
export class CheckoutPage implements OnInit {
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly snackBar = inject(MatSnackBar);
  readonly authService = inject(Auth);
  
  // Form
  addressForm!: FormGroup;
  
  // Signals
  readonly cart = this.cartService.cart;
  readonly isLoading = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly currentStep = signal<number>(0);
  
  // Computed
  readonly items = computed(() => this.cart()?.items ?? []);
  readonly subtotal = computed(() => this.cart()?.cachedSubtotal ?? 0);
  readonly totalItems = computed(() => this.cart()?.totalItems ?? 0);
  readonly isEmpty = computed(() => this.items().length === 0);
  
  // Shipping and totals
  readonly shippingCost = signal<number>(0);
  readonly taxAmount = signal<number>(0);
  readonly totalAmount = computed(() => 
    this.subtotal() + this.shippingCost() + this.taxAmount()
  );
  
  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/auth/login']);
      return;
    }
    
    this.initForm();
    this.loadCart();
  }
  
  private initForm(): void {
    const user = this.authService.currentUser();
    
    this.addressForm = this.fb.group({
      fullName: [user?.name || '', [Validators.required, Validators.minLength(2)]],
      addressLine1: ['', [Validators.required, Validators.minLength(5)]],
      addressLine2: [''],
      city: ['', [Validators.required, Validators.minLength(2)]],
      postalCode: ['', [Validators.required, Validators.pattern(/^\d{4,10}$/)]],
      country: ['', [Validators.required, Validators.minLength(2)]],
      phoneNumber: ['', [Validators.pattern(/^\+?[\d\s-]{7,20}$/)]],
      deliveryNotes: ['']
    });
  }
  
  private loadCart(): void {
    this.isLoading.set(true);
    this.cartService.getCart().subscribe({
      next: (cart) => {
        this.isLoading.set(false);
        if (!cart || !cart.items || cart.items.length === 0) {
          this.snackBar.open('Your cart is empty', 'Close', { duration: 3000 });
          this.router.navigate(['/cart']);
        }
      },
      error: (error) => {
        console.error('Error loading cart:', error);
        this.isLoading.set(false);
        this.snackBar.open('Failed to load cart', 'Close', { duration: 3000 });
        this.router.navigate(['/cart']);
      }
    });
  }
  
  /**
   * Go to next step
   */
  nextStep(): void {
    if (this.currentStep() === 0 && this.addressForm.invalid) {
      this.addressForm.markAllAsTouched();
      return;
    }
    this.currentStep.update(step => step + 1);
  }
  
  /**
   * Go to previous step
   */
  prevStep(): void {
    this.currentStep.update(step => step - 1);
  }
  
  /**
   * Place order (Pay on Delivery)
   */
  placeOrder(): void {
    if (this.addressForm.invalid) {
      this.snackBar.open('Please fill in all required fields', 'Close', { duration: 3000 });
      return;
    }
    
    this.isSubmitting.set(true);
    
    const formValue = this.addressForm.value;
    const shippingAddress: ShippingAddress = {
      fullName: formValue.fullName,
      addressLine1: formValue.addressLine1,
      addressLine2: formValue.addressLine2 || undefined,
      city: formValue.city,
      postalCode: formValue.postalCode,
      country: formValue.country,
      phoneNumber: formValue.phoneNumber || undefined
    };
    
    this.orderService.checkout({
      shippingAddress,
      deliveryNotes: formValue.deliveryNotes || undefined,
      paymentMethod: 'PAY_ON_DELIVERY'
    }).subscribe({
      next: (order) => {
        this.isSubmitting.set(false);
        this.cartService.resetCart();
        this.snackBar.open('Order placed successfully!', 'View Order', { duration: 5000 })
          .onAction().subscribe(() => {
            this.router.navigate(['/orders', order.id]);
          });
        this.router.navigate(['/orders', order.id]);
      },
      error: (error) => {
        console.error('Error placing order:', error);
        this.isSubmitting.set(false);
        this.snackBar.open(
          error.error?.message || 'Failed to place order. Please try again.',
          'Close',
          { duration: 5000 }
        );
      }
    });
  }
  
  /**
   * Back to cart
   */
  backToCart(): void {
    this.router.navigate(['/cart']);
  }
  
  /**
   * Get item subtotal
   */
  getItemSubtotal(item: any): number {
    return (item.cachedPrice ?? 0) * item.quantity;
  }
  
  /**
   * Get form error message
   */
  getErrorMessage(controlName: string): string {
    const control = this.addressForm.get(controlName);
    if (control?.hasError('required')) {
      return 'This field is required';
    }
    if (control?.hasError('minlength')) {
      return `Minimum ${control.errors?.['minlength'].requiredLength} characters required`;
    }
    if (control?.hasError('pattern')) {
      if (controlName === 'postalCode') return 'Invalid postal code';
      if (controlName === 'phoneNumber') return 'Invalid phone number';
    }
    return '';
  }
}
