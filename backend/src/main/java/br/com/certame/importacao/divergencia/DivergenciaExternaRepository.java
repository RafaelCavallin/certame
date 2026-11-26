package br.com.certame.importacao.divergencia;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DivergenciaExternaRepository extends JpaRepository<DivergenciaExterna, UUID> {
}
