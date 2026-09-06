package br.com.certame.questao;

import br.com.certame.questao.dto.RequisicaoQuestao;
import br.com.certame.comum.Paginacao;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.questao.dto.FiltroQuestao;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/questoes")
public class QuestaoAdminController {
    private final QuestaoService service;
    private final PublicacaoService publicacaoService;
    private final BuscaQuestaoService buscaService;

    @Autowired
    public QuestaoAdminController(QuestaoService service, PublicacaoService publicacaoService, BuscaQuestaoService buscaService) {
        this.service = service; this.publicacaoService = publicacaoService; this.buscaService = buscaService;
    }

    public QuestaoAdminController(QuestaoService service, PublicacaoService publicacaoService) {
        this(service, publicacaoService, null);
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) String disciplina,
                                    @RequestParam(required = false) String assunto,
                                    @RequestParam(required = false) String banca,
                                    @RequestParam(required = false) Integer ano,
                                    @RequestParam(required = false) String tipo,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(defaultValue = "0") Integer page,
                                    @RequestParam(defaultValue = "20") Integer size,
                                    @RequestParam(defaultValue = "atualizadoEm,desc") String sort) {
        var filtro = new FiltroQuestao(q, uuid(disciplina, "disciplina"), uuid(assunto, "assunto"), uuid(banca, "banca"), ano,
                enumValue(tipo, TipoQuestao.class, "tipo"), enumValue(status, StatusQuestao.class, "status"));
        return ResponseEntity.ok(buscaService.buscar(filtro, new Paginacao(page, size, sort)));
    }

    @GetMapping("/contagem")
    public ResponseEntity<?> contar(@RequestParam UUID disciplinaId, @RequestParam(required = false) String dificuldade) {
        var dificuldadeValor = enumValue(dificuldade, Dificuldade.class, "dificuldade");
        var total = dificuldadeValor == null
                ? service.contarDisponiveis(disciplinaId)
                : service.contarDisponiveis(disciplinaId, dificuldadeValor);
        return ResponseEntity.ok(Map.of("disponiveis", total));
    }

    private UUID uuid(String valor, String campo) {
        if (valor == null || valor.isBlank()) return null;
        try { return UUID.fromString(valor); } catch (IllegalArgumentException e) { throw new DadosInvalidosException(Map.of(campo, "Informe um UUID válido.")); }
    }

    private <E extends Enum<E>> E enumValue(String valor, Class<E> tipo, String campo) {
        if (valor == null || valor.isBlank()) return null;
        try { return Enum.valueOf(tipo, valor); } catch (IllegalArgumentException e) { throw new DadosInvalidosException(Map.of(campo, "Informe um valor válido.")); }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable UUID id) { return ResponseEntity.ok(service.buscarPorId(id)); }

    @PostMapping
    public ResponseEntity<?> criar(@Valid @RequestBody RequisicaoQuestao requisicao, Authentication autenticacao) {
        var detalhe = service.criar(requisicao, UUID.fromString(autenticacao.getName()));
        return ResponseEntity.created(URI.create("/api/v1/admin/questoes/" + detalhe.id())).body(detalhe);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable UUID id, @Valid @RequestBody RequisicaoQuestao requisicao) {
        return ResponseEntity.ok(service.atualizar(id, requisicao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enviar-revisao")
    public ResponseEntity<?> enviarRevisao(@PathVariable UUID id) { return ResponseEntity.ok(publicacaoService.enviarParaRevisao(id)); }

    @PostMapping("/{id}/publicar")
    public ResponseEntity<?> publicar(@PathVariable UUID id) { return ResponseEntity.ok(publicacaoService.publicar(id)); }

    @PostMapping("/{id}/despublicar")
    public ResponseEntity<?> despublicar(@PathVariable UUID id) { return ResponseEntity.ok(publicacaoService.despublicar(id)); }
}
