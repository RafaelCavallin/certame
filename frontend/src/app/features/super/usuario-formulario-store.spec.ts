import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { UsuarioAdminApi } from '../../core/api/usuario-admin-api';
import { PerfilUsuario } from '../../core/auth/modelos';
import { StatusUsuario, UsuarioGestaoResumo } from '../../core/models/modelos-usuario';
import { UsuarioFormularioStore } from './usuario-formulario-store';

const usuario = (alteracoes: Partial<UsuarioGestaoResumo> = {}): UsuarioGestaoResumo => ({
  id: 'u1',
  nome: 'Marina Duarte',
  email: 'marina.duarte@certame.com',
  perfil: PerfilUsuario.PROFESSOR,
  status: StatusUsuario.ATIVO,
  criadoEm: '2026-08-01T12:30:00Z',
  ...alteracoes,
});

describe('UsuarioFormularioStore', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    api = { criar: vi.fn(), editar: vi.fn() };
    TestBed.configureTestingModule({
      providers: [UsuarioFormularioStore, { provide: UsuarioAdminApi, useValue: api }],
    });
  });

  it('preenche o formulário a partir do usuário informado', () => {
    const store = TestBed.inject(UsuarioFormularioStore);

    store.carregarForm(usuario());

    expect(store.usuarioEditando()?.nome).toBe('Marina Duarte');
    expect(store.formulario.controls.email.value).toBe('marina.duarte@certame.com');
    expect(store.formulario.controls.enviarConvite.value).toBe(false);
  });

  it('reseta o formulário para criação quando nenhum usuário é informado', () => {
    const store = TestBed.inject(UsuarioFormularioStore);

    store.carregarForm(null);

    expect(store.usuarioEditando()).toBeNull();
    expect(store.formulario.controls.status.value).toBe(StatusUsuario.CONVITE_PENDENTE);
    expect(store.formulario.controls.enviarConvite.value).toBe(true);
  });

  it('mapeia o erro 409 de e-mail em uso para o campo correspondente', async () => {
    const store = TestBed.inject(UsuarioFormularioStore);
    store.carregarForm(null);
    store.formulario.setValue({
      nome: 'Novo',
      email: 'em.uso@certame.com',
      perfil: PerfilUsuario.ESTUDANTE,
      status: StatusUsuario.ATIVO,
      enviarConvite: false,
    });
    api['criar'].mockReturnValue(
      throwError(() => ({
        error: {
          code: 'EMAIL_EM_USO',
          message: 'Este e-mail já está em uso.',
          fieldErrors: { email: 'Este e-mail já está em uso.' },
        },
      })),
    );

    const salvou = await store.salvar();

    expect(salvou).toBe(false);
    expect(store.formulario.controls.email.getError('api')).toBe('Este e-mail já está em uso.');
  });

  it('mapeia o erro 409 de último super-admin para a mensagem geral', async () => {
    const store = TestBed.inject(UsuarioFormularioStore);
    store.carregarForm(usuario({ perfil: PerfilUsuario.SUPER_ADMIN }));
    api['editar'].mockReturnValue(
      throwError(() => ({
        error: {
          code: 'ULTIMO_SUPER_ADMIN',
          message: 'O último super-admin ativo não pode ser rebaixado ou desativado.',
          fieldErrors: null,
        },
      })),
    );

    const salvou = await store.salvar();

    expect(salvou).toBe(false);
    expect(store.erro()).toBe('O último super-admin ativo não pode ser rebaixado ou desativado.');
  });

  it('cria um usuário chamando a API sem id de edição', async () => {
    const store = TestBed.inject(UsuarioFormularioStore);
    store.carregarForm(null);
    store.formulario.setValue({
      nome: 'Novo',
      email: 'novo@certame.com',
      perfil: PerfilUsuario.ESTUDANTE,
      status: StatusUsuario.ATIVO,
      enviarConvite: false,
    });
    api['criar'].mockReturnValue(of(usuario({ id: 'u2' })));

    const salvou = await store.salvar();

    expect(salvou).toBe(true);
    expect(api['criar']).toHaveBeenCalled();
    expect(api['editar']).not.toHaveBeenCalled();
  });

  it('edita um usuário existente chamando a API com o id', async () => {
    const store = TestBed.inject(UsuarioFormularioStore);
    store.carregarForm(usuario());
    api['editar'].mockReturnValue(of(usuario({ status: StatusUsuario.INATIVO })));

    const salvou = await store.salvar();

    expect(salvou).toBe(true);
    expect(api['editar']).toHaveBeenCalledWith('u1', expect.anything());
  });
});
