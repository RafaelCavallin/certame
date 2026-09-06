import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { EstadoVazio } from '../../../shared/ui/estado-vazio/estado-vazio';
import { DesempenhoStore } from '../desempenho-store';
import { EstudoApi } from '../../../core/api/estudo-api';
import { EvolucaoChart } from '../evolucao-chart/evolucao-chart';
import { DisciplinasCard } from '../disciplinas-card/disciplinas-card';
import { PontosFracosTable } from '../pontos-fracos-table/pontos-fracos-table';

@Component({
  selector: 'app-desempenho-page',
  imports: [EstadoVazio, EvolucaoChart, DisciplinasCard, PontosFracosTable],
  templateUrl: './desempenho-page.html',
  styleUrl: './desempenho-page.scss',
})
export class DesempenhoPage {
  protected readonly store = inject(DesempenhoStore);
  private readonly estudoApi = inject(EstudoApi);
  private readonly router = inject(Router);
  constructor() {
    void this.store.carregar();
  }
  protected estudar(): void {
    void this.router.navigateByUrl('/estudar');
  }
  protected async treinar(assuntoId: string): Promise<void> {
    const sessao = await firstValueFrom(
      this.estudoApi.abrir({ tamanho: 10, modo: 'FOCO', filtro: { assuntoId } }),
    );
    await this.router.navigate(['/estudar'], { queryParams: { sessao: sessao.sessaoId } });
  }
}
