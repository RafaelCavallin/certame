package br.com.certame.flashcard;

import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.flashcard.dto.RequisicaoFlashcard;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/flashcards")
public class FlashcardController {
    private final FlashcardService service;
    private final FlashcardCicloVidaService cicloVida;
    private final ConsultaFlashcardsService consulta;

    public FlashcardController(FlashcardService service, FlashcardCicloVidaService cicloVida,
            ConsultaFlashcardsService consulta) {
        this.service = service;
        this.cicloVida = cicloVida;
        this.consulta = consulta;
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody RequisicaoFlashcard requisicao, Authentication auth) {
        var resposta = service.criar(usuario(auth), requisicao);
        return ResponseEntity.created(URI.create("/api/v1/flashcards/" + resposta.flashcard().id())).body(resposta);
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String q, @RequestParam(required = false) String deck,
            @RequestParam(required = false) String disciplina, @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "false") boolean arquivados, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "criadoEm,desc") String sort,
            Authentication auth) {
        var filtro = new FiltroFlashcards(q, deckId(deck), uuidOuNulo(disciplina, "disciplina"), estado(estado), arquivados);
        return ResponseEntity.ok(consulta.listar(usuario(auth), filtro, paginavel(page, size, sort)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(consulta.buscar(id, usuario(auth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable UUID id, @RequestBody RequisicaoFlashcard requisicao, Authentication auth) {
        return ResponseEntity.ok(service.atualizar(id, usuario(auth), requisicao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id, Authentication auth) {
        cicloVida.excluir(id, usuario(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/arquivar")
    public ResponseEntity<Void> arquivar(@PathVariable UUID id, Authentication auth) {
        cicloVida.arquivar(id, usuario(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restaurar")
    public ResponseEntity<Void> restaurar(@PathVariable UUID id, Authentication auth) {
        cicloVida.restaurar(id, usuario(auth));
        return ResponseEntity.noContent().build();
    }

    private String deckId(String deck) {
        if (deck == null || deck.isBlank()) return null;
        if (deck.equals(FiltroFlashcards.SEM_DECK)) return deck;
        return uuidOuNulo(deck, "deck").toString();
    }

    private EstadoFlashcard estado(String estado) {
        if (estado == null || estado.isBlank()) return null;
        try { return EstadoFlashcard.valueOf(estado); }
        catch (IllegalArgumentException e) { throw new DadosInvalidosException(Map.of("estado", "Informe um estado válido.")); }
    }

    private UUID uuidOuNulo(String valor, String campo) {
        if (valor == null || valor.isBlank()) return null;
        try { return UUID.fromString(valor); }
        catch (IllegalArgumentException e) { throw new DadosInvalidosException(Map.of(campo, "Informe um identificador válido.")); }
    }

    private Pageable paginavel(int page, int size, String sort) {
        var partes = sort.split(",");
        if (partes.length != 2) throw new DadosInvalidosException(Map.of("sort", "A ordenação informada não é válida."));
        try { return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(partes[1]), partes[0])); }
        catch (IllegalArgumentException e) { throw new DadosInvalidosException(Map.of("sort", "A ordenação informada não é válida.")); }
    }

    private UUID usuario(Authentication auth) { return UUID.fromString(auth.getName()); }
}
