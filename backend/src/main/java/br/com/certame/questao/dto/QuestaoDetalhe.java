package br.com.certame.questao.dto;

import br.com.certame.questao.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestaoDetalhe(
        UUID id, String codigo, TipoQuestao tipo, String enunciado,
        UUID disciplinaId, UUID assuntoId, UUID bancaId, UUID orgaoId,
        Short ano, Dificuldade dificuldade, List<AlternativaDto> alternativas,
        String gabarito, String comentario, StatusQuestao status, UUID autorId,
        Instant publicadoEm, Instant criadoEm, Instant atualizadoEm) {

    public static QuestaoDetalhe de(Questao q) {
        return new QuestaoDetalhe(q.getId(), q.getCodigo(), q.getTipo(), q.getEnunciado(),
                q.getDisciplina() == null ? null : q.getDisciplina().getId(),
                q.getAssunto() == null ? null : q.getAssunto().getId(),
                q.getBanca() == null ? null : q.getBanca().getId(),
                q.getOrgao() == null ? null : q.getOrgao().getId(), q.getAno(), q.getDificuldade(),
                q.getAlternativas().stream().map(a -> new AlternativaDto(a.getLetra(), a.getTexto(), a.getOrdem())).toList(),
                q.getGabarito(), q.getComentario(), q.getStatus(), q.getAutor().getId(),
                q.getPublicadoEm(), q.getCriadoEm(), q.getAtualizadoEm());
    }
}
