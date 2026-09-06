package br.com.certame.desempenho;

import java.util.Comparator;
import java.util.List;

public final class SeletorPontosFracos {
    public static final int MINIMO_RESPOSTAS = 20;

    public List<PontoFraco> selecionar(List<TotalPorAssunto> assuntos, Integer pctMedioUsuario) {
        if (pctMedioUsuario == null) {
            return List.of();
        }
        return assuntos.stream()
                .filter(assunto -> elegivel(assunto, pctMedioUsuario))
                .sorted(Comparator.comparingLong((TotalPorAssunto assunto) -> impacto(assunto, pctMedioUsuario)).reversed())
                .map(assunto -> new PontoFraco(assunto.assuntoId(), assunto.assunto(), assunto.disciplinaId(),
                        assunto.disciplina(), assunto.volume(), assunto.pctAcerto()))
                .toList();
    }

    private boolean elegivel(TotalPorAssunto assunto, int pctMedioUsuario) {
        return assunto.volume() >= MINIMO_RESPOSTAS && assunto.pctAcerto() < pctMedioUsuario;
    }

    private long impacto(TotalPorAssunto assunto, int pctMedioUsuario) {
        return (long) assunto.volume() * (pctMedioUsuario - assunto.pctAcerto());
    }
}
