package br.com.certame.comum.excecao;

public class TentativaJaFinalizadaException extends ErroNegocioException {
    public TentativaJaFinalizadaException() { super("TENTATIVA_JA_FINALIZADA", 409, "Esta tentativa já foi finalizada."); }
}
