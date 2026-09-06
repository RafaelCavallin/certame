import { Component, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { SimuladoApi } from '../../../../core/api/simulado-api';
import { CatalogoApi } from '../../../../core/api/catalogo-api';
import { QuestaoApi } from '../../../../core/api/questao-api';
import { ErroApi, extrairErroApi } from '../../../../core/api/erro-api';
import { ItemComposicao, ResumoComposicao } from '../../../../core/models/modelos-simulado';

@Component({
  selector: 'app-composicao-disciplinas',
  imports: [FormsModule],
  templateUrl: './composicao-disciplinas.html',
  styleUrl: './composicao-disciplinas.scss',
})
export class ComposicaoDisciplinas {
  readonly simuladoId = input.required<string>();
  readonly resumo = input.required<ResumoComposicao>();
  readonly alterado = output<ResumoComposicao>();
  protected readonly api = inject(SimuladoApi);
  protected readonly catalogo = inject(CatalogoApi);
  protected readonly questoes = inject(QuestaoApi);
  protected readonly erro = signal<string | null>(null);
  protected readonly disciplinas = signal<{ id: string; nome: string }[]>([]);
  protected readonly salvando = signal(false);
  protected readonly disponiveisPorLinha = signal<Record<number, number | null>>({});
  protected itens: ItemComposicao[] = [];
  private hidratado = false;
  // Hidrata `itens` só na primeira leitura do resumo (carga inicial): depois disso o formulário é a
  // fonte da verdade local, para o eco do `resumo` após um salvar não sobrescrever uma edição feita
  // enquanto o salvamento anterior ainda estava em voo.
  constructor() {
    effect(() => {
      const resumo = this.resumo();
      if (this.hidratado) return;
      this.hidratado = true;
      this.itens = resumo.itens.map(({ disciplinaId, dificuldade, quantidade }) => ({
        disciplinaId,
        dificuldade,
        quantidade,
      }));
      this.itens.forEach((_, indice) => this.atualizarDisponiveis(indice));
    });
    this.catalogo.disciplinas().subscribe((disciplinas) => this.disciplinas.set(disciplinas));
  }
  adicionar(): void {
    this.itens = [...this.itens, { disciplinaId: '', dificuldade: null, quantidade: 1 }];
  }
  remover(indice: number): void {
    this.itens = this.itens.filter((_, item) => item !== indice);
    const { [indice]: _removido, ...resto } = this.disponiveisPorLinha();
    this.disponiveisPorLinha.set(this.reindexar(resto, indice));
  }
  atualizarDisponiveis(indice: number): void {
    const item = this.itens[indice];
    if (!item.disciplinaId) {
      this.disponiveisPorLinha.update((atual) => ({ ...atual, [indice]: null }));
      return;
    }
    this.questoes.contarDisponiveis(item.disciplinaId, item.dificuldade).subscribe((resposta) => {
      this.disponiveisPorLinha.update((atual) => ({ ...atual, [indice]: resposta.disponiveis }));
    });
  }
  private reindexar(
    mapa: Record<number, number | null>,
    indiceRemovido: number,
  ): Record<number, number | null> {
    const resultado: Record<number, number | null> = {};
    for (const [chave, valor] of Object.entries(mapa)) {
      const indice = Number(chave);
      resultado[indice > indiceRemovido ? indice - 1 : indice] = valor;
    }
    return resultado;
  }
  async salvar(): Promise<void> {
    this.salvando.set(true);
    try {
      const resumo = await firstValueFrom(
        this.api.composicao(this.simuladoId(), { itens: this.itens }),
      );
      this.erro.set(null);
      this.alterado.emit(resumo);
    } catch (e) {
      const api: ErroApi = extrairErroApi(this.corpo(e));
      this.erro.set(api.message);
    } finally {
      this.salvando.set(false);
    }
  }
  private corpo(erro: unknown): unknown {
    return erro && typeof erro === 'object' && 'error' in erro
      ? (erro as { error: unknown }).error
      : erro;
  }
}
