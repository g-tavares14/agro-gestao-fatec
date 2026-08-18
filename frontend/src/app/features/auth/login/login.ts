import { HttpErrorResponse } from '@angular/common/http';
import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { animateAuthSplit } from '../../../shared/motion/motion';
import { BrandMark } from '../../../shared/ui/brand-mark';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    BrandMark,
  ],
  templateUrl: './login.html',
  styleUrl: '../auth-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly googleEnabled = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.required],
  });

  constructor() {
    this.auth.googleEnabled().subscribe((enabled) => this.googleEnabled.set(enabled));
    afterNextRender(() => {
      const ctx = animateAuthSplit(this.host.nativeElement);
      this.destroyRef.onDestroy(() => ctx.revert());
    });
  }

  emailError(): string {
    const control = this.form.controls.email;
    if (control.hasError('required')) {
      return 'Campo obrigatório';
    }
    if (control.hasError('email')) {
      return 'Informe um e-mail válido';
    }
    return '';
  }

  senhaError(): string {
    return this.form.controls.senha.hasError('required') ? 'Campo obrigatório' : '';
  }

  submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const { email, senha } = this.form.getRawValue();
    this.auth.login({ email, password: senha }).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigateByUrl('/app');
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.errorMessage.set(this.messageFor(error));
      },
    });
  }

  enterWithGoogle(): void {
    window.location.assign('/oauth2/authorization/google');
  }

  private messageFor(error: HttpErrorResponse): string {
    if (error.status === 401 || error.status === 403) {
      return 'E-mail ou senha inválidos.';
    }
    return 'Não foi possível entrar. Tente novamente.';
  }
}
