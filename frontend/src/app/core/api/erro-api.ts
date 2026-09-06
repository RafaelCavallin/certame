import { FormControl } from '@angular/forms';

export interface ErroApi {
  code: string;
  message: string;
  fieldErrors: Record<string, string> | null;
}

export function extrairErroApi(erro: unknown): ErroApi {
  if (isErroApi(erro)) {
    return erro;
  }

  return {
    code: 'ERRO_DESCONHECIDO',
    message: 'Não foi possível concluir a operação. Tente novamente.',
    fieldErrors: null,
  };
}

export function aplicarErrosDeCampo(
  fieldErrors: Record<string, string> | null | undefined,
  controles: Record<string, FormControl>,
): void {
  if (!fieldErrors) {
    return;
  }

  for (const [campo, mensagem] of Object.entries(fieldErrors)) {
    const controle = controles[campo];
    if (controle) {
      controle.setErrors({ ...(controle.errors ?? {}), api: mensagem });
    }
  }
}

export function corpoErro(erro: unknown): unknown {
  return erro && typeof erro === 'object' && 'error' in erro
    ? (erro as { error: unknown }).error
    : erro;
}

function isErroApi(erro: unknown): erro is ErroApi {
  if (!erro || typeof erro !== 'object') {
    return false;
  }

  const candidato = erro as Partial<ErroApi>;
  return typeof candidato.code === 'string' && typeof candidato.message === 'string';
}

/** Mensagem do erro de negócio devolvido pela API; `padrao` quando a resposta não segue o contrato. */
export function mensagemErroApi(erro: unknown, padrao: string): string {
  const corpo = corpoErro(erro);
  return isErroApi(corpo) ? corpo.message : padrao;
}
