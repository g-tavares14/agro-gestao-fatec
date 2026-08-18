import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { apiUrl } from '../../../core/api/api';
import { asArray, queryParams } from '../../../shared/http-utils';
import { FieldLog, FieldLogPayload } from '../../../shared/models';

@Injectable({ providedIn: 'root' })
export class FieldLogsService {
  private readonly http = inject(HttpClient);

  list(filters: { propertyId?: string; cropId?: string }): Observable<FieldLog[]> {
    return this.http
      .get<FieldLog[] | { content?: FieldLog[] }>(apiUrl('/field-logs'), {
        params: queryParams(filters),
      })
      .pipe(map((body) => asArray(body)));
  }

  create(payload: FieldLogPayload): Observable<FieldLog> {
    return this.http.post<FieldLog>(apiUrl('/field-logs'), payload);
  }

  update(id: string, payload: FieldLogPayload): Observable<FieldLog> {
    return this.http.put<FieldLog>(apiUrl(`/field-logs/${id}`), payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(apiUrl(`/field-logs/${id}`));
  }
}
