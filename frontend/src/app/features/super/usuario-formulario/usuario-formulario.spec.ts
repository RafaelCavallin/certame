import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { UsuarioAdminApi } from '../../../core/api/usuario-admin-api';
import { PerfilUsuario } from '../../../core/auth/modelos';
import { StatusUsuario, UsuarioGestaoResumo } from '../../../core/models/modelos-usuario';
import { UsuariosListaStore } from '../usuarios-lista-store';
import { UsuarioFormulario } from './usuario-formulario';

const usuario = (alteracoes: Partial<UsuarioGestaoResumo> = {}): UsuarioGestaoResumo => ({
  id: 'u1',
  nome: 'Marina Duarte',
  email: 'marina.duarte@certame.com',
  perfil: PerfilUsuario.PROFESSOR,
  status: StatusUsuario.ATIVO,
  criadoEm: '2026-08-01T12:30:00Z',
  ...alteracoes,
});

describe('UsuarioFormulario', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;
  let router: { navigate: ReturnType<typeof vi.fn> };

  async function montar(id: string | null) {
    router = { navigate: vi.fn(() => Promise.resolve(true)) };
    TestBed.configureTestingModule({
      imports: [UsuarioFormulario],
      providers: [
        { provide: UsuarioAdminApi, useValue: api },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => id } } } },
        { provide: Router, useValue: router },
      ],
    });
    TestBed.inject(UsuariosListaStore).pagina.set({
      content: [usuario()],
      page: 0,
      size: 20,
      totalElements: 1,
    });
    const fixture = TestBed.createComponent(UsuarioFormulario);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    api = {
      listar: vi.fn(() => of({ content: [usuario()], page: 0, size: 20, totalElements: 1 })),
      criar: vi.fn(),
      editar: vi.fn(),
    };
  });

  it('inicia em modo de criação sem usuário editando', async () => {
    const fixture = await montar(null);
    expect(fixture.componentInstance['store'].usuarioEditando()).toBeNull();
  });

  it('exibe erro obrigatório no campo tocado', async () => {
    const fixture = await montar(null);
    fixture.componentInstance['store'].formulario.controls.nome.markAsTouched();
    expect(fixture.componentInstance['mensagem']('nome')).toBe('Preencha este campo.');
  });

  it('não chama a API quando o formulário é inválido', async () => {
    const fixture = await montar(null);

    await fixture.componentInstance['salvar']();

    expect(api['criar']).not.toHaveBeenCalled();
  });

  it('cria um usuário válido e navega para a lista', async () => {
    const fixture = await montar(null);
    fixture.componentInstance['store'].formulario.setValue({
      nome: 'Novo usuário',
      email: 'novo@certame.com',
      perfil: PerfilUsuario.ESTUDANTE,
      status: StatusUsuario.ATIVO,
      enviarConvite: false,
    });
    api['criar'].mockReturnValue(of(usuario({ id: 'u2' })));

    await fixture.componentInstance['salvar']();

    expect(api['criar']).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/super/usuarios']);
  });

  it('carrega o usuário existente pelo id da rota', async () => {
    const fixture = await montar('u1');
    expect(fixture.componentInstance['store'].formulario.controls.nome.value).toBe('Marina Duarte');
  });

  it('edita o usuário existente sem exigir o convite', async () => {
    const fixture = await montar('u1');
    fixture.componentInstance['store'].formulario.controls.status.setValue(StatusUsuario.INATIVO);
    api['editar'].mockReturnValue(of(usuario({ status: StatusUsuario.INATIVO })));

    await fixture.componentInstance['salvar']();

    expect(api['editar']).toHaveBeenCalledWith(
      'u1',
      expect.objectContaining({ status: StatusUsuario.INATIVO }),
    );
  });

  it('mostra a mensagem de bloqueio vinda do backend ao salvar', async () => {
    api['editar'].mockReturnValue(
      throwError(() => ({
        error: {
          code: 'AUTO_MODIFICACAO',
          message: 'Você não pode rebaixar ou desativar o próprio acesso.',
          fieldErrors: null,
        },
      })),
    );
    const fixture = await montar('u1');

    await fixture.componentInstance['salvar']();

    expect(fixture.componentInstance['store'].erro()).toBe(
      'Você não pode rebaixar ou desativar o próprio acesso.',
    );
  });

  it('envia o formulário ao submeter pelo botão de salvar', async () => {
    const fixture = await montar('u1');
    api['editar'].mockReturnValue(of(usuario()));
    const elemento = fixture.nativeElement as HTMLElement;

    (elemento.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(api['editar']).toHaveBeenCalled();
  });
});
