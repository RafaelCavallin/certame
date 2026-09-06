import { Component, computed, input } from '@angular/core';
import { EvolucaoSemanal } from '../../../core/api/modelos-desempenho';
import { LinhaTabela, PontoGrafico } from '../desempenho-store';

const BASE_Y = 190;
const ALTURA_BARRA = 135;
const ORIGEM_X = 30;
const LARGURA = 598;

@Component({
  selector: 'app-evolucao-chart',
  templateUrl: './evolucao-chart.html',
  styleUrl: './evolucao-chart.scss',
})
export class EvolucaoChart {
  readonly evolucao = input.required<EvolucaoSemanal>();
  readonly segmentos = input.required<PontoGrafico[][]>();
  readonly serie = input.required<LinhaTabela[]>();
  readonly linePath = computed(() =>
    this.segmentos()
      .filter((segmento) => segmento.length > 1)
      .map(this.caminho)
      .join(' '),
  );
  readonly areaPath = computed(() =>
    this.segmentos()
      .filter((segmento) => segmento.length > 1)
      .map(
        (segmento) =>
          `${this.caminho(segmento)} L ${segmento.at(-1)?.x} ${BASE_Y} L ${segmento[0].x} ${BASE_Y} Z`,
      )
      .join(' '),
  );
  readonly pontos = computed(() => this.segmentos().flat());
  readonly barras = computed(() => {
    const semanas = this.evolucao().semanas;
    const maior = Math.max(...semanas.map((semana) => semana.volume), 1);
    return semanas.map((semana, indice) => {
      const x =
        semanas.length > 1
          ? ORIGEM_X + (indice * LARGURA) / (semanas.length - 1)
          : ORIGEM_X + LARGURA / 2;
      const altura = (semana.volume / maior) * ALTURA_BARRA;
      return { x: x - 15, y: BASE_Y - altura, altura };
    });
  });

  semanasX(indice: number): number {
    const total = this.evolucao().semanas.length;
    return total > 1 ? ORIGEM_X + (indice * LARGURA) / (total - 1) : ORIGEM_X + LARGURA / 2;
  }

  private caminho(segmento: PontoGrafico[]): string {
    return `M ${segmento.map((ponto) => `${ponto.x} ${ponto.y}`).join(' L ')}`;
  }
}
