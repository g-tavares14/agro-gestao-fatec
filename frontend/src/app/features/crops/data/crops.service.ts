import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { apiUrl } from '../../../core/api/api';
import { asArray } from '../../../shared/http-utils';
import {
  Crop,
  CropCyclePayload,
  CropPayload,
  PdfAnalyzeResponse,
  PdfConfirmPayload,
} from '../../../shared/models';
import { normalizeCrop } from './crop-mappers';

@Injectable({ providedIn: 'root' })
export class CropsService {
  private readonly http = inject(HttpClient);

  list(propertyId: string): Observable<Crop[]> {
    return this.http
      .get<Crop[] | { content?: Crop[] }>(apiUrl('/crops'), { params: { propertyId } })
      .pipe(map((body) => asArray(body).map((crop) => normalizeCrop(crop))));
  }

  get(id: string): Observable<Crop> {
    return this.http.get<Crop>(apiUrl(`/crops/${id}`)).pipe(map((crop) => normalizeCrop(crop)));
  }

  create(payload: CropPayload): Observable<Crop> {
    return this.http.post<Crop>(apiUrl('/crops'), payload).pipe(map((crop) => normalizeCrop(crop)));
  }

  update(id: string, payload: CropPayload): Observable<Crop> {
    return this.http
      .put<Crop>(apiUrl(`/crops/${id}`), payload)
      .pipe(map((crop) => normalizeCrop(crop)));
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(apiUrl(`/crops/${id}`));
  }

  updateCycle(id: string, payload: CropCyclePayload): Observable<Crop> {
    return this.http
      .patch<Crop>(apiUrl(`/crops/${id}/cycle`), payload)
      .pipe(map((crop) => normalizeCrop(crop)));
  }

  analyzePdf(file: File, propertyId: string): Observable<PdfAnalyzeResponse> {
    const body = new FormData();
    body.append('file', file);
    body.append('propertyId', propertyId);
    return this.http.post<PdfAnalyzeResponse>(apiUrl('/crops/pdf/analyze'), body);
  }

  confirmPdf(payload: PdfConfirmPayload): Observable<Crop> {
    return this.http
      .post<Crop>(apiUrl('/crops/pdf/confirm'), payload)
      .pipe(map((crop) => normalizeCrop(crop)));
  }
}
