import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DisciplinasCard } from './disciplinas-card';

describe('DisciplinasCard', () => {
  it('exibe volume e percentual da disciplina', async () => {
    await TestBed.configureTestingModule({ imports: [DisciplinasCard] }).compileComponents();
    const fixture: ComponentFixture<DisciplinasCard> = TestBed.createComponent(DisciplinasCard);
    fixture.componentRef.setInput('disciplinas', [
      { disciplinaId: '1', disciplina: 'Direito', volume: 1482, pctAcerto: 74 },
    ]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('1.482 questões');
    expect(fixture.nativeElement.textContent).toContain('74%');
  });
});
