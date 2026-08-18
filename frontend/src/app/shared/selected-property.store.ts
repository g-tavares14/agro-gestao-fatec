import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'agrogestao.selectedPropertyId';

@Injectable({ providedIn: 'root' })
export class SelectedPropertyStore {
  readonly id = signal<string | null>(this.read());

  set(id: string | null): void {
    this.id.set(id);
    try {
      if (id) {
        sessionStorage.setItem(STORAGE_KEY, id);
      } else {
        sessionStorage.removeItem(STORAGE_KEY);
      }
    } catch {
      /* sessionStorage may be unavailable */
    }
  }

  private read(): string | null {
    try {
      return sessionStorage.getItem(STORAGE_KEY);
    } catch {
      return null;
    }
  }
}
