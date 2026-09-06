package br.com.certame.flashcard;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class FlashcardBuscaRepositoryImpl implements FlashcardBuscaRepository {
    private final EntityManager entityManager;

    FlashcardBuscaRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<Flashcard> buscar(UUID usuarioId, FiltroFlashcards filtro, StatusFlashcard status, Pageable pageable) {
        var where = new StringBuilder(" from flashcard f where f.usuario_id = :usuarioId and f.status = :status");
        var parametros = new ArrayList<Parametro>();
        parametros.add(new Parametro("usuarioId", usuarioId));
        parametros.add(new Parametro("status", status.name()));
        aplicarTermo(where, parametros, filtro);
        aplicarDeck(where, parametros, filtro.deckId());
        aplicarIgualdade(where, parametros, "f.disciplina_id", "disciplinaId", filtro.disciplinaId());
        aplicarIgualdade(where, parametros, "f.estado", "estado", filtro.estado() == null ? null : filtro.estado().name());
        var total = contar(where, parametros);
        var conteudo = buscarPagina(where, parametros, pageable);
        return new PageImpl<>(conteudo, pageable, total);
    }

    private void aplicarTermo(StringBuilder where, List<Parametro> parametros, FiltroFlashcards filtro) {
        if (!filtro.temTermo()) return;
        where.append(" and f.busca @@ websearch_to_tsquery('portuguese', :termo)");
        parametros.add(new Parametro("termo", filtro.termo().trim()));
    }

    private void aplicarDeck(StringBuilder where, List<Parametro> parametros, String deckId) {
        if (deckId == null) return;
        if (deckId.equals(FiltroFlashcards.SEM_DECK)) {
            where.append(" and f.deck_id is null");
            return;
        }
        where.append(" and f.deck_id = :deckId");
        parametros.add(new Parametro("deckId", UUID.fromString(deckId)));
    }

    private void aplicarIgualdade(StringBuilder where, List<Parametro> parametros, String coluna, String nome, Object valor) {
        if (valor == null) return;
        where.append(" and ").append(coluna).append(" = :").append(nome);
        parametros.add(new Parametro(nome, valor));
    }

    private long contar(StringBuilder where, List<Parametro> parametros) {
        var query = entityManager.createNativeQuery("select count(*)" + where);
        aplicar(query, parametros);
        return ((Number) query.getSingleResult()).longValue();
    }

    private List<Flashcard> buscarPagina(StringBuilder where, List<Parametro> parametros, Pageable pageable) {
        var sql = "select f.*" + where + " order by " + ordenacao(pageable.getSort()) + " limit :limite offset :offset";
        var query = entityManager.createNativeQuery(sql, Flashcard.class);
        aplicar(query, parametros);
        query.setParameter("limite", pageable.getPageSize());
        query.setParameter("offset", pageable.getOffset());
        @SuppressWarnings("unchecked")
        List<Flashcard> resultado = query.getResultList();
        return resultado;
    }

    private void aplicar(Query query, List<Parametro> parametros) {
        parametros.forEach(p -> query.setParameter(p.nome(), p.valor()));
    }

    private String ordenacao(Sort sort) {
        if (sort.isUnsorted()) return "f.criado_em desc";
        var ordem = sort.iterator().next();
        var coluna = switch (ordem.getProperty()) {
            case "criadoEm" -> "f.criado_em";
            case "proximaRevisao" -> "f.proxima_revisao";
            default -> throw new IllegalStateException("Ordenacao nao validada antes do repositorio: " + ordem.getProperty());
        };
        return coluna + " " + (ordem.isAscending() ? "asc" : "desc");
    }

    private record Parametro(String nome, Object valor) {
    }
}
