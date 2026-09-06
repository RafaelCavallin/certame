import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { UsuarioAdminApi } from '../../core/api/usuario-admin-api';
import { PerfilUsuario } from '../../core/auth/modelos';
import { StatusUsuario, UsuarioGestaoResumo } from '../../core/models/modelos-usuario';
import { ToastStore } from '../../shared/ui/toast/toast-store';
import { UsuariosListaStore } from './usuarios-lista-store';

const usuario = (alteracoes: Partial<UsuarioGestaoResumo> = {}): UsuarioGestaoResumo => ({
  id: 'u1',
  nome: 'Marina Duarte',
  email: 'marina.duarte@certame.com',
  perfil: PerfilUsuario.PROFESSOR,
  status: StatusUsuario.ATIVO,
  criadoEm: '2026-08-01T12:30:00Z',
  ...alteracoes,
});

describe('UsuariosListaStore', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;
  let toast: ToastStore;

  beforeEach(() => {
    api = { listar: vi.fn(), excluir: vi.fn(), redefinirSenha: vi.fn(), reenviarConvite: vi.fn() };
    TestBed.configureTestingModule({
      providers: [UsuariosListaStore, { provide: UsuarioAdminApi, useValue: api }],
    });
    toast = TestBed.inject(ToastStore);
  });

  it('carrega a lista com os filtros e a ordenação atuais', async () => {
    const store = TestBed.inject(UsuariosListaStore);
    api['listar'].mockReturnValue(
      of({ content: [usuario()], page: 0, size: 20, totalElements: 1 }),
    );
    store.filtroPerfil.set(PerfilUsuario.PROFESSOR);

    await store.carregarLista();

    expect(api['listar']).toHaveBeenCalledWith({
      page: 0,
      size: 20,
      sort: 'criadoEm,desc',
      perfil: PerfilUsuario.PROFESSOR,
      status: undefined,
    });
    expect(store.pagina().content).toHaveLength(1);
  });

  it('alterna a ordenação e recarrega a partir da primeira página', () => {
    const store = TestBed.inject(UsuariosListaStore);
    api['listar'].mockReturnValue(of({ content: [], page: 0, size: 20, totalElements: 0 }));

    store.alternarOrdenacao();

    expect(store.ordenacao()).toBe('criadoEm,asc');
    expect(api['listar']).toHaveBeenCalledWith(
      expect.objectContaining({ sort: 'criadoEm,asc', page: 0 }),
    );
  });

  it('registra o erro de listagem quando a API falha', async () => {
    const store = TestBed.inject(UsuariosListaStore);
    api['listar'].mockReturnValue(
      throwError(() => ({
        error: { code: 'ERRO', message: 'Falha ao carregar.', fieldErrors: null },
      })),
    );

    await store.carregarLista();

    expect(store.erroLista()).toBe('Falha ao carregar.');
  });

  it('exclui um usuário e recarrega a lista', async () => {
    const store = TestBed.inject(UsuariosListaStore);
    api['excluir'].mockReturnValue(of(undefined));
    api['listar'].mockReturnValue(of({ content: [], page: 0, size: 20, totalElements: 0 }));

    const erro = await store.excluir('u1');

    expect(erro).toBeNull();
    expect(api['listar']).toHaveBeenCalled();
  });

  it('retorna o erro da API quando a exclusão é bloqueada por salvaguarda', async () => {
    const store = TestBed.inject(UsuariosListaStore);
    api['excluir'].mockReturnValue(
      throwError(() => ({
        error: {
          code: 'AUTO_MODIFICACAO',
          message: 'Você não pode rebaixar ou desativar o próprio acesso.',
          fieldErrors: null,
        },
      })),
    );

    const erro = await store.excluir('u1');

    expect(erro?.code).toBe('AUTO_MODIFICACAO');
  });

  it('publica um toast de confirmação ao redefinir a senha', async () => {
    const store = TestBed.inject(UsuariosListaStore);
    api['redefinirSenha'].mockReturnValue(of(undefined));
    const publicar = vi.spyOn(toast, 'publicar');

    await store.redefinirSenha(usuario());

    expect(publicar).toHaveBeenCalledWith(
      'Link de redefinição de senha enviado para marina.duarte@certame.com.',
    );
  });

  it('publica um toast de confirmação ao reenviar o convite', async () => {
    const store = TestBed.inject(UsuariosListaStore);
    api['reenviarConvite'].mockReturnValue(of(undefined));
    const publicar = vi.spyOn(toast, 'publicar');

    await store.reenviarConvite(usuario({ status: StatusUsuario.CONVITE_PENDENTE }));

    expect(publicar).toHaveBeenCalledWith('Convite reenviado para marina.duarte@certame.com.');
  });

  it('publica um toast de erro quando redefinir senha falha', async () => {
    const store = TestBed.inject(UsuariosListaStore);
    api['redefinirSenha'].mockReturnValue(
      throwError(() => ({
        error: {
          code: 'STATUS_INCOMPATIVEL',
          message: 'Usuário não está ativo.',
          fieldErrors: null,
        },
      })),
    );
    const publicar = vi.spyOn(toast, 'publicar');

    await store.redefinirSenha(usuario());

    expect(publicar).toHaveBeenCalledWith('Usuário não está ativo.');
  });

  it('muda de página ao chamar irParaPagina', async () => {
    const store = TestBed.inject(UsuariosListaStore);
    api['listar'].mockReturnValue(of({ content: [], page: 1, size: 20, totalElements: 0 }));

    store.irParaPagina(1);
    await Promise.resolve();

    expect(api['listar']).toHaveBeenCalledWith(expect.objectContaining({ page: 1 }));
  });
});
