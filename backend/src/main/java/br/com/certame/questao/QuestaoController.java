package br.com.certame.questao;

import br.com.certame.comum.Paginacao;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.questao.dto.FiltroQuestao;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Listagem pública de questões (TECH-SPEC.md §6 — `GET /questoes`): publicadas para estudante,
 * todas para professor+. Usada pelo seletor de questões dos cadernos (RF-13, CA-04).
 */
@RestController
@RequestMapping("/api/v1/questoes")
public class QuestaoController {
    private final BuscaQuestaoService busca;

    @Autowired
    public QuestaoController(BuscaQuestaoService busca) {
        this.busca = busca;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) String disciplina,
                                    @RequestParam(required = false) String assunto,
                                    @RequestParam(required = false) String banca,
                                    @RequestParam(required = false) Integer ano,
                                    @RequestParam(required = false) String tipo,
                                    @RequestParam(defaultValue = "0") Integer page,
                                    @RequestParam(defaultValue = "20") Integer size,
                                    @RequestParam(defaultValue = "atualizadoEm,desc") String sort,
                                    Authentication autenticacao) {
        var status = editorial(autenticacao) ? null : StatusQuestao.PUBLICADA;
        var filtro = new FiltroQuestao(q, uuid(disciplina, "disciplina"), uuid(assunto, "assunto"), uuid(banca, "banca"), ano,
                enumValue(tipo, TipoQuestao.class, "tipo"), status);
        return ResponseEntity.ok(busca.buscarPublica(filtro, new Paginacao(page, size, sort)));
    }

    private boolean editorial(Authentication autenticacao) {
        return autenticacao.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch(papel -> papel.equals("ROLE_PROFESSOR") || papel.equals("ROLE_ADMIN") || papel.equals("ROLE_SUPER_ADMIN"));
    }

    private UUID uuid(String valor, String campo) {
        if (valor == null || valor.isBlank()) return null;
        try { return UUID.fromString(valor); } catch (IllegalArgumentException e) { throw new DadosInvalidosException(Map.of(campo, "Informe um UUID válido.")); }
    }

    private <E extends Enum<E>> E enumValue(String valor, Class<E> tipo, String campo) {
        if (valor == null || valor.isBlank()) return null;
        try { return Enum.valueOf(tipo, valor); } catch (IllegalArgumentException e) { throw new DadosInvalidosException(Map.of(campo, "Informe um valor válido.")); }
    }
}
