import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { Auth } from '../../../core/services/auth';

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
        MatDividerModule
    ],
    templateUrl: './navbar.html',
    styleUrl: './navbar.css'
})
export class Navbar {
    readonly authService = inject(Auth);
    private readonly router = inject(Router);

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
