import { Route, Router, provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';

import { PerfilUsuario } from './modelos';
import { rotaInicialPorPerfil } from './rota-inicial';

/**
 * A lógica do resolvedor é provada aqui, sobre configurações sintéticas. Que as rotas reais do
 * aplicativo levem cada perfil a uma tela existente é verificado ponta a ponta em
 * `e2e/auth.spec.ts` — importar `app.routes` daqui arrastaria todas as páginas para dentro do
 * teste de unidade.
 */
class Pagina {}

function roteador(rotas: Route[]): Router {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({ providers: [provideRouter(rotas)] });
  return TestBed.inject(Router);
}

const telasDeHoje: Route[] = [
  { path: 'login', component: Pagina },
  {
    path: '',
    component: Pagina,
    children: [
      { path: 'inicio', component: Pagina },
      { path: 'admin/questoes', component: Pagina },
      { path: 'admin/questoes/:id', component: Pagina },
      { path: 'super/usuarios', component: Pagina },
    ],
  },
  { path: '**', redirectTo: '' },
];

describe('rotaInicialPorPerfil', () => {
  it.each([
    [PerfilUsuario.ESTUDANTE, '/inicio'],
    [PerfilUsuario.PROFESSOR, '/admin/questoes'],
    [PerfilUsuario.ADMIN, '/admin/questoes'],
    [PerfilUsuario.SUPER_ADMIN, '/super/usuarios'],
  ])('TU-22: leva %s para a primeira preferência que existe (%s)', (perfil, esperada) => {
    expect(rotaInicialPorPerfil(perfil, roteador(telasDeHoje))).toBe(esperada);
  });

  it('TU-22b: passa a preferir a tela do produto assim que ela é entregue', () => {
    const comSimulados = roteador([...telasDeHoje, { path: 'admin/simulados', component: Pagina }]);

    expect(rotaInicialPorPerfil(PerfilUsuario.ADMIN, comSimulados)).toBe('/admin/simulados');
  });

  it('TU-22c: nunca devolve uma rota que a configuração não resolve', () => {
    const soLogin = roteador([
      { path: 'login', component: Pagina },
      { path: '**', redirectTo: '' },
    ]);

    expect(rotaInicialPorPerfil(PerfilUsuario.ADMIN, soLogin)).toBe('/inicio');
  });

  it('TU-22d: ignora o curinga ao decidir o que existe', () => {
    const soCuringa = roteador([{ path: '**', redirectTo: '' }]);

    expect(rotaInicialPorPerfil(PerfilUsuario.PROFESSOR, soCuringa)).toBe('/inicio');
  });

  it('TU-22e: ignora rota de redirecionamento sem componente', () => {
    const semComponente = roteador([
      { path: 'admin/questoes', redirectTo: 'login' },
      { path: 'inicio', component: Pagina },
    ]);

    expect(rotaInicialPorPerfil(PerfilUsuario.PROFESSOR, semComponente)).toBe('/inicio');
  });
});
