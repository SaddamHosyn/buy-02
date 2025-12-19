/**
 * Custom Form Validators
 * Provides custom validation functions for form controls with user-friendly error messages
 */

import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Price validator - validates positive numbers with optional decimal places
 * @param min Minimum price (default: 0.01)
 * @param max Maximum price (optional)
 * @param maxDecimals Maximum decimal places (default: 2)
 */
export function priceValidator(
  min: number = 0.01,
  max?: number,
  maxDecimals: number = 2
): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    
    // Allow empty value (use Validators.required separately)
    if (value === null || value === undefined || value === '') {
      return null;
    }
    
    const numValue = Number(value);
    
    // Check if valid number
    if (isNaN(numValue)) {
      return { invalidPrice: { message: 'Price must be a valid number' } };
    }
    
    // Check minimum price
    if (numValue < min) {
      return {
        minPrice: {
          min: min,
          actual: numValue,
          message: `Price must be at least €${min.toFixed(2)}`
        }
      };
    }
    
    // Check maximum price
    if (max !== undefined && numValue > max) {
      return {
        maxPrice: {
          max: max,
          actual: numValue,
          message: `Price cannot exceed €${max.toFixed(2)}`
        }
      };
    }
    
    // Check decimal places
    const decimalPart = value.toString().split('.')[1];
    if (decimalPart && decimalPart.length > maxDecimals) {
      return {
        maxDecimals: {
          maxDecimals: maxDecimals,
          actual: decimalPart.length,
          message: `Price can have maximum ${maxDecimals} decimal places`
        }
      };
    }
    
    // Check for negative zero
    if (Object.is(numValue, -0)) {
      return { invalidPrice: { message: 'Price cannot be negative' } };
    }
    
    return null;
  };
}

/**
 * Match validator - validates that two controls have the same value
 * @param controlName Name of the control to match
 * @param matchingControlName Name of the control to match against
 */
export function matchValidator(controlName: string, matchingControlName: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const formGroup = control.parent;
    
    if (!formGroup) {
      return null;
    }
    
    const controlToMatch = formGroup.get(controlName);
    const matchingControl = formGroup.get(matchingControlName);
    
    if (!controlToMatch || !matchingControl) {
      return null;
    }
    
    if (controlToMatch.value !== matchingControl.value) {
      return {
        mismatch: {
          message: `${formatFieldName(controlName)} and ${formatFieldName(matchingControlName)} do not match`
        }
      };
    }
    
    return null;
  };
}

/**
 * Helper function to format field names
 */
function formatFieldName(name: string): string {
  return name
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, (str) => str.toUpperCase())
    .trim();
}

/**
 * Get user-friendly error message from validation errors
 * @param errors Validation errors object
 * @param fieldName Name of the field (optional)
 */
export function getValidationMessage(errors: ValidationErrors | null, fieldName?: string): string {
  if (!errors) {
    return '';
  }
  
  const field = fieldName ? formatFieldName(fieldName) : 'This field';
  
  // Built-in Angular validators
  if (errors['required']) {
    return `${field} is required`;
  }
  
  if (errors['email']) {
    return 'Please enter a valid email address';
  }
  
  if (errors['minlength']) {
    const required = errors['minlength'].requiredLength;
    return `${field} must be at least ${required} characters`;
  }
  
  if (errors['maxlength']) {
    const required = errors['maxlength'].requiredLength;
    return `${field} must not exceed ${required} characters`;
  }
  
  if (errors['min']) {
    const min = errors['min'].min;
    return `${field} must be at least ${min}`;
  }
  
  if (errors['max']) {
    const max = errors['max'].max;
    return `${field} must not exceed ${max}`;
  }
  
  if (errors['pattern']) {
    return `${field} has an invalid format`;
  }
  
  // Custom validators
  if (errors['invalidPrice']?.message) return errors['invalidPrice'].message;
  if (errors['minPrice']?.message) return errors['minPrice'].message;
  if (errors['maxPrice']?.message) return errors['maxPrice'].message;
  if (errors['maxDecimals']?.message) return errors['maxDecimals'].message;
  if (errors['mismatch']?.message) return errors['mismatch'].message;
  
  // Fallback
  return `${field} is invalid`;
}
