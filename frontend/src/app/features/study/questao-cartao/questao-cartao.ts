import { Component, computed, input, output } from '@angular/core';
import { ModoSessao, QuestaoDaSerie } from '../../../core/api/modelos-estudo';

@Component({
  selector: 'app-questao-cartao',
  standalone: true,
  templateUrl: './questao-cartao.html',
  styleUrl: './questao-cartao.scss',
})
export class QuestaoCartao {
  readonly questao = input.required<QuestaoDaSerie>();
  readonly marcada = input<string | null>(null);
  readonly modo = input<ModoSessao>('FOCO');
  /** Trava as alternativas depois que a questão já foi respondida. */
  readonly bloqueada = input(false);
  readonly criadosNaSessao = input<number | null>(null);
  readonly emContexto = computed(() => this.modo() === 'CONTEXTO');
  readonly selecionar = output<string>();
  readonly marcar = output<'favorita' | 'erroRecorrente'>();
  readonly novoFlashcard = output<void>();
  readonly adicionarCaderno = output<void>();
  alternativa(letra: string): void {
    if (this.bloqueada()) return;
    this.selecionar.emit(letra);
  }
}
