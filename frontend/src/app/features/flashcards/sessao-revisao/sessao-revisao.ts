import { Component, computed, inject, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { EscopoFila, NotaRevisao } from '../../../core/api/modelos-revisao';
import { AtalhosRevisao } from '../atalhos-revisao';
import { CronometroSessao } from '../cronometro-sessao';
import { RevisaoStore } from '../revisao-store';
import { CardRevisao } from './card-revisao/card-revisao';
import { FilaVazia } from './fila-vazia/fila-vazia';
import { NotasRevisao } from './notas-revisao/notas-revisao';
import { ResumoRevisao } from './resumo-revisao/resumo-revisao';

function formatarTempo(segundos: number): string {
  const minutos = Math.floor(segundos / 60);
  const resto = segundos % 60;
  return `${String(minutos).padStart(2, '0')}:${String(resto).padStart(2, '0')}`;
}

@Component({
  selector: 'app-sessao-revisao',
  imports: [AtalhosRevisao, CardRevisao, NotasRevisao, ResumoRevisao, FilaVazia],
  templateUrl: './sessao-revisao.html',
  styleUrl: './sessao-revisao.scss',
})
export class SessaoRevisaoPage implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly store = inject(RevisaoStore);
  readonly cronometro = inject(CronometroSessao);
  protected readonly tempoSessao = computed(() => formatarTempo(this.cronometro.segundos()));

  constructor() {
    void this.abrirDaRota();
  }

  ngOnDestroy(): void {
    this.cronometro.parar();
  }

  protected async classificar(nota: NotaRevisao): Promise<void> {
    await this.store.classificar(nota);
  }

  protected async reiniciar(): Promise<void> {
    await this.abrirDaRota();
  }

  protected voltar(): void {
    this.cronometro.parar();
    void this.router.navigateByUrl('/flashcards');
  }

  private async abrirDaRota(): Promise<void> {
    const params = this.route.snapshot.queryParamMap;
    const deckId = params.get('deck');
    const disciplinaId = params.get('disciplina');
    const escopo: EscopoFila = deckId === 'sem-deck' ? 'SEM_DECK' : deckId ? 'DECK' : 'TODOS';
    await this.store.abrir({ escopo, deckId: escopo === 'DECK' ? deckId : null, disciplinaId });
    this.cronometro.iniciar();
  }
}
