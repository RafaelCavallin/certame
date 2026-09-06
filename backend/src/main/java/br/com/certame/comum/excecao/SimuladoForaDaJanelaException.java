package br.com.certame.comum.excecao;

public class SimuladoForaDaJanelaException extends ErroNegocioException {
    public SimuladoForaDaJanelaException() { super("SIMULADO_FORA_DA_JANELA", 409, "Este simulado não aceita novas tentativas fora da janela."); }
}
