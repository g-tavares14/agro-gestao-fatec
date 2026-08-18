import { HttpErrorResponse, HttpParams } from '@angular/common/http';
import { AbstractControl } from '@angular/forms';

export function queryParams(
  values: Record<string, string | number | boolean | null | undefined>,
): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== null && value !== undefined && value !== '') {
      params = params.set(key, String(value));
    }
  }
  return params;
}

export function asArray<T>(body: T[] | { content?: T[]; items?: T[] } | null | undefined): T[] {
  if (Array.isArray(body)) {
    return body;
  }
  if (body?.content && Array.isArray(body.content)) {
    return body.content;
  }
  if (body?.items && Array.isArray(body.items)) {
    return body.items;
  }
  return [];
}

export function toNumber(value: unknown): number | null {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  const parsed = typeof value === 'number' ? value : Number(String(value).replace(',', '.'));
  return Number.isFinite(parsed) ? parsed : null;
}

export function emptyToNull(value: string | null | undefined): string | null {
  const trimmed = value?.trim() ?? '';
  return trimmed.length > 0 ? trimmed : null;
}

export function todayIso(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

export function fieldError(control: AbstractControl | null | undefined): string {
  if (!control || !control.touched || control.valid) {
    return '';
  }
  if (control.hasError('required')) {
    return 'Campo obrigatório';
  }
  if (control.hasError('email')) {
    return 'Informe um e-mail válido';
  }
  if (control.hasError('min')) {
    return 'Informe um valor maior ou igual a zero';
  }
  if (control.hasError('maxlength')) {
    return 'Texto excede o limite permitido';
  }
  return 'Valor inválido';
}

export function apiErrorMessage(
  error: unknown,
  fallback: string,
  options?: { gemini?: boolean },
): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }
  if (error.status === 0) {
    return 'Não foi possível conectar ao servidor.';
  }
  const body = error.error as { message?: string } | string | null;
  if (typeof body === 'string' && body.trim()) {
    return body;
  }
  if (body && typeof body === 'object' && typeof body.message === 'string' && body.message.trim()) {
    return body.message;
  }
  return fallback;
}

export function filenameFromDisposition(
  header: string | null,
  fallback: string,
): string {
  if (!header) {
    return fallback;
  }
  const utfMatch = /filename\*=UTF-8''([^;]+)/i.exec(header);
  if (utfMatch?.[1]) {
    try {
      return decodeURIComponent(utfMatch[1]);
    } catch {
      return utfMatch[1];
    }
  }
  const basicMatch = /filename="?([^"]+)"?/i.exec(header);
  return basicMatch?.[1] ?? fallback;
}

export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) {
    return '—';
  }
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function roundMoney(value: number): number {
  return Math.round(value * 100) / 100;
}
