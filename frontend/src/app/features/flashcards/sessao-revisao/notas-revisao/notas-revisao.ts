import { Component, computed, input, output } from '@angular/core';
import { NotaRevisao, PrevisaoNota } from '../../../../core/api/modelos-revisao';

interface BotaoNota {
  nota: NotaRevisao;
  label: string;
  atalho: string;
  intervalo: string;
  cor: string;
}

const META_NOTAS: Record<NotaRevisao, { label: string; atalho: string; cor: string }> = {
  ERREI: { label: 'Errei', atalho: '1', cor: 'var(--color-danger-300)' },
  DIFICIL: { label: 'Difícil', atalho: '2', cor: 'var(--color-neutral-300)' },
  BOM: { label: 'Bom', atalho: '3', cor: 'var(--color-accent)' },
  FACIL: { label: 'Fácil', atalho: '4', cor: 'var(--color-accent-300)' },
};

@Component({
  selector: 'app-notas-revisao',
  templateUrl: './notas-revisao.html',
  styleUrl: './notas-revisao.scss',
})
export class NotasRevisao {
  readonly previsoes = input.required<PrevisaoNota[]>();
  readonly classificar = output<NotaRevisao>();

  protected readonly botoes = computed<BotaoNota[]>(() =>
    this.previsoes().map((previsao) => ({
      nota: previsao.nota,
      intervalo: previsao.rotulo,
      ...META_NOTAS[previsao.nota],
    })),
  );
}
