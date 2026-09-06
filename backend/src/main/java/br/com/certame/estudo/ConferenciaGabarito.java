package br.com.certame.estudo;

import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.questao.TipoQuestao;
import java.util.Locale;
import java.util.Map;

public class ConferenciaGabarito {
    private static final String CERTO = "CERTO";
    private static final String ERRADO = "ERRADO";
    private static final String MENSAGEM_ME = "Para questões de múltipla escolha, marque uma letra de A a E.";
    private static final String MENSAGEM_CE = "Para questões de certo/errado, marque CERTO ou ERRADO.";

    public boolean corrigir(TipoQuestao tipo, String gabarito, String marcada) {
        exigirValida(tipo, marcada);
        return normalizar(gabarito).equals(normalizar(marcada));
    }

    public boolean valida(TipoQuestao tipo, String marcada) {
        if (tipo == null || marcada == null) return false;
        var valor = normalizar(marcada);
        return tipo == TipoQuestao.ME ? valor.matches("[A-E]") : valor.equals(CERTO) || valor.equals(ERRADO);
    }

    private void exigirValida(TipoQuestao tipo, String marcada) {
        if (valida(tipo, marcada)) return;
        var mensagem = tipo == TipoQuestao.CE ? MENSAGEM_CE : MENSAGEM_ME;
        throw new DadosInvalidosException(Map.of("marcada", mensagem));
    }

    private String normalizar(String valor) { return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT); }
}
