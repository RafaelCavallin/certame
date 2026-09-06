package br.com.certame.estudo;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SessaoEstudoRepository extends JpaRepository<SessaoEstudo, UUID> {
    Optional<SessaoEstudo> findByIdAndUsuarioId(UUID id, UUID usuarioId);
    Optional<SessaoEstudo> findFirstByUsuarioIdAndModoNotAndFinalizadaEmIsNullAndExpiradaEmIsNullOrderByUltimaAtividadeEmDesc(UUID usuarioId, ModoSessao modo);
    List<SessaoEstudo> findByUsuarioIdAndModoNotAndFinalizadaEmIsNullAndExpiradaEmIsNullOrderByUltimaAtividadeEmDesc(UUID usuarioId, ModoSessao modo);
    List<SessaoEstudo> findByUsuarioIdAndIniciadaEmGreaterThanEqualAndIniciadaEmLessThan(UUID usuarioId, Instant inicio, Instant fim);
    @Query("select s from SessaoEstudo s where s.finalizadaEm is null and s.expiradaEm is null and s.ultimaAtividadeEm < :limite")
    List<SessaoEstudo> buscarRetomaveisExpiradas(@Param("limite") Instant limite);
    long countByFinalizadaEmIsNullAndExpiradaEmIsNull();
}
