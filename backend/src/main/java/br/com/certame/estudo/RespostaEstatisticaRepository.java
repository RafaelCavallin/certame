package br.com.certame.estudo;

import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RespostaEstatisticaRepository {
    private final JdbcTemplate jdbc;
    public RespostaEstatisticaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Agregado contarValidas(UUID questaoId, UUID autorId) {
        return jdbc.queryForObject("""
                select count(*) total_respostas, count(*) filter (where r.correta) total_acertos,
                       avg(r.tempo_ms)::integer tempo_medio_ms from resposta r
                where r.questao_id = ? and r.usuario_id <> ?
                """, (rs, rowNum) -> new Agregado(rs.getInt("total_respostas"), rs.getInt("total_acertos"), rs.getObject("tempo_medio_ms", Integer.class)), questaoId, autorId);
    }
    public List<PorSemana> porSemana(UUID usuarioId) { return jdbc.query("select date_trunc('week', s.finalizada_em at time zone 'America/Sao_Paulo')::date semana, count(*) volume, count(*) filter (where r.correta) acertos from resposta r join sessao_estudo s on s.id = r.sessao_id where r.usuario_id = ? and s.finalizada_em is not null group by 1 order by 1", (rs, n) -> new PorSemana(rs.getObject("semana", LocalDate.class), rs.getInt("volume"), rs.getInt("acertos")), usuarioId); }
    public List<PorDisciplina> porDisciplina(UUID usuarioId) { return jdbc.query("select q.disciplina_id, count(*) volume, count(*) filter (where r.correta) acertos from resposta r join sessao_estudo s on s.id = r.sessao_id join questao q on q.id = r.questao_id where r.usuario_id = ? and s.finalizada_em is not null group by q.disciplina_id order by volume desc, q.disciplina_id", (rs, n) -> new PorDisciplina(rs.getObject("disciplina_id", UUID.class), rs.getInt("volume"), rs.getInt("acertos")), usuarioId); }
    public List<PorAssunto> porAssunto(UUID usuarioId) { return jdbc.query("select q.assunto_id, q.disciplina_id, count(*) volume, count(*) filter (where r.correta) acertos from resposta r join sessao_estudo s on s.id = r.sessao_id join questao q on q.id = r.questao_id where r.usuario_id = ? and s.finalizada_em is not null and q.assunto_id is not null group by q.assunto_id, q.disciplina_id order by volume desc, q.assunto_id", (rs, n) -> new PorAssunto(rs.getObject("assunto_id", UUID.class), rs.getObject("disciplina_id", UUID.class), rs.getInt("volume"), rs.getInt("acertos")), usuarioId); }
    public record Agregado(int totalRespostas, int totalAcertos, Integer tempoMedioMs) {}
    public record PorSemana(LocalDate semana, int volume, int acertos) {}
    public record PorDisciplina(UUID disciplinaId, int volume, int acertos) {}
    public record PorAssunto(UUID assuntoId, UUID disciplinaId, int volume, int acertos) {}
}
