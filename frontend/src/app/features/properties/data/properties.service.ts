import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { apiUrl } from '../../../core/api/api';
import { asArray } from '../../../shared/http-utils';
import { Property, PropertyPayload } from '../../../shared/models';

@Injectable({ providedIn: 'root' })
export class PropertiesService {
  private readonly http = inject(HttpClient);

  list(): Observable<Property[]> {
    return this.http
      .get<Property[] | { content?: Property[] }>(apiUrl('/properties'))
      .pipe(map((body) => asArray(body)));
  }

  get(id: string): Observable<Property> {
    return this.http.get<Property>(apiUrl(`/properties/${id}`));
  }

  create(payload: PropertyPayload): Observable<Property> {
    return this.http.post<Property>(apiUrl('/properties'), payload);
  }

  update(id: string, payload: PropertyPayload): Observable<Property> {
    return this.http.put<Property>(apiUrl(`/properties/${id}`), payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(apiUrl(`/properties/${id}`));
  }
}
