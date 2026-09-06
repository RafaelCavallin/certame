import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { RevisaoApi } from '../../../core/api/revisao-api';
import {
  CardRevisao,
  RespostaRevisao,
  RespostaSessaoRevisao,
  ResumoSessao,
} from '../../../core/api/modelos-revisao';
import { SessaoRevisaoPage } from './sessao-revisao';

function respostaAbertura(overrides: Partial<RespostaSessaoRevisao> = {}): RespostaSessaoRevisao {
  return {
    id: 's1',
    iniciadaEm: '2026-08-30T13:02:11Z',
    escopoRotulo: 'Todos os decks',
    total: 0,
    fila: [],
    ...overrides,
  };
}

function card(overrides: Partial<CardRevisao> = {}): CardRevisao {
  return {
    id: 'c1',
    frente: 'Frente',
    verso: 'Verso',
    deckId: 'd1',
    deckNome: 'Direito',
    disciplinaNome: null,
    assuntoNome: null,
    estado: 'REVISAO',
    memoria: { estabilidade: 1, dificuldade: 1, retencaoAtual: null, rotulo: 'novo' },
    previsoes: [],
    ...overrides,
  };
}

function respostaRegistro(overrides: Partial<RespostaRevisao> = {}): RespostaRevisao {
  return {
    flashcardId: 'c1',
    estado: 'REVISAO',
    estabilidade: 1,
    dificuldade: 1,
    intervaloMinutos: 10,
    intervaloRotulo: '10 min',
    proximaRevisaoEm: '2026-08-30T13:12:11Z',
    reenfileirar: false,
    card: null,
    ...overrides,
  };
}

function resumoSessao(overrides: Partial<ResumoSessao> = {}): ResumoSessao {
  return {
    revisados: 1,
    classificacoes: 1,
    distribuicao: { errei: 0, dificil: 0, bom: 1, facil: 0 },
    tempoMs: 1000,
    tempoRotulo: '1 min',
    proximosVencimentos: [],
    ...overrides,
  };
}

function montar(
  query: Record<string, string>,
  api: {
    abrir: ReturnType<typeof vi.fn>;
    registrar?: ReturnType<typeof vi.fn>;
    finalizar?: ReturnType<typeof vi.fn>;
  },
) {
  TestBed.configureTestingModule({
    imports: [SessaoRevisaoPage],
    providers: [
      provideRouter([]),
      {
        provide: RevisaoApi,
        useValue: {
          abrir: api.abrir,
          registrar: api.registrar ?? vi.fn(),
          finalizar: api.finalizar ?? vi.fn(),
          resumo: vi.fn(),
        },
      },
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { queryParamMap: { get: (chave: string) => query[chave] ?? null } } },
      },
    ],
  });
  return TestBed.createComponent(SessaoRevisaoPage);
}

