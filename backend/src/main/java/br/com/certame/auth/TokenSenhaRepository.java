package br.com.certame.auth;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenSenhaRepository extends JpaRepository<TokenSenha, UUID> {
    Optional<TokenSenha> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update TokenSenha t set t.usadoEm = :agora where t.usuario.id = :usuarioId and t.usadoEm is null and t.finalidade = :finalidade")
    int invalidarPendentes(@Param("usuarioId") UUID usuarioId, @Param("finalidade") FinalidadeTokenSenha finalidade,
            @Param("agora") Instant agora);

    long deleteByExpiraEmBefore(Instant instante);
}
