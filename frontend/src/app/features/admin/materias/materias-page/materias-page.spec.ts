import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CatalogoApi } from '../../../../core/api/catalogo-api';
import { DisciplinaArvore } from '../../../../core/api/modelos-questao';
import { MateriasPage } from './materias-page';

describe('MateriasPage', () => {
  const arvore: DisciplinaArvore[] = [
    {
      id: 'd1',
      nome: 'Direito Constitucional',
      totalQuestoes: 10,
      totalPublicadas: 8,
      arquivado: false,
      assuntos: [
        {
          id: 'a1',
          nome: 'Controle de Constitucionalidade',
          totalQuestoes: 4,
          totalPublicadas: 3,
          arquivado: false,
        },
      ],
    },
  ];

  function montar(api: Partial<CatalogoApi> = {}): MateriasPage {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [MateriasPage],
      providers: [{ provide: CatalogoApi, useValue: { disciplinas: () => of(arvore), ...api } }],
    });
    return TestBed.createComponent(MateriasPage).componentInstance;
  }

  it('carrega a árvore de disciplinas e assuntos ao iniciar', () => {
    const page = montar();

    expect(page.arvore()).toEqual(arvore);
  });

  it('exibe o erro da API quando o carregamento da árvore falha', () => {
    const page = montar({
      disciplinas: () =>
        throwError(() => ({ error: { code: 'ERRO', message: 'Falha ao carregar.' } })),
    });

    expect(page.erro()).toBe('Falha ao carregar.');
  });

  it('abre o diálogo de criação de disciplina com o nome em branco', () => {
    const page = montar();

    page.abrir('disciplina');

    expect(page.dialog()).toEqual({ tipo: 'disciplina', id: undefined, disciplinaId: undefined });
    expect(page.nome()).toBe('');
  });

  it('abre o diálogo de edição preenchendo o nome atual da disciplina', () => {
    const page = montar();

    page.abrir('disciplina', 'd1');

    expect(page.nome()).toBe('Direito Constitucional');
    expect(page.dialog()).toEqual({ tipo: 'disciplina', id: 'd1', disciplinaId: undefined });
  });

  it('abre o diálogo de edição preenchendo o nome atual do assunto', () => {
    const page = montar();

    page.abrir('assunto', 'a1', 'd1');

    expect(page.nome()).toBe('Controle de Constitucionalidade');
    expect(page.dialog()).toEqual({ tipo: 'assunto', id: 'a1', disciplinaId: 'd1' });
  });

  it('cria uma nova disciplina, fecha o diálogo e recarrega a árvore', async () => {
    const criarDisciplina = vi.fn().mockReturnValue(of({}));
    const disciplinas = vi.fn().mockReturnValue(of(arvore));
    const page = montar({ criarDisciplina, disciplinas });
    page.abrir('disciplina');
    page.nome.set('Direito Administrativo');

    await page.salvar();

    expect(criarDisciplina).toHaveBeenCalledWith({ nome: 'Direito Administrativo' });
    expect(page.dialog()).toBeNull();
    expect(disciplinas).toHaveBeenCalledTimes(2);
  });

  it('renomeia uma disciplina existente informando o id', async () => {
    const renomearDisciplina = vi.fn().mockReturnValue(of({}));
    const page = montar({ renomearDisciplina });
    page.abrir('disciplina', 'd1');
    page.nome.set('Direito Constitucional Renomeado');

    await page.salvar();

    expect(renomearDisciplina).toHaveBeenCalledWith('d1', {
      nome: 'Direito Constitucional Renomeado',
    });
    expect(page.dialog()).toBeNull();
  });

  it('cria um novo assunto vinculado à disciplina informada', async () => {
    const criarAssunto = vi.fn().mockReturnValue(of({}));
    const page = montar({ criarAssunto });
    page.abrir('assunto', undefined, 'd1');
    page.nome.set('Controle Concentrado');

    await page.salvar();

    expect(criarAssunto).toHaveBeenCalledWith({ nome: 'Controle Concentrado', disciplinaId: 'd1' });
  });

  it('não salva quando o nome está em branco', async () => {
    const criarDisciplina = vi.fn().mockReturnValue(of({}));
    const page = montar({ criarDisciplina });
    page.abrir('disciplina');
    page.nome.set('   ');

    await page.salvar();

    expect(criarDisciplina).not.toHaveBeenCalled();
  });

  it('exibe o erro da API e mantém o diálogo aberto quando salvar falha', async () => {
    const criarDisciplina = vi.fn().mockReturnValue(
      throwError(() => ({
        error: { code: 'NOME_EM_USO', message: 'Já existe uma disciplina com este nome.' },
      })),
    );
    const page = montar({ criarDisciplina });
    page.abrir('disciplina');
    page.nome.set('Direito Constitucional');

    await page.salvar();

    expect(page.erro()).toBe('Já existe uma disciplina com este nome.');
  });

  it('exclui um assunto sem questões vinculadas e recarrega a árvore', async () => {
    const excluirAssunto = vi.fn().mockReturnValue(of(undefined));
    const disciplinas = vi.fn().mockReturnValue(of(arvore));
    const page = montar({ excluirAssunto, disciplinas });
    page.abrirExcluir('assunto', 'a1');

    await page.excluir();

    expect(excluirAssunto).toHaveBeenCalledWith('a1');
    expect(page.dialog()).toBeNull();
    expect(disciplinas).toHaveBeenCalledTimes(2);
  });

  it('trata o erro ASSUNTO_EM_USO oferecendo a opção de arquivar em vez de excluir', async () => {
    const excluirAssunto = vi.fn().mockReturnValue(
      throwError(() => ({
        error: { code: 'ASSUNTO_EM_USO', message: 'Este assunto tem 4 questões vinculadas.' },
      })),
    );
    const page = montar({ excluirAssunto });
    page.abrirExcluir('assunto', 'a1');

    await page.excluir();

    expect(page.recusa()).toBe('Este assunto tem 4 questões vinculadas.');
    expect(page.dialog()).toEqual({ tipo: 'assunto', id: 'a1', excluir: false, arquivar: true });
  });

  it('propaga como erro genérico um código diferente de ASSUNTO_EM_USO ao excluir disciplina', async () => {
    const excluirDisciplina = vi.fn().mockReturnValue(
      throwError(() => ({
        error: { code: 'DISCIPLINA_EM_USO', message: 'Esta disciplina tem questões vinculadas.' },
      })),
    );
    const page = montar({ excluirDisciplina });
    page.abrirExcluir('disciplina', 'd1');

    await page.excluir();

    expect(page.erro()).toBe('Esta disciplina tem questões vinculadas.');
    expect(page.recusa()).toBeNull();
  });

  it('arquiva o assunto recusado, limpa a recusa e fecha o diálogo', async () => {
    const arquivarAssunto = vi.fn().mockReturnValue(of(undefined));
    const disciplinas = vi.fn().mockReturnValue(of(arvore));
    const page = montar({ arquivarAssunto, disciplinas });
    page.recusa.set('Este assunto tem 4 questões vinculadas.');
    page.dialog.set({ tipo: 'assunto', id: 'a1', excluir: false, arquivar: true });

    await page.arquivar();

    expect(arquivarAssunto).toHaveBeenCalledWith('a1');
    expect(page.recusa()).toBeNull();
    expect(page.dialog()).toBeNull();
  });

  it('renderiza a árvore e permite criar uma disciplina pelo diálogo na tela', async () => {
    const criarDisciplina = vi.fn().mockReturnValue(of({}));
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [MateriasPage],
      providers: [
        { provide: CatalogoApi, useValue: { disciplinas: () => of(arvore), criarDisciplina } },
      ],
    });
    const fixture = TestBed.createComponent(MateriasPage);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Direito Constitucional');

    const botaoNovaDisciplina = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (b) => (b as HTMLButtonElement).textContent?.includes('Disciplina'),
    ) as HTMLButtonElement;
    botaoNovaDisciplina.click();
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input.input') as HTMLInputElement;
    input.value = 'Direito Administrativo';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const botaoSalvar = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (b) => (b as HTMLButtonElement).textContent?.trim() === 'Salvar',
    ) as HTMLButtonElement;
    botaoSalvar.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(criarDisciplina).toHaveBeenCalledWith({ nome: 'Direito Administrativo' });
  });

  it('cancela o diálogo de exclusão de assunto sem chamar a API', async () => {
    const excluirAssunto = vi.fn().mockReturnValue(of(undefined));
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [MateriasPage],
      providers: [
        { provide: CatalogoApi, useValue: { disciplinas: () => of(arvore), excluirAssunto } },
      ],
    });
    const fixture = TestBed.createComponent(MateriasPage);
    await fixture.whenStable();
    fixture.detectChanges();

    const botaoExcluirAssunto = fixture.nativeElement.querySelector(
      'button[title="Excluir assunto"]',
    ) as HTMLButtonElement;
    botaoExcluirAssunto.click();
    fixture.detectChanges();

    const botaoCancelar = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (b) => (b as HTMLButtonElement).textContent?.trim() === 'Cancelar',
    ) as HTMLButtonElement;
    botaoCancelar.click();
    fixture.detectChanges();

    expect(excluirAssunto).not.toHaveBeenCalled();
    expect(fixture.componentInstance.dialog()).toBeNull();
  });

  it('confirma a exclusão de assunto pela tela e recusa por questões vinculadas', async () => {
    const excluirAssunto = vi.fn().mockReturnValue(
      throwError(() => ({
        error: { code: 'ASSUNTO_EM_USO', message: 'Este assunto tem 4 questões vinculadas.' },
      })),
    );
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [MateriasPage],
      providers: [
        { provide: CatalogoApi, useValue: { disciplinas: () => of(arvore), excluirAssunto } },
      ],
    });
    const fixture = TestBed.createComponent(MateriasPage);
    await fixture.whenStable();
    fixture.detectChanges();

    const botaoExcluirAssunto = fixture.nativeElement.querySelector(
      'button[title="Excluir assunto"]',
    ) as HTMLButtonElement;
    botaoExcluirAssunto.click();
    fixture.detectChanges();

    const botaoConfirmar = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (b) => (b as HTMLButtonElement).textContent?.trim() === 'Confirmar',
    ) as HTMLButtonElement;
    botaoConfirmar.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(excluirAssunto).toHaveBeenCalledWith('a1');
    expect(fixture.nativeElement.textContent).toContain('Este assunto tem 4 questões vinculadas.');
  });
});
