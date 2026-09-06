package br.com.certame.estudo;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SerieRepository extends JpaRepository<SessaoQuestao, SessaoQuestaoId> {
    @Query(value = """
            select q.id from questao q
            where q.status = 'PUBLICADA' and q.excluido_em is null
              and (cast(:#{#filtro.disciplinaId} as uuid) is null or q.disciplina_id = cast(:#{#filtro.disciplinaId} as uuid))
              and (cast(:#{#filtro.assuntoId} as uuid) is null or q.assunto_id = cast(:#{#filtro.assuntoId} as uuid))
              and (cast(:#{#filtro.bancaId} as uuid) is null or q.banca_id = cast(:#{#filtro.bancaId} as uuid))
              and (cast(:#{#filtro.ano} as integer) is null or q.ano = cast(:#{#filtro.ano} as integer))
              and (cast(:#{#filtro.dificuldade} as text) is null or q.dificuldade = cast(:#{#filtro.dificuldade} as text))
              and (not :#{#filtro.somenteIneditas} or not exists (
                  select 1 from resposta r where r.questao_id = q.id and r.usuario_id = :usuarioId))
              and (not :#{#filtro.excluirAcertadas} or not exists (
                  select 1 from resposta r where r.questao_id = q.id and r.usuario_id = :usuarioId and r.correta = true))
            """, nativeQuery = true)
    List<UUID> buscarElegiveis(@Param("filtro") FiltroSerie filtro, @Param("usuarioId") UUID usuarioId);
}
