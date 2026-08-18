import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { apiUrl } from '../../../core/api/api';
import { asArray } from '../../../shared/http-utils';
import { ProductionCost, ProductionCostPayload } from '../../../shared/models';

@Injectable({ providedIn: 'root' })
export class CostsService {
  private readonly http = inject(HttpClient);

  list(cropId: string): Observable<ProductionCost[]> {
    return this.http
      .get<ProductionCost[] | { content?: ProductionCost[] }>(apiUrl(`/crops/${cropId}/costs`))
      .pipe(map((body) => asArray(body)));
  }

  create(cropId: string, payload: ProductionCostPayload): Observable<ProductionCost> {
    return this.http.post<ProductionCost>(apiUrl(`/crops/${cropId}/costs`), payload);
  }

  update(id: string, payload: ProductionCostPayload): Observable<ProductionCost> {
    return this.http.put<ProductionCost>(apiUrl(`/costs/${id}`), payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(apiUrl(`/costs/${id}`));
  }
}
