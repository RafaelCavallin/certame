import { provideRouter } from '@angular/router';
import { convertToParamMap, ActivatedRoute } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { QuestoesFiltros } from './questoes-filtros';

describe('QuestoesFiltros', () => {
  it('TU-31: aplica filtros na URL e zera a página', async () => {
    const params = new BehaviorSubject(convertToParamMap({ page: '3' }));
    TestBed.configureTestingModule({
      providers: [
        QuestoesFiltros,
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: params.value }, queryParamMap: params },
        },
      ],
    });
    const filtros = TestBed.inject(QuestoesFiltros);
    filtros.termo.set('licitações');
    filtros.pagina.set(3);
    filtros.aplicar();
    expect(filtros.parametros()).toMatchObject({ q: 'licitações', page: 0 });
  });
  it('TU-32: restaura filtros, tamanho e página a partir dos query params', () => {
    const params = new BehaviorSubject(
      convertToParamMap({
        q: 'Q-10',
        disciplina: 'd1',
        banca: 'b1',
        status: 'PUBLICADA',
        page: '2',
        size: '10',
      }),
    );
    TestBed.configureTestingModule({
      providers: [
        QuestoesFiltros,
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: params.value }, queryParamMap: params },
        },
      ],
    });
    const filtros = TestBed.inject(QuestoesFiltros);
    expect(filtros.termo()).toBe('Q-10');
    expect(filtros.disciplinaId()).toBe('d1');
    expect(filtros.pagina()).toBe(2);
    expect(filtros.tamanho()).toBe(10);
  });

  it('TU-31b: limpa todos os filtros e volta para a primeira página', () => {
    const params = new BehaviorSubject(
      convertToParamMap({
        q: 'Q-10',
        disciplina: 'd1',
        banca: 'b1',
        status: 'PUBLICADA',
        page: '2',
      }),
    );
    TestBed.configureTestingModule({
      providers: [
        QuestoesFiltros,
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: params.value }, queryParamMap: params },
        },
      ],
    });
    const filtros = TestBed.inject(QuestoesFiltros);

    filtros.limpar();

    expect(filtros.parametros()).toEqual({
      q: null,
      disciplina: null,
      banca: null,
      status: null,
      page: 0,
      size: 20,
    });
  });

  it('TU-31c: nunca navega para uma página negativa', () => {
    const params = new BehaviorSubject(convertToParamMap({}));
    TestBed.configureTestingModule({
      providers: [
        QuestoesFiltros,
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: params.value }, queryParamMap: params },
        },
      ],
    });
    const filtros = TestBed.inject(QuestoesFiltros);

    filtros.irPara(-1);

    expect(filtros.pagina()).toBe(0);
  });
});
