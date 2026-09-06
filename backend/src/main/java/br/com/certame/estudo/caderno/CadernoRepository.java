package br.com.certame.estudo.caderno;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CadernoRepository extends JpaRepository<Caderno, UUID> {
    Optional<Caderno> findByIdAndUsuarioIdAndExcluidoEmIsNull(UUID id, UUID usuarioId);
    List<Caderno> findByUsuarioIdAndExcluidoEmIsNullOrderByAtualizadoEmDesc(UUID usuarioId);
    boolean existsByUsuarioIdAndNomeIgnoreCaseAndExcluidoEmIsNull(UUID usuarioId, String nome);
    @Query(value = """
            select c.id as caderno_id, count(cq.questao_id) as total_questoes,
                   count(distinct r.questao_id) filter (where s.id is not null) as questoes_respondidas
              from caderno c left join caderno_questao cq on cq.caderno_id = c.id
              left join resposta r on r.questao_id = cq.questao_id and r.usuario_id = c.usuario_id
              left join sessao_estudo s on s.id = r.sessao_id and s.finalizada_em is not null
             where c.usuario_id = :usuarioId and c.excluido_em is null
             group by c.id
             order by c.atualizado_em desc
            """, nativeQuery = true)
    List<Contagem> contarQuestoes(@Param("usuarioId") UUID usuarioId);
    interface Contagem { UUID getCadernoId(); long getTotalQuestoes(); long getQuestoesRespondidas(); }
}
