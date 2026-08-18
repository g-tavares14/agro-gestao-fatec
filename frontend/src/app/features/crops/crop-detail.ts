import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { apiErrorMessage, emptyToNull } from '../../shared/http-utils';
import { CROP_STATUSES, Crop, CropStatus, PlannedItem, PLANNED_GROUPS } from '../../shared/models';
import { NotifyService } from '../../shared/notify.service';
import { PropertyContext } from '../../shared/property-context';
import { confirmDelete } from '../../shared/ui/confirm-dialog';
import { EmptyState } from '../../shared/ui/empty-state';
import { StatusChip } from '../../shared/ui/status-chip';
import { plannedItemsOf } from './data/crop-mappers';
import { CropsService } from './data/crops.service';

@Component({
  selector: 'app-crop-detail',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    EmptyState,
    StatusChip,
  ],
  templateUrl: './crop-detail.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CropDetail {
  private readonly cropsApi = inject(CropsService);
  private readonly context = inject(PropertyContext);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly router = inject(Router);

  readonly id = input.required<string>();
  readonly groups = PLANNED_GROUPS;
  readonly statuses = CROP_STATUSES;

  readonly crop = signal<Crop | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    status: this.fb.nonNullable.control<CropStatus>('PLANEJADA', Validators.required),
    plantingDate: [''],
    expectedHarvestDate: [''],
    notes: [''],
  });

  readonly title = computed(() => this.crop()?.name ?? 'Cultura');

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => this.load(id));
    });
  }

  itemsOf(crop: Crop, key: (typeof PLANNED_GROUPS)[number]['key']): PlannedItem[] {
    return plannedItemsOf(crop, key);
  }

  totalOf(items: PlannedItem[]): number {
    return items.reduce((sum, item) => sum + (item.totalValue ?? 0), 0);
  }

  saveCycle(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const crop = this.crop();
    if (!crop) {
      return;
    }

    const value = this.form.getRawValue();
    this.saving.set(true);
    this.cropsApi
      .updateCycle(crop.id, {
        status: value.status,
        plantingDate: emptyToNull(value.plantingDate),
        expectedHarvestDate: emptyToNull(value.expectedHarvestDate),
        notes: emptyToNull(value.notes),
      })
      .subscribe({
        next: (updated) => {
          this.saving.set(false);
          this.error.set(null);
          this.applyCrop(updated);
          this.notify.success('Ciclo atualizado.');
        },
        error: (error: HttpErrorResponse) => {
          this.saving.set(false);
          this.error.set(apiErrorMessage(error, 'Não foi possível atualizar o ciclo.'));
        },
      });
  }

  remove(): void {
    const crop = this.crop();
    if (!crop) {
      return;
    }
    confirmDelete(this.dialog, `a cultura “${crop.name}”`).subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.cropsApi.delete(crop.id).subscribe({
        next: () => {
          this.notify.success('Cultura excluída.');
          void this.router.navigateByUrl('/app/culturas');
        },
        error: (error: HttpErrorResponse) => {
          this.notify.error(apiErrorMessage(error, 'Não foi possível excluir a cultura.'));
        },
      });
    });
  }

  private load(id: string): void {
    this.loading.set(true);
    this.crop.set(null);
    this.error.set(null);
    this.cropsApi.get(id).subscribe({
      next: (crop) => {
        this.applyCrop(crop);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(error, 'Não foi possível carregar a cultura.'));
      },
    });
  }

  private applyCrop(crop: Crop): void {
    this.crop.set(crop);
    if (crop.propertyId) {
      this.context.select(crop.propertyId);
    }
    this.form.patchValue({
      status: crop.status,
      plantingDate: crop.plantingDate ?? '',
      expectedHarvestDate: crop.expectedHarvestDate ?? '',
      notes: crop.notes ?? '',
    });
  }
}
