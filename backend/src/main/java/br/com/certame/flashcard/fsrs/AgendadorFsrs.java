package br.com.certame.flashcard.fsrs;

import br.com.certame.flashcard.NotaRevisao;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/** Único ponto que calcula agendamento no sistema (RN-08) — puro, sem Spring e sem relógio próprio. */
public final class AgendadorFsrs {

    public Agendamento proximo(EstadoMemoria memoria, NotaRevisao nota, ContextoAgendamento contexto) {
        var dificuldade = calcularDificuldade(memoria, nota);
        var estabilidade = calcularEstabilidade(memoria, nota, dificuldade, contexto);
        var transicao = PassosAprendizado.proximoEstado(memoria.estado(), memoria.passoAprendizado(), nota);
        var atraso = resolverAtraso(transicao, estabilidade, contexto);
        var proximaRevisaoEm = contexto.agora().plus(Duration.ofMinutes(atraso.minutos()));
        return new Agendamento(estabilidade, dificuldade, transicao.estado(), transicao.passo(),
                atraso.dias(), atraso.minutos(), proximaRevisaoEm);
    }

    public PrevisaoIntervalos previsoes(EstadoMemoria memoria, ContextoAgendamento contexto) {
        Map<NotaRevisao, Integer> minutosPorNota = new EnumMap<>(NotaRevisao.class);
        for (var nota : NotaRevisao.values()) {
            minutosPorNota.put(nota, proximo(memoria, nota, contexto).intervaloMinutos());
        }
        return new PrevisaoIntervalos(minutosPorNota);
    }

    private double calcularDificuldade(EstadoMemoria memoria, NotaRevisao nota) {
        if (primeiroContato(memoria)) {
            return CalculoDificuldade.inicial(nota);
        }
        return CalculoDificuldade.proxima(memoria.dificuldade(), nota);
    }

    private double calcularEstabilidade(EstadoMemoria memoria, NotaRevisao nota, double dificuldadeNova,
            ContextoAgendamento contexto) {
        if (primeiroContato(memoria)) {
            return CalculoEstabilidade.inicial(nota);
        }
        var estabilidadeAntiga = memoria.estabilidade();
        var retrievability = CurvaEsquecimento.retrievability(estabilidadeAntiga, contexto.diasDecorridos());
        if (contexto.diasDecorridos() == 0) {
            return estabilidadeMesmoDia(estabilidadeAntiga, dificuldadeNova, retrievability, nota);
        }
        if (nota == NotaRevisao.ERREI) {
            return CalculoEstabilidade.aposLapso(estabilidadeAntiga, dificuldadeNova, retrievability);
        }
        return CalculoEstabilidade.aposAcerto(estabilidadeAntiga, dificuldadeNova, retrievability, nota);
    }

    private double estabilidadeMesmoDia(double estabilidade, double dificuldade, double retrievability,
            NotaRevisao nota) {
        var curtoPrazo = CalculoEstabilidade.curtoPrazo(estabilidade, nota);
        if (nota != NotaRevisao.ERREI) {
            return curtoPrazo;
        }
        var lapso = CalculoEstabilidade.aposLapso(estabilidade, dificuldade, retrievability);
        return Math.min(lapso, curtoPrazo);
    }

    private boolean primeiroContato(EstadoMemoria memoria) {
        return memoria.estabilidade() <= 0;
    }

    private Atraso resolverAtraso(PassosAprendizado.Transicao transicao, double estabilidade,
            ContextoAgendamento contexto) {
        if (transicao.atrasoMinutos() != null) {
            return new Atraso(0, transicao.atrasoMinutos());
        }
        var dias = intervaloLongoDias(estabilidade, contexto);
        return new Atraso(dias, dias * 1440);
    }

    private int intervaloLongoDias(double estabilidade, ContextoAgendamento contexto) {
        var dias = Math.round(CurvaEsquecimento.intervaloParaRetencao(estabilidade, contexto.retencaoAlvo()));
        return (int) Math.max(1, Math.min(contexto.intervaloMaxDias(), dias));
    }

    private record Atraso(int dias, int minutos) {
    }
}
