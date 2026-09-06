package br.com.certame.comum.excecao;

import java.util.Map;

public class PublicacaoInvalidaException extends ErroNegocioException {
    private final Map<String, String> pendencias;

    public PublicacaoInvalidaException(Map<String, String> pendencias) {
        super("PUBLICACAO_INVALIDA", 422,
                "Não é possível publicar: verifique os campos pendentes.");
        this.pendencias = Map.copyOf(pendencias);
    }

    @Override
    public Map<String, String> getFieldErrors() {
        return pendencias;
    }
}
