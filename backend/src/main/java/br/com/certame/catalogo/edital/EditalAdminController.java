package br.com.certame.catalogo.edital;

import br.com.certame.catalogo.edital.dto.EditalAdminResumo;
import br.com.certame.catalogo.edital.dto.EditalDetalhe;
import br.com.certame.catalogo.edital.dto.RequisicaoEdital;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/editais")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class EditalAdminController {
    private final EditalService editalService;
    private final ConsultaEditaisService consulta;

    public EditalAdminController(EditalService editalService, ConsultaEditaisService consulta) {
        this.editalService = editalService;
        this.consulta = consulta;
    }

    @GetMapping
    public ResponseEntity<List<EditalAdminResumo>> listar() {
        return ResponseEntity.ok(consulta.listarParaAdministracao());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EditalDetalhe> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(consulta.buscarParaEdicao(id));
    }

    @PostMapping
    public ResponseEntity<EditalDetalhe> criar(@Valid @RequestBody RequisicaoEdital requisicao) {
        var edital = editalService.criar(requisicao);
        return ResponseEntity.created(URI.create("/api/v1/admin/editais/" + edital.id())).body(edital);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EditalDetalhe> atualizar(
            @PathVariable UUID id, @Valid @RequestBody RequisicaoEdital requisicao) {
        return ResponseEntity.ok(editalService.atualizar(id, requisicao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        editalService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
