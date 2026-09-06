package br.com.certame.estudo.caderno;

import br.com.certame.comum.PaginaResposta;
import br.com.certame.estudo.caderno.dto.QuestaoCadernoResumo;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaQuestoesCadernoService {
    private final JdbcTemplate jdbc;
    private final CadernoService cadernos;
    private final CadernoAutomaticoService automaticos;

    public ConsultaQuestoesCadernoService(JdbcTemplate jdbc, CadernoService cadernos, CadernoAutomaticoService automaticos) {
        this.jdbc = jdbc; this.cadernos = cadernos; this.automaticos = automaticos;
    }

    @Transactional(readOnly = true)
    public PaginaResposta<QuestaoCadernoResumo> listar(UUID usuarioId, String id, int page, int size) {
        var referencia = ReferenciaCaderno.de(id);
        var ids = referencia.automatico() ? automaticos.questoes(usuarioId, referencia.tipoAutomatico()) : idsManual(usuarioId, referencia.manualId());
        var total = ids.size();
        var inicio = Math.min(page * size, total);
        var fim = Math.min(inicio + size, total);
        var conteudo = buscar(usuarioId, ids.subList(inicio, fim));
        return new PaginaResposta<>(conteudo, page, size, total);
    }

    private List<UUID> idsManual(UUID usuarioId, UUID id) {
        cadernos.obter(usuarioId, id);
        return jdbc.query("select questao_id from caderno_questao where caderno_id = ? order by adicionada_em, questao_id",
                (rs, row) -> rs.getObject("questao_id", UUID.class), id);
    }

    private List<QuestaoCadernoResumo> buscar(UUID usuarioId, List<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        var placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        var parametros = new ArrayList<Object>();
        parametros.add(usuarioId);
        parametros.addAll(ids);
        var encontrados = jdbc.query("""
                select q.id, q.codigo, left(q.enunciado, 160) enunciado, d.nome disciplina, a.nome assunto,
                       exists (select 1 from resposta r join sessao_estudo s on s.id = r.sessao_id
                               where r.usuario_id = ? and r.questao_id = q.id and s.finalizada_em is not null) respondida
                  from questao q join disciplina d on d.id = q.disciplina_id left join assunto a on a.id = q.assunto_id
                 where q.id in (""" + placeholders + ")", (rs, row) -> new QuestaoCadernoResumo(rs.getObject("id", UUID.class), rs.getString("codigo"),
                rs.getString("enunciado"), rs.getString("disciplina"), rs.getString("assunto"), rs.getBoolean("respondida")), parametros.toArray());
        var porId = encontrados.stream().collect(java.util.stream.Collectors.toMap(QuestaoCadernoResumo::questaoId, q -> q));
        return ids.stream().map(porId::get).filter(Objects::nonNull).toList();
    }
}
