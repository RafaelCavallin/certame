package br.com.certame.estudo;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarcacaoRepository extends JpaRepository<Marcacao, UUID> {
    Optional<Marcacao> findByUsuarioIdAndQuestaoIdAndTipo(UUID usuarioId, UUID questaoId, TipoMarcacao tipo);
    List<Marcacao> findByUsuarioIdAndQuestaoId(UUID usuarioId, UUID questaoId);
}
