import { AbstractControl, FormGroup, ValidationErrors } from '@angular/forms';

/**
 * Form Validation Helper Service
 * Provides consistent validation error messages across the application
 */
export class FormValidationHelper {
  
  /**
   * Get user-friendly error message for a form control
   */
  static getErrorMessage(control: AbstractControl | null, fieldName: string): string {
    if (!control || !control.errors || !control.touched) {
      return '';
    }

    const errors = control.errors;

    // Required
    if (errors['required']) {
      return `${fieldName} is required`;
    }

    // Email
    if (errors['email']) {
      return 'Please enter a valid email address';
    }

    // Min length
    if (errors['minlength']) {
      const requiredLength = errors['minlength'].requiredLength;
      const actualLength = errors['minlength'].actualLength;
      return `${fieldName} must be at least ${requiredLength} characters (current: ${actualLength})`;
    }

    // Max length
    if (errors['maxlength']) {
      const requiredLength = errors['maxlength'].requiredLength;
      return `${fieldName} must not exceed ${requiredLength} characters`;
    }

    // Min value
    if (errors['min']) {
      const min = errors['min'].min;
      return `${fieldName} must be at least ${min}`;
    }

    // Max value
    if (errors['max']) {
      const max = errors['max'].max;
      return `${fieldName} must not exceed ${max}`;
    }

    // Pattern
    if (errors['pattern']) {
      return this.getPatternErrorMessage(fieldName, errors['pattern']);
    }

    // Password mismatch
    if (errors['passwordMismatch']) {
      return 'Passwords do not match';
    }

    // Incorrect password
    if (errors['incorrect']) {
      return 'Incorrect password';
    }

    // Custom error message
    if (errors['custom']) {
      return errors['custom'];
    }

    return 'Invalid value';
  }

  /**
   * Get pattern-specific error messages
   */
  private static getPatternErrorMessage(fieldName: string, error: ValidationErrors): string {
    const pattern = error['requiredPattern'];
    
    // Phone number pattern
    if (pattern.includes('^\\+?[\\d\\s-]')) {
      return 'Please enter a valid phone number (e.g., +1234567890)';
    }
    
    // Postal code pattern
    if (pattern.includes('^\\d{4,10}$')) {
      return 'Please enter a valid postal code (4-10 digits)';
    }
    
    // URL pattern
    if (pattern.includes('https?://')) {
      return 'Please enter a valid URL';
    }
    
    return `${fieldName} format is invalid`;
  }

  /**
   * Get all error messages for a form
   */
  static getAllErrors(form: FormGroup): string[] {
    const errors: string[] = [];
    
    Object.keys(form.controls).forEach(key => {
      const control = form.get(key);
      if (control && control.invalid && control.touched) {
        const message = this.getErrorMessage(control, this.formatFieldName(key));
        if (message) {
          errors.push(message);
        }
      }
    });
    
    return errors;
  }

  /**
   * Check if form has any errors
   */
  static hasErrors(form: FormGroup): boolean {
    return form.invalid && form.touched;
  }

  /**
   * Mark all fields as touched to show errors
   */
  static markAllAsTouched(form: FormGroup): void {
    Object.keys(form.controls).forEach(key => {
      const control = form.get(key);
      control?.markAsTouched();
    });
  }

  /**
   * Format field name for display
   */
  private static formatFieldName(fieldName: string): string {
    // Convert camelCase to Title Case
    return fieldName
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, str => str.toUpperCase())
      .trim();
  }

  /**
   * Validate field on blur
   */
  static validateOnBlur(control: AbstractControl): void {
    control.markAsTouched();
    control.updateValueAndValidity();
  }

  /**
   * Check if specific error exists
   */
  static hasError(control: AbstractControl | null, errorName: string): boolean {
    return !!(control && control.errors && control.errors[errorName] && control.touched);
  }

  /**
   * Get first error message from form
   */
  static getFirstError(form: FormGroup): string | null {
    const errors = this.getAllErrors(form);
    return errors.length > 0 ? errors[0] : null;
  }

  /**
   * Reset form and clear all errors
   */
  static resetForm(form: FormGroup): void {
    form.reset();
    Object.keys(form.controls).forEach(key => {
      form.get(key)?.setErrors(null);
      form.get(key)?.markAsUntouched();
      form.get(key)?.markAsPristine();
    });
  }

  /**
   * Set custom error on control
   */
  static setCustomError(control: AbstractControl, message: string): void {
    control.setErrors({ custom: message });
    control.markAsTouched();
  }

  /**
   * Clear specific error from control
   */
  static clearError(control: AbstractControl, errorName: string): void {
    if (control.errors) {
      const errors = { ...control.errors };
      delete errors[errorName];
      
      if (Object.keys(errors).length === 0) {
        control.setErrors(null);
      } else {
        control.setErrors(errors);
      }
    }
  }
}
