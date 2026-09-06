import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CatalogoApi } from '../../../../core/api/catalogo-api';
import { QuestaoApi } from '../../../../core/api/questao-api';
import { ErroApi, extrairErroApi } from '../../../../core/api/erro-api';
import { PaginaResposta, QuestaoResumo, StatusQuestao } from '../../../../core/api/modelos-questao';
import { AuthStore } from '../../../../core/auth/auth-store';
import { QuestoesFiltros } from '../questoes-filtros';
import { ConfirmacaoDialog } from '../../../../shared/ui/confirmacao-dialog/confirmacao-dialog';
@Component({
  selector: 'app-questoes-page',
  imports: [FormsModule, RouterLink, ConfirmacaoDialog],
  providers: [QuestoesFiltros],
  templateUrl: './questoes-page.html',
  styleUrl: './questoes-page.scss',
})
export class QuestoesPage {
  readonly filtros = inject(QuestoesFiltros);
  private readonly api = inject(QuestaoApi);
  private readonly catalogo = inject(CatalogoApi);
  readonly auth = inject(AuthStore);
  readonly pagina = signal<PaginaResposta<QuestaoResumo>>({
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
  });
  readonly disciplinas = signal<import('../../../../core/api/modelos-questao').DisciplinaArvore[]>(
    [],
  );
  readonly bancas = signal<import('../../../../core/api/modelos-questao').ItemCatalogo[]>([]);
  readonly erro = signal<string | null>(null);
  readonly confirmacao = signal<{ acao: 'excluir' | 'despublicar'; id: string } | null>(null);
  constructor() {
    void this.carregar();
    void this.catalogo.disciplinas().subscribe((v) => this.disciplinas.set(v));
    void this.catalogo.bancas().subscribe((v) => this.bancas.set(v));
  }
  async carregar(): Promise<void> {
    try {
      this.pagina.set(await firstValueFrom(this.api.listar({ ...this.filtros.parametros() })));
    } catch (e) {
      this.erro.set(extrairErroApi(this.corpo(e)).message);
    }
  }
  aplicar(): void {
    this.filtros.pagina.set(0);
    this.filtros.aplicar();
    void this.carregar();
  }
  publicar(id: string): void {
    void firstValueFrom(this.api.publicar(id))
      .then(() => this.carregar())
      .catch((e) => this.erro.set(extrairErroApi(this.corpo(e)).message));
  }
  excluir(id: string): void {
    this.confirmacao.set({ acao: 'excluir', id });
  }
  despublicar(id: string): void {
    this.confirmacao.set({ acao: 'despublicar', id });
  }
  async confirmar(): Promise<void> {
    const c = this.confirmacao();
    if (!c) return;
    try {
      if (c.acao === 'excluir') await firstValueFrom(this.api.excluir(c.id));
      else await firstValueFrom(this.api.despublicar(c.id));
      this.confirmacao.set(null);
      await this.carregar();
    } catch (e) {
      const erro: ErroApi = extrairErroApi(this.corpo(e));
      this.confirmacao.set(null);
      this.erro.set(
        erro.code === 'QUESTAO_EM_USO'
          ? `${erro.message} Despublique a questão antes de tentar novamente.`
          : erro.message,
      );
    }
  }
  statusLabel(s: StatusQuestao): string {
    return { RASCUNHO: 'Rascunho', EM_REVISAO: 'Em revisão', PUBLICADA: 'Publicada' }[s];
  }
  private corpo(e: unknown): unknown {
    return e && typeof e === 'object' && 'error' in e ? (e as { error: unknown }).error : e;
  }
}
