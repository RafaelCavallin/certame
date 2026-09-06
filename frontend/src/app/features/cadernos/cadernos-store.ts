import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CadernoApi } from '../../core/api/caderno-api';
import { EstudoApi } from '../../core/api/estudo-api';
import { CadernoResumo, RequisicaoCaderno } from '../../core/api/modelos-desempenho';

@Injectable({ providedIn: 'root' })
export class CadernosStore {
  private readonly api = inject(CadernoApi);
  private readonly estudoApi = inject(EstudoApi);
  private readonly lista = signal<CadernoResumo[]>([]);
  readonly cadernos = computed(() => [...this.lista()].sort((a, b) => this.ordem(a, b)));
  readonly vazio = computed(() => this.lista().every((caderno) => caderno.totalQuestoes === 0));
  readonly carregando = signal(false);
  readonly erro = signal<string | null>(null);

  async carregar(): Promise<void> {
    this.carregando.set(true);
    try {
      this.lista.set(await firstValueFrom(this.api.listar()));
      this.erro.set(null);
    } catch (erro) {
      this.erro.set(this.mensagem(erro));
    } finally {
      this.carregando.set(false);
    }
  }
  async criar(dados: RequisicaoCaderno): Promise<void> {
    this.lista.set([...this.lista(), await firstValueFrom(this.api.criar(dados))]);
  }
  async renomear(id: string, dados: RequisicaoCaderno): Promise<void> {
    await this.atualizarLocal(await firstValueFrom(this.api.atualizar(id, dados)));
  }
  async excluir(id: string): Promise<void> {
    await firstValueFrom(this.api.excluir(id));
    this.lista.set(this.lista().filter((caderno) => caderno.id !== id));
  }
  async adicionarQuestoes(id: string, questaoIds: string[]): Promise<void> {
    await this.atualizarLocal(await firstValueFrom(this.api.adicionarQuestoes(id, { questaoIds })));
  }
  async estudar(id: string): Promise<string> {
    const sessao = await firstValueFrom(
      this.estudoApi.abrir({ origem: 'CADERNO', origemRef: id, modo: 'FOCO' }),
    );
    return sessao.sessaoId;
  }
  private async atualizarLocal(caderno: CadernoResumo): Promise<void> {
    this.lista.set(this.lista().map((item) => (item.id === caderno.id ? caderno : item)));
  }
  private ordem(a: CadernoResumo, b: CadernoResumo): number {
    return a.tipo !== b.tipo
      ? a.tipo === 'AUTOMATICO'
        ? -1
        : 1
      : (b.atualizadoEm ?? '').localeCompare(a.atualizadoEm ?? '');
  }
  private mensagem(erro: unknown): string {
    return (
      (erro as { error?: { message?: string } })?.error?.message ??
      'Não foi possível carregar os cadernos.'
    );
  }
}
