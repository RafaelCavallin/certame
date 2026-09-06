import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { DesempenhoStore } from '../desempenho-store';
import { DesempenhoPage } from './desempenho-page';
import { EstudoApi } from '../../../core/api/estudo-api';
import {
  EvolucaoSemanal,
  PontosFracos,
  ResumoDesempenho,
} from '../../../core/api/modelos-desempenho';

describe('DesempenhoPage', () => {
  let fixture: ComponentFixture<DesempenhoPage>;
  const store = {
    resumo: signal<ResumoDesempenho | null>(null),
    evolucao: signal<EvolucaoSemanal | null>(null),
    disciplinas: signal([]),
    pontosFracos: signal<PontosFracos | null>(null),
    carregando: signal(false),
    erro: signal(null),
    possuiHistorico: signal(false),
    segmentosLinha: signal([]),
    serieTextual: signal([]),
    carregar: vi.fn(),
  };
  const router = { navigateByUrl: vi.fn(), navigate: vi.fn() };
  const estudoApi = { abrir: vi.fn(() => of({ sessaoId: 'sessao-1' })) };

  beforeEach(async () => {
    store.resumo.set(null);
    store.evolucao.set(null);
    store.pontosFracos.set(null);
    store.possuiHistorico.set(false);
    router.navigate.mockClear();
    estudoApi.abrir.mockClear();
    await TestBed.configureTestingModule({
      imports: [DesempenhoPage],
      providers: [
        { provide: DesempenhoStore, useValue: store },
        { provide: Router, useValue: router },
        { provide: EstudoApi, useValue: estudoApi },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(DesempenhoPage);
    fixture.detectChanges();
  });

  it('TU-41 renderiza o estado vazio sem histórico', () => {
    expect(fixture.nativeElement.textContent).toContain('Seu desempenho começa aqui');
    expect(fixture.nativeElement.querySelector('app-evolucao-chart')).toBeNull();
    expect(store.carregar).toHaveBeenCalled();
  });

  it('renderiza os painéis quando existe histórico', () => {
    store.resumo.set({
      totalRespostas: 10,
      acertos: 7,
      erros: 3,
      pctAproveitamento: 70,
      possuiHistorico: true,
    });
    store.possuiHistorico.set(true);
    store.evolucao.set({
      semanas: [{ semana: '2026-08-24', rotulo: '24/08', volume: 10, pctAcerto: 70 }],
      pctMediaPeriodo: 70,
      melhorSemana: null,
      volumeTotal: 10,
    });
    store.pontosFracos.set({ pctMediaUsuario: 70, minimoRespostas: 20, itens: [] });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-evolucao-chart')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('70%');
  });

  it('abre uma sessão restrita ao assunto e navega para ela ao treinar', async () => {
    await fixture.componentInstance['treinar']('assunto-1');
    expect(estudoApi.abrir).toHaveBeenCalledWith({
      tamanho: 10,
      modo: 'FOCO',
      filtro: { assuntoId: 'assunto-1' },
    });
    expect(router.navigate).toHaveBeenCalledWith(['/estudar'], {
      queryParams: { sessao: 'sessao-1' },
    });
  });
});
