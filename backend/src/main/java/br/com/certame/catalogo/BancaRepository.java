package br.com.certame.catalogo;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BancaRepository extends JpaRepository<Banca, UUID> {
    List<Banca> findAllByOrderByNomeAsc();
    Optional<Banca> findByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCase(String nome);
    @org.springframework.data.jpa.repository.Query(value = "select count(*) from questao where banca_id = :id and excluido_em is null", nativeQuery = true)
    long countQuestoesAtivas(@org.springframework.data.repository.query.Param("id") UUID id);
}
