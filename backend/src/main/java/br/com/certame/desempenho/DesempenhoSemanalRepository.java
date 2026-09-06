package br.com.certame.desempenho;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DesempenhoSemanalRepository extends JpaRepository<DesempenhoSemanal, DesempenhoSemanal.Id> {
    List<DesempenhoSemanal> findByIdUsuarioIdOrderByIdSemanaAsc(UUID usuarioId);
    List<DesempenhoSemanal> findByIdUsuarioIdAndIdSemanaBetweenOrderByIdSemanaAsc(UUID usuarioId, LocalDate inicio, LocalDate fim);
    @Modifying
    @Query(value = "delete from desempenho_semanal where usuario_id = :usuarioId", nativeQuery = true)
    void excluirDoUsuario(@Param("usuarioId") UUID usuarioId);
    @Modifying
    @Query(value = """
            insert into desempenho_semanal (usuario_id, semana, volume, acertos, pct_acerto)
            values (:usuarioId, :semana, :volume, :acertos, :pctAcerto)
            on conflict (usuario_id, semana) do update set volume = excluded.volume,
              acertos = excluded.acertos, pct_acerto = excluded.pct_acerto, atualizado_em = now()
            """, nativeQuery = true)
    void upsert(@Param("usuarioId") UUID usuarioId, @Param("semana") LocalDate semana, @Param("volume") int volume,
            @Param("acertos") int acertos, @Param("pctAcerto") short pctAcerto);
}
