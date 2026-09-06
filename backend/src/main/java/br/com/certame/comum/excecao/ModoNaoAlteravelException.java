package br.com.certame.comum.excecao;

public class ModoNaoAlteravelException extends ErroNegocioException {
    public ModoNaoAlteravelException() { super("MODO_NAO_ALTERAVEL", 409, "O Modo Prova é escolhido ao iniciar a sessão."); }
}
