import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { extrairErroApi } from '../../core/api/erro-api';
import { FlashcardApi } from '../../core/api/flashcard-api';
import {
  FiltroFlashcards,
  IndicadoresFlashcards,
  PaginaFlashcards,
} from '../../core/api/modelos-flashcard';

const TAMANHO_PAGINA = 20;
const ATRASO_BUSCA_MS = 350;
const INDICADORES_VAZIOS: IndicadoresFlashcards = {
  vencendoHoje: 0,
  proximosSeteDias: 0,
  emFixacao: 0,
  total: 0,
  decks: 0,
};
const PAGINA_VAZIA: PaginaFlashcards = {
  content: [],
  page: 0,
  size: TAMANHO_PAGINA,
  totalElements: 0,
  indicadores: INDICADORES_VAZIOS,
};

@Injectable()
export class FlashcardsStore {
  private readonly api = inject(FlashcardApi);
  readonly pagina = signal<PaginaFlashcards>(PAGINA_VAZIA);
  readonly filtro = signal<FiltroFlashcards>({
    page: 0,
    size: TAMANHO_PAGINA,
    sort: 'criadoEm,desc',
  });
  readonly carregando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly selecionados = signal<string[]>([]);
  readonly indicadores = computed(() => this.pagina().indicadores);
  readonly acervoVazio = computed(() => this.indicadores().total === 0);
  readonly vazio = computed(
    () => !this.carregando() && this.pagina().content.length === 0 && !this.acervoVazio(),
  );
  private temporizadorBusca: ReturnType<typeof setTimeout> | null = null;

  async carregar(): Promise<void> {
    this.carregando.set(true);
    try {
      this.pagina.set(await firstValueFrom(this.api.listar(this.filtro())));
      this.erro.set(null);
    } catch (erro) {
      this.erro.set(extrairErroApi(this.corpo(erro)).message);
    } finally {
      this.carregando.set(false);
    }
  }

  filtrar(alteracoes: Partial<FiltroFlashcards>, comAtraso = false): void {
    if (this.temporizadorBusca) clearTimeout(this.temporizadorBusca);
    const aplicar = () => {
      this.filtro.update((f) => ({ ...f, ...alteracoes, page: 0 }));
      this.selecionados.set([]);
      void this.carregar();
    };
    if (!comAtraso) {
      aplicar();
      return;
    }
    this.temporizadorBusca = setTimeout(aplicar, ATRASO_BUSCA_MS);
  }

  irPara(pagina: number): void {
    this.filtro.update((f) => ({ ...f, page: Math.max(0, pagina) }));
    void this.carregar();
  }
  async arquivar(id: string): Promise<void> {
    await this.executar(this.api.arquivar(id));
  }
  async restaurar(id: string): Promise<void> {
    await this.executar(this.api.restaurar(id));
  }
  async excluir(id: string): Promise<void> {
    await this.executar(this.api.excluir(id));
  }

  alternarSelecao(id: string): void {
    this.selecionados.update((atuais) =>
      atuais.includes(id) ? atuais.filter((x) => x !== id) : [...atuais, id],
    );
  }

  alternarTodos(): void {
    const idsDaPagina = this.pagina().content.map((c) => c.id);
    const todosMarcados =
      idsDaPagina.length > 0 && idsDaPagina.every((id) => this.selecionados().includes(id));
    this.selecionados.set(todosMarcados ? [] : idsDaPagina);
  }

  async moverSelecionados(deckId: string): Promise<void> {
    if (this.selecionados().length === 0) return;
    try {
      await firstValueFrom(this.api.moverParaDeck(deckId, { flashcardIds: this.selecionados() }));
      this.selecionados.set([]);
      await this.carregar();
    } catch (erro) {
      this.erro.set(extrairErroApi(this.corpo(erro)).message);
    }
  }

  private async executar(chamada: Observable<void>): Promise<void> {
    try {
      await firstValueFrom(chamada);
      this.selecionados.set([]);
      await this.carregar();
    } catch (erro) {
      this.erro.set(extrairErroApi(this.corpo(erro)).message);
    }
  }

  private corpo(erro: unknown): unknown {
    return erro && typeof erro === 'object' && 'error' in erro
      ? (erro as { error: unknown }).error
      : erro;
  }
}
