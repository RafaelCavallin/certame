package br.com.certame.usuario;

import br.com.certame.comum.Paginacao;
import br.com.certame.comum.excecao.DadosInvalidosException;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Converte a Paginacao da listagem de usuários em Pageable, restrita às ordenações suportadas. */
final class PaginadorUsuarios {
    private static final Set<String> ORDENACOES_PERMITIDAS = Set.of("criadoEm,asc", "criadoEm,desc");

    private PaginadorUsuarios() {
    }

    static Pageable paginavel(Paginacao paginacao) {
        if (!ORDENACOES_PERMITIDAS.contains(paginacao.sort())) {
            throw new DadosInvalidosException(Map.of("sort", "A ordenação informada não é válida."));
        }
        var partes = paginacao.sort().split(",");
        return PageRequest.of(paginacao.page(), paginacao.size(), Sort.by(Sort.Direction.fromString(partes[1]), partes[0]));
    }
}
