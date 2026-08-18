import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  inject,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { animateLanding } from '../../shared/motion/motion';
import { BrandMark } from '../../shared/ui/brand-mark';

interface ModuleCard {
  icon: string;
  title: string;
  description: string;
}

interface Step {
  title: string;
  description: string;
}

@Component({
  selector: 'app-landing',
  imports: [RouterLink, MatButtonModule, BrandMark],
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Landing {
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);

  readonly year = new Date().getFullYear();

  readonly modules: ModuleCard[] = [
    {
      icon: 'grass',
      title: 'Culturas',
      description: 'Ciclo, variedade e produtividade por talhão — com o histórico da lavoura à mão.',
    },
    {
      icon: 'picture_as_pdf',
      title: 'Extração de PDF',
      description: 'A IA lê a ficha EMATER e separa ações mecânicas, manuais, insumos e outros.',
    },
    {
      icon: 'calculate',
      title: 'Custos',
      description: 'Lançamentos por cultura, com a mesma classificação da planilha de campo.',
    },
    {
      icon: 'menu_book',
      title: 'Diário de campo',
      description: 'O dia a dia da propriedade: tratos, ocorrências e observações do talhão.',
    },
    {
      icon: 'payments',
      title: 'Financeiro',
      description: 'Receitas, despesas e o demonstrativo simples — por cultura ou consolidado.',
    },
    {
      icon: 'folder',
      title: 'Documentos',
      description: 'Contratos, notas e laudos da propriedade, guardados com acesso autenticado.',
    },
  ];

  readonly steps: Step[] = [
    {
      title: 'Cadastre a propriedade',
      description: 'Nome, município e área. O painel nasce da terra que você gerencia.',
    },
    {
      title: 'Importe a ficha ou lance à mão',
      description: 'Envie o PDF, revise os quatro grupos e confirme. Nada entra sem o seu olho.',
    },
    {
      title: 'Acompanhe o ciclo e o resultado',
      description: 'Status da cultura, diário, custos e DRE — o pulso da operação num só lugar.',
    },
  ];

  constructor() {
    afterNextRender(() => {
      const ctx = animateLanding(this.host.nativeElement);
      this.destroyRef.onDestroy(() => ctx.revert());
    });
  }
}
