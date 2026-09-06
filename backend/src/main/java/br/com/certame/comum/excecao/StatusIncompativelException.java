package br.com.certame.comum.excecao;

public class StatusIncompativelException extends ConflitoException {
    public StatusIncompativelException(String message) {
        super("STATUS_INCOMPATIVEL", message);
    }
}
