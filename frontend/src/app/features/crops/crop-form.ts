import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { apiErrorMessage, emptyToNull, fieldError, toNumber } from '../../shared/http-utils';
import { CROP_STATUSES, CropStatus, Property } from '../../shared/models';
import { NotifyService } from '../../shared/notify.service';
import { PropertyContext } from '../../shared/property-context';
import { EmptyState } from '../../shared/ui/empty-state';
import { PropertiesService } from '../properties/data/properties.service';
import { CropsService } from './data/crops.service';

@Component({
  selector: 'app-crop-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    EmptyState,
  ],
  templateUrl: './crop-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CropForm {
  private readonly fb = inject(FormBuilder);
  private readonly propertiesApi = inject(PropertiesService);
  private readonly cropsApi = inject(CropsService);
  private readonly context = inject(PropertyContext);
  private readonly notify = inject(NotifyService);
  private readonly router = inject(Router);

  readonly statuses = CROP_STATUSES;
  readonly fieldError = fieldError;
  readonly properties = signal<Property[]>([]);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    propertyId: [this.context.id() ?? '', Validators.required],
    name: ['', Validators.required],
    variety: [''],
    irrigationSystem: [''],
    areaHa: [null as number | null, Validators.min(0)],
    plantingDate: [''],
    expectedHarvestDate: [''],
    status: this.fb.nonNullable.control<CropStatus>('PLANEJADA', Validators.required),
    expectedYield: [''],
    notes: [''],
  });

  constructor() {
    this.propertiesApi.list().subscribe({
      next: (items) => {
        this.properties.set(items);
        this.form.controls.propertyId.setValue(this.context.resolve(items) ?? '');
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(error, 'Não foi possível carregar as propriedades.'));
      },
    });
  }

  submit(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.submitting.set(true);
    this.cropsApi
      .create({
        propertyId: value.propertyId,
        name: value.name.trim(),
        variety: emptyToNull(value.variety),
        irrigationSystem: emptyToNull(value.irrigationSystem),
        areaHa: toNumber(value.areaHa),
        plantingDate: emptyToNull(value.plantingDate),
        expectedHarvestDate: emptyToNull(value.expectedHarvestDate),
        status: value.status,
        expectedYield: emptyToNull(value.expectedYield),
        notes: emptyToNull(value.notes),
      })
      .subscribe({
        next: (crop) => {
          this.context.select(value.propertyId);
          this.notify.success('Cultura cadastrada.');
          void this.router.navigate(crop?.id ? ['/app/culturas', crop.id] : ['/app/culturas']);
        },
        error: (error: HttpErrorResponse) => {
          this.submitting.set(false);
          this.error.set(apiErrorMessage(error, 'Não foi possível cadastrar a cultura.'));
        },
      });
  }
}
