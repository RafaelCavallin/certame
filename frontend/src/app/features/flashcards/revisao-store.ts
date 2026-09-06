import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { RevisaoApi } from '../../core/api/revisao-api';
import {
  CardRevisao,
  EscopoFila,
  NotaRevisao,
  RespostaRevisao,
  ResumoSessao,
} from '../../core/api/modelos-revisao';
import { proximaFila } from './reenfileiramento-revisao';

export interface EscopoRevisao {
  escopo: EscopoFila;
  deckId: string | null;
  disciplinaId: string | null;
}

@Injectable({ providedIn: 'root' })
export class RevisaoStore {
  private readonly api = inject(RevisaoApi);
  private readonly sessaoId = signal<string | null>(null);
  private readonly totalInicial = signal(0);
  private readonly fila = signal<CardRevisao[]>([]);
  private readonly proximaRevisaoPorCard = new Map<string, string>();
  private cardIniciadoEm = 0;
  readonly carregando = signal(false);
  readonly escopoRotulo = signal('');
  readonly revelado = signal(false);
  readonly resumo = signal<ResumoSessao | null>(null);
  readonly classificacoes = signal(0);
  readonly reenfileirado = signal<string | null>(null);
  private readonly avaliadoEm = signal(0);

  private readonly disponiveis = computed<CardRevisao[]>(() => {
    const agora = this.avaliadoEm();
    return this.fila().filter((card) => this.vencidoAte(card, agora));
  });
  readonly cardAtual = computed<CardRevisao | null>(() => this.disponiveis()[0] ?? null);
  readonly restantes = computed(() => this.disponiveis().length);
  readonly adiados = computed(() => this.fila().length - this.disponiveis().length);
  readonly total = this.totalInicial.asReadonly();
  readonly progressoPct = computed(() => {
    const total = this.totalInicial();
    if (!total) return 0;
    const consumidos = total - this.fila().length;
    return Math.max(0, Math.min(100, Math.round((100 * consumidos) / total)));
  });
  readonly filaVazia = computed(
    () =>
      !this.carregando() && this.sessaoId() !== null && this.totalInicial() === 0 && !this.resumo(),
  );

  async abrir(escopo: EscopoRevisao): Promise<void> {
    this.carregando.set(true);
    try {
      const resposta = await firstValueFrom(
        this.api.abrir({
          escopo: escopo.escopo,
          deckId: escopo.deckId,
          disciplinaId: escopo.disciplinaId,
        }),
      );
      this.sessaoId.set(resposta.id);
      this.escopoRotulo.set(resposta.escopoRotulo);
      this.totalInicial.set(resposta.total);
      this.fila.set(resposta.fila);
      this.resumo.set(null);
      this.revelado.set(false);
      this.proximaRevisaoPorCard.clear();
      this.classificacoes.set(0);
      this.reenfileirado.set(null);
      this.avaliadoEm.set(Date.now());
      this.cardIniciadoEm = Date.now();
    } finally {
      this.carregando.set(false);
    }
  }

  revelar(): void {
    this.revelado.set(true);
    this.reenfileirado.set(null);
  }

  async classificar(nota: NotaRevisao): Promise<void> {
    const sessaoId = this.sessaoId();
    const card = this.cardAtual();
    if (!sessaoId || !card) return;
    const tempoMs = Date.now() - this.cardIniciadoEm;
    const resposta = await firstValueFrom(
      this.api.registrar(sessaoId, { flashcardId: card.id, nota, tempoMs }),
    );
    this.classificacoes.update((valor) => valor + 1);
    this.avancar(card.id, resposta);
    if (this.restantes() === 0) await this.finalizar();
  }

  async finalizar(): Promise<void> {
    const sessaoId = this.sessaoId();
    if (!sessaoId) return;
    this.resumo.set(await firstValueFrom(this.api.finalizar(sessaoId)));
  }

  private avancar(classificadoId: string, resposta: RespostaRevisao): void {
    const restante = this.fila().filter((card) => card.id !== classificadoId);
    const fila = proximaFila(restante, resposta, this.proximaRevisaoPorCard);
    const agora = Date.now();
    this.fila.set(fila);
    this.avaliadoEm.set(agora);
    this.reenfileirado.set(resposta.reenfileirar ? resposta.intervaloRotulo : null);
    this.revelado.set(false);
    this.cardIniciadoEm = agora;
  }

  private vencidoAte(card: CardRevisao, agora: number): boolean {
    const proxima = this.proximaRevisaoPorCard.get(card.id);
    if (proxima === undefined) return true;
    const instante = Date.parse(proxima);
    return Number.isNaN(instante) || instante <= agora;
  }
}
