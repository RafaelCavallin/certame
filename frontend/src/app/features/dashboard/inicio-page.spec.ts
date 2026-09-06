import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { EstudoApi } from '../../core/api/estudo-api';
import { ResumoInicio } from '../../core/api/modelos-estudo';
import { InicioPage } from './inicio-page';

const resumoBase: ResumoInicio = {
  dia: '2026-09-03',
  questoesResolvidas: 12,
  pctAcerto: 75,
  tempoEstudoSegundos: 1800,
  tempoQuestoesSegundos: 1200,
  tempoFlashcardsSegundos: 600,
  sessoesRetomaveis: [],
};

function montar(api: { inicio: ReturnType<typeof vi.fn> }) {
  const router = { navigateByUrl: vi.fn(), navigate: vi.fn() };
  TestBed.configureTestingModule({
    imports: [InicioPage],
    providers: [
      { provide: EstudoApi, useValue: api },
      { provide: Router, useValue: router },
    ],
  });
  const fixture = TestBed.createComponent(InicioPage);
  fixture.detectChanges();
  return { fixture, router };
}

describe('InicioPage', () => {
  it('carrega o resumo do dia e exibe os dados', () => {
    // Arrange & Act
    const { fixture } = montar({ inicio: vi.fn(() => of(resumoBase)) });

    // Assert
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('12');
    expect(texto).toContain('75%');
    expect(texto).toContain('30 min');
  });

  it('exibe erro quando a busca do resumo falha', () => {
    // Arrange & Act
    const { fixture } = montar({ inicio: vi.fn(() => throwError(() => new Error('falhou'))) });

    // Assert
    expect(fixture.componentInstance.erro()).toBe('Não foi possível carregar seu resumo.');
  });

  it('exibe o cartão de retomar quando há sessão retomável', () => {
    // Arrange & Act
    const { fixture } = montar({
      inicio: vi.fn(() =>
        of({
          ...resumoBase,
          sessoesRetomaveis: [
            {
              sessaoId: 's1',
              modo: 'FOCO' as const,
              origem: 'LIVRE' as const,
              titulo: 'Direito Constitucional',
              indiceAtual: 2,
              total: 10,
              ultimaAtividadeEm: '2026-09-03T10:00:00Z',
            },
          ],
        }),
      ),
    });

    // Assert
    const texto = fixture.nativeElement.textContent as string;
    expect(fixture.nativeElement.querySelector('.retomar')).not.toBeNull();
    expect(texto).toContain('Direito Constitucional');
    expect(texto).toContain('questão 3 de 10');
  });

  it('exibe um cartão para cada sessão retomável', () => {
    // Arrange & Act
    const { fixture } = montar({
      inicio: vi.fn(() =>
        of({
          ...resumoBase,
          sessoesRetomaveis: [
            {
              sessaoId: 's1',
              modo: 'FOCO' as const,
              origem: 'LIVRE' as const,
              titulo: 'Direito Constitucional',
              indiceAtual: 2,
              total: 10,
              ultimaAtividadeEm: '2026-09-03T10:00:00Z',
            },
            {
              sessaoId: 's2',
              modo: 'CONTEXTO' as const,
              origem: 'CADERNO' as const,
              titulo: 'Direito Administrativo',
              indiceAtual: 0,
              total: 1,
              ultimaAtividadeEm: '2026-09-03T09:00:00Z',
            },
          ],
        }),
      ),
    });

    // Assert
    const texto = fixture.nativeElement.textContent as string;
    expect(fixture.nativeElement.querySelectorAll('.retomar').length).toBe(2);
    expect(texto).toContain('Direito Constitucional');
    expect(texto).toContain('Direito Administrativo');
  });

  it('não ultrapassa o total ao exibir sessão com todas as questões respondidas', () => {
    // Arrange & Act
    const { fixture } = montar({
      inicio: vi.fn(() =>
        of({
          ...resumoBase,
          sessoesRetomaveis: [
            {
              sessaoId: 's1',
              modo: 'FOCO' as const,
              origem: 'LIVRE' as const,
              titulo: null,
              indiceAtual: 1,
              total: 1,
              ultimaAtividadeEm: '2026-09-03T10:00:00Z',
            },
          ],
        }),
      ),
    });

    // Assert
    expect(fixture.nativeElement.textContent).toContain('questão 1 de 1');
  });

  it('esconde o cartão de retomar quando não há sessão retomável', () => {
    // Arrange & Act
    const { fixture } = montar({ inicio: vi.fn(() => of(resumoBase)) });

    // Assert
    expect(fixture.nativeElement.querySelector('.retomar')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Nenhuma questão hoje');
  });

  it('navega para a rota de estudo ao continuar a sessão retomável', () => {
    // Arrange
    const { fixture, router } = montar({ inicio: vi.fn(() => of(resumoBase)) });

    // Act
    fixture.componentInstance.continuar('s1');

    // Assert
    expect(router.navigate).toHaveBeenCalledWith(['/estudar'], { queryParams: { sessao: 's1' } });
  });

  it('navega para a rota de estudo ao iniciar uma nova sessão', () => {
    // Arrange
    const { fixture, router } = montar({ inicio: vi.fn(() => of(resumoBase)) });

    // Act
    fixture.componentInstance.iniciar();

    // Assert
    expect(router.navigateByUrl).toHaveBeenCalledWith('/estudar');
  });
});
