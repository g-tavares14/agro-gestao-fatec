import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-empty-state',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    <div class="empty-state">
      <mat-icon aria-hidden="true">{{ icon() }}</mat-icon>
      <h3>{{ title() }}</h3>
      @if (message()) {
        <p>{{ message() }}</p>
      }
      @if (ctaLabel() && ctaLink()) {
        <a mat-flat-button color="primary" [routerLink]="ctaLink()">{{ ctaLabel() }}</a>
      }
      <ng-content />
    </div>
  `,
  styles: `
    :host {
      display: block;
      width: 100%;
    }

    .empty-state {
      display: grid;
      justify-items: center;
      text-align: center;
      gap: 0.45rem;
      padding: 1.4rem 1.2rem;
      background: #fff;
      border: 1px dashed var(--agro-line);
      border-radius: 16px;
    }

    :host-context(.panel) .empty-state {
      border: 0;
      background: transparent;
      padding: 0.75rem 0 0.25rem;
    }

    mat-icon {
      color: var(--agro-green-700);
      width: 32px;
      height: 32px;
      font-size: 32px;
      margin-bottom: 0.25rem;
    }

    h3 {
      margin: 0;
      font-family: var(--agro-font-brand);
      font-size: 1.25rem;
    }

    p {
      margin: 0 auto 0.55rem;
      color: var(--agro-ink-soft);
      max-width: 40rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyState {
  readonly icon = input('inbox');
  readonly title = input.required<string>();
  readonly message = input('');
  readonly ctaLabel = input<string | undefined>(undefined);
  readonly ctaLink = input<string | undefined>(undefined);
}
