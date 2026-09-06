package br.com.certame.comum.excecao;

public class SessaoEncerradaException extends ErroNegocioException {
    public SessaoEncerradaException() { super("SESSAO_ENCERRADA", 409, "A sessão de estudo já foi encerrada."); }
}
