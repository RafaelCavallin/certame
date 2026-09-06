import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { DesempenhoApi } from '../../core/api/desempenho-api';
import {
  EvolucaoSemanal,
  PontosFracos,
  ResumoDesempenho,
  DesempenhoDisciplinaResumo,
} from '../../core/api/modelos-desempenho';

export interface PontoGrafico {
  x: number;
  y: number;
  semana: string;
  pct: number;
}
export interface LinhaTabela {
  semana: string;
  percentual: string;
  volume: string;
}

const LARGURA_GRAFICO = 598;
const ORIGEM_X = 30;
const BASE_Y = 190;
const ESCALA_Y = 1.5;

@Injectable({ providedIn: 'root' })
export class DesempenhoStore {
  private readonly api = inject(DesempenhoApi);
  readonly resumo = signal<ResumoDesempenho | null>(null);
  readonly evolucao = signal<EvolucaoSemanal | null>(null);
  readonly disciplinas = signal<DesempenhoDisciplinaResumo[]>([]);
  readonly pontosFracos = signal<PontosFracos | null>(null);
  readonly carregando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly possuiHistorico = computed(() => this.resumo()?.possuiHistorico ?? false);
  readonly segmentosLinha = computed(() => this.criarSegmentos(this.evolucao()?.semanas ?? []));
  readonly serieTextual = computed(() =>
    (this.evolucao()?.semanas ?? []).map((semana) => ({
      semana: semana.rotulo,
      percentual: semana.pctAcerto === null ? 'Sem atividade' : `${semana.pctAcerto}%`,
      volume: semana.volume.toLocaleString('pt-BR'),
    })),
  );

  async carregar(): Promise<void> {
    this.carregando.set(true);
    try {
      const resumo = await firstValueFrom(this.api.resumo());
      this.resumo.set(resumo);
      if (!resumo.possuiHistorico) return;
      const [evolucao, disciplinas, pontosFracos] = await Promise.all([
        firstValueFrom(this.api.evolucao()),
        firstValueFrom(this.api.disciplinas()),
        firstValueFrom(this.api.pontosFracos()),
      ]);
      this.evolucao.set(evolucao);
      this.disciplinas.set(disciplinas);
      this.pontosFracos.set(pontosFracos);
      this.erro.set(null);
    } catch {
      this.erro.set('Não foi possível carregar seu desempenho.');
    } finally {
      this.carregando.set(false);
    }
  }

  private criarSegmentos(semanas: EvolucaoSemanal['semanas']): PontoGrafico[][] {
    const segmentos: PontoGrafico[][] = [];
    let atual: PontoGrafico[] = [];
    semanas.forEach((semana, indice) => {
      if (semana.pctAcerto === null) {
        if (atual.length) segmentos.push(atual);
        atual = [];
        return;
      }
      atual.push({
        x: this.x(indice, semanas.length),
        y: BASE_Y - semana.pctAcerto * ESCALA_Y,
        semana: semana.rotulo,
        pct: semana.pctAcerto,
      });
    });
    if (atual.length) segmentos.push(atual);
    return segmentos;
  }

  private x(indice: number, total: number): number {
    return total > 1
      ? ORIGEM_X + (indice * LARGURA_GRAFICO) / (total - 1)
      : ORIGEM_X + LARGURA_GRAFICO / 2;
  }
}
