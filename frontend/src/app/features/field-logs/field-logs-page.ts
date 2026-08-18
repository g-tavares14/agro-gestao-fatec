import { DatePipe } from '@angular/common';
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
import { catchError, EMPTY, map, merge, Subject, switchMap } from 'rxjs';
import {
  apiErrorMessage,
  emptyToNull,
  fieldError,
  todayIso,
} from '../../shared/http-utils';
import { Crop, FieldLog, Property } from '../../shared/models';
import { NotifyService } from '../../shared/notify.service';
import { cropLabel, PropertyContext } from '../../shared/property-context';
import { confirmDelete } from '../../shared/ui/confirm-dialog';
import { EmptyState } from '../../shared/ui/empty-state';
import { PropertiesService } from '../properties/data/properties.service';
import { FieldLogsService } from './data/field-logs.service';

@Component({
  selector: 'app-field-logs-page',
  imports: [
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
  templateUrl: './field-logs-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FieldLogsPage {
  private readonly propertiesApi = inject(PropertiesService);
  private readonly logsApi = inject(FieldLogsService);
  private readonly context = inject(PropertyContext);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly reload$ = new Subject<void>();

  readonly fieldError = fieldError;
  readonly properties = signal<Property[]>([]);
  readonly crops = signal<Crop[]>([]);
  readonly logs = signal<FieldLog[]>([]);
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
    date: [todayIso(), Validators.required],
    activity: ['', Validators.required],
    notes: [''],
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
            this.logs.set([]);
            this.loading.set(false);
            return EMPTY;
          }
          this.loading.set(true);
          this.error.set(null);
          return this.logsApi
            .list({
              propertyId,
              cropId: this.filterForm.controls.cropId.value || undefined,
            })
            .pipe(
              catchError((error: HttpErrorResponse) => {
                this.loading.set(false);
                this.error.set(
                  apiErrorMessage(error, 'Não foi possível carregar o diário de campo.'),
                );
                return EMPTY;
              }),
            );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((items) => {
        this.logs.set(items);
        this.loading.set(false);
      });
  }

  cropName(log: FieldLog): string {
    return cropLabel(log, this.crops());
  }

  startEdit(log: FieldLog): void {
    this.editingId.set(log.id);
    this.form.patchValue({
      cropId: log.cropId ?? '',
      date: log.date,
      activity: log.activity,
      notes: log.notes ?? '',
    });
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.resetForm();
  }

  submit(): void {
    const propertyId = this.filterForm.controls.propertyId.value;
    if (!propertyId) {
      this.error.set('Selecione uma propriedade para registrar o diário.');
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
      date: value.date,
      activity: value.activity.trim(),
      notes: emptyToNull(value.notes),
    };
    const editingId = this.editingId();
    this.submitting.set(true);
    const request = editingId
      ? this.logsApi.update(editingId, payload)
      : this.logsApi.create(payload);

    request.subscribe({
      next: () => {
        this.submitting.set(false);
        this.notify.success(editingId ? 'Registro atualizado.' : 'Registro lançado.');
        this.cancelEdit();
        this.reloadLogs();
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.error.set(apiErrorMessage(error, 'Não foi possível salvar o registro.'));
      },
    });
  }

  remove(log: FieldLog): void {
    confirmDelete(this.dialog, `o registro de ${log.activity}`).subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.logsApi.delete(log.id).subscribe({
        next: () => {
          this.notify.success('Registro excluído.');
          if (this.editingId() === log.id) {
            this.cancelEdit();
          }
          this.reloadLogs();
        },
        error: (error: HttpErrorResponse) => {
          this.notify.error(apiErrorMessage(error, 'Não foi possível excluir o registro.'));
        },
      });
    });
  }

  private reloadLogs(): void {
    this.reload$.next();
  }

  private resetForm(): void {
    this.form.reset({
      cropId: this.filterForm.controls.cropId.value,
      date: todayIso(),
      activity: '',
      notes: '',
    });
  }
}
