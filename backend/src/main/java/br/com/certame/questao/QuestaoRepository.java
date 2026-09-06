package br.com.certame.questao;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface QuestaoRepository extends JpaRepository<Questao, UUID>, QuestaoBuscaRepository {
    @EntityGraph(attributePaths = "alternativas")
    Optional<Questao> findByIdAndExcluidoEmIsNull(UUID id);
    Optional<Questao> findByIdAndStatusAndExcluidoEmIsNull(UUID id, StatusQuestao status);
    Optional<Questao> findByCodigoAndExcluidoEmIsNull(String codigo);
    long countByAssuntoIdAndExcluidoEmIsNull(UUID assuntoId);
    long countByDisciplinaIdAndExcluidoEmIsNull(UUID disciplinaId);
    long countByBancaId(UUID bancaId);
    long countByOrgaoId(UUID orgaoId);
    long countByDisciplinaIdAndStatusAndExcluidoEmIsNull(UUID disciplinaId, StatusQuestao status);
    long countByDisciplinaIdAndDificuldadeAndStatusAndExcluidoEmIsNull(UUID disciplinaId, Dificuldade dificuldade,
            StatusQuestao status);
    @Query("select q.id from Questao q where q.status = br.com.certame.questao.StatusQuestao.PUBLICADA and q.excluidoEm is null")
    List<UUID> buscarIdsPublicadas();
    @Query("select q.id from Questao q where q.disciplina.id = :disciplinaId and q.status = br.com.certame.questao.StatusQuestao.PUBLICADA and q.excluidoEm is null")
    List<UUID> buscarIdsPublicadasPorDisciplina(@Param("disciplinaId") UUID disciplinaId);
    @Query("select q.id from Questao q where q.disciplina.id = :disciplinaId and q.dificuldade = :dificuldade and q.status = br.com.certame.questao.StatusQuestao.PUBLICADA and q.excluidoEm is null")
    List<UUID> buscarIdsPublicadasPorDisciplinaEDificuldade(@Param("disciplinaId") UUID disciplinaId,
            @Param("dificuldade") Dificuldade dificuldade);
    @Query(value = "select count(*) from resposta where questao_id = :id", nativeQuery = true)
    long countRespostas(@Param("id") UUID questaoId);
}
