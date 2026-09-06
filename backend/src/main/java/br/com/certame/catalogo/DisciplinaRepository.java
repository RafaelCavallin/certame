package br.com.certame.catalogo;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DisciplinaRepository extends JpaRepository<Disciplina, UUID> {
    Optional<Disciplina> findByIdAndExcluidoEmIsNull(UUID id);
    List<Disciplina> findByIdInAndExcluidoEmIsNull(Collection<UUID> ids);
    boolean existsByNomeIgnoreCaseAndExcluidoEmIsNull(String nome);
    List<Disciplina> findByExcluidoEmIsNullOrderByNomeAsc();
    @Query(value = "select count(*) from questao where disciplina_id = :id and excluido_em is null", nativeQuery = true)
    long countQuestoesAtivas(@Param("id") UUID disciplinaId);
    interface ContagemArvore {
        UUID getDisciplinaId(); String getDisciplinaNome(); UUID getAssuntoId(); String getAssuntoNome();
        java.time.Instant getAssuntoArquivadoEm(); java.time.Instant getDisciplinaArquivadaEm();
        long getTotalQuestoes(); long getTotalPublicadas();
    }
    /**
     * Uma linha por disciplina (assunto_id nulo, com o total da disciplina) e uma por assunto.
     * Feito por `union all` em vez de `grouping sets`: o PostgreSQL recusa `grouping()` dentro de
     * `filter`, e a versão anterior falhava com "grouping operations are not allowed in FILTER".
     */
    @Query(value = """
            select d.id as disciplina_id, d.nome as disciplina_nome, d.arquivado_em as disciplina_arquivada_em,
                   cast(null as uuid) as assunto_id, cast(null as text) as assunto_nome,
                   cast(null as timestamptz) as assunto_arquivado_em,
                   count(q.id) as total_questoes,
                   count(q.id) filter (where q.status = 'PUBLICADA') as total_publicadas
            from disciplina d
            left join questao q on q.disciplina_id = d.id and q.excluido_em is null
            where d.excluido_em is null and (:incluir = true or d.arquivado_em is null)
            group by d.id, d.nome, d.arquivado_em
            union all
            select d.id, d.nome, d.arquivado_em, a.id, a.nome, a.arquivado_em,
                   count(q.id),
                   count(q.id) filter (where q.status = 'PUBLICADA')
            from disciplina d
            join assunto a on a.disciplina_id = d.id and a.excluido_em is null
                 and (:incluir = true or a.arquivado_em is null)
            left join questao q on q.assunto_id = a.id and q.excluido_em is null
            where d.excluido_em is null and (:incluir = true or d.arquivado_em is null)
            group by d.id, d.nome, d.arquivado_em, a.id, a.nome, a.arquivado_em
            order by 2, 5 nulls first
            """, nativeQuery = true)
    List<ContagemArvore> contarArvore(@Param("incluir") boolean incluir);
}
