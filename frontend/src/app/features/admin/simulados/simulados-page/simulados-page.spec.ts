import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CatalogoApi } from '../../../../core/api/catalogo-api';
import { SimuladoApi } from '../../../../core/api/simulado-api';
import { SimuladosPage } from './simulados-page';

describe('SimuladosPage', () => {
  let page: SimuladosPage;
  let listarMock: ReturnType<typeof vi.fn>;
  function montar(
    listarRetorno = of({ content: [], page: 0, size: 20, totalElements: 0 }),
  ): SimuladosPage {
    TestBed.resetTestingModule();
    listarMock = vi.fn().mockReturnValue(listarRetorno);
    TestBed.configureTestingModule({
      imports: [SimuladosPage],
      providers: [
        { provide: SimuladoApi, useValue: { listar: listarMock } },
        { provide: CatalogoApi, useValue: { bancas: () => of([]) } },
        { provide: ActivatedRoute, useValue: {} },
      ],
    });
    return TestBed.createComponent(SimuladosPage).componentInstance;
  }
  it('converte o estado derivado em rótulo textual', () => {
    page = montar();
    expect(page.statusLabel('DISPONIVEL')).toBe('Disponível');
    expect(page.statusLabel('RASCUNHO')).toBe('Rascunho');
    expect(page.statusLabel('AGENDADO')).toBe('Agendado');
    expect(page.statusLabel('ENCERRADO')).toBe('Encerrado');
  });
  it('formata duração em minutos e horas', () => {
    page = montar();
    expect(page.duracao(1800)).toBe('30 min');
    expect(page.duracao(3600)).toBe('1 h');
    expect(page.duracao(7200)).toBe('2 h');
  });
  it('avança e retorna a página sem ultrapassar os limites', () => {
    page = montar();
    page['pagina'].set({ content: [], page: 0, size: 20, totalElements: 40 });
    page.mudarPagina(1);
    expect(page['page']).toBe(1);
    page.mudarPagina(1);
    expect(page['page']).toBe(1);
    page.mudarPagina(-1);
    expect(page['page']).toBe(0);
  });
  it('carrega a primeira página ao aplicar o filtro', async () => {
    page = montar();
    page['page'] = 2;
    page.aplicar();
    expect(page['page']).toBe(0);
    await page.carregar();
    expect(listarMock).toHaveBeenCalled();
    expect(page['pagina']().totalElements).toBe(0);
  });
  it('exibe o erro da API quando a listagem falha', async () => {
    page = montar(throwError(() => ({ error: { code: 'ERRO', message: 'Falha ao listar.' } })));
    await page.carregar();
    expect(page['erro']()).toBe('Falha ao listar.');
  });
  it('impede página negativa', () => {
    page = montar();
    page['pagina'].set({ content: [], page: 0, size: 20, totalElements: 40 });
    page.mudarPagina(-1);
    expect(page['page']).toBe(0);
  });
  it('renderiza a linha do simulado com rótulo de estado textual', async () => {
    TestBed.resetTestingModule();
    const simulado = {
      id: 's1',
      codigo: 'SM-001',
      nome: 'Simulado TRF',
      banca: 'Cebraspe',
      numeroQuestoes: 80,
      duracaoSegundos: 3600,
      estado: 'DISPONIVEL',
      janelaAbertura: null,
      janelaEncerramento: null,
      tentativasConcluidas: 3,
      composicaoFechada: true,
    };
    TestBed.configureTestingModule({
      imports: [SimuladosPage],
      providers: [
        {
          provide: SimuladoApi,
          useValue: {
            listar: () => of({ content: [simulado], page: 0, size: 20, totalElements: 1 }),
          },
        },
        { provide: CatalogoApi, useValue: { bancas: () => of([]) } },
        { provide: ActivatedRoute, useValue: {} },
      ],
    });
    const fixture = TestBed.createComponent(SimuladosPage);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('SM-001');
    expect(fixture.nativeElement.textContent).toContain('Disponível');
    expect(fixture.nativeElement.textContent).toContain('Fechada');
    for (const botao of Array.from(fixture.nativeElement.querySelectorAll('button'))) {
      (botao as HTMLButtonElement).click();
    }
    await fixture.whenStable();
    expect(fixture.componentInstance['page']).toBe(0);
  });
});
