import { Routes } from '@angular/router';
import { authGuard, perfilGuard, visitanteGuard } from './core/auth/auth-guard';
import { PerfilUsuario } from './core/auth/modelos';
import { AcessoPage } from './features/auth/acesso-page/acesso-page';
import { DefinirSenhaPage } from './features/auth/definir-senha-page/definir-senha-page';
import { EsqueciSenhaPage } from './features/auth/esqueci-senha-page/esqueci-senha-page';
import { InicioPage } from './features/dashboard/inicio-page';
import { ShellPage } from './layout/shell/shell';
import { QuestoesPage } from './features/admin/questoes/questoes-page/questoes-page';
import { QuestaoFormPage } from './features/admin/questoes/questao-form-page/questao-form-page';
import { MateriasPage } from './features/admin/materias/materias-page/materias-page';
import { CatalogoPage } from './features/admin/catalogo/catalogo-page/catalogo-page';
import { EditaisAdminPage } from './features/admin/editais/editais-admin-page/editais-admin-page';
import { EditalFormPage } from './features/admin/editais/edital-form-page/edital-form-page';
import { CadernosPage } from './features/cadernos/cadernos-page/cadernos-page';
import { EditaisPage } from './features/editais/editais-page/editais-page';
import { DesempenhoPage } from './features/stats/desempenho-page/desempenho-page';
import { EstudarPage } from './features/study/estudar-page/estudar-page';
import { sairSessaoGuard } from './features/study/sair-sessao-guard';
import { FlashcardsPage } from './features/flashcards/flashcards-page/flashcards-page';
import { DecksPage } from './features/flashcards/decks-page/decks-page';
import { SessaoRevisaoPage } from './features/flashcards/sessao-revisao/sessao-revisao';
import { SimuladosPage as AdminSimuladosPage } from './features/admin/simulados/simulados-page/simulados-page';
import { SimuladosPage } from './features/simulados/simulados-page/simulados-page';
import { SimuladoResultadoPage } from './features/simulados/simulado-resultado-page/simulado-resultado-page';
import { SimuladoFormPage } from './features/admin/simulados/simulado-form-page/simulado-form-page';
import { UsuariosPage } from './features/super/usuarios/usuarios-page';
import { UsuarioFormulario } from './features/super/usuario-formulario/usuario-formulario';

export const routes: Routes = [
  { path: 'login', component: AcessoPage, canActivate: [visitanteGuard], data: { aba: 'entrar' } },
  {
    path: 'cadastro',
    component: AcessoPage,
    canActivate: [visitanteGuard],
    data: { aba: 'cadastro' },
  },
  { path: 'esqueci-senha', component: EsqueciSenhaPage, canActivate: [visitanteGuard] },
  { path: 'definir-senha/:token', component: DefinirSenhaPage, canActivate: [visitanteGuard] },
  {
    path: '',
    component: ShellPage,
    canActivate: [authGuard],
    children: [
      { path: 'inicio', component: InicioPage },
      { path: 'estudar', component: EstudarPage, canDeactivate: [sairSessaoGuard] },
      { path: 'cadernos', component: CadernosPage },
      { path: 'flashcards', component: FlashcardsPage },
      { path: 'flashcards/decks', component: DecksPage },
      { path: 'flashcards/estudar', component: SessaoRevisaoPage },
      { path: 'flashcards/:id', component: FlashcardsPage },
      { path: 'editais', component: EditaisPage },
      { path: 'editais/:id', component: EditaisPage },
      { path: 'desempenho', component: DesempenhoPage },
      { path: 'simulados', component: SimuladosPage },
      { path: 'simulados/:id/resultado', component: SimuladoResultadoPage },
      {
        path: 'admin/questoes',
        component: QuestoesPage,
        canActivate: [
          perfilGuard([PerfilUsuario.PROFESSOR, PerfilUsuario.ADMIN, PerfilUsuario.SUPER_ADMIN]),
        ],
      },
      {
        path: 'admin/questoes/nova',
        component: QuestaoFormPage,
        canActivate: [
          perfilGuard([PerfilUsuario.PROFESSOR, PerfilUsuario.ADMIN, PerfilUsuario.SUPER_ADMIN]),
        ],
      },
      {
        path: 'admin/questoes/:id',
        component: QuestaoFormPage,
        canActivate: [
          perfilGuard([PerfilUsuario.PROFESSOR, PerfilUsuario.ADMIN, PerfilUsuario.SUPER_ADMIN]),
        ],
      },
      {
        path: 'admin/materias',
        component: MateriasPage,
        canActivate: [
          perfilGuard([PerfilUsuario.PROFESSOR, PerfilUsuario.ADMIN, PerfilUsuario.SUPER_ADMIN]),
        ],
      },
      {
        path: 'admin/editais',
        component: EditaisAdminPage,
        canActivate: [perfilGuard([PerfilUsuario.ADMIN, PerfilUsuario.SUPER_ADMIN])],
      },
      {
        path: 'admin/editais/novo',
        component: EditalFormPage,
        canActivate: [perfilGuard([PerfilUsuario.ADMIN, PerfilUsuario.SUPER_ADMIN])],
      },
      {
        path: 'admin/editais/:id',
        component: EditalFormPage,
        canActivate: [perfilGuard([PerfilUsuario.ADMIN, PerfilUsuario.SUPER_ADMIN])],
      },
      {
        path: 'admin/catalogo',
        component: CatalogoPage,
        canActivate: [perfilGuard([PerfilUsuario.ADMIN, PerfilUsuario.SUPER_ADMIN])],
      },
      {
        path: 'admin/simulados',
        component: AdminSimuladosPage,
        canActivate: [
          perfilGuard([PerfilUsuario.PROFESSOR, PerfilUsuario.ADMIN, PerfilUsuario.SUPER_ADMIN]),
        ],
      },
      {
        path: 'admin/simulados/nova',
        component: SimuladoFormPage,
        canActivate: [
          perfilGuard([PerfilUsuario.PROFESSOR, PerfilUsuario.ADMIN, PerfilUsuario.SUPER_ADMIN]),
        ],
      },
      {
        path: 'admin/simulados/:id',
        component: SimuladoFormPage,
        canActivate: [
          perfilGuard([PerfilUsuario.PROFESSOR, PerfilUsuario.ADMIN, PerfilUsuario.SUPER_ADMIN]),
        ],
      },
      {
        path: 'super/usuarios',
        component: UsuariosPage,
        canActivate: [perfilGuard([PerfilUsuario.SUPER_ADMIN])],
      },
      {
        path: 'super/usuarios/novo',
        component: UsuarioFormulario,
        canActivate: [perfilGuard([PerfilUsuario.SUPER_ADMIN])],
      },
      {
        path: 'super/usuarios/:id/editar',
        component: UsuarioFormulario,
        canActivate: [perfilGuard([PerfilUsuario.SUPER_ADMIN])],
      },
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
    ],
  },
  // Para '' e não para 'login': o authGuard é quem decide. Quem já entrou vai para o próprio
  // início; quem não entrou vai para o login com `retorno`. Antes, qualquer URL inexistente
  // expulsava o usuário autenticado para a tela de login — e o destino de ADMIN caía justamente
  // aqui, fechando um ciclo de redirecionamento entre o `**` e o visitanteGuard.
  { path: '**', redirectTo: '' },
];
