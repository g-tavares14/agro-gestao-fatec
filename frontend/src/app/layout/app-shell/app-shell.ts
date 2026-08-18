import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { BrandMark } from '../../shared/ui/brand-mark';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatIconModule, BrandMark],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppShell {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly breakpoints = inject(BreakpointObserver);

  readonly navItems: NavItem[] = [
    { path: '/app/dashboard', label: 'Painel', icon: 'dashboard' },
    { path: '/app/propriedades', label: 'Propriedades', icon: 'landscape' },
    { path: '/app/culturas', label: 'Culturas', icon: 'grass' },
    { path: '/app/diario', label: 'Diário', icon: 'menu_book' },
    { path: '/app/financeiro', label: 'Financeiro', icon: 'payments' },
    { path: '/app/documentos', label: 'Documentos', icon: 'folder' },
  ];

  readonly sidenavOpen = signal(false);
  readonly isMobile = toSignal(
    this.breakpoints.observe([Breakpoints.Handset, Breakpoints.TabletPortrait]).pipe(
      map((state) => state.matches),
    ),
    { initialValue: false },
  );

  readonly userName = computed(() => this.auth.user()?.name ?? 'Conta');

  readonly pageTitle = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      startWith(null),
      map(() => this.resolveTitle()),
    ),
    { initialValue: this.resolveTitle() },
  );

  toggleSidenav(): void {
    this.sidenavOpen.update((open) => !open);
  }

  closeMobileSidenav(): void {
    this.sidenavOpen.set(false);
  }

  logout(): void {
    this.auth.logout('/');
  }

  private resolveTitle(): string {
    let route = this.router.routerState.root;
    while (route.firstChild) {
      route = route.firstChild;
    }
    return (route.snapshot?.data?.['title'] as string | undefined) ?? 'Agro-Gestão';
  }
}
