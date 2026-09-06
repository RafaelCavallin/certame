package br.com.certame.comum.excecao;

public class RespostaDuplicadaException extends ErroNegocioException {
    public RespostaDuplicadaException() { super("RESPOSTA_DUPLICADA", 409, "Esta questão já foi respondida nesta sessão."); }
}
