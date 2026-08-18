import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, EMPTY, merge, Subject, switchMap } from 'rxjs';
import { apiErrorMessage } from '../../shared/http-utils';
import { Crop, Property } from '../../shared/models';
import { NotifyService } from '../../shared/notify.service';
import { PropertyContext } from '../../shared/property-context';
import { confirmDelete } from '../../shared/ui/confirm-dialog';
import { EmptyState } from '../../shared/ui/empty-state';
import { StatusChip } from '../../shared/ui/status-chip';
import { PropertiesService } from '../properties/data/properties.service';
import { CropsService } from './data/crops.service';

@Component({
  selector: 'app-crops-list',
  imports: [
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    EmptyState,
    StatusChip,
  ],
  templateUrl: './crops-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CropsList {
  private readonly propertiesApi = inject(PropertiesService);
  private readonly cropsApi = inject(CropsService);
  private readonly context = inject(PropertyContext);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly reload$ = new Subject<void>();

  readonly propertyControl = new FormControl<string>('', { nonNullable: true });
  readonly properties = signal<Property[]>([]);
  readonly crops = signal<Crop[]>([]);
  readonly loadingProperties = signal(true);
  readonly loadingCrops = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    this.propertiesApi
      .list()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (items) => {
          this.properties.set(items);
          this.loadingProperties.set(false);
          const fromQuery = this.route.snapshot.queryParamMap.get('propertyId');
          if (fromQuery) {
            this.context.select(fromQuery);
          }
          this.propertyControl.setValue(this.context.resolve(items) ?? '');
        },
        error: (error: HttpErrorResponse) => {
          this.loadingProperties.set(false);
          this.error.set(apiErrorMessage(error, 'Não foi possível carregar as propriedades.'));
        },
      });

    this.propertyControl.valueChanges.pipe(takeUntilDestroyed()).subscribe((id) => {
      this.context.select(id || null);
      void this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { propertyId: id || null },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      });
    });

    merge(this.propertyControl.valueChanges, this.reload$)
      .pipe(
        switchMap(() => {
          const id = this.propertyControl.value;
          if (!id) {
            this.crops.set([]);
            this.loadingCrops.set(false);
            return EMPTY;
          }
          this.loadingCrops.set(true);
          this.error.set(null);
          return this.cropsApi.list(id).pipe(
            catchError((error: HttpErrorResponse) => {
              this.loadingCrops.set(false);
              this.error.set(apiErrorMessage(error, 'Não foi possível carregar as culturas.'));
              this.crops.set([]);
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((items) => {
        this.crops.set(items);
        this.loadingCrops.set(false);
      });
  }

  remove(crop: Crop): void {
    confirmDelete(this.dialog, `a cultura “${crop.name}”`).subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.cropsApi.delete(crop.id).subscribe({
        next: () => {
          this.notify.success('Cultura excluída.');
          this.reload$.next();
        },
        error: (error: HttpErrorResponse) => {
          this.notify.error(apiErrorMessage(error, 'Não foi possível excluir a cultura.'));
        },
      });
    });
  }
}
