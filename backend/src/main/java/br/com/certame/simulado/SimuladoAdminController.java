package br.com.certame.simulado;

import br.com.certame.comum.Paginacao;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.simulado.dto.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/simulados")
public class SimuladoAdminController {
    private static final int TAMANHO_MAXIMO = 100;
    private final SimuladoAdminService service;
    private final PublicacaoSimuladoService publicacao;

    public SimuladoAdminController(SimuladoAdminService service, PublicacaoSimuladoService publicacao) {
        this.service = service;
        this.publicacao = publicacao;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String q,
            @RequestParam(required = false) String estado, @RequestParam(required = false) UUID bancaId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        validarPaginacao(page, size);
        validarEstado(estado);
        return ResponseEntity.ok(service.listar(new FiltroSimulado(q, normalizar(estado), bancaId), new Paginacao(page, size)));
    }

    @PostMapping
    public ResponseEntity<?> criar(@Valid @RequestBody RequisicaoSimulado requisicao) {
        var detalhe = service.criar(requisicao);
        return ResponseEntity.created(URI.create("/api/v1/admin/simulados/" + detalhe.id())).body(detalhe);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detalhar(@PathVariable UUID id) { return ResponseEntity.ok(service.detalhar(id)); }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable UUID id, @Valid @RequestBody RequisicaoSimulado requisicao) {
        return ResponseEntity.ok(service.atualizar(id, requisicao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/composicao")
    public ResponseEntity<?> substituirComposicao(@PathVariable UUID id, @Valid @RequestBody RequisicaoComposicao requisicao) {
        return ResponseEntity.ok(service.substituirComposicao(id, requisicao.itens()));
    }

    @PostMapping("/{id}/publicar")
    public ResponseEntity<?> publicar(@PathVariable UUID id) {
        publicacao.publicar(id);
        return ResponseEntity.ok(service.detalhar(id));
    }

    @PostMapping("/{id}/despublicar")
    public ResponseEntity<?> despublicar(@PathVariable UUID id) {
        publicacao.despublicar(id);
        return ResponseEntity.ok(service.detalhar(id));
    }

    private void validarPaginacao(int page, int size) {
        if (page < 0) throw new DadosInvalidosException(Map.of("page", "A página deve ser maior ou igual a zero."));
        if (size < 1 || size > TAMANHO_MAXIMO) throw new DadosInvalidosException(Map.of("size", "O tamanho deve estar entre 1 e 100."));
    }

    private void validarEstado(String estado) {
        if (estado == null || estado.isBlank()) return;
        try { EstadoSimulado.valueOf(estado); } catch (IllegalArgumentException excecao) {
            throw new DadosInvalidosException(Map.of("estado", "Informe um estado válido."));
        }
    }

    private String normalizar(String valor) { return valor == null || valor.isBlank() ? null : valor; }
}
