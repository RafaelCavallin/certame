package br.com.certame.comum.excecao;

public class SerieVaziaException extends ErroNegocioException {
    public SerieVaziaException() {
        super("SERIE_VAZIA", 422, "Nenhuma questão publicada atende a esses filtros. Amplie a busca e tente de novo.");
    }
}
