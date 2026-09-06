package br.com.certame.flashcard.revisao;

import br.com.certame.flashcard.revisao.dto.RequisicaoRevisao;
import br.com.certame.flashcard.revisao.dto.RequisicaoSessaoRevisao;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/flashcards")
public class RevisaoController {
    private final SessaoRevisaoService sessoes;
    private final RegistroRevisaoService registro;
    private final ResumoRevisaoService resumo;

    public RevisaoController(SessaoRevisaoService sessoes, RegistroRevisaoService registro, ResumoRevisaoService resumo) {
        this.sessoes = sessoes;
        this.registro = registro;
        this.resumo = resumo;
    }

    @PostMapping("/sessoes")
    public ResponseEntity<?> abrir(@Valid @RequestBody RequisicaoSessaoRevisao requisicao, Authentication auth) {
        var resposta = sessoes.abrir(usuario(auth), requisicao);
        return ResponseEntity.created(URI.create("/api/v1/flashcards/sessoes/" + resposta.id())).body(resposta);
    }

    @PostMapping("/sessoes/{id}/revisoes")
    public ResponseEntity<?> revisar(@PathVariable UUID id, @Valid @RequestBody RequisicaoRevisao requisicao, Authentication auth) {
        return ResponseEntity.ok(registro.registrar(id, usuario(auth), requisicao));
    }

    @PostMapping("/sessoes/{id}/finalizar")
    public ResponseEntity<?> finalizar(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(sessoes.finalizar(id, usuario(auth)));
    }

    @GetMapping("/resumo")
    public ResponseEntity<?> resumo(Authentication auth) {
        return ResponseEntity.ok(resumo.resumoDoUsuario(usuario(auth)));
    }

    private UUID usuario(Authentication auth) { return UUID.fromString(auth.getName()); }
}
