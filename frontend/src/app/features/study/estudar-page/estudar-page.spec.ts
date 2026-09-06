import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { EstudoApi } from '../../../core/api/estudo-api';
import {
  ModoSessao,
  QuestaoDaSerie,
  RespostaRegistrada,
  RespostaSessao,
} from '../../../core/api/modelos-estudo';
import { SimuladoApi } from '../../../core/api/simulado-api';
import { Cronometro } from '../cronometro';
import { StudySessionStore } from '../study-session-store';
import { EstudarPage } from './estudar-page';

function questao(id: string, ordem: number): QuestaoDaSerie {
  return {
    id,
    ordem,
    codigo: `Q-${ordem}`,
    tipo: 'ME',
    enunciado: `Enunciado ${ordem}`,
    disciplina: null,
    assunto: null,
    banca: null,
    orgao: null,
    ano: null,
    dificuldade: null,
    alternativas: [
      { letra: 'A', texto: 'Alternativa A', ordem: 1 },
      { letra: 'B', texto: 'Alternativa B', ordem: 2 },
    ],
    favorita: false,
    erroRecorrente: false,
  };
}

function sessao(modo: ModoSessao, respostas: RespostaRegistrada[] = []): RespostaSessao {
  return {
    sessaoId: 's1',
    modo,
    origem: 'LIVRE',
    origemRef: null,
    total: 2,
    indiceAtual: 0,
    iniciadaEm: '2026-09-03T10:00:00Z',
    decorridoSegundos: 0,
    questoes: [questao('q1', 1), questao('q2', 2)],
    respostas,
  };
}

function rotaMock(params: Record<string, string | null>) {
  return { snapshot: { queryParamMap: { get: (chave: string) => params[chave] ?? null } } };
}

function flushCatalogo(http: HttpTestingController): void {
  http.expectOne('/api/v1/admin/disciplinas?incluirArquivados=false').flush([]);
  http.expectOne('/api/v1/admin/bancas').flush([]);
}

/**
 * Monta a página. Sem `?sessao=` na rota, ela abre na tela de preparo: por padrão o helper
 * confirma esse preparo (`iniciar`) para os testes que já querem uma sessão em andamento.
 * Passe `{ preparar: false }` para inspecionar a própria tela de preparo.
 */
function montar(
  params: Record<string, string | null>,
  apiMock: Record<string, ReturnType<typeof vi.fn>>,
  opcoes: { preparar?: boolean } = {},
) {
  const simuladoApiMock = { finalizarTentativa: vi.fn(() => of({})) };
  const router = { navigate: vi.fn(), navigateByUrl: vi.fn() };
  const apiComPadroes = {
    pausar: vi.fn(() => of(undefined)),
    retomarSessao: vi.fn(() => of(sessao('FOCO'))),
    ...apiMock,
  };
  TestBed.configureTestingModule({
    imports: [EstudarPage],
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: EstudoApi, useValue: apiComPadroes },
      { provide: SimuladoApi, useValue: simuladoApiMock },
      { provide: ActivatedRoute, useValue: rotaMock(params) },
      { provide: Router, useValue: router },
    ],
  });
  const fixture = TestBed.createComponent(EstudarPage);
  const http = TestBed.inject(HttpTestingController);
  if (!params['sessao'] && opcoes.preparar !== false) {
    fixture.detectChanges();
    flushCatalogo(http);
    fixture.componentInstance.iniciar({
      tamanho: 10,
      modo: (params['modo'] as ModoSessao) ?? 'FOCO',
    });
  }
  return { fixture, router, simuladoApiMock, http };
}

