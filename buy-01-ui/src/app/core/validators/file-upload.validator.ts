/**
 * File Upload Validators
 * Provides validation functions for file uploads with user-friendly error messages
 */

export interface FileValidationResult {
  valid: boolean;
  errors: string[];
}

export interface FileValidationConfig {
  maxSize?: number; // in bytes
  allowedTypes?: string[]; // MIME types
  allowedExtensions?: string[]; // file extensions
  minWidth?: number; // for images
  minHeight?: number; // for images
  maxWidth?: number; // for images
  maxHeight?: number; // for images
}

/**
 * Validate a single file
 */
export function validateFile(
  file: File,
  config: FileValidationConfig
): FileValidationResult {
  const errors: string[] = [];
  
  // Validate file size
  if (config.maxSize && file.size > config.maxSize) {
    const maxSizeMB = (config.maxSize / (1024 * 1024)).toFixed(2);
    const fileSizeMB = (file.size / (1024 * 1024)).toFixed(2);
    errors.push(`File size (${fileSizeMB}MB) exceeds maximum allowed size of ${maxSizeMB}MB`);
  }
  
  // Validate file type (MIME type)
  if (config.allowedTypes && !config.allowedTypes.includes(file.type)) {
    const allowedTypesStr = config.allowedTypes
      .map(type => type.split('/')[1]?.toUpperCase() || type)
      .join(', ');
    errors.push(`Invalid file type. Only ${allowedTypesStr} files are allowed`);
  }
  
  // Validate file extension
  if (config.allowedExtensions) {
    const extension = '.' + file.name.split('.').pop()?.toLowerCase();
    if (!config.allowedExtensions.includes(extension)) {
      const allowedExtStr = config.allowedExtensions.join(', ');
      errors.push(`Invalid file extension. Only ${allowedExtStr} files are allowed`);
    }
  }
  
  return {
    valid: errors.length === 0,
    errors
  };
}

/**
 * Validate multiple files
 */
export function validateFiles(
  files: File[],
  config: FileValidationConfig
): Map<string, FileValidationResult> {
  const results = new Map<string, FileValidationResult>();
  
  files.forEach(file => {
    results.set(file.name, validateFile(file, config));
  });
  
  return results;
}

/**
 * Validate image dimensions (requires loading the image)
 */
export function validateImageDimensions(
  file: File,
  config: FileValidationConfig
): Promise<FileValidationResult> {
  return new Promise((resolve) => {
    const errors: string[] = [];
    
    // Check if it's an image file
    if (!file.type.startsWith('image/')) {
      errors.push('File is not an image');
      resolve({ valid: false, errors });
      return;
    }
    
    const img = new Image();
    const url = URL.createObjectURL(file);
    
    img.onload = () => {
      // Validate minimum width
      if (config.minWidth && img.width < config.minWidth) {
        errors.push(`Image width (${img.width}px) is below minimum required width of ${config.minWidth}px`);
      }
      
      // Validate minimum height
      if (config.minHeight && img.height < config.minHeight) {
        errors.push(`Image height (${img.height}px) is below minimum required height of ${config.minHeight}px`);
      }
      
      // Validate maximum width
      if (config.maxWidth && img.width > config.maxWidth) {
        errors.push(`Image width (${img.width}px) exceeds maximum allowed width of ${config.maxWidth}px`);
      }
      
      // Validate maximum height
      if (config.maxHeight && img.height > config.maxHeight) {
        errors.push(`Image height (${img.height}px) exceeds maximum allowed height of ${config.maxHeight}px`);
      }
      
      URL.revokeObjectURL(url);
      resolve({ valid: errors.length === 0, errors });
    };
    
    img.onerror = () => {
      errors.push('Failed to load image. The file may be corrupted.');
      URL.revokeObjectURL(url);
      resolve({ valid: false, errors });
    };
    
    img.src = url;
  });
}

/**
 * Common validation configurations
 */
export const ValidationPresets = {
  AVATAR: {
    maxSize: 2 * 1024 * 1024, // 2MB
    allowedTypes: ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'],
    allowedExtensions: ['.jpg', '.jpeg', '.png', '.webp'],
    maxWidth: 2000,
    maxHeight: 2000
  },
  
  PRODUCT_IMAGE: {
    maxSize: 2 * 1024 * 1024, // 2MB
    allowedTypes: ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'],
    allowedExtensions: ['.jpg', '.jpeg', '.png', '.gif', '.webp'],
    maxWidth: 4000,
    maxHeight: 4000
  },
  
  DOCUMENT: {
    maxSize: 10 * 1024 * 1024, // 10MB
    allowedTypes: ['application/pdf', 'text/plain', 'application/json'],
    allowedExtensions: ['.pdf', '.txt', '.json']
  }
};
