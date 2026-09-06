package br.com.certame.desempenho;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DesempenhoAssuntoRepository extends JpaRepository<DesempenhoAssunto, DesempenhoAssunto.Id> {
    List<DesempenhoAssunto> findByIdUsuarioIdAndVolumeGreaterThanEqualOrderByVolumeDesc(UUID usuarioId, int volume);
    @Modifying
    @Query(value = "delete from desempenho_assunto where usuario_id = :usuarioId", nativeQuery = true)
    void excluirDoUsuario(@Param("usuarioId") UUID usuarioId);
    @Modifying
    @Query(value = """
            insert into desempenho_assunto (usuario_id, assunto_id, disciplina_id, volume, acertos, pct_acerto)
            values (:usuarioId, :assuntoId, :disciplinaId, :volume, :acertos, :pctAcerto)
            on conflict (usuario_id, assunto_id) do update set disciplina_id = excluded.disciplina_id,
              volume = excluded.volume, acertos = excluded.acertos, pct_acerto = excluded.pct_acerto, atualizado_em = now()
            """, nativeQuery = true)
    void upsert(@Param("usuarioId") UUID usuarioId, @Param("assuntoId") UUID assuntoId, @Param("disciplinaId") UUID disciplinaId,
            @Param("volume") int volume, @Param("acertos") int acertos, @Param("pctAcerto") short pctAcerto);
}
