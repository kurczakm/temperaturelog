import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { Auth } from '../../auth/auth';

@Component({
  selector: 'app-nav',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './nav.component.html',
  styleUrl: './nav.component.scss'
})
export class NavComponent {
  constructor(
    public auth: Auth,
    private router: Router
  ) {}

  signOut(): void {
    this.auth.signOut();
    this.router.navigate(['/signin']);
  }

  isAdmin(): boolean {
    return this.auth.currentUser()?.role === 'ADMIN';
  }
}
