import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { SimuladoApi } from '../../../core/api/simulado-api';
import { SimuladoConcluido, SimuladoResumo } from '../../../core/models/modelos-simulado';
import { SimuladosPage } from './simulados-page';

const disponivel: SimuladoResumo = {
  id: '1',
  codigo: 'SM-001',
  nome: 'Disponível',
  banca: null,
  numeroQuestoes: 10,
  duracaoSegundos: 600,
  estado: 'DISPONIVEL',
  janelaAbertura: '',
  janelaEncerramento: '',
  tentativaEmAndamentoId: null,
};
const concluido: SimuladoConcluido = {
  id: '2',
  codigo: 'SM-002',
  nome: 'Concluído',
  banca: null,
  numeroQuestoes: 10,
  duracaoSegundos: 600,
  tentativaId: 't1',
  nota: 100,
  acertos: 10,
  erros: 0,
  naoRespondidas: 0,
  tempoTotalSegundos: 500,
  concluidoEm: '2026-08-30T10:00:00Z',
  podeRefazer: true,
};

describe('SimuladosPage estudante', () => {
  let api: {
    listarDoEstudante: ReturnType<typeof vi.fn>;
    iniciarTentativa: ReturnType<typeof vi.fn>;
  };
  let router: { navigate: ReturnType<typeof vi.fn> };

  function montarFixture() {
    TestBed.configureTestingModule({
      imports: [SimuladosPage],
      providers: [
        { provide: SimuladoApi, useValue: api },
        { provide: Router, useValue: router },
      ],
    });
    return TestBed.createComponent(SimuladosPage);
  }

  function montar() {
    return montarFixture().componentInstance;
  }

  beforeEach(() => {
    api = {
      listarDoEstudante: vi.fn(() =>
        of({ proximo: null, disponiveis: [], agendados: [], concluidos: [] }),
      ),
      iniciarTentativa: vi.fn(() =>
        of({
          tentativaId: 't1',
          simuladoId: '1',
          codigo: 'SM-001',
          tipo: 'VALIDA',
          sessao: { sessaoId: 'sess-1' },
        }),
      ),
    };
    router = { navigate: vi.fn(() => Promise.resolve(true)) };
  });

  it('carrega os três agrupamentos e o destaque', async () => {
    api.listarDoEstudante.mockReturnValue(
      of({ proximo: null, disponiveis: [disponivel], agendados: [], concluidos: [] }),
    );
    const page = montar();

    await page.carregar();

    expect(page['dados']().disponiveis).toHaveLength(1);
    expect(page['dados']().agendados).toEqual([]);
  });

  it('preserva estado vazio quando a API retorna listas vazias', async () => {
    const page = montar();

    await page.carregar();

    expect(page['dados']().concluidos).toEqual([]);
  });

  it('sinaliza erro quando o carregamento falha', async () => {
    api.listarDoEstudante.mockReturnValue(throwError(() => new Error('falhou')));
    const page = montar();

    await page.carregar();

    expect(page['erro']()).toBe('Não foi possível carregar os simulados.');
  });

  it('inicia a tentativa e navega para a tela de estudo', async () => {
    const page = montar();

    await page.iniciar(disponivel);

    expect(api.iniciarTentativa).toHaveBeenCalledWith('1');
    expect(router.navigate).toHaveBeenCalledWith(['/estudar'], {
      queryParams: { sessao: 'sess-1', simulado: '1', tentativa: 't1' },
    });
  });

  it('aguarda confirmação antes de navegar quando a tentativa é treino', async () => {
    api.iniciarTentativa.mockReturnValue(
      of({
        tentativaId: 't2',
        simuladoId: '2',
        codigo: 'SM-002',
        tipo: 'TREINO',
        sessao: { sessaoId: 'sess-2' },
      }),
    );
    const page = montar();

    await page.iniciar(concluido);

    expect(router.navigate).not.toHaveBeenCalled();
    expect(page['confirmacaoTreino']()).not.toBeNull();
  });

  it('cancela o início quando o usuário recusa a confirmação de treino', async () => {
    api.iniciarTentativa.mockReturnValue(
      of({
        tentativaId: 't2',
        simuladoId: '2',
        codigo: 'SM-002',
        tipo: 'TREINO',
        sessao: { sessaoId: 'sess-2' },
      }),
    );
    const page = montar();
    await page.iniciar(concluido);

    page.cancelarTreino();

    expect(page['confirmacaoTreino']()).toBeNull();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('navega ao confirmar o início de uma tentativa de treino', async () => {
    api.iniciarTentativa.mockReturnValue(
      of({
        tentativaId: 't2',
        simuladoId: '2',
        codigo: 'SM-002',
        tipo: 'TREINO',
        sessao: { sessaoId: 'sess-2' },
      }),
    );
    const page = montar();
    await page.iniciar(concluido);

    await page.confirmarTreino();

    expect(router.navigate).toHaveBeenCalledWith(['/estudar'], {
      queryParams: { sessao: 'sess-2', simulado: '2', tentativa: 't2' },
    });
  });

  it('sinaliza erro quando iniciar a tentativa falha', async () => {
    api.iniciarTentativa.mockReturnValue(throwError(() => new Error('falhou')));
    const page = montar();

    await page.iniciar(disponivel);

    expect(page['erro']()).toBe('Não foi possível iniciar o simulado.');
  });

  it('navega para o resultado ao revisar um simulado concluído', async () => {
    const page = montar();

    await page.revisar(concluido);

    expect(router.navigate).toHaveBeenCalledWith(['/simulados', '2', 'resultado'], {
      queryParams: { tentativa: 't1' },
    });
  });

  it('mostra as mensagens de lista vazia nos três grupos quando não há dados', async () => {
    const fixture = montarFixture();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhum simulado disponível no momento.');
    expect(fixture.nativeElement.textContent).toContain('Você não tem simulados agendados.');
    expect(fixture.nativeElement.textContent).toContain('Você ainda não concluiu nenhum simulado.');
  });

  it('mostra o destaque do próximo simulado e a mensagem de erro quando o carregamento falha', async () => {
    api.listarDoEstudante.mockReturnValue(throwError(() => new Error('falhou')));
    const fixture = montarFixture();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'Não foi possível carregar os simulados.',
    );
  });

  it('renderiza o card de destaque quando há um próximo simulado agendado', async () => {
    api.listarDoEstudante.mockReturnValue(
      of({
        proximo: {
          ...disponivel,
          estado: 'AGENDADO',
          janelaAbertura: new Date(Date.now() + 86400000).toISOString(),
        },
        disponiveis: [],
        agendados: [],
        concluidos: [],
      }),
    );
    const fixture = montarFixture();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Próximo simulado');
  });

  it('clica no botão Iniciar do card renderizado e delega para iniciar a tentativa', async () => {
    api.listarDoEstudante.mockReturnValue(
      of({ proximo: null, disponiveis: [disponivel], agendados: [], concluidos: [] }),
    );
    const fixture = montarFixture();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.nativeElement.querySelector('button.btn-primary').click();
    await fixture.whenStable();

    expect(api.iniciarTentativa).toHaveBeenCalledWith('1');
  });

  it('clica no botão Revisar do card concluído renderizado', async () => {
    api.listarDoEstudante.mockReturnValue(
      of({ proximo: null, disponiveis: [], agendados: [], concluidos: [concluido] }),
    );
    const fixture = montarFixture();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.nativeElement.querySelector('button.btn-secondary').click();
    await fixture.whenStable();
    expect(router.navigate).toHaveBeenCalledWith(['/simulados', '2', 'resultado'], {
      queryParams: { tentativa: 't1' },
    });
  });
});
