import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { SimuladoApi } from '../../../core/api/simulado-api';
import {
  RespostaSimulados,
  RespostaTentativa,
  SimuladoConcluido,
  SimuladoResumo,
} from '../../../core/models/modelos-simulado';
import { ProximoSimuladoCard } from '../proximo-simulado-card/proximo-simulado-card';
import { SimuladoCard } from '../simulado-card/simulado-card';
import { ConfirmacaoDialog } from '../../../shared/ui/confirmacao-dialog/confirmacao-dialog';

@Component({
  selector: 'app-simulados-page',
  standalone: true,
  imports: [ProximoSimuladoCard, SimuladoCard, ConfirmacaoDialog],
  templateUrl: './simulados-page.html',
  styleUrl: './simulados-page.scss',
})
export class SimuladosPage {
  protected readonly dados = signal<RespostaSimulados>({
    proximo: null,
    disponiveis: [],
    agendados: [],
    concluidos: [],
  });
  protected readonly erro = signal<string | null>(null);
  protected readonly confirmacaoTreino = signal<RespostaTentativa | null>(null);
  private readonly api = inject(SimuladoApi);
  private readonly router = inject(Router);
  constructor() {
    void this.carregar();
  }
  async carregar(): Promise<void> {
    try {
      this.dados.set(await firstValueFrom(this.api.listarDoEstudante()));
    } catch {
      this.erro.set('Não foi possível carregar os simulados.');
    }
  }
  async iniciar(simulado: SimuladoResumo | SimuladoConcluido): Promise<void> {
    try {
      const tentativa = await firstValueFrom(this.api.iniciarTentativa(simulado.id));
      if (tentativa.tipo === 'TREINO') {
        this.confirmacaoTreino.set(tentativa);
        return;
      }
      await this.navegarParaSessao(tentativa);
    } catch {
      this.erro.set('Não foi possível iniciar o simulado.');
    }
  }
  async confirmarTreino(): Promise<void> {
    const tentativa = this.confirmacaoTreino();
    this.confirmacaoTreino.set(null);
    if (tentativa) await this.navegarParaSessao(tentativa);
  }
  cancelarTreino(): void {
    this.confirmacaoTreino.set(null);
  }
  private async navegarParaSessao(tentativa: RespostaTentativa): Promise<void> {
    const sessao = tentativa.sessao as { sessaoId?: string; id?: string };
    await this.router.navigate(['/estudar'], {
      queryParams: {
        sessao: sessao.sessaoId ?? sessao.id,
        simulado: tentativa.simuladoId,
        tentativa: tentativa.tentativaId,
      },
    });
  }
  async revisar(simulado: SimuladoConcluido): Promise<void> {
    await this.router.navigate(['/simulados', simulado.id, 'resultado'], {
      queryParams: { tentativa: simulado.tentativaId },
    });
  }
}
