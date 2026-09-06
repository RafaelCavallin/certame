package br.com.certame.comum.excecao;

public class TokenInvalidoException extends ErroNegocioException {
    public TokenInvalidoException() {
        super("TOKEN_INVALIDO", 400, "Este link expirou ou já foi utilizado. Solicite um novo.");
    }
}
