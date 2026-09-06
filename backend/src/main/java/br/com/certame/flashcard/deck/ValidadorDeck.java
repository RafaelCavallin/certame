package br.com.certame.flashcard.deck;

import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.flashcard.dto.RequisicaoDeck;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ValidadorDeck {
    private static final int MIN_NOME = 3;
    private static final int MAX_NOME = 60;
    private static final int MAX_DESCRICAO = 200;

    private ValidadorDeck() {
    }

    static String nome(String nomeBruto) {
        var nome = nomeBruto == null ? "" : nomeBruto.trim();
        if (nome.length() < MIN_NOME || nome.length() > MAX_NOME) {
            throw new DadosInvalidosException(Map.of("nome", "O nome deve ter entre 3 e 60 caracteres."));
        }
        return nome;
    }

    static String descricao(String descricaoBruta) {
        if (descricaoBruta == null) {
            return null;
        }
        var descricao = descricaoBruta.trim();
        if (descricao.isEmpty()) {
            return null;
        }
        if (descricao.length() > MAX_DESCRICAO) {
            throw new DadosInvalidosException(Map.of("descricao", "A descrição deve ter no máximo 200 caracteres."));
        }
        return descricao;
    }

    static void listaFlashcards(List<UUID> flashcardIds, int maxIds) {
        if (flashcardIds == null || flashcardIds.isEmpty() || flashcardIds.size() > maxIds) {
            throw new DadosInvalidosException(Map.of("flashcardIds", "Informe de 1 a 200 flashcards."));
        }
    }

    static LimitesDeck limites(Deck deck, RequisicaoDeck requisicao) {
        var novosDia = requisicao.novosDia() != null ? requisicao.novosDia() : deck.getNovosDia();
        var revisoesDia = requisicao.revisoesDia() != null ? requisicao.revisoesDia() : deck.getRevisoesDia();
        var intervaloMaxDias = requisicao.intervaloMaxDias() != null
                ? requisicao.intervaloMaxDias() : deck.getIntervaloMaxDias();
        var retencaoAlvo = requisicao.retencaoAlvo() != null ? requisicao.retencaoAlvo() : deck.getRetencaoAlvo();
        return new LimitesDeck(novosDia, revisoesDia, intervaloMaxDias, retencaoAlvo);
    }
}
