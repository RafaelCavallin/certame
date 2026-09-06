package br.com.certame.catalogo;

import br.com.certame.catalogo.dto.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class CatalogoController {
    private final TaxonomiaService taxonomia; private final CatalogoService catalogo;
    public CatalogoController(TaxonomiaService taxonomia, CatalogoService catalogo) { this.taxonomia=taxonomia; this.catalogo=catalogo; }
    @GetMapping("/disciplinas") public ResponseEntity<?> disciplinas(@RequestParam(defaultValue="false") boolean incluirArquivados) { return ResponseEntity.ok(taxonomia.arvore(incluirArquivados)); }
    @PostMapping("/disciplinas") public ResponseEntity<?> criarDisciplina(@Valid @RequestBody RequisicaoNoTaxonomia r) { return ResponseEntity.status(201).body(taxonomia.criarDisciplina(r)); }
    @PutMapping("/disciplinas/{id}") public ResponseEntity<?> renomearDisciplina(@PathVariable UUID id, @Valid @RequestBody RequisicaoNoTaxonomia r) { return ResponseEntity.ok(taxonomia.renomearDisciplina(id, r)); }
    @DeleteMapping("/disciplinas/{id}") public ResponseEntity<Void> excluirDisciplina(@PathVariable UUID id) { taxonomia.excluirDisciplina(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/assuntos") public ResponseEntity<?> criarAssunto(@Valid @RequestBody RequisicaoNoTaxonomia r) { return ResponseEntity.status(201).body(taxonomia.criarAssunto(r)); }
    @PutMapping("/assuntos/{id}") public ResponseEntity<?> renomearAssunto(@PathVariable UUID id, @Valid @RequestBody RequisicaoNoTaxonomia r) { return ResponseEntity.ok(taxonomia.renomearAssunto(id, r)); }
    @DeleteMapping("/assuntos/{id}") public ResponseEntity<Void> excluirAssunto(@PathVariable UUID id) { taxonomia.excluirAssunto(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/assuntos/{id}/arquivar") public ResponseEntity<Void> arquivarAssunto(@PathVariable UUID id) { taxonomia.arquivarAssunto(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/bancas") public ResponseEntity<?> listarBancas() { return ResponseEntity.ok(catalogo.listarBancas()); }
    @PostMapping("/bancas") public ResponseEntity<?> criarBanca(@Valid @RequestBody RequisicaoItemCatalogo r) { return ResponseEntity.status(201).body(catalogo.criarBanca(r)); }
    @PutMapping("/bancas/{id}") public ResponseEntity<?> renomearBanca(@PathVariable UUID id, @Valid @RequestBody RequisicaoItemCatalogo r) { return ResponseEntity.ok(catalogo.renomearBanca(id, r)); }
    @DeleteMapping("/bancas/{id}") public ResponseEntity<Void> excluirBanca(@PathVariable UUID id) { catalogo.excluirBanca(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/orgaos") public ResponseEntity<?> listarOrgaos() { return ResponseEntity.ok(catalogo.listarOrgaos()); }
    @PostMapping("/orgaos") public ResponseEntity<?> criarOrgao(@Valid @RequestBody RequisicaoItemCatalogo r) { return ResponseEntity.status(201).body(catalogo.criarOrgao(r)); }
    @PutMapping("/orgaos/{id}") public ResponseEntity<?> renomearOrgao(@PathVariable UUID id, @Valid @RequestBody RequisicaoItemCatalogo r) { return ResponseEntity.ok(catalogo.renomearOrgao(id, r)); }
    @DeleteMapping("/orgaos/{id}") public ResponseEntity<Void> excluirOrgao(@PathVariable UUID id) { catalogo.excluirOrgao(id); return ResponseEntity.noContent().build(); }
}
