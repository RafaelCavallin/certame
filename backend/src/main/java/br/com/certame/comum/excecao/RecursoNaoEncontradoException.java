package br.com.certame.comum.excecao;

public class RecursoNaoEncontradoException extends ErroNegocioException {
    public RecursoNaoEncontradoException() {
        super("RECURSO_NAO_ENCONTRADO", 404, "O recurso solicitado não foi encontrado.");
    }

    public RecursoNaoEncontradoException(String code) {
        super(code, 404, "O recurso solicitado não foi encontrado.");
    }
}
