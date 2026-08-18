import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { catchError, EMPTY, forkJoin, map, merge, Subject, switchMap } from 'rxjs';
import {
  apiErrorMessage,
  emptyToNull,
  fieldError,
  todayIso,
  toNumber,
} from '../../shared/http-utils';
import { financeTypeLabel } from '../../shared/labels';
import {
  Crop,
  FINANCE_TYPES,
  FinanceEntry,
  FinanceStatement,
  FinanceType,
  Property,
} from '../../shared/models';
import { NotifyService } from '../../shared/notify.service';
import { cropLabel, PropertyContext } from '../../shared/property-context';
import { confirmDelete } from '../../shared/ui/confirm-dialog';
import { EmptyState } from '../../shared/ui/empty-state';
import { PropertiesService } from '../properties/data/properties.service';
import { FinanceService } from './data/finance.service';

const EMPTY_STATEMENT: FinanceStatement = {
  receitaBruta: 0,
  custosProducao: 0,
  outrasDespesas: 0,
  resultado: 0,
};

@Component({
  selector: 'app-finance-page',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    EmptyState,
  ],
  templateUrl: './finance-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FinancePage {
  private readonly propertiesApi = inject(PropertiesService);
  private readonly financeApi = inject(FinanceService);
  private readonly context = inject(PropertyContext);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly reload$ = new Subject<void>();

  readonly types = FINANCE_TYPES;
  readonly fieldError = fieldError;
  readonly typeLabel = financeTypeLabel;

  readonly properties = signal<Property[]>([]);
  readonly crops = signal<Crop[]>([]);
  readonly entries = signal<FinanceEntry[]>([]);
  readonly statement = signal<FinanceStatement | null>(null);
  readonly loadingProperties = signal(true);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly editingId = signal<string | null>(null);

  readonly filterForm = this.fb.nonNullable.group({
    propertyId: [this.context.id() ?? ''],
    cropId: [''],
  });

  readonly form = this.fb.nonNullable.group({
    cropId: [''],
    type: this.fb.nonNullable.control<FinanceType>('RECEITA', Validators.required),
    category: [''],
    description: ['', Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(0)]],
    date: [todayIso(), Validators.required],
  });

  constructor() {
    this.propertiesApi
      .list()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (items) => {
          this.properties.set(items);
          this.loadingProperties.set(false);
          this.filterForm.controls.propertyId.setValue(this.context.resolve(items) ?? '');
        },
        error: (error: HttpErrorResponse) => {
          this.loadingProperties.set(false);
          this.loading.set(false);
          this.error.set(apiErrorMessage(error, 'Não foi possível carregar as propriedades.'));
        },
      });

    this.filterForm.controls.propertyId.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((id) => {
        this.context.select(id || null);
        this.filterForm.controls.cropId.setValue('', { emitEvent: false });
        this.statement.set(null);
      });

    this.context
      .cropsFor(
        this.filterForm.controls.propertyId.valueChanges.pipe(map((id) => id || null)),
        (error) => this.error.set(apiErrorMessage(error, 'Não foi possível carregar as culturas.')),
      )
      .pipe(takeUntilDestroyed())
      .subscribe((crops) => this.crops.set(crops));

    merge(
      this.filterForm.controls.propertyId.valueChanges,
      this.filterForm.controls.cropId.valueChanges,
      this.reload$,
    )
      .pipe(
        switchMap(() => {
          const propertyId = this.filterForm.controls.propertyId.value;
          if (!propertyId) {
            this.entries.set([]);
            this.statement.set(null);
            this.loading.set(false);
            return EMPTY;
          }
          const filters = {
            propertyId,
            cropId: this.filterForm.controls.cropId.value || undefined,
          };
          this.loading.set(true);
          this.error.set(null);
          return forkJoin({
            entries: this.financeApi.list(filters),
            statement: this.financeApi.statement(filters),
          }).pipe(
            catchError((error: HttpErrorResponse) => {
              this.loading.set(false);
              this.error.set(apiErrorMessage(error, 'Não foi possível carregar o financeiro.'));
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe(({ entries, statement }) => {
        this.entries.set(entries);
        this.statement.set({ ...EMPTY_STATEMENT, ...(statement ?? {}) });
        this.loading.set(false);
      });
  }

  cropName(entry: FinanceEntry): string {
    return cropLabel(entry, this.crops());
  }

  startEdit(entry: FinanceEntry): void {
    this.editingId.set(entry.id);
    this.form.patchValue({
      cropId: entry.cropId ?? '',
      type: entry.type,
      category: entry.category ?? '',
      description: entry.description,
      amount: entry.amount,
      date: entry.date,
    });
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.resetForm();
  }

  submit(): void {
    const propertyId = this.filterForm.controls.propertyId.value;
    if (!propertyId) {
      this.error.set('Selecione uma propriedade para lançar o movimento.');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const payload = {
      propertyId,
      cropId: emptyToNull(value.cropId),
      type: value.type,
      category: emptyToNull(value.category),
      description: value.description.trim(),
      amount: toNumber(value.amount) ?? 0,
      date: value.date,
    };
    const editingId = this.editingId();
    this.submitting.set(true);
    const request = editingId
      ? this.financeApi.update(editingId, payload)
      : this.financeApi.create(payload);

    request.subscribe({
      next: () => {
        this.submitting.set(false);
        this.notify.success(editingId ? 'Lançamento atualizado.' : 'Lançamento registrado.');
        this.cancelEdit();
        this.reload();
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.error.set(apiErrorMessage(error, 'Não foi possível salvar o lançamento.'));
      },
    });
  }

  remove(entry: FinanceEntry): void {
    confirmDelete(this.dialog, `o lançamento “${entry.description}”`).subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.financeApi.delete(entry.id).subscribe({
        next: () => {
          this.notify.success('Lançamento excluído.');
          if (this.editingId() === entry.id) {
            this.cancelEdit();
          }
          this.reload();
        },
        error: (error: HttpErrorResponse) => {
          this.notify.error(apiErrorMessage(error, 'Não foi possível excluir o lançamento.'));
        },
      });
    });
  }

  private reload(): void {
    this.reload$.next();
  }

  private resetForm(): void {
    this.form.reset({
      cropId: this.filterForm.controls.cropId.value,
      type: 'RECEITA',
      category: '',
      description: '',
      amount: null,
      date: todayIso(),
    });
  }
}
