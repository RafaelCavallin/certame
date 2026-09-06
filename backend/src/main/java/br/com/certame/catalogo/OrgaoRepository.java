package br.com.certame.catalogo;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgaoRepository extends JpaRepository<Orgao, UUID> {
    List<Orgao> findAllByOrderByNomeAsc();
    Optional<Orgao> findByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCase(String nome);
    @org.springframework.data.jpa.repository.Query(value = "select count(*) from questao where orgao_id = :id and excluido_em is null", nativeQuery = true)
    long countQuestoesAtivas(@org.springframework.data.repository.query.Param("id") UUID id);
}
