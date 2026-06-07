import { Component, inject, signal, AfterViewInit, ElementRef, ViewChild, NgZone, OnDestroy } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { AuthStore } from '../../auth.store';
import { filter, Subscription } from 'rxjs';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
})
export class NavbarComponent implements AfterViewInit, OnDestroy {
  auth = inject(AuthStore);
  private router = inject(Router);
  private zone = inject(NgZone);
  isMobileMenuOpen = signal(false);

  @ViewChild('desktopLinks') desktopLinksRef!: ElementRef<HTMLDivElement>;
  @ViewChild('navIndicator') navIndicatorRef!: ElementRef<HTMLSpanElement>;

  indicatorStyle = signal({ left: '0px', width: '0px', opacity: '0' });

  private routerSub!: Subscription;

  get isAdmin(): boolean {
    return this.router.url.startsWith('/admin');
  }

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
    if (!this.desktopLinksRef?.nativeElement) return;

    const container = this.desktopLinksRef.nativeElement;
    const activeLink = container.querySelector('.nav-link--active') as HTMLElement;

    if (!activeLink) {
      this.indicatorStyle.set({ left: '0px', width: '0px', opacity: '0' });
      return;
    }

    const containerRect = container.getBoundingClientRect();
    const linkRect = activeLink.getBoundingClientRect();

    this.indicatorStyle.set({
      left: `${linkRect.left - containerRect.left}px`,
      width: `${linkRect.width}px`,
      opacity: '1',
    });
  }

  logout() {
    this.auth.logout();
    this.isMobileMenuOpen.set(false);
    this.router.navigate(['/']);
  }
}
