package br.com.certame.flashcard.revisao;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessaoRevisaoRepository extends JpaRepository<SessaoRevisao, UUID> {

    Optional<SessaoRevisao> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    @Query("select coalesce(sum(s.resultado.tempoMs), 0) from SessaoRevisao s"
            + " where s.usuario.id = :usuarioId and s.resultado.finalizadaEm is not null"
            + "   and s.resultado.finalizadaEm >= :inicio and s.resultado.finalizadaEm < :fim")
    long somarTempoMsFinalizadasEntre(@Param("usuarioId") UUID usuarioId, @Param("inicio") Instant inicio,
            @Param("fim") Instant fim);
}
