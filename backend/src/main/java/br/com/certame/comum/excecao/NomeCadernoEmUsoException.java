package br.com.certame.comum.excecao;

import java.util.Map;

public class NomeCadernoEmUsoException extends ConflitoException {
    public NomeCadernoEmUsoException() { super("NOME_CADERNO_EM_USO", "Você já tem um caderno com esse nome."); }
    @Override public Map<String, String> getFieldErrors() { return Map.of("nome", getMessage()); }
}
