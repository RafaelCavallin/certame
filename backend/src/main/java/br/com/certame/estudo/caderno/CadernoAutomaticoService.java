package br.com.certame.estudo.caderno;

import br.com.certame.estudo.caderno.dto.CadernoResumo;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CadernoAutomaticoService {
    private static final String ERROS = "Questões que você marcou como erro recorrente durante as sessões.";
    private static final String FAVORITAS = "Questões que você favoritou para rever depois.";
    private final JdbcTemplate jdbc;

    public CadernoAutomaticoService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<CadernoResumo> projetar(UUID usuarioId) {
        return List.of(projetar(usuarioId, TipoCadernoAutomatico.ERRO_RECORRENTE, "Erros recorrentes", ERROS),
                projetar(usuarioId, TipoCadernoAutomatico.FAVORITA, "Favoritas", FAVORITAS));
    }

    public List<UUID> questoes(UUID usuarioId, TipoCadernoAutomatico tipo) {
        return jdbc.query("select questao_id from marcacao where usuario_id = ? and tipo = ? order by questao_id",
                (rs, row) -> rs.getObject("questao_id", UUID.class), usuarioId, tipo.name());
    }

    private CadernoResumo projetar(UUID usuarioId, TipoCadernoAutomatico tipo, String nome, String descricao) {
        var dados = jdbc.queryForObject("""
                select count(*) as total, max(criado_em) as atualizado
                  from marcacao where usuario_id = ? and tipo = ?
                """, (rs, row) -> new Dados(rs.getInt("total"), atualizado(rs)), usuarioId, tipo.name());
        var total = dados == null ? 0 : dados.total;
        var atualizada = dados == null ? null : dados.atualizado;
        var respondidas = respondidas(usuarioId, tipo);
        return CadernoResumo.automatico(tipo, nome, descricao, total, respondidas,
                ProgressoCaderno.percentual(respondidas, total), atualizada);
    }

    private Instant atualizado(java.sql.ResultSet rs) throws java.sql.SQLException {
        var timestamp = rs.getTimestamp("atualizado");
        return timestamp == null ? null : timestamp.toInstant();
    }

    private int respondidas(UUID usuarioId, TipoCadernoAutomatico tipo) {
        Integer total = jdbc.queryForObject("""
                select count(distinct r.questao_id) from resposta r join sessao_estudo s on s.id = r.sessao_id
                 where r.usuario_id = ? and s.finalizada_em is not null and r.questao_id in
                   (select questao_id from marcacao where usuario_id = ? and tipo = ?)
                """, Integer.class, usuarioId, usuarioId, tipo.name());
        return total == null ? 0 : total;
    }

    private record Dados(int total, Instant atualizado) {}
}
