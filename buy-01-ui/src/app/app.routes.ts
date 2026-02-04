// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { roleGuard } from './core/guards/role-guard';

/**
 * Modern Angular routing configuration
 * - Standalone components with lazy loading
 * - Functional guards for auth and role-based access
 * - No NgModules - pure CSR
 */
export const routes: Routes = [
  {
    path: '',
    redirectTo: '/products',
    pathMatch: 'full'
  },

  // Auth routes (public - no guards)
  {
    path: 'auth',
    children: [
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login/login').then(m => m.Login),
        title: 'Login - Buy-02'
      },
      {
        path: 'register',
        loadComponent: () => import('./features/auth/register/register').then(m => m.Register),
        title: 'Register - Buy-02'
      },
      {
        path: '',
        redirectTo: 'login',
        pathMatch: 'full'
      }
    ]
  },

  // Products routes (public - no authentication required)
  {
    path: 'products',
    children: [
      {
        path: '',
        loadComponent: () => import('./features/products/product-list/product-list').then(m => m.ProductList),
        title: 'Products - Buy-02'
      },
      {
        path: ':id',
        loadComponent: () => import('./features/products/product-detail/product-detail').then(m => m.ProductDetail),
        title: 'Product Details - Buy-02'
      }
    ]
  },

  // Profile route (protected - requires authentication)
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./features/profile/profile').then(m => m.Profile),
    title: 'My Profile - Buy-02'
  },

  // Seller routes (protected - requires authentication + SELLER role)
  {
    path: 'seller',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['SELLER', 'ADMIN'] }, // Only sellers and admins can access
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/seller/dashboard/dashboard').then(m => m.Dashboard),
        title: 'Seller Dashboard - Buy-02'
      },
      {
        path: 'product-form',
        loadComponent: () => import('./features/seller/product-form/product-form').then(m => m.ProductForm),
        title: 'Create Product - Buy-02'
      },
      {
        path: 'product-form/:id',
        loadComponent: () => import('./features/seller/product-form/product-form').then(m => m.ProductForm),
        title: 'Edit Product - Buy-02'
      },
      {
        path: 'media-manager',
        loadComponent: () => import('./features/seller/media-manager/media-manager').then(m => m.MediaManager),
        title: 'Media Manager - Buy-02'
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  },

  // 404 - Redirect to products
  {
    path: '**',
    redirectTo: '/products'
  }
];

