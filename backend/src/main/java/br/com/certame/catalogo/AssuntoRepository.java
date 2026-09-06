package br.com.certame.catalogo;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssuntoRepository extends JpaRepository<Assunto, UUID> {
    Optional<Assunto> findByIdAndExcluidoEmIsNull(UUID id);
    List<Assunto> findByDisciplinaIdAndExcluidoEmIsNullOrderByNomeAsc(UUID disciplinaId);
    boolean existsByDisciplinaIdAndNomeIgnoreCaseAndExcluidoEmIsNull(UUID disciplinaId, String nome);
    @org.springframework.data.jpa.repository.Query(value = "select count(*) from questao where assunto_id = :id and excluido_em is null", nativeQuery = true)
    long countQuestoesAtivas(@org.springframework.data.repository.query.Param("id") UUID assuntoId);
}
