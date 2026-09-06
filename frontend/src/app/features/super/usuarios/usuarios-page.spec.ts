import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { UsuarioAdminApi } from '../../../core/api/usuario-admin-api';
import { PerfilUsuario } from '../../../core/auth/modelos';
import { StatusUsuario, UsuarioGestaoResumo } from '../../../core/models/modelos-usuario';
import { UsuariosPage } from './usuarios-page';

const usuario = (alteracoes: Partial<UsuarioGestaoResumo> = {}): UsuarioGestaoResumo => ({
  id: 'u1',
  nome: 'Marina Duarte',
  email: 'marina.duarte@certame.com',
  perfil: PerfilUsuario.PROFESSOR,
  status: StatusUsuario.ATIVO,
  criadoEm: '2026-08-01T12:30:00Z',
  ...alteracoes,
});

describe('UsuariosPage', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;

  async function montar() {
    TestBed.configureTestingModule({
      imports: [UsuariosPage],
      providers: [provideRouter([]), { provide: UsuarioAdminApi, useValue: api }],
    });
    const fixture = TestBed.createComponent(UsuariosPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    api = {
      listar: vi.fn(() => of({ content: [usuario()], page: 0, size: 20, totalElements: 1 })),
      excluir: vi.fn(),
      redefinirSenha: vi.fn(),
      reenviarConvite: vi.fn(),
    };
  });

  it('carrega a lista de usuários ao montar', async () => {
    const fixture = await montar();

    expect(api['listar']).toHaveBeenCalled();
    expect(fixture.componentInstance['store'].pagina().content).toHaveLength(1);
  });

  it('exibe um título de nível 1 acessível na página', async () => {
    const fixture = await montar();
    const elemento = fixture.nativeElement as HTMLElement;

    expect(elemento.querySelector('h1')?.textContent).toBe('Usuários');
  });

  it('abre a confirmação de exclusão com o nome do usuário', async () => {
    const fixture = await montar();

    fixture.componentInstance['confirmarExclusao'](usuario());

    expect(fixture.componentInstance['exclusao']()?.nome).toBe('Marina Duarte');
  });

  it('exclui o usuário confirmado e fecha o diálogo', async () => {
    const fixture = await montar();
    api['excluir'].mockReturnValue(of(undefined));
    fixture.componentInstance['confirmarExclusao'](usuario());

    await fixture.componentInstance['excluir']();

    expect(api['excluir']).toHaveBeenCalledWith('u1');
    expect(fixture.componentInstance['exclusao']()).toBeNull();
  });

  it('mostra o erro de salvaguarda quando a exclusão é recusada', async () => {
    const fixture = await montar();
    api['excluir'].mockReturnValue(
      throwError(() => ({
        error: {
          code: 'ULTIMO_SUPER_ADMIN',
          message: 'O último super-admin ativo não pode ser rebaixado ou desativado.',
          fieldErrors: null,
        },
      })),
    );
    fixture.componentInstance['confirmarExclusao'](usuario());

    await fixture.componentInstance['excluir']();

    expect(fixture.componentInstance['store'].erroLista()).toBe(
      'O último super-admin ativo não pode ser rebaixado ou desativado.',
    );
  });

  it('monta as iniciais a partir do nome completo', async () => {
    const fixture = await montar();
    expect(fixture.componentInstance['iniciais']('Marina Duarte')).toBe('MD');
  });

  it('traduz perfil e status para rótulo e classe em português', async () => {
    const fixture = await montar();
    const c = fixture.componentInstance;

    expect(c['rotuloPerfil'](PerfilUsuario.SUPER_ADMIN)).toBe('Super admin');
    expect(c['classePerfil'](PerfilUsuario.SUPER_ADMIN)).toBe('tag tag-perigo');
    expect(c['rotuloStatus'](StatusUsuario.CONVITE_PENDENTE)).toBe('Convite pendente');
    expect(c['classeStatus'](StatusUsuario.INATIVO)).toBe('tag tag-inativo');
  });

  it('descreve a paginação com o total de usuários filtrados', async () => {
    const fixture = await montar();
    expect(fixture.componentInstance['paginacaoLabel']()).toBe('Mostrando 1–1 de 1 usuários');
  });

  it('descreve a ausência de resultados na paginação', async () => {
    api['listar'].mockReturnValue(of({ content: [], page: 0, size: 20, totalElements: 0 }));
    const fixture = await montar();
    expect(fixture.componentInstance['paginacaoLabel']()).toBe(
      'Nenhum usuário encontrado para os filtros aplicados',
    );
  });

  it('aplica os filtros ao alterar os seletores e clicar em Filtrar', async () => {
    const fixture = await montar();
    const elemento = fixture.nativeElement as HTMLElement;
    const perfilSelect = elemento.querySelector('select[aria-label="Perfil"]') as HTMLSelectElement;
    const statusSelect = elemento.querySelector('select[aria-label="Status"]') as HTMLSelectElement;

    perfilSelect.value = 'ADMIN';
    perfilSelect.dispatchEvent(new Event('change'));
    statusSelect.value = 'INATIVO';
    statusSelect.dispatchEvent(new Event('change'));
    (elemento.querySelector('.filters button') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(fixture.componentInstance['store'].filtroPerfil()).toBe('ADMIN');
    expect(fixture.componentInstance['store'].filtroStatus()).toBe('INATIVO');
    expect(api['listar']).toHaveBeenCalledWith(
      expect.objectContaining({ perfil: 'ADMIN', status: 'INATIVO' }),
    );
  });

  it('alterna a ordenação ao clicar no cabeçalho de Criado em', async () => {
    const fixture = await montar();
    const elemento = fixture.nativeElement as HTMLElement;

    (elemento.querySelector('.ordenavel') as HTMLElement).click();
    await fixture.whenStable();

    expect(fixture.componentInstance['store'].ordenacao()).toBe('criadoEm,asc');
  });

  it('muda de página ao clicar em Próxima e Anterior', async () => {
    api['listar'].mockImplementation((filtro: { page: number }) =>
      of({ content: [usuario()], page: filtro.page, size: 1, totalElements: 2 }),
    );
    const fixture = await montar();
    const elemento = fixture.nativeElement as HTMLElement;
    const [anterior, proxima] = Array.from(
      elemento.querySelectorAll('.pagination button'),
    ) as HTMLButtonElement[];

    proxima.click();
    await fixture.whenStable();
    expect(fixture.componentInstance['store'].pagina().page).toBe(1);

    anterior.click();
    await fixture.whenStable();
    expect(fixture.componentInstance['store'].pagina().page).toBe(0);
  });

  it('aciona redefinir senha e reenviar convite pelos botões da linha', async () => {
    api['listar'].mockReturnValue(
      of({
        content: [usuario({ status: StatusUsuario.CONVITE_PENDENTE })],
        page: 0,
        size: 20,
        totalElements: 1,
      }),
    );
    api['redefinirSenha'].mockReturnValue(of(undefined));
    api['reenviarConvite'].mockReturnValue(of(undefined));
    const fixture = await montar();
    const elemento = fixture.nativeElement as HTMLElement;

    (elemento.querySelector('[title="Reenviar convite"]') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(api['reenviarConvite']).toHaveBeenCalledWith('u1');
  });

  it('abre e confirma a exclusão pelo botão da linha e pelo diálogo', async () => {
    api['excluir'].mockReturnValue(of(undefined));
    const fixture = await montar();
    const elemento = fixture.nativeElement as HTMLElement;

    (elemento.querySelector('[title="Excluir"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    (elemento.querySelector('.dialog .btn-primary') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(api['excluir']).toHaveBeenCalledWith('u1');
  });
});
