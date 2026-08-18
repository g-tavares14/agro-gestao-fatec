import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { animateAuthSplit } from '../../../shared/motion/motion';
import { BrandMark } from '../../../shared/ui/brand-mark';

@Component({
  selector: 'app-callback',
  imports: [RouterLink, MatButtonModule, MatProgressSpinnerModule, BrandMark],
  templateUrl: './callback.html',
  styleUrl: '../auth-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Callback {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);

  readonly error = signal(false);
  readonly errorDetail = signal('Não foi possível concluir o login com o Google. Tente novamente.');

  constructor() {
    afterNextRender(() => {
      const ctx = animateAuthSplit(this.host.nativeElement);
      this.destroyRef.onDestroy(() => ctx.revert());
    });

    const params = this.route.snapshot.queryParamMap;
    const error = params.get('error');
    if (error) {
      if (error === 'email_not_verified') {
        this.errorDetail.set('O e-mail da conta Google não está verificado.');
      } else if (error === 'oauth_failed') {
        this.errorDetail.set('Não foi possível concluir o login com o Google. Tente novamente.');
      }
      this.error.set(true);
      return;
    }

    const code = params.get('code');
    if (code) {
      this.auth.exchangeOAuth(code).subscribe({
        next: () => {
          void this.router.navigateByUrl('/app', { replaceUrl: true });
        },
        error: () => {
          this.errorDetail.set('Não foi possível concluir o login com o Google. Tente novamente.');
          this.error.set(true);
        },
      });
      return;
    }

    this.error.set(true);
  }
}
