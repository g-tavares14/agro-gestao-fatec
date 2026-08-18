import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { apiUrl } from '../../../core/api/api';
import { asArray, queryParams } from '../../../shared/http-utils';
import { FinanceEntry, FinancePayload, FinanceStatement } from '../../../shared/models';

@Injectable({ providedIn: 'root' })
export class FinanceService {
  private readonly http = inject(HttpClient);

  list(filters: { propertyId?: string; cropId?: string }): Observable<FinanceEntry[]> {
    return this.http
      .get<FinanceEntry[] | { content?: FinanceEntry[] }>(apiUrl('/finance'), {
        params: queryParams(filters),
      })
      .pipe(map((body) => asArray(body)));
  }

  statement(filters: { propertyId?: string; cropId?: string }): Observable<FinanceStatement> {
    return this.http.get<FinanceStatement>(apiUrl('/finance/statement'), {
      params: queryParams(filters),
    });
  }

  create(payload: FinancePayload): Observable<FinanceEntry> {
    return this.http.post<FinanceEntry>(apiUrl('/finance'), payload);
  }

  update(id: string, payload: FinancePayload): Observable<FinanceEntry> {
    return this.http.put<FinanceEntry>(apiUrl(`/finance/${id}`), payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(apiUrl(`/finance/${id}`));
  }
}
