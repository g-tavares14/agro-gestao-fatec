import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import {
  apiErrorMessage,
  emptyToNull,
  fieldError,
  roundMoney,
  toNumber,
} from '../../shared/http-utils';
import {
  CROP_STATUSES,
  CropStatus,
  ITEM_CATEGORIES,
  ItemCategory,
  PdfExtracted,
  PlannedGroupKey,
  PlannedItem,
  PLANNED_GROUPS,
  Property,
} from '../../shared/models';
import { NotifyService } from '../../shared/notify.service';
import { SelectedPropertyStore } from '../../shared/selected-property.store';
import { EmptyState } from '../../shared/ui/empty-state';
import { PropertiesService } from '../properties/data/properties.service';
import { mapExtractedCrop } from './data/crop-mappers';
import { CropsService } from './data/crops.service';

type ItemForm = FormGroup<{
  description: FormControl<string>;
  unit: FormControl<string>;
  quantity: FormControl<number | null>;
  unitValue: FormControl<number | null>;
  totalValue: FormControl<number | null>;
}>;

@Component({
  selector: 'app-crop-pdf',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    EmptyState,
  ],
  templateUrl: './crop-pdf.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CropPdf {
  private readonly fb = inject(FormBuilder);
  private readonly propertiesApi = inject(PropertiesService);
  private readonly cropsApi = inject(CropsService);
  private readonly selectedProperty = inject(SelectedPropertyStore);
  private readonly notify = inject(NotifyService);
  private readonly router = inject(Router);

  readonly groups = PLANNED_GROUPS;
  readonly statuses = CROP_STATUSES;
  readonly categories = ITEM_CATEGORIES;
  readonly fieldError = fieldError;

  readonly step = signal<'upload' | 'review'>('upload');
  readonly properties = signal<Property[]>([]);
  readonly loadingProperties = signal(true);
  readonly analyzing = signal(false);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly warnings = signal<string[]>([]);
  readonly selectedFile = signal<File | null>(null);

  private analysisId = '';

  readonly uploadForm = this.fb.nonNullable.group({
    propertyId: [this.selectedProperty.id() ?? '', Validators.required],
  });

  readonly reviewForm = this.fb.nonNullable.group({
    propertyId: ['', Validators.required],
    name: ['', Validators.required],
    variety: [''],
    irrigationSystem: [''],
    areaHa: [null as number | null, Validators.min(0)],
    plantingDate: [''],
    expectedHarvestDate: [''],
    status: this.fb.nonNullable.control<CropStatus>('PLANEJADA', Validators.required),
    expectedYield: [''],
    notes: [''],
    importCosts: [true],
    acoesMecanicas: this.fb.array<ItemForm>([]),
    acoesManuais: this.fb.array<ItemForm>([]),
    insumos: this.fb.array<ItemForm>([]),
    outros: this.fb.array<ItemForm>([]),
  });

  constructor() {
    this.propertiesApi.list().subscribe({
      next: (items) => {
        this.properties.set(items);
        const current = this.uploadForm.controls.propertyId.value;
        if (!items.some((item) => item.id === current)) {
          this.uploadForm.controls.propertyId.setValue(items.length === 1 ? items[0].id : '');
        }
        this.loadingProperties.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loadingProperties.set(false);
        this.error.set(apiErrorMessage(error, 'Não foi possível carregar as propriedades.'));
      },
    });
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (file && file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
      this.error.set('Envie um arquivo PDF.');
      this.selectedFile.set(null);
      input.value = '';
      return;
    }
    this.error.set(null);
    this.selectedFile.set(file);
  }

  analyze(): void {
    this.error.set(null);
    if (this.uploadForm.invalid) {
      this.uploadForm.markAllAsTouched();
      return;
    }
    const file = this.selectedFile();
    if (!file) {
      this.error.set('Selecione o PDF da ficha de custo.');
      return;
    }

    const propertyId = this.uploadForm.controls.propertyId.value;
    this.analyzing.set(true);
    this.cropsApi.analyzePdf(file, propertyId).subscribe({
      next: (response) => {
        this.analysisId = response.analysisId;
        this.warnings.set(response.warnings ?? []);
        this.populateReview(propertyId, response.extracted);
        this.analyzing.set(false);
        this.step.set('review');
      },
      error: (error: HttpErrorResponse) => {
        this.analyzing.set(false);
        this.error.set(
          apiErrorMessage(error, 'Não foi possível analisar o PDF.', { gemini: true }),
        );
      },
    });
  }

  itemsOf(key: PlannedGroupKey): FormArray<ItemForm> {
    return this.reviewForm.controls[key];
  }

  addItem(key: PlannedGroupKey): void {
    this.itemsOf(key).push(this.itemGroup());
  }

  removeItem(key: PlannedGroupKey, index: number): void {
    this.itemsOf(key).removeAt(index);
  }

  moveItem(from: PlannedGroupKey, index: number, category: ItemCategory): void {
    const target = this.groups.find((group) => group.category === category);
    if (!target || target.key === from) {
      return;
    }
    const source = this.itemsOf(from);
    const item = source.at(index);
    source.removeAt(index);
    this.itemsOf(target.key).push(item);
  }

  groupTotal(key: PlannedGroupKey): number {
    return this.itemsOf(key).controls.reduce((sum, item) => {
      const quantity = toNumber(item.controls.quantity.value);
      const unitValue = toNumber(item.controls.unitValue.value);
      if (quantity !== null && unitValue !== null) {
        return sum + roundMoney(quantity * unitValue);
      }
      return sum + (toNumber(item.controls.totalValue.value) ?? 0);
    }, 0);
  }

  categoryOf(key: PlannedGroupKey): ItemCategory {
    return this.groups.find((group) => group.key === key)?.category ?? 'OUTRO';
  }

  confirm(): void {
    this.error.set(null);
    if (this.reviewForm.invalid) {
      this.reviewForm.markAllAsTouched();
      return;
    }

    this.recalcItemTotals();
    const value = this.reviewForm.getRawValue();
    this.submitting.set(true);
    this.cropsApi
      .confirmPdf({
        analysisId: this.analysisId,
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
        acoesMecanicas: this.payloadItems('acoesMecanicas'),
        acoesManuais: this.payloadItems('acoesManuais'),
        insumos: this.payloadItems('insumos'),
        outros: this.payloadItems('outros'),
        importCosts: value.importCosts,
      })
      .subscribe({
        next: (crop) => {
          this.selectedProperty.set(value.propertyId);
          this.notify.success('Cultura importada com sucesso.');
          void this.router.navigate(crop?.id ? ['/app/culturas', crop.id] : ['/app/culturas']);
        },
        error: (error: HttpErrorResponse) => {
          this.submitting.set(false);
          this.error.set(
            apiErrorMessage(error, 'Não foi possível confirmar a importação.', { gemini: true }),
          );
        },
      });
  }

  backToUpload(): void {
    this.step.set('upload');
    this.submitting.set(false);
  }

  private populateReview(propertyId: string, extracted: PdfExtracted | null | undefined): void {
    const mapped = mapExtractedCrop(extracted);
    this.reviewForm.patchValue({
      propertyId,
      name: mapped.name,
      variety: mapped.variety,
      irrigationSystem: mapped.irrigationSystem,
      areaHa: mapped.areaHa,
      plantingDate: mapped.plantingDate,
      expectedHarvestDate: mapped.expectedHarvestDate,
      status: mapped.status,
      expectedYield: mapped.expectedYield,
      notes: mapped.notes,
      importCosts: true,
    });
    for (const group of this.groups) {
      const array = this.itemsOf(group.key);
      array.clear();
      for (const item of mapped.groups[group.key]) {
        array.push(this.itemGroup(item));
      }
    }
  }

  private itemGroup(item?: PlannedItem): ItemForm {
    const group = this.fb.group({
      description: this.fb.nonNullable.control(item?.description ?? '', Validators.required),
      unit: this.fb.nonNullable.control(item?.unit ?? ''),
      quantity: this.fb.control<number | null>(item?.quantity ?? null, Validators.min(0)),
      unitValue: this.fb.control<number | null>(item?.unitValue ?? null, Validators.min(0)),
      totalValue: this.fb.control<number | null>(item?.totalValue ?? null, Validators.min(0)),
    });

    return group;
  }

  private recalcItemTotals(): void {
    for (const group of this.groups) {
      for (const item of this.itemsOf(group.key).controls) {
        this.recalcTotal(item);
      }
    }
  }

  private recalcTotal(group: ItemForm): void {
    const quantity = toNumber(group.controls.quantity.value);
    const unitValue = toNumber(group.controls.unitValue.value);
    if (quantity === null || unitValue === null) {
      return;
    }
    group.controls.totalValue.setValue(roundMoney(quantity * unitValue), { emitEvent: false });
  }

  private payloadItems(key: PlannedGroupKey): PlannedItem[] {
    const category = this.categoryOf(key);
    return this.itemsOf(key)
      .getRawValue()
      .filter((item) => item.description.trim())
      .map((item) => ({
        description: item.description.trim(),
        unit: emptyToNull(item.unit),
        quantity: toNumber(item.quantity),
        unitValue: toNumber(item.unitValue),
        totalValue: toNumber(item.totalValue),
        category,
      }));
  }
}
