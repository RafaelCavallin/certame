package br.com.certame.comum.excecao;

import java.util.Map;

public class EmailEmUsoException extends ErroNegocioException {
    public EmailEmUsoException() {
        super("EMAIL_EM_USO", 409, "Este e-mail já está em uso.");
    }

    @Override
    public Map<String, String> getFieldErrors() {
        return Map.of("email", "Este e-mail já está em uso.");
    }
}
