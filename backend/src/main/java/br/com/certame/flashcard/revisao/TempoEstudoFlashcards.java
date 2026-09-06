package br.com.certame.flashcard.revisao;

import br.com.certame.comum.ZonaAplicacao;
import br.com.certame.estudo.inicio.TempoEstudoExterno;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Porta de leitura para RF-06 (CA-11): soma o tempo das sessões finalizadas do dia informado. */
@Service
public class TempoEstudoFlashcards implements TempoEstudoExterno {
    private static final int MS_POR_SEGUNDO = 1000;
    private final SessaoRevisaoRepository sessoes;

    public TempoEstudoFlashcards(SessaoRevisaoRepository sessoes) {
        this.sessoes = sessoes;
    }

    @Override
    public int segundosNoDia(UUID usuarioId, LocalDate dia) {
        var inicio = dia.atStartOfDay(ZonaAplicacao.AMERICA_SAO_PAULO).toInstant();
        var fim = dia.plusDays(1).atStartOfDay(ZonaAplicacao.AMERICA_SAO_PAULO).toInstant();
        var tempoMs = sessoes.somarTempoMsFinalizadasEntre(usuarioId, inicio, fim);
        return (int) (tempoMs / MS_POR_SEGUNDO);
    }
}
