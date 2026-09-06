package br.com.certame.simulado;

import br.com.certame.estudo.dto.RequisicaoFinalizacao;
import br.com.certame.simulado.dto.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/simulados")
public class SimuladoController {
    private final SimuladoCatalogoService catalogo;
    private final TentativaSimuladoService tentativas;

    public SimuladoController(SimuladoCatalogoService catalogo, TentativaSimuladoService tentativas) { this.catalogo = catalogo; this.tentativas = tentativas; }

    @GetMapping
    public ResponseEntity<RespostaSimulados> listar(Authentication auth) { return ResponseEntity.ok(catalogo.listarDoEstudante(usuario(auth))); }

    @PostMapping("/{id}/tentativas")
    public ResponseEntity<RespostaTentativa> iniciar(@PathVariable UUID id, Authentication auth) {
        var retomada = tentativas.temEmAndamento(usuario(auth), id);
        var resposta = tentativas.iniciar(usuario(auth), id);
        var localizacao = URI.create("/api/v1/estudo/sessoes/" + resposta.sessao().sessaoId());
        return retomada ? ResponseEntity.ok(resposta) : ResponseEntity.created(localizacao).body(resposta);
    }

    @PostMapping("/{id}/tentativas/{tentativaId}/finalizar")
    public ResponseEntity<RespostaResultado> finalizar(@PathVariable UUID id, @PathVariable UUID tentativaId, @Valid @RequestBody RequisicaoFinalizacao requisicao, Authentication auth) {
        return ResponseEntity.ok(tentativas.finalizar(usuario(auth), id, tentativaId, requisicao));
    }

    @GetMapping("/{id}/tentativas/{tentativaId}")
    public ResponseEntity<RespostaResultado> resultado(@PathVariable UUID id, @PathVariable UUID tentativaId, Authentication auth) {
        return ResponseEntity.ok(tentativas.resultado(usuario(auth), id, tentativaId));
    }

    private UUID usuario(Authentication auth) { return UUID.fromString(auth.getName()); }
}
