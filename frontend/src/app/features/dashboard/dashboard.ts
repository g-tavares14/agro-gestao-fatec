import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../shared/http-utils';
import { locationLabel } from '../../shared/labels';
import { DashboardCrop, DashboardKpis, DashboardProperty } from '../../shared/models';
import { PropertyContext } from '../../shared/property-context';
import { EmptyState } from '../../shared/ui/empty-state';
import { StatusChip } from '../../shared/ui/status-chip';
import { DashboardService } from './data/dashboard.service';

const EMPTY_KPIS: DashboardKpis = {
  receitaMes: 0,
  despesaMes: 0,
  resultadoMes: 0,
  culturasAtivas: 0,
  areaCultivadaHa: 0,
};

@Component({
  selector: 'app-dashboard',
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    RouterLink,
    MatProgressSpinnerModule,
    EmptyState,
    StatusChip,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard {
  private readonly auth = inject(AuthService);
  private readonly dashboardApi = inject(DashboardService);
  private readonly context = inject(PropertyContext);

  readonly today = new Date();
  readonly greeting = computed(() => {
    const name = this.auth.user()?.name;
    return name ? `Olá, ${name}` : 'Olá';
  });

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly kpis = signal<DashboardKpis>(EMPTY_KPIS);
  readonly properties = signal<DashboardProperty[]>([]);
  readonly activeCrops = signal<DashboardCrop[]>([]);

  constructor() {
    this.dashboardApi.get().subscribe({
      next: (data) => {
        this.kpis.set({ ...EMPTY_KPIS, ...(data.kpis ?? {}) });
        this.properties.set(data.properties ?? []);
        this.activeCrops.set(data.activeCrops ?? []);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(error, 'Não foi possível carregar o painel.'));
      },
    });
  }

  locationOf(property: DashboardProperty): string {
    return locationLabel(property.city, property.state);
  }

  selectProperty(id: string): void {
    this.context.select(id);
  }
}
