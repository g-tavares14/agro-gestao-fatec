import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-brand-mark',
  imports: [RouterLink],
  template: `
    <a
      [routerLink]="to()"
      class="brand-mark"
      [class.inverse]="variant() === 'inverse'"
      [class.compact]="compact()"
      [attr.aria-label]="compact() ? 'Agro-Gestão' : null"
    >
      <img src="logo.svg" width="36" height="36" alt="" />
      @if (!compact()) {
        <span class="text">
          Agro-Gestão
          @if (tagline(); as line) {
            <small>{{ line }}</small>
          }
        </span>
      }
    </a>
  `,
  styles: `
    :host {
      display: inline-flex;
    }

    :host-context(.sidebar) {
      display: block;
    }

    :host-context(.sidebar) .brand-mark {
      display: flex;
      width: 100%;
      padding: 1.4rem 1.15rem 1.15rem;
    }

    .brand-mark {
      display: inline-flex;
      align-items: center;
      gap: 0.7rem;
      color: var(--agro-green-900);
      text-decoration: none;
      font-family: var(--agro-font-brand), Syne, 'Segoe UI', sans-serif;
      font-size: 1.3rem;
      font-weight: 600;
      letter-spacing: -0.03em;
    }

    img {
      display: block;
      width: 36px;
      height: 36px;
      object-fit: contain;
      flex-shrink: 0;
    }

    .text {
      display: grid;
      line-height: 1.1;
    }

    .brand-mark.inverse {
      color: var(--agro-cream);
      font-size: 1.25rem;
      font-weight: 700;
    }

    .brand-mark.compact img {
      width: 32px;
      height: 32px;
    }

    small {
      margin-top: 0.2rem;
      font-family: var(--agro-font-plain);
      font-size: 0.68rem;
      font-weight: 700;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      color: var(--agro-amber-500);
    }

    @media (max-width: 640px) {
      .brand-mark:not(.inverse) {
        font-size: 1.1rem;
        gap: 0.5rem;

        img {
          width: 30px;
          height: 30px;
        }
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandMark {
  readonly to = input('/');
  readonly variant = input<'default' | 'inverse'>('default');
  readonly tagline = input<string | undefined>(undefined);
  readonly compact = input(false);
}