describe('EstudarPage', () => {
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
  });

  it('não abre sessão sozinha: mostra a tela de preparo sem ?sessao= na rota', () => {
    // Arrange
    const apiMock = { abrir: vi.fn(() => of(sessao('FOCO'))), retomar: vi.fn() };

    // Act
    const { fixture, http } = montar({}, apiMock, { preparar: false });
    fixture.detectChanges();
    flushCatalogo(http);
    fixture.detectChanges();

    // Assert
    expect(apiMock.abrir).not.toHaveBeenCalled();
    expect(apiMock.retomar).not.toHaveBeenCalled();
    expect(fixture.componentInstance.preparando()).toBe(true);
    expect(fixture.nativeElement.querySelector('app-painel-filtros')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Monte sua sessão');
    expect(fixture.nativeElement.querySelector('app-questao-cartao')).toBeNull();
  });

  it('abre a sessão com o tamanho, o modo e os filtros escolhidos no preparo', () => {
    // Arrange
    const apiMock = { abrir: vi.fn(() => of(sessao('PROVA'))), retomar: vi.fn() };
    const { fixture, http } = montar({}, apiMock, { preparar: false });
    fixture.detectChanges();
    flushCatalogo(http);
    const requisicao = {
      tamanho: 30,
      modo: 'PROVA' as ModoSessao,
      filtro: { disciplinaId: 'd1', assuntoId: null },
    };

    // Act
    fixture.componentInstance.iniciar(requisicao);
    fixture.detectChanges();

    // Assert
    expect(apiMock.abrir).toHaveBeenCalledWith(requisicao);
    expect(fixture.componentInstance.preparando()).toBe(false);
    expect(fixture.componentInstance.store.sessao()?.sessaoId).toBe('s1');
    expect(fixture.nativeElement.querySelector('app-questao-cartao')).not.toBeNull();
  });

  it('falha ao abrir a série mostra o motivo da API e volta ao painel de preparo', () => {
    // Arrange
    const apiMock = { abrir: vi.fn(() => of(sessao('FOCO'))), retomar: vi.fn() };
    const { fixture, http } = montar({}, apiMock, { preparar: false });
    fixture.detectChanges();
    flushCatalogo(http);
    fixture.componentInstance.iniciar({ tamanho: 10, modo: 'FOCO' });
    fixture.detectChanges();
    apiMock.abrir.mockReturnValue(
      throwError(() => ({
        error: {
          code: 'SERIE_VAZIA',
          message: 'Amplie a busca e tente de novo.',
          fieldErrors: null,
        },
      })),
    );

    // Act
    fixture.componentInstance.iniciar({ tamanho: 10, modo: 'FOCO', filtro: { bancaId: 'b1' } });
    fixture.detectChanges();
    flushCatalogo(http);
    fixture.detectChanges();

    // Assert
    expect(fixture.componentInstance.erro()).toBe('Amplie a busca e tente de novo.');
    expect(fixture.componentInstance.preparando()).toBe(true);
    expect(fixture.componentInstance.store.sessao()).toBeNull();
    expect(fixture.nativeElement.querySelector('app-questao-cartao')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Amplie a busca e tente de novo.');
  });

  it('falha ao aplicar os filtros laterais preserva a sessão em curso', () => {
    // Arrange
    const apiMock = { abrir: vi.fn(() => of(sessao('CONTEXTO'))), retomar: vi.fn() };
    const { fixture, http } = montar({ modo: 'CONTEXTO' }, apiMock, { preparar: false });
    fixture.detectChanges();
    flushCatalogo(http);
    fixture.componentInstance.iniciar({ tamanho: 10, modo: 'CONTEXTO' });
    fixture.detectChanges();
    flushCatalogo(http);
    apiMock.abrir.mockReturnValue(
      throwError(() => ({
        error: {
          code: 'SERIE_VAZIA',
          message: 'Amplie a busca e tente de novo.',
          fieldErrors: null,
        },
      })),
    );

    // Act
    fixture.componentInstance.aplicarFiltrosLaterais({ tamanho: 10, filtro: { bancaId: 'b1' } });
    fixture.detectChanges();

    // Assert
    expect(fixture.componentInstance.erro()).toBe('Amplie a busca e tente de novo.');
    expect(fixture.componentInstance.preparando()).toBe(false);
    expect(fixture.componentInstance.store.sessao()?.sessaoId).toBe('s1');
    expect(fixture.nativeElement.querySelector('app-questao-cartao')).not.toBeNull();
  });

  it('o preparo herda o modo vindo de ?modo= na rota', () => {
    // Arrange
    const apiMock = { abrir: vi.fn(), retomar: vi.fn() };

    // Act
    const { fixture, http } = montar({ modo: 'CONTEXTO' }, apiMock, { preparar: false });
    fixture.detectChanges();
    flushCatalogo(http);

    // Assert
    expect(fixture.componentInstance.modoInicial()).toBe('CONTEXTO');
  });

  it('refazer a série reaproveita o preparo da sessão anterior', () => {
    // Arrange
    const apiMock = { abrir: vi.fn(() => of(sessao('FOCO'))), retomar: vi.fn() };
    const { fixture, http } = montar({}, apiMock, { preparar: false });
    fixture.detectChanges();
    flushCatalogo(http);
    const requisicao = { tamanho: 50, modo: 'FOCO' as ModoSessao, filtro: { bancaId: 'b1' } };
    fixture.componentInstance.iniciar(requisicao);

    // Act
    fixture.componentInstance.reiniciar();

    // Assert
    expect(apiMock.abrir).toHaveBeenNthCalledWith(2, requisicao);
  });

  it('retoma uma sessão existente quando há ?sessao= na rota', () => {
    // Arrange
    const apiMock = { abrir: vi.fn(), retomarSessao: vi.fn(() => of(sessao('FOCO'))) };

    // Act
    const { fixture } = montar({ sessao: 's1' }, apiMock);
    fixture.detectChanges();

    // Assert
    expect(apiMock.retomarSessao).toHaveBeenCalledWith('s1');
    expect(apiMock.abrir).not.toHaveBeenCalled();
  });

  it('responder() chama a API e atualiza o store fora do Modo Prova', () => {
    // Arrange
    const respostaRegistrada: RespostaRegistrada = {
      questaoId: 'q1',
      marcada: 'A',
      correta: true,
      gabarito: 'A',
      comentario: null,
      estatistica: null,
      tempoMs: 5000,
      respondidaEm: '2026-09-03T10:00:05Z',
    };
    const apiMock = {
      abrir: vi.fn(() => of(sessao('FOCO'))),
      responder: vi.fn(() => of(respostaRegistrada)),
    };
    const { fixture } = montar({}, apiMock);
    fixture.detectChanges();
    fixture.componentInstance.selecionar('A');

    // Act
    fixture.componentInstance.responder();

    // Assert
    expect(apiMock.responder).toHaveBeenCalledWith(
      's1',
      expect.objectContaining({ questaoId: 'q1', marcada: 'A' }),
    );
    expect(fixture.componentInstance.store.respostaDa('q1')?.correta).toBe(true);
  });

  it('responder() no Modo Prova não chama a API de resposta e avança marcando localmente', () => {
    // Arrange
    const apiMock = { abrir: vi.fn(() => of(sessao('PROVA'))), responder: vi.fn() };
    const { fixture } = montar({ modo: 'PROVA' }, apiMock);
    fixture.detectChanges();
    fixture.componentInstance.selecionar('A');

    // Act
    fixture.componentInstance.responder();

    // Assert
    expect(apiMock.responder).not.toHaveBeenCalled();
    expect(fixture.componentInstance.store.alternativaMarcada('q1')).toBe('A');
    expect(fixture.componentInstance.store.indice()).toBe(1);
  });

  it('finalizar() chama a API de finalização da sessão e seta a correção', () => {
    // Arrange
    const correcao = {
      sessaoId: 's1',
      pctAcerto: 100,
      acertos: 2,
      erros: 0,
      naoRespondidas: 0,
      total: 2,
      tempoTotalSegundos: 60,
      finalizadaEm: '2026-09-03T10:05:00Z',
      itens: [],
    };
    const apiMock = {
      abrir: vi.fn(() => of(sessao('PROVA'))),
      finalizar: vi.fn(() => of(correcao)),
    };
    const { fixture } = montar({ modo: 'PROVA' }, apiMock);
    fixture.detectChanges();
    const cronometro = TestBed.inject(Cronometro);
    const pararEspiao = vi.spyOn(cronometro, 'parar');

    // Act
    fixture.componentInstance.finalizar();

    // Assert
    expect(apiMock.finalizar).toHaveBeenCalledWith(
      's1',
      expect.objectContaining({ respostas: [] }),
    );
    expect(fixture.componentInstance.correcao()).toEqual(correcao);
    expect(pararEspiao).toHaveBeenCalledOnce();
  });

  it('finalizar() com ?simulado= e ?tentativa= chama a API de simulado e navega ao resultado', () => {
    // Arrange
    const apiMock = { abrir: vi.fn(() => of(sessao('PROVA'))), finalizar: vi.fn() };
    const { fixture, router, simuladoApiMock } = montar(
      { modo: 'PROVA', simulado: 'sim1', tentativa: 't1' },
      apiMock,
    );
    fixture.detectChanges();

    // Act
    fixture.componentInstance.finalizar();

    // Assert
    expect(simuladoApiMock.finalizarTentativa).toHaveBeenCalledWith(
      'sim1',
      't1',
      expect.any(Object),
    );
    expect(apiMock.finalizar).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/simulados', 'sim1', 'resultado'], {
      queryParams: { tentativa: 't1' },
    });
  });

  it('trocarModo() chama a API e atualiza o modo do store', () => {
    // Arrange
    const apiMock = {
      abrir: vi.fn(() => of(sessao('FOCO'))),
      trocarModo: vi.fn(() => of(sessao('CONTEXTO'))),
    };
    const { fixture, http } = montar({}, apiMock);
    fixture.detectChanges();

    // Act
    fixture.componentInstance.trocarModo('CONTEXTO');
    fixture.detectChanges();
    flushCatalogo(http);

    // Assert
    expect(apiMock.trocarModo).toHaveBeenCalledWith('s1', 'CONTEXTO');
    expect(fixture.componentInstance.store.modo()).toBe('CONTEXTO');
  });

  it('não chama a API ao tentar trocar para o Modo Prova pelo seletor', () => {
    // Arrange
    const apiMock = { abrir: vi.fn(() => of(sessao('FOCO'))), trocarModo: vi.fn() };
    const { fixture } = montar({}, apiMock);
    fixture.detectChanges();

    // Act
    fixture.componentInstance.trocarModo('PROVA');

    // Assert
    expect(apiMock.trocarModo).not.toHaveBeenCalled();
  });

  it('exibe o painel de filtros lateral somente quando o modo é CONTEXTO', () => {
    // Arrange
    const apiMock = { abrir: vi.fn(() => of(sessao('CONTEXTO'))) };
    const { fixture, http } = montar({ modo: 'CONTEXTO' }, apiMock);

    // Act
    fixture.detectChanges();
    flushCatalogo(http);
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.querySelector('app-painel-filtros')).not.toBeNull();
  });

  it('não exibe o painel de filtros lateral no Modo Foco', () => {
    // Arrange
    const apiMock = { abrir: vi.fn(() => of(sessao('FOCO'))) };
    const { fixture } = montar({}, apiMock);

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.querySelector('app-painel-filtros')).toBeNull();
  });

  it('responder() não reenvia a resposta de uma questão já respondida e apenas avança', () => {
    // Arrange
    const respondida: RespostaRegistrada = {
      questaoId: 'q1',
      marcada: 'A',
      correta: false,
      gabarito: 'B',
      comentario: null,
      estatistica: null,
      tempoMs: 5000,
      respondidaEm: '2026-09-03T10:00:05Z',
    };
    const apiMock = {
      abrir: vi.fn(() => of(sessao('FOCO', [respondida]))),
      responder: vi.fn(),
    };
    const { fixture } = montar({}, apiMock);
    fixture.detectChanges();

    // Act
    fixture.componentInstance.responder();

    // Assert
    expect(apiMock.responder).not.toHaveBeenCalled();
    expect(fixture.componentInstance.erro()).toBeNull();
    expect(fixture.componentInstance.store.indice()).toBe(1);
  });

  it('finalizar() fora do Modo Prova não envia respostas e para o cronômetro', () => {
    // Arrange
    const correcao = {
      sessaoId: 's1',
      pctAcerto: 50,
      acertos: 1,
      erros: 1,
      naoRespondidas: 0,
      total: 2,
      tempoTotalSegundos: 60,
      finalizadaEm: '2026-09-03T10:05:00Z',
      itens: [],
    };
    const respondida: RespostaRegistrada = {
      questaoId: 'q1',
      marcada: 'A',
      correta: true,
      gabarito: 'A',
      comentario: null,
      estatistica: null,
      tempoMs: 5000,
      respondidaEm: '2026-09-03T10:00:05Z',
    };
    const apiMock = {
      abrir: vi.fn(() => of(sessao('FOCO', [respondida]))),
      finalizar: vi.fn(() => of(correcao)),
    };
    const { fixture } = montar({}, apiMock);
    fixture.detectChanges();
    const pararEspiao = vi.spyOn(TestBed.inject(Cronometro), 'parar');

    // Act
    fixture.componentInstance.finalizar();

    // Assert
    expect(apiMock.finalizar).toHaveBeenCalledWith(
      's1',
      expect.objectContaining({ respostas: [] }),
    );
    expect(fixture.componentInstance.correcao()).toEqual(correcao);
    expect(pararEspiao).toHaveBeenCalledOnce();
  });

  it('ngOnDestroy pausa e para o cronômetro da própria sessão e limpa o store ao sair sem finalizar', () => {
    // Arrange
    const apiMock = { abrir: vi.fn(() => of(sessao('FOCO'))) };
    const { fixture } = montar({}, apiMock);
    fixture.detectChanges();
    const cronometro = TestBed.inject(Cronometro);
    const pausarEspiao = vi.spyOn(cronometro, 'pausar');
    const pararEspiao = vi.spyOn(cronometro, 'parar');

    // Act
    fixture.destroy();

    // Assert
    expect(pausarEspiao).toHaveBeenCalledOnce();
    expect(pararEspiao).toHaveBeenCalledWith('s1');
    expect(fixture.componentInstance.store.sessao()).toBeNull();
  });

  it('ngOnDestroy não pausa novamente uma sessão já finalizada', () => {
    // Arrange
    const correcao = {
      sessaoId: 's1',
      pctAcerto: 100,
      acertos: 2,
      erros: 0,
      naoRespondidas: 0,
      total: 2,
      tempoTotalSegundos: 60,
      finalizadaEm: '2026-09-03T10:05:00Z',
      itens: [],
    };
    const apiMock = {
      abrir: vi.fn(() => of(sessao('PROVA'))),
      finalizar: vi.fn(() => of(correcao)),
    };
    const { fixture } = montar({ modo: 'PROVA' }, apiMock);
    fixture.detectChanges();
    fixture.componentInstance.finalizar();
    const cronometro = TestBed.inject(Cronometro);
    const pausarEspiao = vi.spyOn(cronometro, 'pausar');

    // Act
    fixture.destroy();

    // Assert
    expect(pausarEspiao).not.toHaveBeenCalled();
  });

  it('pedirFinalizacao() abre o diálogo de confirmação e cancelar o fecha sem finalizar', () => {
    // Arrange
    const apiMock = { abrir: vi.fn(() => of(sessao('FOCO'))), finalizar: vi.fn() };
    const { fixture } = montar({}, apiMock);
    fixture.detectChanges();

    // Act
    fixture.componentInstance.pedirFinalizacao();
    fixture.detectChanges();
    const comDialogo = fixture.nativeElement.querySelector('app-confirmacao-dialog');
    fixture.componentInstance.cancelarFinalizacao();
    fixture.detectChanges();

    // Assert
    expect(comDialogo).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-confirmacao-dialog')).toBeNull();
    expect(apiMock.finalizar).not.toHaveBeenCalled();
  });

  it('selecionar() não troca a alternativa de uma questão já respondida fora do Modo Prova', () => {
    // Arrange
    const respondida: RespostaRegistrada = {
      questaoId: 'q1',
      marcada: 'A',
      correta: false,
      gabarito: 'B',
      comentario: null,
      estatistica: null,
      tempoMs: 5000,
      respondidaEm: '2026-09-03T10:00:05Z',
    };
    const apiMock = { abrir: vi.fn(() => of(sessao('FOCO', [respondida]))), responder: vi.fn() };
    const { fixture } = montar({}, apiMock);
    fixture.detectChanges();

    // Act
    fixture.componentInstance.selecionar('B');

    // Assert
    expect(fixture.componentInstance.selecionada()).toBe('A');
  });
});
