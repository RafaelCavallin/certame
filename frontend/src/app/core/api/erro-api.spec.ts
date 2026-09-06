import { FormControl } from '@angular/forms';

import { aplicarErrosDeCampo, extrairErroApi, mensagemErroApi } from './erro-api';

describe('aplicarErrosDeCampo', () => {
  it('TU-21: aplica somente os erros devolvidos pela API', () => {
    const email = new FormControl('');
    const senha = new FormControl('');

    aplicarErrosDeCampo({ email: 'Informe um e-mail válido.' }, { email, senha });

    expect(email.getError('api')).toBe('Informe um e-mail válido.');
    expect(senha.errors).toBeNull();
  });

  it('usa erro genérico para payload inválido e ignora mapa nulo', () => {
    expect(extrairErroApi(null).code).toBe('ERRO_DESCONHECIDO');
    aplicarErrosDeCampo(null, {});
  });
});

describe('mensagemErroApi', () => {
  it('usa a mensagem de negócio devolvida pela API', () => {
    const erro = { error: { code: 'SERIE_VAZIA', message: 'Amplie a busca.', fieldErrors: null } };

    expect(mensagemErroApi(erro, 'Padrão.')).toBe('Amplie a busca.');
  });

  it('cai no padrão quando a resposta não segue o contrato de erro', () => {
    expect(mensagemErroApi({ error: { status: 500 } }, 'Padrão.')).toBe('Padrão.');
    expect(mensagemErroApi(null, 'Padrão.')).toBe('Padrão.');
  });
});
