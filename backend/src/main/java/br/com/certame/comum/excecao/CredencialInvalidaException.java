package br.com.certame.comum.excecao;

public class CredencialInvalidaException extends ErroNegocioException {
    public CredencialInvalidaException() {
        super("CREDENCIAL_INVALIDA", 401, "E-mail ou senha inválidos.");
    }
}
