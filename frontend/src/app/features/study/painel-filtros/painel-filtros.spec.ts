import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ModoSessao } from '../../../core/api/modelos-estudo';
import { DisciplinaArvore, ItemCatalogo } from '../../../core/api/modelos-questao';
import { PainelFiltros } from './painel-filtros';

const disciplinas: DisciplinaArvore[] = [
  {
    id: 'd1',
    nome: 'Direito Constitucional',
    totalQuestoes: 10,
    totalPublicadas: 10,
    arquivado: false,
    assuntos: [
      {
        id: 'a1',
        nome: 'Controle de constitucionalidade',
        totalQuestoes: 5,
        totalPublicadas: 5,
        arquivado: false,
      },
    ],
  },
  {
    id: 'd2',
    nome: 'Direito Administrativo',
    totalQuestoes: 4,
    totalPublicadas: 4,
    arquivado: false,
    assuntos: [],
  },
];
const bancas: ItemCatalogo[] = [{ id: 'b1', nome: 'Cebraspe', sigla: 'CB' }];

function montar(entradas: { variante?: 'lateral' | 'preparo'; modoInicial?: ModoSessao } = {}) {
  TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
  const fixture = TestBed.createComponent(PainelFiltros);
  const http = TestBed.inject(HttpTestingController);
  if (entradas.variante) fixture.componentRef.setInput('variante', entradas.variante);
  if (entradas.modoInicial) fixture.componentRef.setInput('modoInicial', entradas.modoInicial);
  fixture.detectChanges();
  http.expectOne('/api/v1/admin/disciplinas?incluirArquivados=false').flush(disciplinas);
  http.expectOne('/api/v1/admin/bancas').flush(bancas);
  fixture.detectChanges();
  return { fixture, http };
}

describe('PainelFiltros', () => {
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
  });

  it('carrega disciplinas e bancas ao inicializar e popula os selects', () => {
    // Arrange & Act
    const { fixture } = montar();

    // Assert
    expect(fixture.componentInstance.disciplinas()).toHaveLength(2);
    expect(fixture.componentInstance.bancas()).toHaveLength(1);
    const opcoesDisciplina = fixture.nativeElement.querySelectorAll('#disciplina option');
    expect(opcoesDisciplina).toHaveLength(3);
    const opcoesBanca = fixture.nativeElement.querySelectorAll('#banca option');
    expect(opcoesBanca).toHaveLength(2);
  });

  it('deriva os assuntos a partir da disciplina selecionada', () => {
    // Arrange
    const { fixture } = montar();

    // Act
    fixture.componentInstance.selecionarDisciplina('d1');

    // Assert
    expect(fixture.componentInstance.assuntos()).toHaveLength(1);
    expect(fixture.componentInstance.assuntos()[0].nome).toBe('Controle de constitucionalidade');
  });

  it('esvazia os assuntos quando a disciplina não tem nenhum', () => {
    // Arrange
    const { fixture } = montar();

    // Act
    fixture.componentInstance.selecionarDisciplina('d2');

    // Assert
    expect(fixture.componentInstance.assuntos()).toHaveLength(0);
  });

  it('limpa o assunto selecionado ao trocar de disciplina', () => {
    // Arrange
    const { fixture } = montar();
    fixture.componentInstance.selecionarDisciplina('d1');
    fixture.componentInstance.assuntoId.set('a1');

    // Act
    fixture.componentInstance.selecionarDisciplina('d2');

    // Assert
    expect(fixture.componentInstance.assuntoId()).toBeNull();
  });

  it('emite aplicar com o filtro completo preenchido pelos valores escolhidos', () => {
    // Arrange
    const { fixture } = montar();
    const aplicar = vi.fn();
    fixture.componentInstance.aplicar.subscribe(aplicar);
    fixture.componentInstance.selecionarDisciplina('d1');
    fixture.componentInstance.assuntoId.set('a1');
    fixture.componentInstance.bancaId.set('b1');
    fixture.componentInstance.ano.set(2024);
    fixture.componentInstance.dificuldade.set('DIFICIL');
    fixture.componentInstance.tamanho.set(50);
    fixture.componentInstance.somenteIneditas.set(true);
    fixture.componentInstance.excluirAcertadas.set(true);

    // Act
    fixture.componentInstance.aplicarFiltros();

    // Assert
    expect(aplicar).toHaveBeenCalledWith({
      tamanho: 50,
      filtro: {
        disciplinaId: 'd1',
        assuntoId: 'a1',
        bancaId: 'b1',
        ano: 2024,
        dificuldade: 'DIFICIL',
        somenteIneditas: true,
        excluirAcertadas: true,
      },
    });
  });

  it('na variante lateral não oferece escolha de modo nem emite modo', () => {
    // Arrange
    const { fixture } = montar();
    const aplicar = vi.fn();
    fixture.componentInstance.aplicar.subscribe(aplicar);

    // Act
    fixture.componentInstance.aplicarFiltros();

    // Assert
    expect(fixture.nativeElement.querySelector('.seg')).toBeNull();
    expect(aplicar.mock.calls[0][0].modo).toBeUndefined();
  });

  it('na variante preparo mostra os três modos e parte do modo inicial', () => {
    // Arrange & Act
    const { fixture } = montar({ variante: 'preparo', modoInicial: 'CONTEXTO' });

    // Assert
    const rotulos = [...fixture.nativeElement.querySelectorAll('.seg-opt')].map(
      (opcao: HTMLElement) => opcao.textContent?.trim(),
    );
    expect(rotulos).toEqual(['Foco', 'Contexto', 'Modo Prova']);
    expect(fixture.componentInstance.modo()).toBe('CONTEXTO');
    expect(fixture.nativeElement.querySelector('button[type="submit"]').textContent).toContain(
      'Iniciar sessão',
    );
  });

  it('na variante preparo emite o modo escolhido junto do tamanho e dos filtros', () => {
    // Arrange
    const { fixture } = montar({ variante: 'preparo' });
    const aplicar = vi.fn();
    fixture.componentInstance.aplicar.subscribe(aplicar);
    fixture.componentInstance.modo.set('PROVA');
    fixture.componentInstance.tamanho.set(50);
    fixture.componentInstance.selecionarDisciplina('d1');

    // Act
    fixture.componentInstance.aplicarFiltros();

    // Assert
    expect(aplicar).toHaveBeenCalledWith(
      expect.objectContaining({
        modo: 'PROVA',
        tamanho: 50,
        filtro: expect.objectContaining({ disciplinaId: 'd1' }),
      }),
    );
  });

  it('submete o formulário chamando aplicarFiltros', () => {
    // Arrange
    const { fixture } = montar();
    const aplicar = vi.fn();
    fixture.componentInstance.aplicar.subscribe(aplicar);

    // Act
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));

    // Assert
    expect(aplicar).toHaveBeenCalledOnce();
  });
});
