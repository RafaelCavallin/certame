package br.com.certame.comum.excecao;

public class ConflitoException extends ErroNegocioException {
    public ConflitoException(String code, String message) {
        super(code, 409, message);
    }
}
