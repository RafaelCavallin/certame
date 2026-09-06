package br.com.certame.catalogo.edital;

import br.com.certame.comum.excecao.DadosInvalidosException;
import java.time.LocalDate;
import java.util.Map;

final class EditalRegras {
    private static final String MSG_PRAZO = "Informe o prazo de inscrição para um edital aberto.";
    private static final String MSG_URL_INVALIDA =
            "Informe uma URL válida, iniciando com http:// ou https://.";

    private EditalRegras() {}

    static void garantirPrazoQuandoAberto(StatusEdital status, LocalDate prazo) {
        if (status == StatusEdital.ABERTO && prazo == null)
            throw new DadosInvalidosException(Map.of("prazoInscricao", MSG_PRAZO));
    }

    static void garantirUrlValida(String urlOficial) {
        if (urlOficial == null) return;
        if (!urlOficial.startsWith("http://") && !urlOficial.startsWith("https://"))
            throw new DadosInvalidosException(Map.of("urlOficial", MSG_URL_INVALIDA));
    }
}
