package br.com.certame.simulado;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TentativaSimuladoRepository extends JpaRepository<TentativaSimulado, UUID> {
    Optional<TentativaSimulado> findBySimuladoIdAndUsuarioIdAndStatus(UUID simuladoId, UUID usuarioId, StatusTentativa status);
    boolean existsBySimuladoIdAndUsuarioIdAndTipoAndStatus(UUID simuladoId, UUID usuarioId, TipoTentativa tipo, StatusTentativa status);
    Optional<TentativaSimulado> findBySimuladoIdAndUsuarioIdAndTipoAndStatus(UUID simuladoId, UUID usuarioId, TipoTentativa tipo, StatusTentativa status);
    boolean existsBySimuladoIdAndStatus(UUID simuladoId, StatusTentativa status);
    List<TentativaSimulado> findByUsuarioIdAndStatus(UUID usuarioId, StatusTentativa status);
    List<TentativaSimulado> findByStatus(StatusTentativa status);
    Optional<TentativaSimulado> findByIdAndUsuarioId(UUID id, UUID usuarioId);
    long countBySimuladoIdAndTipoAndStatus(UUID simuladoId, TipoTentativa tipo, StatusTentativa status);
}
