import { Component, computed, input, output } from '@angular/core';
import { ResumoSessao } from '../../../../core/api/modelos-revisao';

interface CartaoResumo {
  label: string;
  valor: number;
  cor: string;
}

@Component({
  selector: 'app-resumo-revisao',
  templateUrl: './resumo-revisao.html',
  styleUrl: './resumo-revisao.scss',
})
export class ResumoRevisao {
  readonly resumo = input.required<ResumoSessao>();
  readonly reiniciar = output<void>();
  readonly voltar = output<void>();

  protected readonly cartoes = computed<CartaoResumo[]>(() => {
    const distribuicao = this.resumo().distribuicao;
    return [
      { label: 'Revisados', valor: this.resumo().revisados, cor: 'var(--color-text)' },
      {
        label: 'Bom + Fácil',
        valor: distribuicao.bom + distribuicao.facil,
        cor: 'var(--color-accent-300)',
      },
      { label: 'Difícil', valor: distribuicao.dificil, cor: 'var(--color-text)' },
      { label: 'Errei', valor: distribuicao.errei, cor: 'var(--color-danger-300)' },
    ];
  });

  protected readonly subtitulo = computed(
    () =>
      `${this.resumo().revisados} cards revisados em ${this.resumo().tempoRotulo} · somado ao seu tempo de estudo de hoje`,
  );
}
