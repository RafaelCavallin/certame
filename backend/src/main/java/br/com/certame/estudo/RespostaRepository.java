package br.com.certame.estudo;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespostaRepository extends JpaRepository<Resposta, UUID> {
    List<Resposta> findBySessaoIdOrderByRespondidaEm(UUID sessaoId);
    Optional<Resposta> findBySessaoIdAndQuestaoId(UUID sessaoId, UUID questaoId);
    List<Resposta> findByUsuarioIdAndRespondidaEmGreaterThanEqualAndRespondidaEmLessThan(UUID usuarioId, Instant inicio, Instant fim);
}
