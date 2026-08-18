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
import { passwordMatchValidator } from '../../../shared/validators/password-match';

@Component({
  selector: 'app-register',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    BrandMark,
  ],
  templateUrl: './register.html',
  styleUrl: '../auth-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Register {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    afterNextRender(() => {
      const ctx = animateAuthSplit(this.host.nativeElement);
      this.destroyRef.onDestroy(() => ctx.revert());
    });
  }

  readonly form = this.fb.nonNullable.group(
    {
      nome: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      senha: ['', [Validators.required, Validators.minLength(8)]],
      confirmarSenha: ['', Validators.required],
    },
    { validators: passwordMatchValidator() },
  );

  nomeError(): string {
    return this.form.controls.nome.hasError('required') ? 'Campo obrigatório' : '';
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
    const control = this.form.controls.senha;
    if (control.hasError('required')) {
      return 'Campo obrigatório';
    }
    if (control.hasError('minlength')) {
      return 'A senha deve ter no mínimo 8 caracteres';
    }
    return '';
  }

  confirmError(): string {
    if (this.form.controls.confirmarSenha.hasError('required')) {
      return 'Campo obrigatório';
    }
    if (this.form.hasError('passwordMismatch') && this.form.controls.confirmarSenha.touched) {
      return 'As senhas não coincidem';
    }
    return '';
  }

  submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const { nome, email, senha } = this.form.getRawValue();
    this.auth.register({ name: nome, email, password: senha }).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigateByUrl(this.auth.isAuthenticated() ? '/app' : '/login');
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.errorMessage.set(this.messageFor(error));
      },
    });
  }

  private messageFor(error: HttpErrorResponse): string {
    if (error.status === 409) {
      return 'Este e-mail já está cadastrado.';
    }
    return 'Não foi possível concluir o cadastro. Tente novamente.';
  }
}
