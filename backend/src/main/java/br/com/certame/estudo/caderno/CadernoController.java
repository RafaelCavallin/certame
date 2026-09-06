package br.com.certame.estudo.caderno;

import br.com.certame.estudo.caderno.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @Validated @RequestMapping("/api/v1/cadernos")
public class CadernoController {
    private final CadernoService service;
    private final ConsultaCadernosService consulta;
    private final ConsultaQuestoesCadernoService consultaQuestoes;

    public CadernoController(CadernoService service, ConsultaCadernosService consulta, ConsultaQuestoesCadernoService consultaQuestoes) {
        this.service = service; this.consulta = consulta; this.consultaQuestoes = consultaQuestoes;
    }

    @GetMapping
    public ResponseEntity<?> listar(Authentication auth) { return ResponseEntity.ok(consulta.listar(usuario(auth))); }

    @PostMapping
    public ResponseEntity<?> criar(@Valid @RequestBody RequisicaoCaderno requisicao, Authentication auth) {
        var resumo = service.criar(usuario(auth), requisicao);
        return ResponseEntity.created(URI.create("/api/v1/cadernos/" + resumo.id())).body(resumo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable String id, @Valid @RequestBody RequisicaoCaderno requisicao, Authentication auth) {
        return ResponseEntity.ok(service.atualizar(usuario(auth), id, requisicao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable String id, Authentication auth) {
        service.excluir(usuario(auth), id); return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/questoes")
    public ResponseEntity<?> listarQuestoes(@PathVariable String id, @RequestParam(defaultValue = "0") @Min(0) int page,
                                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size, Authentication auth) {
        return ResponseEntity.ok(consultaQuestoes.listar(usuario(auth), id, page, size));
    }

    @PostMapping("/{id}/questoes")
    public ResponseEntity<?> adicionar(@PathVariable String id, @Valid @RequestBody RequisicaoQuestoesCaderno requisicao, Authentication auth) {
        return ResponseEntity.ok(service.adicionarQuestoes(usuario(auth), id, requisicao.questaoIds()));
    }

    @DeleteMapping("/{id}/questoes/{questaoId}")
    public ResponseEntity<Void> remover(@PathVariable String id, @PathVariable UUID questaoId, Authentication auth) {
        service.removerQuestao(usuario(auth), id, questaoId); return ResponseEntity.noContent().build();
    }

    private UUID manual(String id) {
        var referencia = ReferenciaCaderno.de(id);
        if (referencia.automatico()) throw new br.com.certame.comum.excecao.CadernoAutomaticoImutavelException();
        return referencia.manualId();
    }

    private UUID usuario(Authentication auth) { return UUID.fromString(auth.getName()); }
}
