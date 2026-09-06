package br.com.certame.comum.excecao;

public class CodigoEmUsoException extends ConflitoException {
    public CodigoEmUsoException() { super("CODIGO_EM_USO", "O código do simulado já está em uso."); }
}
