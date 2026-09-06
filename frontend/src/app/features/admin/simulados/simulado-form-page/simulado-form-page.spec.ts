import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CatalogoApi } from '../../../../core/api/catalogo-api';
import { SimuladoApi } from '../../../../core/api/simulado-api';
import { SimuladoDetalhe } from '../../../../core/models/modelos-simulado';
import { SimuladoFormPage } from './simulado-form-page';

const composicaoFechada = {
  itens: [],
  somaComposicao: 10,
  numeroQuestoes: 10,
  fechada: true,
  acervoSuficiente: true,
};
const composicaoAberta = {
  itens: [],
  somaComposicao: 0,
  numeroQuestoes: 10,
  fechada: false,
  acervoSuficiente: true,
};

const detalhe = (overrides: Partial<SimuladoDetalhe> = {}): SimuladoDetalhe => ({
  id: 'sim-1',
  codigo: 'SM-001',
  nome: 'Simulado teste',
  descricao: null,
  bancaId: null,
  numeroQuestoes: 10,
  duracaoSegundos: 600,
  janelaAbertura: null,
  janelaEncerramento: null,
  status: 'RASCUNHO',
  publicadoEm: null,
  composicao: composicaoAberta,
  ...overrides,
});

describe('SimuladoFormPage', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;
  let router: { navigate: ReturnType<typeof vi.fn> };

  async function montar(id: string | null) {
    router = { navigate: vi.fn(() => Promise.resolve(true)) };
    TestBed.configureTestingModule({
      imports: [SimuladoFormPage],
      providers: [
        { provide: SimuladoApi, useValue: api },
        { provide: CatalogoApi, useValue: { bancas: () => of([]), disciplinas: () => of([]) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => id } } } },
        { provide: Router, useValue: router },
      ],
    });
    const fixture = TestBed.createComponent(SimuladoFormPage);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  }

  beforeEach(() => {
    api = {
      criar: vi.fn(() => of(detalhe())),
      obter: vi.fn(() => of(detalhe())),
      atualizar: vi.fn(() => of(detalhe({ nome: 'Atualizado' }))),
      publicar: vi.fn(() => of(detalhe({ status: 'PUBLICADO', composicao: composicaoFechada }))),
      despublicar: vi.fn(() => of(detalhe())),
      composicao: vi.fn(() => of(composicaoAberta)),
      excluir: vi.fn(() => of(void 0)),
    };
  });

  it('inicia com publicação bloqueada sem composição fechada', async () => {
    const fixture = await montar(null);
    expect(fixture.componentInstance['detalhe']()).toBeNull();
  });

  it('exibe erro obrigatório no campo tocado', async () => {
    const fixture = await montar(null);
    fixture.componentInstance['formulario'].controls.nome.markAsTouched();
    expect(fixture.componentInstance.mensagem('nome')).toBe('Preencha este campo.');
  });

  it('carrega o simulado existente e preenche o formulário', async () => {
    const fixture = await montar('sim-1');
    expect(api['obter']).toHaveBeenCalledWith('sim-1');
    expect(fixture.componentInstance['formulario'].controls.nome.value).toBe('Simulado teste');
  });

  it('cria um novo simulado e navega para a página de edição', async () => {
    const fixture = await montar(null);
    fixture.componentInstance['formulario'].patchValue({ nome: 'Novo simulado' });
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(api['criar']).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/admin/simulados', 'sim-1']);
  });

  it('atualiza um simulado existente sem navegar', async () => {
    const fixture = await montar('sim-1');
    fixture.componentInstance['formulario'].patchValue({ nome: 'Nome alterado' });

    await fixture.componentInstance.salvar();

    expect(api['atualizar']).toHaveBeenCalledWith(
      'sim-1',
      expect.objectContaining({ nome: 'Nome alterado' }),
    );
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('não chama a API quando o formulário é inválido', async () => {
    const fixture = await montar(null);
    fixture.componentInstance['formulario'].patchValue({ nome: '' });

    await fixture.componentInstance.salvar();

    expect(api['criar']).not.toHaveBeenCalled();
  });

  it('mostra a mensagem de erro e os erros de campo quando salvar falha', async () => {
    api['criar'].mockReturnValue(
      throwError(() => ({
        error: {
          code: 'DADOS_INVALIDOS',
          message: 'Verifique os campos.',
          fieldErrors: { nome: 'Nome inválido.' },
        },
      })),
    );
    const fixture = await montar(null);
    fixture.componentInstance['formulario'].patchValue({ nome: 'Nome qualquer' });

    await fixture.componentInstance.salvar();

    expect(fixture.componentInstance['erro']()).toBe('Verifique os campos.');
    expect(fixture.componentInstance.mensagem('nome')).toBe('Nome inválido.');
  });

  it('não publica quando a composição não está fechada', async () => {
    const fixture = await montar('sim-1');

    await fixture.componentInstance.publicar();

    expect(api['publicar']).not.toHaveBeenCalled();
  });

  it('publica quando a composição está fechada', async () => {
    api['obter'].mockReturnValue(of(detalhe({ composicao: composicaoFechada })));
    const fixture = await montar('sim-1');

    await fixture.componentInstance.publicar();

    expect(api['publicar']).toHaveBeenCalledWith('sim-1');
    expect(fixture.componentInstance['detalhe']()?.status).toBe('PUBLICADO');
  });

  it('despublica o simulado', async () => {
    const fixture = await montar('sim-1');

    await fixture.componentInstance.despublicar();

    expect(api['despublicar']).toHaveBeenCalledWith('sim-1');
  });

  it('exclui o simulado após confirmação e navega para a lista', async () => {
    const fixture = await montar('sim-1');

    fixture.componentInstance.excluir();
    await fixture.componentInstance.confirmarExclusao();

    expect(api['excluir']).toHaveBeenCalledWith('sim-1');
    expect(router.navigate).toHaveBeenCalledWith(['/admin/simulados']);
  });

  it('não exclui o simulado quando a confirmação é recusada', async () => {
    const fixture = await montar('sim-1');

    fixture.componentInstance.excluir();
    fixture.componentInstance.cancelarExclusao();

    expect(api['excluir']).not.toHaveBeenCalled();
  });

  it('atualiza a composição exibida quando o editor emite um novo resumo', async () => {
    const fixture = await montar('sim-1');

    fixture.componentInstance.atualizouComposicao(composicaoFechada);

    expect(fixture.componentInstance['detalhe']()?.composicao).toBe(composicaoFechada);
  });

  it('exibe erro quando o carregamento do simulado falha', async () => {
    api['obter'].mockReturnValue(
      throwError(() => ({
        error: {
          code: 'SIMULADO_NAO_ENCONTRADO',
          message: 'Simulado não encontrado.',
          fieldErrors: null,
        },
      })),
    );
    const fixture = await montar('sim-1');

    expect(fixture.componentInstance['erro']()).toBe('Simulado não encontrado.');
  });

  it('exibe erro quando a publicação falha', async () => {
    api['obter'].mockReturnValue(of(detalhe({ composicao: composicaoFechada })));
    api['publicar'].mockReturnValue(
      throwError(() => ({
        error: {
          code: 'PUBLICACAO_INVALIDA',
          message: 'Não foi possível publicar.',
          fieldErrors: null,
        },
      })),
    );
    const fixture = await montar('sim-1');

    await fixture.componentInstance.publicar();

    expect(fixture.componentInstance['erro']()).toBe('Não foi possível publicar.');
  });

  it('converte a janela recebida em UTC para o horário local do input datetime-local', async () => {
    api['obter'].mockReturnValue(
      of(detalhe({ janelaAbertura: '2026-09-06T21:45:00Z', janelaEncerramento: null })),
    );
    const fixture = await montar('sim-1');

    expect(fixture.componentInstance['formulario'].controls.janelaAbertura.value).toBe(
      '2026-09-06T18:45',
    );
  });

  it('envia a janela preenchida no horário local convertida de volta para UTC', async () => {
    const fixture = await montar('sim-1');
    fixture.componentInstance['formulario'].patchValue({ janelaAbertura: '2026-09-06T18:45' });

    await fixture.componentInstance.salvar();

    expect(api['atualizar']).toHaveBeenCalledWith(
      'sim-1',
      expect.objectContaining({ janelaAbertura: '2026-09-06T21:45:00.000Z' }),
    );
  });

  it('ignora composição quando ainda não há detalhe carregado', async () => {
    const fixture = await montar(null);

    fixture.componentInstance.atualizouComposicao(composicaoFechada);

    expect(fixture.componentInstance['detalhe']()).toBeNull();
  });
});
