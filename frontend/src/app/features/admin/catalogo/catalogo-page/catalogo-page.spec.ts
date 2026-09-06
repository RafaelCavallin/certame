import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CatalogoApi } from '../../../../core/api/catalogo-api';
import { ItemCatalogo } from '../../../../core/api/modelos-questao';
import { CatalogoPage } from './catalogo-page';

describe('CatalogoPage', () => {
  const bancas: ItemCatalogo[] = [{ id: 'b1', nome: 'Cebraspe', sigla: 'CBSP' }];
  const orgaos: ItemCatalogo[] = [{ id: 'o1', nome: 'TRF1', sigla: null }];

  function montar(api: Partial<CatalogoApi> = {}): CatalogoPage {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [CatalogoPage],
      providers: [
        {
          provide: CatalogoApi,
          useValue: { bancas: () => of(bancas), orgaos: () => of(orgaos), ...api },
        },
      ],
    });
    return TestBed.createComponent(CatalogoPage).componentInstance;
  }

  it('carrega a lista de bancas por padrão', () => {
    const page = montar();

    expect(page.aba()).toBe('banca');
    expect(page.itens()).toEqual(bancas);
  });

  it('troca para a aba de órgãos e recarrega a lista correspondente', () => {
    const page = montar();

    page.trocar('orgao');

    expect(page.aba()).toBe('orgao');
    expect(page.itens()).toEqual(orgaos);
  });

  it('limpa o formulário ao trocar de aba', () => {
    const page = montar();
    page.abrir(bancas[0]);

    page.trocar('orgao');

    expect(page.formAberto()).toBe(false);
    expect(page.editando()).toBeNull();
    expect(page.nome()).toBe('');
  });

  it('exibe o erro da API quando o carregamento falha', () => {
    const page = montar({
      bancas: () => throwError(() => ({ error: { message: 'Falha ao listar bancas.' } })),
    });

    expect(page.erro()).toBe('Falha ao listar bancas.');
  });

  it('abre o formulário em branco para criação', () => {
    const page = montar();

    page.abrir();

    expect(page.formAberto()).toBe(true);
    expect(page.editando()).toBeNull();
    expect(page.nome()).toBe('');
    expect(page.sigla()).toBe('');
  });

  it('abre o formulário preenchido para edição', () => {
    const page = montar();

    page.abrir(bancas[0]);

    expect(page.editando()).toBe('b1');
    expect(page.nome()).toBe('Cebraspe');
    expect(page.sigla()).toBe('CBSP');
  });

  it('recusa salvar com nome menor que 2 caracteres', async () => {
    const criarBanca = vi.fn().mockReturnValue(of({}));
    const page = montar({ criarBanca });
    page.abrir();
    page.nome.set('C');

    await page.salvar();

    expect(criarBanca).not.toHaveBeenCalled();
    expect(page.erro()).toBe('Informe um nome com ao menos 2 caracteres.');
  });

  it('cria uma nova banca, fecha o formulário e recarrega a lista', async () => {
    const criarBanca = vi.fn().mockReturnValue(of({}));
    const listarBancas = vi.fn().mockReturnValue(of(bancas));
    const page = montar({ criarBanca, bancas: listarBancas });
    page.abrir();
    page.nome.set('FGV');
    page.sigla.set('fgv');

    await page.salvar();

    expect(criarBanca).toHaveBeenCalledWith({ nome: 'FGV', sigla: 'fgv' });
    expect(page.formAberto()).toBe(false);
    expect(listarBancas).toHaveBeenCalledTimes(2);
  });

  it('atualiza uma banca existente quando editando um item', async () => {
    const atualizarBanca = vi.fn().mockReturnValue(of({}));
    const page = montar({ atualizarBanca });
    page.abrir(bancas[0]);
    page.nome.set('Cebraspe Renomeada');

    await page.salvar();

    expect(atualizarBanca).toHaveBeenCalledWith('b1', {
      nome: 'Cebraspe Renomeada',
      sigla: 'CBSP',
    });
  });

  it('cria um novo órgão quando a aba ativa é órgãos', async () => {
    const criarOrgao = vi.fn().mockReturnValue(of({}));
    const page = montar({ criarOrgao, orgaos: () => of(orgaos) });
    page.trocar('orgao');
    page.abrir();
    page.nome.set('TCU');

    await page.salvar();

    expect(criarOrgao).toHaveBeenCalledWith({ nome: 'TCU', sigla: null });
  });

  it('exibe o erro da API quando salvar falha por nome duplicado', async () => {
    const criarBanca = vi
      .fn()
      .mockReturnValue(
        throwError(() => ({ error: { message: 'Já existe uma banca com este nome.' } })),
      );
    const page = montar({ criarBanca });
    page.abrir();
    page.nome.set('Cebraspe');

    await page.salvar();

    expect(page.erro()).toBe('Já existe uma banca com este nome.');
  });

  it('abre o diálogo de confirmação de exclusão', () => {
    const page = montar();

    page.excluir('b1');

    expect(page.dialog()).toEqual({ id: 'b1' });
  });

  it('exclui a banca confirmada, fecha o diálogo e recarrega a lista', async () => {
    const excluirBanca = vi.fn().mockReturnValue(of(undefined));
    const listarBancas = vi.fn().mockReturnValue(of(bancas));
    const page = montar({ excluirBanca, bancas: listarBancas });
    page.excluir('b1');

    await page.confirmarExclusao();

    expect(excluirBanca).toHaveBeenCalledWith('b1');
    expect(page.dialog()).toBeNull();
    expect(listarBancas).toHaveBeenCalledTimes(2);
  });

  it('exclui o órgão confirmado quando a aba ativa é órgãos', async () => {
    const excluirOrgao = vi.fn().mockReturnValue(of(undefined));
    const page = montar({ excluirOrgao, orgaos: () => of(orgaos) });
    page.trocar('orgao');
    page.excluir('o1');

    await page.confirmarExclusao();

    expect(excluirOrgao).toHaveBeenCalledWith('o1');
  });

  it('fecha o diálogo e exibe o erro quando a exclusão é recusada por vínculo', async () => {
    const excluirBanca = vi.fn().mockReturnValue(
      throwError(() => ({
        error: { message: 'Este item tem 3 questões vinculadas e não pode ser excluído.' },
      })),
    );
    const page = montar({ excluirBanca });
    page.excluir('b1');

    await page.confirmarExclusao();

    expect(page.dialog()).toBeNull();
    expect(page.erro()).toBe('Este item tem 3 questões vinculadas e não pode ser excluído.');
  });

  it('renderiza a tela, troca de aba e cria um órgão pelo formulário exibido', async () => {
    const criarOrgao = vi.fn().mockReturnValue(of({}));
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [CatalogoPage],
      providers: [
        {
          provide: CatalogoApi,
          useValue: { bancas: () => of(bancas), orgaos: () => of(orgaos), criarOrgao },
        },
      ],
    });
    const fixture = TestBed.createComponent(CatalogoPage);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Cebraspe');

    const abaOrgaos = Array.from(fixture.nativeElement.querySelectorAll('button.seg-opt')).find(
      (b) => (b as HTMLButtonElement).textContent?.trim() === 'Órgãos',
    ) as HTMLButtonElement;
    abaOrgaos.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('TRF1');

    const botaoNovoItem = Array.from(fixture.nativeElement.querySelectorAll('button')).find((b) =>
      (b as HTMLButtonElement).textContent?.includes('Novo item'),
    ) as HTMLButtonElement;
    botaoNovoItem.click();
    fixture.detectChanges();

    const inputs = fixture.nativeElement.querySelectorAll('.dialog input.input');
    (inputs[0] as HTMLInputElement).value = 'TCU';
    (inputs[0] as HTMLInputElement).dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const botaoSalvar = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (b) => (b as HTMLButtonElement).textContent?.trim() === 'Salvar',
    ) as HTMLButtonElement;
    botaoSalvar.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(criarOrgao).toHaveBeenCalledWith({ nome: 'TCU', sigla: null });
  });

  it('abre a confirmação de exclusão pela tela, cancela e depois confirma', async () => {
    const excluirBanca = vi.fn().mockReturnValue(of(undefined));
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [CatalogoPage],
      providers: [
        {
          provide: CatalogoApi,
          useValue: { bancas: () => of(bancas), orgaos: () => of(orgaos), excluirBanca },
        },
      ],
    });
    const fixture = TestBed.createComponent(CatalogoPage);
    await fixture.whenStable();
    fixture.detectChanges();

    const botaoExcluir = fixture.nativeElement.querySelector('button.danger') as HTMLButtonElement;
    botaoExcluir.click();
    fixture.detectChanges();

    const botaoCancelar = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (b) => (b as HTMLButtonElement).textContent?.trim() === 'Cancelar',
    ) as HTMLButtonElement;
    botaoCancelar.click();
    fixture.detectChanges();
    expect(excluirBanca).not.toHaveBeenCalled();
    expect(fixture.componentInstance.dialog()).toBeNull();

    botaoExcluir.click();
    fixture.detectChanges();
    const botaoConfirmar = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (b) => (b as HTMLButtonElement).textContent?.trim() === 'Excluir',
    ) as HTMLButtonElement;
    botaoConfirmar.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(excluirBanca).toHaveBeenCalledWith('b1');
  });
});
