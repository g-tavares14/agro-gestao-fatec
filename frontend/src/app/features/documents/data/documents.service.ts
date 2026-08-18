import { HttpClient, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { apiUrl } from '../../../core/api/api';
import { asArray, queryParams } from '../../../shared/http-utils';
import { DocumentFile } from '../../../shared/models';

@Injectable({ providedIn: 'root' })
export class DocumentsService {
  private readonly http = inject(HttpClient);

  list(propertyId: string): Observable<DocumentFile[]> {
    return this.http
      .get<DocumentFile[] | { content?: DocumentFile[] }>(apiUrl('/documents'), {
        params: queryParams({ propertyId }),
      })
      .pipe(map((body) => asArray(body)));
  }

  upload(file: File, propertyId: string, cropId?: string | null): Observable<DocumentFile> {
    const body = new FormData();
    body.append('file', file);
    body.append('propertyId', propertyId);
    if (cropId) {
      body.append('cropId', cropId);
    }
    return this.http.post<DocumentFile>(apiUrl('/documents'), body);
  }

  download(id: string): Observable<HttpResponse<Blob>> {
    return this.http.get(apiUrl(`/documents/${id}/download`), {
      responseType: 'blob',
      observe: 'response',
    });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(apiUrl(`/documents/${id}`));
  }
}
