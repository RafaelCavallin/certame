package br.com.certame.comum.excecao;

import java.util.Map;

public abstract class ErroNegocioException extends RuntimeException {
    private final String code;
    private final int status;

    protected ErroNegocioException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public int getStatus() { return status; }
    public Map<String, String> getFieldErrors() { return null; }
}
