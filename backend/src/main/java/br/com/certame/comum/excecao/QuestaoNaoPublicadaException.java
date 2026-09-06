package br.com.certame.comum.excecao;

import java.util.Map;

public class QuestaoNaoPublicadaException extends ErroNegocioException {
    private final Map<String, String> erros;
    public QuestaoNaoPublicadaException(int quantidade) {
        super("QUESTAO_NAO_PUBLICADA", 422, "Só é possível adicionar questões publicadas ao caderno.");
        erros = Map.of("questaoIds", quantidade + " questão(ões) enviada(s) não está(ão) publicada(s).");
    }
    @Override public Map<String, String> getFieldErrors() { return erros; }
}