describe('SessaoRevisaoPage', () => {
  it('abre a sessão com escopo TODOS quando a rota não traz deck', async () => {
    const api = { abrir: vi.fn(() => of(respostaAbertura())) };
    const fixture = montar({}, api);
    await fixture.whenStable();

    expect(api.abrir).toHaveBeenCalledWith({ escopo: 'TODOS', deckId: null, disciplinaId: null });
  });

  it('abre a sessão restrita ao deck informado na rota', async () => {
    const api = { abrir: vi.fn(() => of(respostaAbertura())) };
    const fixture = montar({ deck: 'd1' }, api);
    await fixture.whenStable();

    expect(api.abrir).toHaveBeenCalledWith({ escopo: 'DECK', deckId: 'd1', disciplinaId: null });
  });

  it('mostra a fila vazia sem cards vencidos nem novos', async () => {
    const api = { abrir: vi.fn(() => of(respostaAbertura({ total: 0, fila: [] }))) };
    const fixture = montar({}, api);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nada vencido por hoje');
  });

  it('exibe o cronômetro e o contador de classificações no cabeçalho', async () => {
    const api = { abrir: vi.fn(() => of(respostaAbertura({ total: 1, fila: [card()] }))) };
    const fixture = montar({}, api);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('00:00');
    expect(fixture.nativeElement.textContent).toContain('0 classificações · 1 restantes hoje');
  });

  it('avisa quando o card é reenfileirado e diz que ele pode ficar para a próxima', async () => {
    const api = {
      abrir: vi.fn(() => of(respostaAbertura({ total: 2, fila: [card(), card({ id: 'c2' })] }))),
      registrar: vi.fn(() =>
        of(respostaRegistro({ reenfileirar: true, card: card({ estado: 'REAPRENDENDO' }) })),
      ),
    };
    const fixture = montar({}, api);
    await fixture.whenStable();
    fixture.componentInstance.store.revelar();
    await fixture.componentInstance.store.classificar('ERREI');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Classificação registrada — este card só volta daqui a 10 min. Se a sessão acabar antes, ele fica para a próxima.',
    );
    expect(fixture.nativeElement.textContent).toContain('1 classificação · 2 restantes hoje');
  });

  it('no resumo, avisa que o card adiado volta na próxima sessão', async () => {
    const api = {
      abrir: vi.fn(() => of(respostaAbertura({ total: 2, fila: [card(), card({ id: 'c2' })] }))),
      registrar: vi
        .fn()
        .mockReturnValueOnce(
          of(
            respostaRegistro({
              reenfileirar: true,
              card: card({ estado: 'REAPRENDENDO' }),
              proximaRevisaoEm: new Date(Date.now() + 10 * 60_000).toISOString(),
            }),
          ),
        )
        .mockReturnValueOnce(of(respostaRegistro({ flashcardId: 'c2' }))),
      finalizar: vi.fn(() => of(resumoSessao())),
    };
    const fixture = montar({}, api);
    await fixture.whenStable();
    fixture.componentInstance.store.revelar();
    await fixture.componentInstance.store.classificar('ERREI');
    fixture.componentInstance.store.revelar();
    await fixture.componentInstance.store.classificar('FACIL');
    fixture.detectChanges();

    expect(api.finalizar).toHaveBeenCalledWith('s1');
    expect(fixture.nativeElement.textContent).toContain(
      '1 card ainda não venceu — ele volta na próxima sessão, sem espera aqui.',
    );
  });

  it('abre a sessão com escopo SEM_DECK quando a rota traz deck=sem-deck', async () => {
    const api = { abrir: vi.fn(() => of(respostaAbertura())) };
    const fixture = montar({ deck: 'sem-deck' }, api);
    await fixture.whenStable();

    expect(api.abrir).toHaveBeenCalledWith({
      escopo: 'SEM_DECK',
      deckId: null,
      disciplinaId: null,
    });
  });

  it('mostra "Carregando fila…" enquanto a abertura está pendente', async () => {
    const pendente = new Subject<RespostaSessaoRevisao>();
    const api = { abrir: vi.fn(() => pendente.asObservable()) };
    const fixture = montar({}, api);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Carregando fila…');
    pendente.next(respostaAbertura());
    pendente.complete();
  });

  it('clicar em "Ver todos os cards" na fila vazia aciona voltar()', async () => {
    const api = { abrir: vi.fn(() => of(respostaAbertura({ total: 0, fila: [] }))) };
    const fixture = montar({}, api);
    await fixture.whenStable();
    fixture.detectChanges();

    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigateByUrl');
    const botao: HTMLButtonElement = fixture.nativeElement.querySelector('app-fila-vazia button');
    botao.click();

    expect(navegar).toHaveBeenCalledWith('/flashcards');
  });

  it('resumo da sessão: "Revisar de novo" reinicia e "Voltar" navega', async () => {
    const registrar = vi.fn(() => of(respostaRegistro({ reenfileirar: false, card: null })));
    const finalizar = vi.fn(() => of(resumoSessao()));
    const api = {
      abrir: vi.fn(() => of(respostaAbertura({ total: 1, fila: [card()] }))),
      registrar,
      finalizar,
    };
    const fixture = montar({}, api);
    const componente = fixture.componentInstance;
    await fixture.whenStable();
    componente.store.revelar();
    await componente.store.classificar('BOM');
    fixture.detectChanges();

    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigateByUrl');
    const botaoVoltar: HTMLButtonElement = fixture.nativeElement.querySelector(
      'app-resumo-revisao .btn-secondary',
    );
    botaoVoltar.click();
    expect(navegar).toHaveBeenCalledWith('/flashcards');

    const botaoReiniciar: HTMLButtonElement = fixture.nativeElement.querySelector(
      'app-resumo-revisao .btn-primary',
    );
    botaoReiniciar.click();
    await fixture.whenStable();
    expect(api.abrir).toHaveBeenCalledTimes(2);
  });

  it('notas de revisão: clicar em uma nota classifica o card', async () => {
    const registrar = vi.fn(() => of(respostaRegistro({ reenfileirar: false, card: null })));
    const finalizar = vi.fn(() => of(resumoSessao()));
    const api = {
      abrir: vi.fn(() =>
        of(
          respostaAbertura({
            total: 1,
            fila: [card({ previsoes: [{ nota: 'BOM', minutos: 10, rotulo: '10 min' }] })],
          }),
        ),
      ),
      registrar,
      finalizar,
    };
    const fixture = montar({}, api);
    const componente = fixture.componentInstance;
    await fixture.whenStable();
    componente.store.revelar();
    fixture.detectChanges();

    const botaoNota: HTMLButtonElement = fixture.nativeElement.querySelector(
      'app-notas-revisao button',
    );
    botaoNota.click();
    await fixture.whenStable();

    expect(registrar).toHaveBeenCalledWith(
      's1',
      expect.objectContaining({ flashcardId: 'c1', nota: 'BOM' }),
    );
  });

  it('a tecla espaço revela o card atual', async () => {
    const api = { abrir: vi.fn(() => of(respostaAbertura({ total: 1, fila: [card()] }))) };
    const fixture = montar({}, api);
    const componente = fixture.componentInstance;
    await fixture.whenStable();
    fixture.detectChanges();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: ' ', code: 'Space' }));

    expect(componente.store.revelado()).toBe(true);
  });

  it('exibe o card atual e o revela ao clicar em "Mostrar resposta"', async () => {
    const api = { abrir: vi.fn(() => of(respostaAbertura({ total: 1, fila: [card()] }))) };
    const fixture = montar({}, api);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Direito');
    const botaoRevelar: HTMLButtonElement = fixture.nativeElement.querySelector('.btn-primary');
    botaoRevelar.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-notas-revisao')).toBeTruthy();
  });

  it('classifica o card atual e finaliza a sessão quando a fila esvazia', async () => {
    const registrar = vi.fn(() => of(respostaRegistro({ reenfileirar: false, card: null })));
    const finalizar = vi.fn(() => of(resumoSessao()));
    const api = {
      abrir: vi.fn(() => of(respostaAbertura({ total: 1, fila: [card()] }))),
      registrar,
      finalizar,
    };
    const fixture = montar({}, api);
    const componente = fixture.componentInstance;
    await fixture.whenStable();

    componente.store.revelar();
    await (componente as unknown as { classificar: (n: 'BOM') => Promise<void> }).classificar(
      'BOM',
    );

    expect(registrar).toHaveBeenCalledWith(
      's1',
      expect.objectContaining({ flashcardId: 'c1', nota: 'BOM' }),
    );
    expect(finalizar).toHaveBeenCalledWith('s1');
    expect(componente.store.resumo()).toEqual(resumoSessao());
  });

  it('reinicia a sessão reabrindo a fila a partir da rota', async () => {
    const api = { abrir: vi.fn(() => of(respostaAbertura({ total: 0, fila: [] }))) };
    const fixture = montar({}, api);
    const componente = fixture.componentInstance;
    await fixture.whenStable();

    await (componente as unknown as { reiniciar: () => Promise<void> }).reiniciar();

    expect(api.abrir).toHaveBeenCalledTimes(2);
  });

  it('voltar() para o cronômetro e navega para /flashcards', async () => {
    const api = { abrir: vi.fn(() => of(respostaAbertura())) };
    const fixture = montar({}, api);
    const componente = fixture.componentInstance;
    await fixture.whenStable();

    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigateByUrl');
    const pararCronometro = vi.spyOn(componente.cronometro, 'parar');

    (componente as unknown as { voltar: () => void }).voltar();

    expect(pararCronometro).toHaveBeenCalled();
    expect(navegar).toHaveBeenCalledWith('/flashcards');
  });

  it('clicar em "Voltar para meus flashcards" aciona voltar()', async () => {
    const api = { abrir: vi.fn(() => of(respostaAbertura())) };
    const fixture = montar({}, api);
    await fixture.whenStable();
    fixture.detectChanges();

    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigateByUrl');
    const botaoVoltar: HTMLButtonElement = fixture.nativeElement.querySelector('.btn-ghost');
    botaoVoltar.click();

    expect(navegar).toHaveBeenCalledWith('/flashcards');
  });

  it('ngOnDestroy para o cronômetro', async () => {
    const api = { abrir: vi.fn(() => of(respostaAbertura())) };
    const fixture = montar({}, api);
    const componente = fixture.componentInstance;
    await fixture.whenStable();

    const pararCronometro = vi.spyOn(componente.cronometro, 'parar');
    fixture.destroy();

    expect(pararCronometro).toHaveBeenCalled();
  });
});
