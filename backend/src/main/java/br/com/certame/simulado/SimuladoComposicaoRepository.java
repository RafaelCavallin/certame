package br.com.certame.simulado;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimuladoComposicaoRepository extends JpaRepository<SimuladoComposicao, UUID> {
    List<SimuladoComposicao> findBySimuladoId(UUID simuladoId);
    void deleteBySimuladoId(UUID simuladoId);
    long countBySimuladoId(UUID simuladoId);
}
