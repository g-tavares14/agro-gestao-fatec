import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
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
import { RouterLink } from '@angular/router';
import {
  apiErrorMessage,
  fieldError,
  todayIso,
  toNumber,
} from '../../shared/http-utils';
import { itemCategoryLabel } from '../../shared/labels';
import { Crop, ITEM_CATEGORIES, ItemCategory, ProductionCost } from '../../shared/models';
import { NotifyService } from '../../shared/notify.service';
import { PropertyContext } from '../../shared/property-context';
import { confirmDelete } from '../../shared/ui/confirm-dialog';
import { EmptyState } from '../../shared/ui/empty-state';
import { CostsService } from './data/costs.service';
import { CropsService } from './data/crops.service';

@Component({
  selector: 'app-crop-costs',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    EmptyState,
  ],
  templateUrl: './crop-costs.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CropCosts {
  private readonly cropsApi = inject(CropsService);
  private readonly costsApi = inject(CostsService);
  private readonly context = inject(PropertyContext);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);

  readonly id = input.required<string>();
  readonly categories = ITEM_CATEGORIES;
  readonly fieldError = fieldError;
  readonly categoryLabel = itemCategoryLabel;

  readonly crop = signal<Crop | null>(null);
  readonly costs = signal<ProductionCost[]>([]);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly editingId = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    description: ['', Validators.required],
    category: this.fb.nonNullable.control<ItemCategory>('INSUMO', Validators.required),
    amount: [null as number | null, [Validators.required, Validators.min(0)]],
    date: [todayIso(), Validators.required],
  });

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => this.load(id));
    });
  }

  total(): number {
    return this.costs().reduce((sum, item) => sum + (item.amount ?? 0), 0);
  }

  startEdit(cost: ProductionCost): void {
    this.editingId.set(cost.id);
    this.form.patchValue({
      description: cost.description,
      category: cost.category,
      amount: cost.amount,
      date: cost.date,
    });
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.resetForm();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const cropId = this.id();
    const value = this.form.getRawValue();
    const payload = {
      description: value.description.trim(),
      category: value.category,
      amount: toNumber(value.amount) ?? 0,
      date: value.date,
    };
    const editingId = this.editingId();
    this.submitting.set(true);
    const request = editingId
      ? this.costsApi.update(editingId, payload)
      : this.costsApi.create(cropId, payload);

    request.subscribe({
      next: () => {
        this.submitting.set(false);
        this.notify.success(editingId ? 'Custo atualizado.' : 'Custo lançado.');
        this.cancelEdit();
        this.loadCosts(cropId);
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.error.set(apiErrorMessage(error, 'Não foi possível salvar o custo.'));
      },
    });
  }

  remove(cost: ProductionCost): void {
    confirmDelete(this.dialog, `o custo “${cost.description}”`).subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.costsApi.delete(cost.id).subscribe({
        next: () => {
          this.notify.success('Custo excluído.');
          if (this.editingId() === cost.id) {
            this.cancelEdit();
          }
          this.loadCosts(this.id());
        },
        error: (error: HttpErrorResponse) => {
          this.notify.error(apiErrorMessage(error, 'Não foi possível excluir o custo.'));
        },
      });
    });
  }

  private load(id: string): void {
    this.loading.set(true);
    this.crop.set(null);
    this.costs.set([]);
    this.error.set(null);
    this.cropsApi.get(id).subscribe({
      next: (crop) => {
        this.crop.set(crop);
        if (crop.propertyId) {
          this.context.select(crop.propertyId);
        }
        this.loadCosts(id);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(error, 'Não foi possível carregar a cultura.'));
      },
    });
  }

  private loadCosts(cropId: string): void {
    this.costsApi.list(cropId).subscribe({
      next: (items) => {
        this.costs.set(items);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(error, 'Não foi possível carregar os custos.'));
      },
    });
  }

  private resetForm(): void {
    this.form.reset({
      description: '',
      category: 'INSUMO',
      amount: null,
      date: todayIso(),
    });
  }
}
