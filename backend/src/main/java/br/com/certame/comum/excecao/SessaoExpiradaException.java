package br.com.certame.comum.excecao;

public class SessaoExpiradaException extends ErroNegocioException {
    public SessaoExpiradaException() {
        super("SESSAO_EXPIRADA", 401, "Sua sessão expirou. Entre novamente.");
    }
}
