import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Auth } from '../../core/services/auth';
import { NotificationService } from '../../core/services/notification.service';
import { validateFile, ValidationPresets } from '../../core/validators/file-upload.validator';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    MatDividerModule,
    MatTooltipModule,
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(Auth);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly notification = inject(NotificationService);

  // Signals for state management
  readonly isLoading = signal<boolean>(false);
  readonly selectedFile = signal<File | null>(null);
  readonly imagePreview = signal<string | null>(null);
  readonly uploadError = signal<string>('');
  readonly currentUser = this.authService.currentUser;
  readonly showPasswordFields = signal<boolean>(false);

  // Computed - current avatar or preview
  readonly displayAvatar = computed(() => {
    return this.imagePreview() || this.currentUser()?.avatarUrl || null;
  });

  // Profile form
  readonly profileForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: [{ value: '', disabled: true }],
  });

  // Password form
  readonly passwordForm: FormGroup = this.fb.group(
    {
      currentPassword: ['', [Validators.required, Validators.minLength(6)]],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required, Validators.minLength(6)]],
    },
    { validators: this.passwordMatchValidator }
  );

  ngOnInit(): void {
    const user = this.currentUser();
    if (user) {
      this.profileForm.patchValue({ name: user.name, email: user.email });
      if (user.avatarUrl) {
        this.imagePreview.set(user.avatarUrl);
      }
    } else {
      this.router.navigate(['/auth/login']);
    }
  }

  goBack(): void {
    this.location.back();
  }

  togglePasswordFields(): void {
    this.showPasswordFields.update((v) => !v);
    if (!this.showPasswordFields()) {
      this.passwordForm.reset();
    }
  }

  /**
   * Custom validator for password match
   */
  passwordMatchValidator(group: FormGroup): { [key: string]: boolean } | null {
    const newPassword = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;

    if (newPassword && confirmPassword && newPassword !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  /**
   * Handle file selection with validation
   */
  async onFileSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    // Reset previous state
    this.uploadError.set('');
    this.selectedFile.set(null);

    if (!file) {
      return;
    }

    // Validate file
    const validation = validateFile(file, ValidationPresets.AVATAR);

    if (!validation.valid) {
      const errorMsg = validation.errors[0] || 'Invalid file';
      this.uploadError.set(errorMsg);
      this.notification.error(errorMsg);
      input.value = '';
      return;
    }

    // Set selected file
    this.selectedFile.set(file);

    // Generate preview
    const reader = new FileReader();
    reader.onload = (e) => {
      this.imagePreview.set(e.target?.result as string);
    };
    reader.readAsDataURL(file);
  }

  /**
   * Upload avatar to backend
   */
  uploadAvatar(): void {
    const file = this.selectedFile();
    if (!file) return;

    this.isLoading.set(true);
    this.authService.uploadAvatar(file).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.notification.success('Avatar updated successfully!');
        this.selectedFile.set(null);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.notification.error('Failed to upload avatar. ' + (err.message || ''));
        this.imagePreview.set(this.currentUser()?.avatarUrl || null);
      },
    });
  }

  /**
   * Remove avatar
   */
  removeAvatar(): void {
    this.selectedFile.set(null);
    this.imagePreview.set(null);
    this.uploadError.set('');

    // Reset file input
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  /**
   * Save profile changes (name and avatar if selected)
   */
  onSubmit(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const hasAvatar = this.selectedFile() !== null;
    const hasNameChange = this.profileForm.dirty;

    if (!hasAvatar && !hasNameChange) {
      this.notification.info('No changes to save.');
      return;
    }

    this.isLoading.set(true);

    // If avatar is selected, upload it first
    if (hasAvatar) {
      this.uploadAvatar();
    }

    // If name changed, update it
    if (hasNameChange) {
      const newName = this.profileForm.get('name')?.value;

      this.authService.updateName(newName).subscribe({
        next: () => {
          this.isLoading.set(false);
          this.notification.success('Profile updated successfully!');
          this.profileForm.markAsPristine();
        },
        error: (err) => {
          this.isLoading.set(false);
          const errorMsg = err.error?.message || 'Failed to update profile';
          this.notification.error(errorMsg);
          // Revert to original name
          this.profileForm.patchValue({ name: this.currentUser()?.name });
        },
      });
    } else if (hasAvatar) {
      // Only avatar was changed, loading is handled by uploadAvatar
      this.isLoading.set(false);
    }
  }

  /**
   * Change password
   */
  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    // Check password match
    if (this.passwordForm.hasError('passwordMismatch')) {
      this.notification.error('Passwords do not match');
      return;
    }

    this.isLoading.set(true);
    const { currentPassword, newPassword } = this.passwordForm.value;

    this.authService.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.notification.success('Password changed successfully!');
        this.passwordForm.reset();
        this.showPasswordFields.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        const errorMessage = err.error?.message || '';
        if (errorMessage.includes('Incorrect current password')) {
          this.passwordForm.get('currentPassword')?.setErrors({ incorrect: true });
          this.notification.error('Incorrect current password.');
        } else {
          this.notification.error('Failed to change password. Please try again.');
        }
      },
    });
  }

  /**
   * Get form control error message
   */
  getErrorMessage(controlName: string): string {
    // Check which form contains the control
    let control = this.profileForm.get(controlName);
    if (!control) {
      control = this.passwordForm.get(controlName);
    }

    if (!control) return '';

    if (control.hasError('required')) {
      return `${this.getFieldLabel(controlName)} is required`;
    }
    if (control.hasError('minlength')) {
      const requiredLength = control.errors?.['minlength']?.requiredLength;
      return `Must be at least ${requiredLength} characters`;
    }
    if (control.hasError('passwordMismatch')) {
      return 'Passwords do not match';
    }
    if (control.hasError('incorrect')) {
      return 'Incorrect password';
    }

    return '';
  }

  /**
   * Get field label for display
   */
  private getFieldLabel(controlName: string): string {
    const nameMap: { [key: string]: string } = {
      name: 'Full Name',
      email: 'Email',
      currentPassword: 'Current password',
      newPassword: 'New password',
      confirmPassword: 'Confirm password',
    };
    return nameMap[controlName] || controlName;
  }
}
