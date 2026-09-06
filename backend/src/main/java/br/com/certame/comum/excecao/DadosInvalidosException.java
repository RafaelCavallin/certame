package br.com.certame.comum.excecao;

import java.util.Map;

public class DadosInvalidosException extends ErroNegocioException {
    private final Map<String, String> fieldErrors;

    public DadosInvalidosException(Map<String, String> fieldErrors) {
        super("DADOS_INVALIDOS", 422, "Verifique os campos destacados.");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    @Override
    public Map<String, String> getFieldErrors() { return fieldErrors; }
}
