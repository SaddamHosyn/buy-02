import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, forkJoin } from 'rxjs';
import { environment } from '../../../environments/environment';
import { validateFile, validateFiles, ValidationPresets } from '../validators/file-upload.validator';

export interface Media {
  id: string;
  url: string;
  originalFilename: string;
  size: number;
  contentType: string;
  userId?: string;
  productId?: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class MediaService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = environment.mediaUrl;
  private readonly mediaSignal = signal<Media[]>([]);
  readonly media = this.mediaSignal.asReadonly();

  uploadFile(file: File): Observable<Media> {
    const validation = validateFile(file, ValidationPresets.PRODUCT_IMAGE);
    if (!validation.valid) {
      throw new Error(validation.errors[0]);
    }
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Media>(`${this.API_URL}/images`, formData).pipe(
      tap(media => this.mediaSignal.update(mediaList => [...mediaList, media]))
    );
  }

  uploadFiles(files: File[]): Observable<Media[]> {
    const validationResults = validateFiles(files, ValidationPresets.PRODUCT_IMAGE);
    const invalidFiles: string[] = [];
    
    validationResults.forEach((result, filename) => {
      if (!result.valid) {
        invalidFiles.push(`${filename}: ${result.errors[0]}`);
      }
    });
    
    if (invalidFiles.length > 0) {
      throw new Error(`Some files are invalid: ${invalidFiles.join('; ')}`);
    }
    
    const uploads = files.map(file => this.uploadFile(file));
    return forkJoin(uploads);
  }

  getMediaUrl(id: string): string {
    return `${this.API_URL}/images/${id}`;
  }

  getAllMedia(): Observable<Media[]> {
    return this.http.get<Media[]>(`${this.API_URL}/images`).pipe(
      tap(media => this.mediaSignal.set(media))
    );
  }

  deleteMedia(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/images/${id}`).pipe(
      tap(() => this.mediaSignal.update(media => media.filter(m => m.id !== id)))
    );
  }

  deleteMediaFiles(ids: string[]): Observable<void[]> {
    const deletions = ids.map(id => this.deleteMedia(id));
    return forkJoin(deletions);
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }
}
