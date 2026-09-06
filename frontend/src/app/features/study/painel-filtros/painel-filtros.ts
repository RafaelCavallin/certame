import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { CatalogoApi } from '../../../core/api/catalogo-api';
import { DisciplinaArvore, ItemCatalogo } from '../../../core/api/modelos-questao';
import { DificuldadeEstudo, ModoSessao, RequisicaoSessao } from '../../../core/api/modelos-estudo';

const TAMANHO_PADRAO_SERIE = 30;

interface OpcaoModo {
  valor: ModoSessao;
  rotulo: string;
  descricao: string;
}

const OPCOES_MODO: OpcaoModo[] = [
  {
    valor: 'FOCO',
    rotulo: 'Foco',
    descricao: 'Só o enunciado e as alternativas. O gabarito aparece assim que você responde.',
  },
  {
    valor: 'CONTEXTO',
    rotulo: 'Contexto',
    descricao: 'Igual ao Foco, com o painel de filtros ao lado para trocar a série sem sair.',
  },
  {
    valor: 'PROVA',
    rotulo: 'Modo Prova',
    descricao: 'Sem gabarito durante a série: você marca tudo e recebe a correção no fim.',
  },
];

@Component({
  selector: 'app-painel-filtros',
  standalone: true,
  templateUrl: './painel-filtros.html',
  styleUrl: './painel-filtros.scss',
})
export class PainelFiltros implements OnInit {
  private readonly catalogo = inject(CatalogoApi);
  readonly aplicar = output<RequisicaoSessao>();
  readonly variante = input<'lateral' | 'preparo'>('lateral');
  readonly modoInicial = input<ModoSessao>('FOCO');
  readonly modo = signal<ModoSessao>('FOCO');
  protected readonly opcoesModo = OPCOES_MODO;
  protected readonly preparo = computed(() => this.variante() === 'preparo');
  protected readonly descricaoModo = computed(
    () => OPCOES_MODO.find((opcao) => opcao.valor === this.modo())?.descricao ?? '',
  );
  readonly disciplinas = signal<DisciplinaArvore[]>([]);
  readonly bancas = signal<ItemCatalogo[]>([]);
  readonly disciplinaId = signal<string | null>(null);
  readonly assuntoId = signal<string | null>(null);
  readonly bancaId = signal<string | null>(null);
  readonly ano = signal<number | null>(null);
  readonly dificuldade = signal<DificuldadeEstudo | null>(null);
  readonly tamanho = signal(TAMANHO_PADRAO_SERIE);
  readonly somenteIneditas = signal(false);
  readonly excluirAcertadas = signal(false);
  readonly assuntos = computed(
    () => this.disciplinas().find((d) => d.id === this.disciplinaId())?.assuntos ?? [],
  );
  ngOnInit(): void {
    this.modo.set(this.modoInicial());
    this.catalogo.disciplinas().subscribe((disciplinas) => this.disciplinas.set(disciplinas));
    this.catalogo.bancas().subscribe((bancas) => this.bancas.set(bancas));
  }
  selecionarDisciplina(id: string): void {
    this.disciplinaId.set(id || null);
    this.assuntoId.set(null);
  }
  aplicarFiltros(): void {
    this.aplicar.emit({
      tamanho: this.tamanho(),
      ...(this.preparo() ? { modo: this.modo() } : {}),
      filtro: {
        disciplinaId: this.disciplinaId(),
        assuntoId: this.assuntoId(),
        bancaId: this.bancaId(),
        ano: this.ano(),
        dificuldade: this.dificuldade(),
        somenteIneditas: this.somenteIneditas(),
        excluirAcertadas: this.excluirAcertadas(),
      },
    });
  }
}
