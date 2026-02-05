import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Auth } from '../../../core/services/auth';
import { CartService } from '../../../core/services/cart.service';

@Component({
    selector: 'app-navbar',
    standalone: true,
    imports: [
        CommonModule,
        RouterLink,
        RouterLinkActive,
        MatToolbarModule,
        MatButtonModule,
        MatIconModule,
        MatMenuModule,
        MatDividerModule,
        MatBadgeModule,
        MatTooltipModule
    ],
    templateUrl: './navbar.html',
    styleUrl: './navbar.css'
})
export class Navbar {
    readonly authService = inject(Auth);
    readonly cartService = inject(CartService);
    private readonly router = inject(Router);

    readonly cartCount = computed(() => this.cartService.cart()?.totalItems ?? 0);


    logout(): void {
        this.authService.logout();
        this.router.navigate(['/auth/login']);
    }

    goToDashboard(): void {
        if (this.authService.isSeller()) {
            this.router.navigate(['/seller/dashboard']);
        }
    }
}
