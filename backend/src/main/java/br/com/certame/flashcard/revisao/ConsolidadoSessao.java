package br.com.certame.flashcard.revisao;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Consolida as classificações de uma sessão para CA-10: cards distintos, distribuição por nota e tempo total. */
record ConsolidadoSessao(int revisados, int classificacoes, int errei, int dificil, int bom, int facil, int tempoMs) {

    static ConsolidadoSessao de(List<FlashcardRevisao> registros) {
        var revisados = registros.stream().map(r -> r.getFlashcard().getId()).distinct().count();
        var porNota = registros.stream().collect(Collectors.groupingBy(FlashcardRevisao::getNota, Collectors.counting()));
        var tempoMs = registros.stream().mapToInt(FlashcardRevisao::getTempoMs).sum();
        return new ConsolidadoSessao((int) revisados, registros.size(), contagem(porNota, "ERREI"),
                contagem(porNota, "DIFICIL"), contagem(porNota, "BOM"), contagem(porNota, "FACIL"), tempoMs);
    }

    private static int contagem(Map<String, Long> porNota, String nota) {
        return porNota.getOrDefault(nota, 0L).intValue();
    }
}
