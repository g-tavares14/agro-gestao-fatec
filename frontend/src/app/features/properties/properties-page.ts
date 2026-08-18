import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { apiErrorMessage } from '../../shared/http-utils';
import { locationLabel } from '../../shared/labels';
import { Property } from '../../shared/models';
import { NotifyService } from '../../shared/notify.service';
import { SelectedPropertyStore } from '../../shared/selected-property.store';
import { confirmDelete } from '../../shared/ui/confirm-dialog';
import { EmptyState } from '../../shared/ui/empty-state';
import { PropertiesService } from './data/properties.service';
import { PropertyDialog } from './property-dialog';

@Component({
  selector: 'app-properties-page',
  imports: [
    DecimalPipe,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    EmptyState,
  ],
  templateUrl: './properties-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PropertiesPage {
  private readonly propertiesApi = inject(PropertiesService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly selectedProperty = inject(SelectedPropertyStore);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly properties = signal<Property[]>([]);

  constructor() {
    this.reload();
  }

  locationOf(property: Property): string {
    return locationLabel(property.city, property.state);
  }

  openCreate(): void {
    this.openDialog(null);
  }

  openEdit(property: Property): void {
    this.openDialog(property);
  }

  remove(property: Property): void {
    confirmDelete(this.dialog, `a propriedade “${property.name}”`).subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.propertiesApi.delete(property.id).subscribe({
        next: () => {
          if (this.selectedProperty.id() === property.id) {
            this.selectedProperty.set(null);
          }
          this.notify.success('Propriedade excluída.');
          this.reload();
        },
        error: (error: HttpErrorResponse) => {
          this.notify.error(apiErrorMessage(error, 'Não foi possível excluir a propriedade.'));
        },
      });
    });
  }

  private openDialog(property: Property | null): void {
    this.dialog
      .open(PropertyDialog, { width: '36rem', data: property })
      .afterClosed()
      .subscribe((payload) => {
        if (!payload) {
          return;
        }
        const request = property
          ? this.propertiesApi.update(property.id, payload)
          : this.propertiesApi.create(payload);
        request.subscribe({
          next: (saved) => {
            this.notify.success(property ? 'Propriedade atualizada.' : 'Propriedade cadastrada.');
            if (!property) {
              this.selectedProperty.set(saved.id);
            }
            this.reload();
          },
          error: (error: HttpErrorResponse) => {
            this.notify.error(apiErrorMessage(error, 'Não foi possível salvar a propriedade.'));
          },
        });
      });
  }

  private reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.propertiesApi.list().subscribe({
      next: (items) => {
        this.properties.set(items);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(error, 'Não foi possível carregar as propriedades.'));
      },
    });
  }
}
