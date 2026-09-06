package br.com.certame.comum.excecao;

import java.util.Map;

public class NomeDeckEmUsoException extends ConflitoException {
    public NomeDeckEmUsoException() { super("NOME_EM_USO", "Já existe um deck com esse nome."); }
    @Override public Map<String, String> getFieldErrors() { return Map.of("nome", getMessage()); }
}
