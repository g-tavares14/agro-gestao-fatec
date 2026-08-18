import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { catchError, EMPTY, map, merge, Subject, switchMap } from 'rxjs';
import {
  apiErrorMessage,
  emptyToNull,
  filenameFromDisposition,
  formatBytes,
} from '../../shared/http-utils';
import { Crop, DocumentFile, Property } from '../../shared/models';
import { NotifyService } from '../../shared/notify.service';
import { cropLabel, PropertyContext } from '../../shared/property-context';
import { confirmDelete } from '../../shared/ui/confirm-dialog';
import { EmptyState } from '../../shared/ui/empty-state';
import { PropertiesService } from '../properties/data/properties.service';
import { DocumentsService } from './data/documents.service';

@Component({
  selector: 'app-documents-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    EmptyState,
  ],
  templateUrl: './documents-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentsPage {
  private readonly propertiesApi = inject(PropertiesService);
  private readonly documentsApi = inject(DocumentsService);
  private readonly context = inject(PropertyContext);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly reload$ = new Subject<void>();

  readonly formatBytes = formatBytes;
  readonly properties = signal<Property[]>([]);
  readonly crops = signal<Crop[]>([]);
  readonly documents = signal<DocumentFile[]>([]);
  readonly loadingProperties = signal(true);
  readonly loading = signal(true);
  readonly uploading = signal(false);
  readonly error = signal<string | null>(null);
  readonly selectedFile = signal<File | null>(null);

  readonly filterForm = this.fb.nonNullable.group({
    propertyId: [this.context.id() ?? '', Validators.required],
  });

  readonly uploadForm = this.fb.nonNullable.group({
    cropId: [''],
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
        this.uploadForm.controls.cropId.setValue('');
      });

    this.context
      .cropsFor(
        this.filterForm.controls.propertyId.valueChanges.pipe(map((id) => id || null)),
        (error) => this.error.set(apiErrorMessage(error, 'Não foi possível carregar as culturas.')),
      )
      .pipe(takeUntilDestroyed())
      .subscribe((crops) => this.crops.set(crops));

    merge(this.filterForm.controls.propertyId.valueChanges, this.reload$)
      .pipe(
        switchMap(() => {
          const propertyId = this.filterForm.controls.propertyId.value;
          if (!propertyId) {
            this.documents.set([]);
            this.loading.set(false);
            return EMPTY;
          }
          this.loading.set(true);
          this.error.set(null);
          return this.documentsApi.list(propertyId).pipe(
            catchError((error: HttpErrorResponse) => {
              this.loading.set(false);
              this.error.set(apiErrorMessage(error, 'Não foi possível carregar os documentos.'));
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((items) => {
        this.documents.set(items);
        this.loading.set(false);
      });
  }

  cropName(doc: DocumentFile): string {
    return cropLabel(doc, this.crops());
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile.set(input.files?.[0] ?? null);
  }

  upload(): void {
    const propertyId = this.filterForm.controls.propertyId.value;
    const file = this.selectedFile();
    if (!propertyId) {
      this.error.set('Selecione uma propriedade para enviar o documento.');
      return;
    }
    if (!file) {
      this.error.set('Selecione um arquivo para enviar.');
      return;
    }

    this.uploading.set(true);
    this.error.set(null);
    this.documentsApi
      .upload(file, propertyId, emptyToNull(this.uploadForm.controls.cropId.value))
      .subscribe({
        next: () => {
          this.uploading.set(false);
          this.selectedFile.set(null);
          this.notify.success('Documento enviado.');
          this.reload();
        },
        error: (error: HttpErrorResponse) => {
          this.uploading.set(false);
          this.error.set(apiErrorMessage(error, 'Não foi possível enviar o documento.'));
        },
      });
  }

  download(doc: DocumentFile): void {
    this.documentsApi.download(doc.id).subscribe({
      next: (response) => {
        const blob = response.body;
        if (!blob) {
          this.notify.error('Arquivo vazio.');
          return;
        }
        const name = filenameFromDisposition(
          response.headers.get('content-disposition'),
          doc.originalName,
        );
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = name;
        anchor.click();
        URL.revokeObjectURL(url);
      },
      error: (error: HttpErrorResponse) => {
        this.notify.error(apiErrorMessage(error, 'Não foi possível baixar o documento.'));
      },
    });
  }

  remove(doc: DocumentFile): void {
    confirmDelete(this.dialog, `o documento “${doc.originalName}”`).subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.documentsApi.delete(doc.id).subscribe({
        next: () => {
          this.notify.success('Documento excluído.');
          this.reload();
        },
        error: (error: HttpErrorResponse) => {
          this.notify.error(apiErrorMessage(error, 'Não foi possível excluir o documento.'));
        },
      });
    });
  }

  private reload(): void {
    this.reload$.next();
  }
}
