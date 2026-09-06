import { Component, OnDestroy, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { EstudoApi } from '../../../core/api/estudo-api';
import { SimuladoApi } from '../../../core/api/simulado-api';
import {
  ModoSessao,
  QuestaoDaSerie,
  RequisicaoSessao,
  RespostaCorrecao,
} from '../../../core/api/modelos-estudo';
import {
  ContextoQuestaoFlashcard,
  RespostaFlashcardCriado,
} from '../../../core/api/modelos-flashcard';
import { mensagemErroApi } from '../../../core/api/erro-api';
import { DialogService } from '../../../shared/ui/dialog/dialog-service';
import { FlashcardDialog } from '../../flashcards/flashcard-dialog/flashcard-dialog';
import { ConfirmacaoDialog } from '../../../shared/ui/confirmacao-dialog/confirmacao-dialog';
import { AtalhosTeclado } from '../atalhos-teclado';
import { BarraAcao } from '../barra-acao/barra-acao';
import { CorrecaoProva } from '../correcao-prova/correcao-prova';
import { GabaritoPainel } from '../gabarito-painel/gabarito-painel';
import { PainelFiltros } from '../painel-filtros/painel-filtros';
import { PainelNavegacao } from '../painel-navegacao/painel-navegacao';
import { QuestaoCartao } from '../questao-cartao/questao-cartao';
import { Cronometro } from '../cronometro';
import { StudySessionStore } from '../study-session-store';
import { gravarContadorFlashcards, lerContadorFlashcards } from '../contador-flashcards-sessao';
import { AdicionarCadernoDialog } from '../adicionar-caderno-dialog/adicionar-caderno-dialog';
import { ToastStore } from '../../../shared/ui/toast/toast-store';

const TAMANHO_PADRAO_SERIE = 10;

@Component({
  selector: 'app-estudar-page',
  standalone: true,
  imports: [
    QuestaoCartao,
    BarraAcao,
    CorrecaoProva,
    GabaritoPainel,
    PainelFiltros,
    PainelNavegacao,
    AtalhosTeclado,
    ConfirmacaoDialog,
    AdicionarCadernoDialog,
  ],
  templateUrl: './estudar-page.html',
  styleUrl: './estudar-page.scss',
})
export class EstudarPage implements OnDestroy {
  readonly store = inject(StudySessionStore);
  readonly cronometro = inject(Cronometro);
  readonly carregando = signal(false);
  readonly preparando = signal(false);
  readonly modoInicial = signal<ModoSessao>('FOCO');
  readonly erro = signal<string | null>(null);
  readonly correcao = signal<RespostaCorrecao | null>(null);
  readonly selecionada = signal<string | null>(null);
  readonly criadosFlashcards = signal<number | null>(null);
  readonly confirmandoFinalizacao = signal(false);
  readonly finalizando = signal(false);
  readonly adicionandoCaderno = signal(false);
  private readonly api = inject(EstudoApi);
  private readonly simuladoApi = inject(SimuladoApi);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialogService = inject(DialogService);
  private readonly toast = inject(ToastStore);
  private ultimaRequisicao: RequisicaoSessao | null = null;
  constructor() {
    this.modoInicial.set((this.rota.snapshot.queryParamMap.get('modo') as ModoSessao) || 'FOCO');
    const sessao = this.rota.snapshot.queryParamMap.get('sessao');
    if (sessao) {
      this.carregar(
        () => this.api.retomarSessao(sessao),
        () => this.voltarAoPreparo(),
      );
      return;
    }
    this.preparando.set(true);
  }
  iniciar(dados: RequisicaoSessao): void {
    this.ultimaRequisicao = dados;
    this.preparando.set(false);
    this.store.limpar();
    this.carregar(
      () => this.api.abrir(dados),
      () => this.voltarAoPreparo(),
    );
  }
  ngOnDestroy(): void {
    const sessaoId = this.store.sessao()?.sessaoId;
    if (sessaoId && this.cronometro.ativo() && !this.correcao()) this.cronometro.pausar();
    this.cronometro.parar(sessaoId);
    this.store.limpar();
  }
  /** Troca a série sem sair da sessão: se a abertura falhar, a sessão em curso continua na tela. */
  aplicarFiltrosLaterais(dados: RequisicaoSessao): void {
    const requisicao = { ...dados, modo: this.store.modo() ?? 'FOCO' };
    this.ultimaRequisicao = requisicao;
    this.carregar(() => this.api.abrir(requisicao));
  }
  /** Devolve a tela ao painel de filtros quando a abertura falha, para não exibir a sessão anterior. */
  private voltarAoPreparo(): void {
    this.cronometro.parar();
    this.modoInicial.set(this.store.modo() ?? this.modoInicial());
    this.store.limpar();
    this.preparando.set(true);
  }
  private carregar(requisicao: () => ReturnType<EstudoApi['abrir']>, aoFalhar?: () => void): void {
    this.erro.set(null);
    this.carregando.set(true);
    requisicao().subscribe({
      next: (sessao) => {
        this.store.iniciar(sessao);
        this.selecionada.set(
          this.store.alternativaMarcada(sessao.questoes[sessao.indiceAtual]?.id),
        );
        this.cronometro.iniciar(sessao.sessaoId, sessao.decorridoSegundos);
        this.criadosFlashcards.set(lerContadorFlashcards(sessao.sessaoId));
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemErroApi(erro, 'Não foi possível carregar a sessão.'));
        this.carregando.set(false);
        aoFalhar?.();
      },
    });
  }
  abrirFlashcard(): void {
    const questao = this.questao();
    const sessaoId = this.store.sessao()!.sessaoId;
    const devolverFocoPara = document.getElementById('enunciado') as HTMLElement;
    const contexto: ContextoQuestaoFlashcard = {
      questaoId: questao.id,
      codigo: questao.codigo,
      disciplinaId: null,
      disciplinaNome: questao.disciplina,
      assuntoId: null,
      assuntoNome: questao.assunto,
      sessaoId,
      devolverFocoPara,
    };
    this.dialogService
      .abrir<RespostaFlashcardCriado>(FlashcardDialog, { contexto, devolverFocoPara })
      .subscribe((resposta) => this.atualizarContadorFlashcards(sessaoId, resposta));
  }
  private atualizarContadorFlashcards(
    sessaoId: string,
    resposta: RespostaFlashcardCriado | undefined,
  ): void {
    if (!resposta || resposta.criadosNaSessao === null) return;
    this.criadosFlashcards.set(resposta.criadosNaSessao);
    gravarContadorFlashcards(sessaoId, resposta.criadosNaSessao);
  }
  selecionar(marcada: string): void {
    // Fora do Modo Prova a resposta é definitiva: uma vez registrada, a
    // alternativa não muda mais (nem pelo atalho de teclado).
    if (!this.store.emProva() && this.store.respostaDa(this.questao().id)) return;
    this.selecionada.set(marcada);
    if (this.store.emProva()) this.store.marcarAlternativa(this.questao().id, marcada);
  }
  responder(): void {
    const questao = this.questao();
    const marcada = this.selecionada();
    if (!marcada) return;
    if (!this.store.emProva() && this.store.respostaDa(questao.id)) {
      this.proximo();
      return;
    }
    this.erro.set(null);
    if (this.store.emProva()) {
      this.store.marcarAlternativa(questao.id, marcada);
      this.proximo();
      return;
    }
    this.api
      .responder(this.store.sessao()!.sessaoId, {
        questaoId: questao.id,
        marcada,
        tempoMs: this.cronometro.segundos() * 1000,
      })
      .subscribe({
        next: (resposta) => this.store.marcarResposta(resposta),
        error: (erro: unknown) =>
          this.erro.set(mensagemErroApi(erro, 'Não foi possível registrar sua resposta.')),
      });
  }
  trocarModo(modo: ModoSessao): void {
    if (modo === 'PROVA') return;
    this.api.trocarModo(this.store.sessao()!.sessaoId, modo).subscribe({
      next: (sessao) => {
        this.store.trocarModo(sessao.modo);
      },
      error: (erro: unknown) =>
        this.erro.set(mensagemErroApi(erro, 'Não foi possível trocar o modo.')),
    });
  }
  proximo(): void {
    this.store.proxima();
    this.selecionada.set(this.store.alternativaMarcada(this.questao().id));
  }
  anterior(): void {
    this.store.anterior();
    this.selecionada.set(this.store.alternativaMarcada(this.questao().id));
  }
  selecionarIndice(indice: number): void {
    this.store.irPara(indice);
    this.selecionada.set(this.store.alternativaMarcada(this.questao().id));
  }
  pedirFinalizacao(): void {
    this.confirmandoFinalizacao.set(true);
  }
  cancelarFinalizacao(): void {
    this.confirmandoFinalizacao.set(false);
  }
  finalizar(): void {
    const sessao = this.store.sessao()!;
    this.confirmandoFinalizacao.set(false);
    this.erro.set(null);
    this.finalizando.set(true);
    const dados = {
      respostas: this.store.emProva()
        ? this.store
            .respostas()
            .filter((item) => item.marcada)
            .map((item) => ({
              questaoId: item.questaoId,
              marcada: item.marcada,
              tempoMs: item.tempoMs,
            }))
        : [],
    };
    const simuladoId = this.rota.snapshot.queryParamMap.get('simulado');
    const tentativaId = this.rota.snapshot.queryParamMap.get('tentativa');
    if (simuladoId && tentativaId) {
      this.simuladoApi.finalizarTentativa(simuladoId, tentativaId, dados).subscribe({
        next: () => {
          this.finalizando.set(false);
          this.cronometro.parar(sessao.sessaoId);
          void this.router.navigate(['/simulados', simuladoId, 'resultado'], {
            queryParams: { tentativa: tentativaId },
          });
        },
        error: (erro: unknown) => {
          this.finalizando.set(false);
          this.erro.set(mensagemErroApi(erro, 'Não foi possível finalizar o simulado.'));
        },
      });
      return;
    }
    this.api.finalizar(sessao.sessaoId, dados).subscribe({
      next: (resultado) => {
        this.finalizando.set(false);
        this.correcao.set(resultado);
        this.cronometro.parar(sessao.sessaoId);
      },
      error: (erro: unknown) => {
        this.finalizando.set(false);
        this.erro.set(mensagemErroApi(erro, 'Não foi possível finalizar a sessão.'));
      },
    });
  }
  reiniciar(): void {
    const requisicao = this.ultimaRequisicao ?? {
      tamanho: TAMANHO_PADRAO_SERIE,
      modo: this.store.modo() ?? 'FOCO',
    };
    this.correcao.set(null);
    this.iniciar(requisicao);
  }
  questao() {
    return this.store.questaoAtual()!;
  }
  respondidas(): Set<number> {
    return new Set(
      this.store
        .respostas()
        .map((resposta) =>
          this.store.sessao()!.questoes.findIndex((questao) => questao.id === resposta.questaoId),
        ),
    );
  }
  voltarInicio(): void {
    void this.router.navigateByUrl('/inicio');
  }
  abrirAdicionarCaderno(): void {
    this.adicionandoCaderno.set(true);
  }
  fecharAdicionarCaderno(): void {
    this.adicionandoCaderno.set(false);
  }
  aoAdicionarCaderno(nomesCadernos: string[]): void {
    this.adicionandoCaderno.set(false);
    const nomes = nomesCadernos.map((nome) => `"${nome}"`).join(', ');
    const mensagem =
      nomesCadernos.length > 1
        ? `Questão adicionada aos cadernos ${nomes}.`
        : `Questão adicionada ao caderno ${nomes}.`;
    this.toast.publicar(mensagem);
  }
  alternarMarcacao(questao: QuestaoDaSerie, tipo: 'favorita' | 'erroRecorrente'): void {
    const marcacoes = {
      favorita: questao.favorita,
      erroRecorrente: questao.erroRecorrente,
      [tipo]: !questao[tipo],
    };
    this.api.marcar(questao.id, marcacoes).subscribe({
      next: (atualizadas) => this.store.atualizarQuestao({ ...questao, ...atualizadas }),
      error: (erro: unknown) =>
        this.erro.set(mensagemErroApi(erro, 'Não foi possível atualizar a marcação.')),
    });
  }
}
