package br.com.certame.estudo;

import br.com.certame.estudo.dto.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/estudo")
public class SessaoEstudoController {
    private static final Logger LOG = LoggerFactory.getLogger(SessaoEstudoController.class);
    private final SessaoEstudoService sessoes;
    private final RegistroRespostaService respostas;
    private final MarcacaoService marcacoes;
    private final EstudoObservabilidade observabilidade;

    public SessaoEstudoController(SessaoEstudoService sessoes, RegistroRespostaService respostas,
                                  MarcacaoService marcacoes, EstudoObservabilidade observabilidade) {
        this.sessoes = sessoes; this.respostas = respostas; this.marcacoes = marcacoes; this.observabilidade = observabilidade;
    }

    @PostMapping("/sessoes")
    public ResponseEntity<RespostaSessao> abrir(@Valid @RequestBody RequisicaoSessao req, Authentication auth) {
        var inicio = observabilidade.iniciar();
        var retorno = sessoes.abrir(usuario(auth), req);
        observabilidade.abertura(inicio, retorno.modo(), retorno.total(), retorno.origem());
        LOG.info("sessao_aberta sessaoId={} usuarioId={} modo={} tamanho={} origem={}", retorno.sessaoId(), usuario(auth), retorno.modo(), retorno.total(), retorno.origem());
        return ResponseEntity.created(URI.create("/api/v1/estudo/sessoes/" + retorno.sessaoId())).body(retorno);
    }

    @GetMapping("/sessoes/{id}")
    public ResponseEntity<RespostaSessao> retomar(@PathVariable UUID id, Authentication auth) { return ResponseEntity.ok(sessoes.retomar(usuario(auth), id)); }
    @PostMapping("/sessoes/{id}/pausar")
    public ResponseEntity<Void> pausar(@PathVariable UUID id, Authentication auth) { sessoes.pausar(usuario(auth), id); return ResponseEntity.noContent().build(); }
    @PostMapping("/sessoes/{id}/retomar")
    public ResponseEntity<RespostaSessao> retomarSessao(@PathVariable UUID id, Authentication auth) { return ResponseEntity.ok(sessoes.retomarSessao(usuario(auth), id)); }
    @PatchMapping("/sessoes/{id}/modo")
    public ResponseEntity<RespostaSessao> modo(@PathVariable UUID id, @Valid @RequestBody RequisicaoModo req, Authentication auth) { return ResponseEntity.ok(sessoes.trocarModo(usuario(auth), id, req.modo())); }
    @PostMapping("/sessoes/{id}/respostas")
    public ResponseEntity<RespostaRegistrada> responder(@PathVariable UUID id, @Valid @RequestBody RequisicaoResposta req, Authentication auth) {
        var inicio = observabilidade.iniciar();
        var retorno = respostas.registrar(usuario(auth), id, req);
        observabilidade.resposta(inicio, modoAtual(usuario(auth), id));
        return ResponseEntity.status(201).body(retorno);
    }
    @PostMapping("/sessoes/{id}/finalizar")
    public ResponseEntity<RespostaCorrecao> finalizar(@PathVariable UUID id, @Valid @RequestBody RequisicaoFinalizacao req, Authentication auth) {
        var inicio = observabilidade.iniciar();
        var modo = modoAtual(usuario(auth), id);
        var retorno = sessoes.finalizar(usuario(auth), id, req);
        observabilidade.finalizacao(inicio, modo, retorno.total());
        LOG.info("sessao_finalizada sessaoId={} usuarioId={} modo={} tamanho={}", id, usuario(auth), modo, retorno.total());
        return ResponseEntity.ok(retorno);
    }
    @PutMapping("/questoes/{id}/marcacoes")
    public ResponseEntity<MarcacoesQuestao> marcar(@PathVariable UUID id, @Valid @RequestBody RequisicaoMarcacoes req, Authentication auth) { return ResponseEntity.ok(marcacoes.aplicar(usuario(auth), id, req)); }
    private UUID usuario(Authentication auth) { return UUID.fromString(auth.getName()); }
    private ModoSessao modoAtual(UUID usuarioId, UUID sessaoId) { return sessoes.modoAtual(usuarioId, sessaoId); }
}
