import { Component, inject, signal, AfterViewInit, ElementRef, ViewChild, OnDestroy } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { AuthStore } from '../../auth.store';
import { filter, Subscription } from 'rxjs';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css',
})
export class AdminComponent implements AfterViewInit, OnDestroy {
  auth = inject(AuthStore);
  router = inject(Router);
  isMobileMenuOpen = signal(false);

  @ViewChild('adminDesktopLinks') adminDesktopLinksRef!: ElementRef<HTMLDivElement>;

  adminIndicatorStyle = signal({ left: '0px', width: '0px', opacity: '0' });

  private routerSub!: Subscription;

  ngAfterViewInit() {
    setTimeout(() => this.updateIndicator(), 100);

    this.routerSub = this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => {
        setTimeout(() => this.updateIndicator(), 50);
      });
  }

  ngOnDestroy() {
    this.routerSub?.unsubscribe();
  }

  private updateIndicator() {
    if (!this.adminDesktopLinksRef?.nativeElement) return;

    const container = this.adminDesktopLinksRef.nativeElement;
    const activeLink = container.querySelector('.admin-nav-active') as HTMLElement;

    if (!activeLink) {
      this.adminIndicatorStyle.set({ left: '0px', width: '0px', opacity: '0' });
      return;
    }

    const containerRect = container.getBoundingClientRect();
    const linkRect = activeLink.getBoundingClientRect();

    this.adminIndicatorStyle.set({
      left: `${linkRect.left - containerRect.left}px`,
      width: `${linkRect.width}px`,
      opacity: '1',
    });
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
