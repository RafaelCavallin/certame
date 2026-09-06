import { Route, Router } from '@angular/router';

import { PerfilUsuario } from './modelos';

/**
 * Destino de cada perfil depois de entrar, em ordem de preferência. A primeira opção é a intenção
 * do produto; as seguintes são o que existe hoje. `rotaInicialPorPerfil` escolhe a primeira que a
 * configuração do roteador realmente casa, então o dia em que `/admin/simulados` e
 * `/super/usuarios` forem entregues o destino passa a valer sozinho, sem editar este arquivo.
 */
const PREFERENCIAS: Record<PerfilUsuario, readonly string[]> = {
  [PerfilUsuario.ESTUDANTE]: ['/inicio'],
  [PerfilUsuario.PROFESSOR]: ['/admin/questoes', '/inicio'],
  [PerfilUsuario.ADMIN]: ['/admin/simulados', '/admin/questoes', '/inicio'],
  [PerfilUsuario.SUPER_ADMIN]: ['/super/usuarios'],
};

const ULTIMO_RECURSO = '/inicio';

export function rotaInicialPorPerfil(perfil: PerfilUsuario, router: Router): string {
  const existentes = caminhosConfigurados(router.config);
  return PREFERENCIAS[perfil].find((rota) => existentes.has(rota)) ?? ULTIMO_RECURSO;
}

/** Caminhos absolutos que a configuração resolve para um componente, `**` de fora. */
function caminhosConfigurados(
  rotas: readonly Route[],
  prefixo: readonly string[] = [],
): Set<string> {
  const caminhos = new Set<string>();
  for (const rota of rotas) {
    if (rota.path === undefined || rota.path === '**') continue;
    const segmentos = [...prefixo, ...rota.path.split('/').filter(Boolean)];
    if (rota.component || rota.loadComponent) caminhos.add(`/${segmentos.join('/')}`);
    for (const filho of caminhosConfigurados(rota.children ?? [], segmentos)) caminhos.add(filho);
  }
  return caminhos;
}

export const PREFERENCIAS_DE_ROTA_INICIAL = PREFERENCIAS;
