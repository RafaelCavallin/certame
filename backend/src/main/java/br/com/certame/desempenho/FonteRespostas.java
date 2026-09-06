package br.com.certame.desempenho;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FonteRespostas {
    List<TotalPorChave<LocalDate>> porSemana(UUID usuarioId);
    List<TotalPorDisciplina> porDisciplina(UUID usuarioId);
    List<TotalPorAssunto> porAssunto(UUID usuarioId);

    record TotalPorChave<T>(T chave, int volume, int acertos) {}
    record TotalPorDisciplina(UUID disciplinaId, int volume, int acertos) {}
    record TotalPorAssunto(UUID assuntoId, UUID disciplinaId, int volume, int acertos) {}
}
