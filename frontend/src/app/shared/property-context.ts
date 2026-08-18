import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, of, switchMap } from 'rxjs';
import { CropsService } from '../features/crops/data/crops.service';
import { Crop, Property } from './models';
import { SelectedPropertyStore } from './selected-property.store';

@Injectable({ providedIn: 'root' })
export class PropertyContext {
  private readonly store = inject(SelectedPropertyStore);
  private readonly cropsApi = inject(CropsService);

  readonly id = this.store.id;

  select(id: string | null): void {
    this.store.set(id);
  }

  resolve(properties: Property[]): string | null {
    const current = this.store.id();
    if (current && properties.some((property) => property.id === current)) {
      return current;
    }
    if (properties.length === 1) {
      const id = properties[0].id;
      this.store.set(id);
      return id;
    }
    this.store.set(null);
    return null;
  }

  cropsFor(
    propertyId: Observable<string | null>,
    onError?: (error: HttpErrorResponse) => void,
  ): Observable<Crop[]> {
    return propertyId.pipe(
      switchMap((id) => {
        if (!id) {
          return of([] as Crop[]);
        }
        return this.cropsApi.list(id).pipe(
          catchError((error: HttpErrorResponse) => {
            onError?.(error);
            return of([] as Crop[]);
          }),
        );
      }),
    );
  }
}

export function cropLabel(
  entity: { cropName?: string | null; cropId?: string | null },
  crops: readonly Pick<Crop, 'id' | 'name'>[],
): string {
  if (entity.cropName) {
    return entity.cropName;
  }
  return crops.find((crop) => crop.id === entity.cropId)?.name ?? '—';
}
