package br.com.certame.catalogo.edital;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EditalRepository extends JpaRepository<Edital, UUID> {
    @EntityGraph(attributePaths = {"banca", "orgao", "disciplinas"})
    List<Edital> findByExcluidoEmIsNull();

    @EntityGraph(attributePaths = {"banca", "orgao", "disciplinas"})
    List<Edital> findByStatusAndExcluidoEmIsNull(StatusEdital status);

    @EntityGraph(attributePaths = {"banca", "orgao", "disciplinas"})
    Optional<Edital> findByIdAndExcluidoEmIsNull(UUID id);

    @EntityGraph(attributePaths = {"banca", "orgao", "disciplinas"})
    List<Edital> findByStatusAndExcluidoEmIsNullOrderByPrazoInscricaoAsc(StatusEdital status);

    @EntityGraph(attributePaths = {"banca", "orgao", "disciplinas"})
    List<Edital> findByStatusAndExcluidoEmIsNullOrderByNomeAsc(StatusEdital status);

    @EntityGraph(attributePaths = {"banca", "orgao", "disciplinas"})
    @Query("""
            select e from Edital e
            where e.status = :status and e.excluidoEm is null
            order by case when e.prazoInscricao is null then 1 else 0 end, e.prazoInscricao desc
            """)
    List<Edital> findPorStatusOrdenadosPorPrazoDescendente(@Param("status") StatusEdital status);

    boolean existsByNomeIgnoreCaseAndExcluidoEmIsNull(String nome);

    boolean existsByNomeIgnoreCaseAndExcluidoEmIsNullAndIdNot(String nome, UUID id);

    @Query(value = """
            select count(*) from edital_disciplina ed
            join edital e on e.id = ed.edital_id
            where ed.disciplina_id = :disciplinaId and e.excluido_em is null
            """, nativeQuery = true)
    long contarEditaisComDisciplina(@Param("disciplinaId") UUID disciplinaId);
}
