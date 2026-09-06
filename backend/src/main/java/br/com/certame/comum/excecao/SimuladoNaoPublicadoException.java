package br.com.certame.comum.excecao;

public class SimuladoNaoPublicadoException extends ErroNegocioException {
    public SimuladoNaoPublicadoException() { super("SIMULADO_NAO_PUBLICADO", 409, "Este simulado ainda não foi publicado."); }
}
