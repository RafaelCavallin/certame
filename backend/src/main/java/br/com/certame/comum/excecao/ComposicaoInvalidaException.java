package br.com.certame.comum.excecao;

import java.util.Map;

public class ComposicaoInvalidaException extends ErroNegocioException {
    private final Map<String, String> fieldErrors;
    public ComposicaoInvalidaException(Map<String, String> fieldErrors) {
        super("COMPOSICAO_INVALIDA", 422, "Verifique a composição do simulado.");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }
    @Override public Map<String, String> getFieldErrors() { return fieldErrors; }
}
