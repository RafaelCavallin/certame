package br.com.certame.flashcard.fsrs;

import br.com.certame.flashcard.EstadoFlashcard;
import br.com.certame.flashcard.NotaRevisao;

/** Máquina de estados dos passos curtos do FSRS-5 com memória de curto prazo (RN-08). */
public final class PassosAprendizado {

    private PassosAprendizado() {
    }

    public static Transicao proximoEstado(EstadoFlashcard estadoAtual, int passo, NotaRevisao nota) {
        if (estadoAtual == EstadoFlashcard.REVISAO) {
            return emRevisao(nota);
        }
        if (estadoAtual == EstadoFlashcard.REAPRENDENDO) {
            return reaprendendo(passo, nota);
        }
        return aprendendo(passo, nota);
    }

    private static Transicao emRevisao(NotaRevisao nota) {
        if (nota == NotaRevisao.ERREI) {
            return new Transicao(EstadoFlashcard.REAPRENDENDO, 0, PesosFsrs.PASSOS_REAPRENDIZADO_MINUTOS[0]);
        }
        return new Transicao(EstadoFlashcard.REVISAO, 0, null);
    }

    private static Transicao reaprendendo(int passo, NotaRevisao nota) {
        if (nota == NotaRevisao.ERREI) {
            return new Transicao(EstadoFlashcard.REAPRENDENDO, 0, PesosFsrs.PASSOS_REAPRENDIZADO_MINUTOS[0]);
        }
        if (nota == NotaRevisao.DIFICIL) {
            return new Transicao(EstadoFlashcard.REAPRENDENDO, passo, PesosFsrs.PASSOS_REAPRENDIZADO_MINUTOS[passo]);
        }
        return new Transicao(EstadoFlashcard.REVISAO, 0, null);
    }

    private static Transicao aprendendo(int passo, NotaRevisao nota) {
        if (nota == NotaRevisao.ERREI) {
            return new Transicao(EstadoFlashcard.APRENDENDO, 0, PesosFsrs.PASSOS_APRENDIZADO_MINUTOS[0]);
        }
        if (nota == NotaRevisao.DIFICIL) {
            return new Transicao(EstadoFlashcard.APRENDENDO, passo, PesosFsrs.PASSOS_APRENDIZADO_MINUTOS[passo]);
        }
        if (nota == NotaRevisao.FACIL) {
            return new Transicao(EstadoFlashcard.REVISAO, 0, null);
        }
        return bomEmAprendizado(passo);
    }

    private static Transicao bomEmAprendizado(int passo) {
        var proximoPasso = passo + 1;
        if (proximoPasso >= PesosFsrs.PASSOS_APRENDIZADO_MINUTOS.length) {
            return new Transicao(EstadoFlashcard.REVISAO, 0, null);
        }
        return new Transicao(EstadoFlashcard.APRENDENDO, proximoPasso, PesosFsrs.PASSOS_APRENDIZADO_MINUTOS[proximoPasso]);
    }

    /** Atraso {@code null} significa que o intervalo vem do FSRS de longo prazo, não dos passos. */
    public record Transicao(EstadoFlashcard estado, int passo, Integer atrasoMinutos) {
    }
}
