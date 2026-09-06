package br.com.certame.catalogo.dto;

import java.util.List;
import java.util.UUID;

public record DisciplinaArvore(UUID id, String nome, long totalQuestoes, long totalPublicadas,
                               boolean arquivado, List<AssuntoArvore> assuntos) {
    public DisciplinaArvore { assuntos = List.copyOf(assuntos); }
}
