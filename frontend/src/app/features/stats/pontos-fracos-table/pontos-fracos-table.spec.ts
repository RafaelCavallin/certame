import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PontosFracosTable } from './pontos-fracos-table';

describe('PontosFracosTable', () => {
  let fixture: ComponentFixture<PontosFracosTable>;
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [PontosFracosTable] }).compileComponents();
    fixture = TestBed.createComponent(PontosFracosTable);
  });
  it('explica o mínimo configurado quando a lista está vazia', () => {
    fixture.componentRef.setInput('dados', { pctMediaUsuario: 70, minimoRespostas: 25, itens: [] });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('25 respostas');
  });
  it('emite o assunto ao clicar em Treinar', () => {
    fixture.componentRef.setInput('dados', {
      pctMediaUsuario: 70,
      minimoRespostas: 20,
      itens: [
        {
          assuntoId: 'a1',
          assunto: 'Licitações',
          disciplinaId: 'd1',
          disciplina: 'Direito',
          volume: 25,
          pctAcerto: 40,
        },
      ],
    });
    fixture.detectChanges();
    const emitir = vi.fn();
    fixture.componentInstance.treinar.subscribe(emitir);
    fixture.nativeElement.querySelector('button').click();
    expect(emitir).toHaveBeenCalledWith('a1');
  });
});
