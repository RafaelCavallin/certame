package br.com.certame.flashcard.deck;

import br.com.certame.flashcard.dto.RequisicaoDeck;
import br.com.certame.flashcard.dto.RequisicaoMoverCards;
import br.com.certame.flashcard.dto.RespostaExclusaoDeck;
import br.com.certame.flashcard.dto.RespostaMoverCards;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/flashcards/decks")
public class DeckController {
    private final DeckService service;

    public DeckController(DeckService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> listar(Authentication auth) {
        return ResponseEntity.ok(service.listar(usuario(auth)));
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody RequisicaoDeck requisicao, Authentication auth) {
        var resumo = service.criar(usuario(auth), requisicao);
        return ResponseEntity.created(URI.create("/api/v1/flashcards/decks/" + resumo.id())).body(resumo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable UUID id, @Valid @RequestBody RequisicaoDeck requisicao, Authentication auth) {
        return ResponseEntity.ok(service.atualizar(id, usuario(auth), requisicao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(new RespostaExclusaoDeck(service.excluir(id, usuario(auth))));
    }

    @PostMapping("/{id}/mover")
    public ResponseEntity<?> mover(@PathVariable UUID id, @RequestBody RequisicaoMoverCards requisicao, Authentication auth) {
        var movidos = service.mover(id, usuario(auth), requisicao.flashcardIds());
        return ResponseEntity.ok(new RespostaMoverCards(movidos));
    }

    private UUID usuario(Authentication auth) { return UUID.fromString(auth.getName()); }
}
