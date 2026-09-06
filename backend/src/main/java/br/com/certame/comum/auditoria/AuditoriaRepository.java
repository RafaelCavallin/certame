package br.com.certame.comum.auditoria;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID> {
    List<Auditoria> findByEntidadeAndEntidadeIdOrderByCriadoEmDesc(String entidade, UUID entidadeId);
}
