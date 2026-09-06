package br.com.certame.simulado;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimuladoQuestaoRepository extends JpaRepository<SimuladoQuestao, SimuladoQuestaoId> {
    List<SimuladoQuestao> findBySimuladoIdOrderByIdOrdem(UUID simuladoId);
    void deleteBySimuladoId(UUID simuladoId);
}
