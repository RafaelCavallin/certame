package br.com.certame.questao;

import br.com.certame.comum.Paginacao;
import br.com.certame.comum.PaginaResposta;
import br.com.certame.questao.dto.FiltroQuestao;
import br.com.certame.questao.dto.QuestaoResumo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class QuestaoBuscaRepositoryImpl implements QuestaoBuscaRepository {
    private final EntityManager entityManager;

    QuestaoBuscaRepositoryImpl(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override
    public PaginaResposta<QuestaoResumo> buscar(FiltroQuestao filtro, Paginacao paginacao, String termoCodigo) {
        var where = new StringBuilder(" from questao q left join disciplina d on d.id = q.disciplina_id left join banca b on b.id = q.banca_id where q.excluido_em is null");
        var parametros = new ArrayList<Parametro>();
        if (termoCodigo != null) { where.append(" and q.codigo like :codigo"); parametros.add(new Parametro("codigo", termoCodigo + "%")); }
        else if (filtro.temTermo()) {
            where.append(" and (q.busca @@ plainto_tsquery('portuguese', certame_unaccent(:termo)) or certame_unaccent(q.enunciado) ilike '%' || certame_unaccent(:termo) || '%' or certame_unaccent(q.enunciado) ilike '%' || replace(certame_unaccent(:termo), 'ao', 'oes') || '%')");
            parametros.add(new Parametro("termo", filtro.q().trim()));
        }
        if (filtro.disciplina() != null) adicionar(where, parametros, "q.disciplina_id = :disciplina", "disciplina", filtro.disciplina());
        if (filtro.assunto() != null) adicionar(where, parametros, "q.assunto_id = :assunto", "assunto", filtro.assunto());
        if (filtro.banca() != null) adicionar(where, parametros, "q.banca_id = :banca", "banca", filtro.banca());
        if (filtro.ano() != null) adicionar(where, parametros, "q.ano = :ano", "ano", filtro.ano());
        if (filtro.tipo() != null) adicionar(where, parametros, "q.tipo = :tipo", "tipo", filtro.tipo().name());
        if (filtro.status() != null) adicionar(where, parametros, "q.status = :status", "status", filtro.status().name());

        var count = entityManager.createNativeQuery("select count(*)" + where);
        aplicar(count, parametros);
        var total = ((Number) count.getSingleResult()).longValue();
        var sql = "select q.id, q.codigo, left(q.enunciado, 180), d.nome, b.nome, q.ano, q.tipo, q.status, q.atualizado_em" + where
                + " order by " + coluna(paginacao.sort()) + " " + direcao(paginacao.sort()) + " limit :limite offset :offset";
        var query = entityManager.createNativeQuery(sql);
        aplicar(query, parametros);
        query.setParameter("limite", paginacao.size());
        query.setParameter("offset", paginacao.page() * paginacao.size());
        @SuppressWarnings("unchecked") var linhas = (List<Object[]>) query.getResultList();
        var itens = linhas.stream().map(this::resumo).toList();
        return new PaginaResposta<>(itens, paginacao.page(), paginacao.size(), total);
    }

    private void adicionar(StringBuilder where, List<Parametro> parametros, String clausula, String nome, Object valor) {
        where.append(" and ").append(clausula); parametros.add(new Parametro(nome, valor));
    }
    private void aplicar(Query query, List<Parametro> parametros) { parametros.forEach(p -> query.setParameter(p.nome(), p.valor())); }
    private String coluna(String sort) {
        return switch (sort.split(",", -1)[0]) { case "atualizadoEm" -> "q.atualizado_em"; case "codigo" -> "q.codigo"; case "criadoEm" -> "q.criado_em"; default -> throw new IllegalArgumentException("sort"); };
    }
    private String direcao(String sort) {
        var partes = sort.split(",", -1); if (partes.length == 1 || partes[1].equals("desc")) return "desc";
        if (partes[1].equals("asc")) return "asc"; throw new IllegalArgumentException("sort");
    }
    private QuestaoResumo resumo(Object[] r) {
        return new QuestaoResumo((UUID) r[0], (String) r[1], (String) r[2], (String) r[3], (String) r[4],
                r[5] == null ? null : ((Number) r[5]).shortValue(), TipoQuestao.valueOf((String) r[6]),
                StatusQuestao.valueOf((String) r[7]), toInstant(r[8]));
    }
    private Instant toInstant(Object valor) {
        if (valor instanceof Instant instant) return instant;
        if (valor instanceof Timestamp timestamp) return timestamp.toInstant();
        return ((java.time.OffsetDateTime) valor).toInstant();
    }
    private record Parametro(String nome, Object valor) {}
}
