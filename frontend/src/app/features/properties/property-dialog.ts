import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { emptyToNull, fieldError, toNumber } from '../../shared/http-utils';
import { BRAZILIAN_STATES, Property, PropertyPayload } from '../../shared/models';

@Component({
  selector: 'app-property-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data ? 'Editar propriedade' : 'Nova propriedade' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <mat-dialog-content class="dialog-body">
        <mat-form-field appearance="outline">
          <mat-label>Nome</mat-label>
          <input matInput formControlName="name" />
          @if (fieldError(form.controls.name); as message) {
            <mat-error>{{ message }}</mat-error>
          }
        </mat-form-field>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Município</mat-label>
            <input matInput formControlName="city" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="uf-field">
            <mat-label>UF</mat-label>
            <mat-select formControlName="state" panelClass="property-uf-panel">
              <mat-select-trigger>
                @if (selectedState(); as state) {
                  <span class="uf-trigger">{{ state.uf }} — {{ state.name }}</span>
                }
              </mat-select-trigger>
              <mat-option [value]="''">—</mat-option>
              @for (state of states; track state.uf) {
                <mat-option [value]="state.uf">{{ state.uf }} — {{ state.name }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline">
          <mat-label>Área total (ha)</mat-label>
          <input matInput type="number" min="0" step="0.01" formControlName="totalAreaHa" />
          @if (fieldError(form.controls.totalAreaHa); as message) {
            <mat-error>{{ message }}</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Descrição</mat-label>
          <textarea matInput rows="3" formControlName="description"></textarea>
        </mat-form-field>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" [mat-dialog-close]="null">Cancelar</button>
        <button mat-flat-button color="primary" type="submit">Salvar</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .dialog-body {
      display: grid;
      gap: 0.15rem;
      min-width: min(34rem, calc(100vw - 2rem));
      padding-top: 0.35rem;
    }

    mat-form-field {
      width: 100%;
    }

    .row {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(17.5rem, 0.9fr);
      gap: 0.75rem;
      align-items: start;
    }

    .uf-trigger {
      display: block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    @media (max-width: 560px) {
      .dialog-body {
        min-width: 0;
      }

      .row {
        grid-template-columns: 1fr;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PropertyDialog {
  private readonly fb = inject(FormBuilder);
  private readonly ref = inject(MatDialogRef<PropertyDialog, PropertyPayload | null>);
  readonly data = inject<Property | null>(MAT_DIALOG_DATA);

  readonly states = BRAZILIAN_STATES;
  readonly fieldError = fieldError;

  selectedState(): { uf: string; name: string } | null {
    const uf = this.form.controls.state.value;
    return this.states.find((state) => state.uf === uf) ?? null;
  }

  readonly form = this.fb.nonNullable.group({
    name: [this.data?.name ?? '', Validators.required],
    city: [this.data?.city ?? ''],
    state: [this.data?.state ?? ''],
    totalAreaHa: [this.data?.totalAreaHa ?? (null as number | null), Validators.min(0)],
    description: [this.data?.description ?? ''],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.ref.close({
      name: value.name.trim(),
      city: emptyToNull(value.city),
      state: emptyToNull(value.state),
      totalAreaHa: toNumber(value.totalAreaHa),
      description: emptyToNull(value.description),
    });
  }
}
