import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CatalogoApi } from '../../../../core/api/catalogo-api';
import { ErroApi, extrairErroApi } from '../../../../core/api/erro-api';
import { SimuladoApi } from '../../../../core/api/simulado-api';
import {
  EstadoSimulado,
  PaginaResposta,
  SimuladoResumoAdmin,
} from '../../../../core/models/modelos-simulado';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-admin-simulados-page',
  imports: [FormsModule, RouterLink],
  templateUrl: './simulados-page.html',
  styleUrl: './simulados-page.scss',
})
export class SimuladosPage {
  protected readonly api = inject(SimuladoApi);
  protected readonly catalogo = inject(CatalogoApi);
  protected readonly pagina = signal<PaginaResposta<SimuladoResumoAdmin>>({
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
  });
  protected readonly bancas = signal<{ id: string; nome: string }[]>([]);
  protected readonly erro = signal<string | null>(null);
  protected q = '';
  protected estado: EstadoSimulado | '' = '';
  protected bancaId = '';
  protected page = 0;
  constructor() {
    void this.carregar();
    this.catalogo.bancas().subscribe((bancas) => this.bancas.set(bancas));
  }
  async carregar(): Promise<void> {
    try {
      this.pagina.set(
        await firstValueFrom(
          this.api.listar({
            q: this.q,
            estado: this.estado,
            bancaId: this.bancaId,
            page: this.page,
          }),
        ),
      );
    } catch (e) {
      this.erro.set(extrairErroApi(this.corpo(e)).message);
    }
  }
  aplicar(): void {
    this.page = 0;
    void this.carregar();
  }
  mudarPagina(delta: number): void {
    if (
      this.page + delta < 0 ||
      (this.page + delta) * this.pagina().size >= this.pagina().totalElements
    )
      return;
    this.page += delta;
    void this.carregar();
  }
  statusLabel(status: EstadoSimulado): string {
    return {
      RASCUNHO: 'Rascunho',
      AGENDADO: 'Agendado',
      DISPONIVEL: 'Disponível',
      ENCERRADO: 'Encerrado',
    }[status];
  }
  duracao(segundos: number): string {
    return segundos % 3600 ? `${segundos / 60} min` : `${segundos / 3600} h`;
  }
  private corpo(erro: unknown): unknown {
    return erro && typeof erro === 'object' && 'error' in erro
      ? (erro as { error: unknown }).error
      : erro;
  }
}
