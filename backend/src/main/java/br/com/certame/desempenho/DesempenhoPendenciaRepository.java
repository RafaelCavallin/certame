package br.com.certame.desempenho;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesempenhoPendenciaRepository extends JpaRepository<DesempenhoPendencia, UUID> {
    List<DesempenhoPendencia> findAllByOrderByMarcadoEmAsc();
}
