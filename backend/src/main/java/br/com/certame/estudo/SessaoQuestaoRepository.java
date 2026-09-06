package br.com.certame.estudo;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SessaoQuestaoRepository extends JpaRepository<SessaoQuestao, SessaoQuestaoId> {
    @EntityGraph(attributePaths = "questao")
    List<SessaoQuestao> findBySessaoIdOrderByIdOrdem(UUID sessaoId);

    @Query("""
            select distinct q.disciplina.nome from SessaoQuestao sq join sq.questao q
             where sq.id.sessaoId = :sessaoId and q.disciplina is not null
            """)
    List<String> disciplinasDaSessao(@Param("sessaoId") UUID sessaoId);
}
