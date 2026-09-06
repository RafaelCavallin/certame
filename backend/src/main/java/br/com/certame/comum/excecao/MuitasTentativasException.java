package br.com.certame.comum.excecao;

public class MuitasTentativasException extends ErroNegocioException {
    private final long retryAfter;

    public MuitasTentativasException(long retryAfter) {
        super("MUITAS_TENTATIVAS", 429, "Muitas tentativas. Tente novamente mais tarde.");
        this.retryAfter = retryAfter;
    }

    public long getRetryAfter() { return retryAfter; }
}
