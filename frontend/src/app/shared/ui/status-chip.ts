import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { cropStatusLabel } from '../labels';
import { CropStatus } from '../models';

@Component({
  selector: 'app-status-chip',
  template: `<span class="chip" [attr.data-status]="status()">{{ label() }}</span>`,
  styles: `
    .chip {
      display: inline-flex;
      align-items: center;
      padding: 0.15rem 0.6rem;
      border-radius: 999px;
      font-size: 0.78rem;
      font-weight: 600;
      letter-spacing: 0.01em;
      background: #ece6d6;
      color: var(--agro-earth-800);
    }

    .chip[data-status='PLANEJADA'] {
      background: #efe6d4;
      color: var(--agro-earth-800);
    }

    .chip[data-status='PLANTADA'],
    .chip[data-status='EM_DESENVOLVIMENTO'] {
      background: #dceee1;
      color: var(--agro-green-800);
    }

    .chip[data-status='COLHIDA'] {
      background: #f7e6c3;
      color: var(--agro-amber-700);
    }

    .chip[data-status='ENCERRADA'] {
      background: #e8e8e8;
      color: #555;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusChip {
  readonly status = input.required<CropStatus | string>();

  label(): string {
    return cropStatusLabel(this.status());
  }
}
