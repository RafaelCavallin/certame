package br.com.certame.flashcard.revisao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certame.flashcard.AgendamentoFsrs;
import br.com.certame.flashcard.EstadoFlashcard;
import br.com.certame.flashcard.Flashcard;
import br.com.certame.flashcard.FlashcardRepository;
import br.com.certame.flashcard.RegistroTemporalFlashcard;
import br.com.certame.flashcard.RelogioEstudo;
import br.com.certame.flashcard.deck.Deck;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MontadorFilaTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 3, 4);
    private static final Instant INICIO_DIA = Instant.parse("2026-03-04T03:00:00Z");
    private static final Instant BASE = Instant.parse("2026-03-01T12:00:00Z");
    @Mock FlashcardRepository flashcards;
    @Mock ConsumoDiarioRepositorio consumoDiario;
    @Mock RelogioEstudo relogio;
    private final UUID usuarioId = UUID.randomUUID();
    private final AtomicLong sequencia = new AtomicLong();
    private MontadorFila montador;

    @BeforeEach
    void preparar() {
        montador = new MontadorFila(flashcards, consumoDiario);
        when(relogio.hoje()).thenReturn(HOJE);
        when(relogio.inicioDoDia()).thenReturn(INICIO_DIA);
    }

    @Test
    void TU31_respeitaNovosERevisoesPorDiaDeCadaDeck() {
        var deck = deck((short) 10, (short) 60);
        var candidatos = candidatos(deck, EstadoFlashcard.NOVO, 40, EstadoFlashcard.REVISAO, 200);
        semEscopo(candidatos);
        semConsumo();

        var fila = montador.montar(usuarioId, todos(), relogio);

        assertThat(fila).hasSize(70);
        assertThat(fila).filteredOn(f -> f.getAgendamento().getEstado() == EstadoFlashcard.NOVO).hasSize(10);
        assertThat(fila).filteredOn(f -> f.getAgendamento().getEstado() == EstadoFlashcard.REVISAO).hasSize(60);
    }

    @Test
    void TU32_descontaOConsumoJaRegistradoNoDia() {
        var deck = deck((short) 10, (short) 60);
        var candidatos = cards(deck, EstadoFlashcard.REVISAO, 50);
        semEscopo(candidatos);
        var deckId = deck.getId();
        when(consumoDiario.porDeck(usuarioId, INICIO_DIA)).thenReturn(List.of(new ConsumoDiario(deckId, 0, 30)));

        var fila = montador.montar(usuarioId, todos(), relogio);

        assertThat(fila).hasSize(30);
    }

    @Test
    void TU33_contaCardDistintoNaoClassificacao() {
        var deck = deck((short) 10, (short) 60);
        var candidatos = cards(deck, EstadoFlashcard.NOVO, 10);
        semEscopo(candidatos);
        var deckId = deck.getId();
        when(consumoDiario.porDeck(usuarioId, INICIO_DIA)).thenReturn(List.of(new ConsumoDiario(deckId, 1, 0)));

        var fila = montador.montar(usuarioId, todos(), relogio);

        assertThat(fila).hasSize(9);
    }

    @Test
    void TU34_aplicaLimitesDeckADeckSemMisturar() {
        var deckA = deck((short) 5, (short) 5);
        var deckB = deck((short) 3, (short) 3);
        var candidatos = new ArrayList<>(candidatos(deckA, EstadoFlashcard.NOVO, 10, EstadoFlashcard.REVISAO, 10));
        candidatos.addAll(candidatos(deckB, EstadoFlashcard.NOVO, 10, EstadoFlashcard.REVISAO, 10));
        semEscopo(candidatos);
        semConsumo();

        var fila = montador.montar(usuarioId, todos(), relogio);

        assertThat(fila).hasSize(16);
        assertThat(fila).filteredOn(f -> f.getDeck() == deckA).hasSize(10);
        assertThat(fila).filteredOn(f -> f.getDeck() == deckB).hasSize(6);
    }

    @Test
    void TU35_usaOsLimitesPadraoParaCardsSemDeck() {
        var candidatos = candidatos(null, EstadoFlashcard.NOVO, 20, EstadoFlashcard.REVISAO, 100);
        semEscopo(candidatos);
        semConsumo();

        var fila = montador.montar(usuarioId, todos(), relogio);

        assertThat(fila).hasSize(70);
    }

    @Test
    void TU36_ordenaPorVencimentoCrescente() {
        var deck = deck((short) 10, (short) 10);
        var atrasado = card(deck, EstadoFlashcard.REVISAO, BASE, BASE);
        var empateAntigo = card(deck, EstadoFlashcard.REVISAO, BASE.plusSeconds(60), BASE);
        var empateNovo = card(deck, EstadoFlashcard.REVISAO, BASE.plusSeconds(60), BASE.plusSeconds(30));
        semEscopo(List.of(empateNovo, atrasado, empateAntigo));
        semConsumo();

        var fila = montador.montar(usuarioId, todos(), relogio);

        assertThat(fila).containsExactly(atrasado, empateAntigo, empateNovo);
    }

    @Test
    void TU37_naoAntecipaCardsDeVencimentoFuturo() {
        semEscopo(List.of());
        semConsumo();

        var fila = montador.montar(usuarioId, todos(), relogio);

        assertThat(fila).isEmpty();
        verify(flashcards).buscarFilaPorEscopo(usuarioId, HOJE, "TODOS", null, null);
    }

    @Test
    void TU38_ignoraCardsArquivadosEExcluidos() {
        var deck = deck((short) 10, (short) 60);
        var ativos = cards(deck, EstadoFlashcard.REVISAO, 3);
        semEscopo(ativos);
        semConsumo();

        var fila = montador.montar(usuarioId, todos(), relogio);

        assertThat(fila).hasSize(3);
        assertThat(fila).containsExactlyInAnyOrderElementsOf(ativos);
    }

    @Test
    void TU39_restringePorEscopoDeDeckEPorDisciplina() {
        var deckId = UUID.randomUUID();
        var disciplinaId = UUID.randomUUID();
        var filtro = new MontadorFila.FiltroFila(EscopoFila.DECK, deckId, disciplinaId);
        when(flashcards.buscarFilaPorEscopo(usuarioId, HOJE, "DECK", deckId, disciplinaId)).thenReturn(List.of());
        semConsumo();

        montador.montar(usuarioId, filtro, relogio);

        verify(flashcards).buscarFilaPorEscopo(usuarioId, HOJE, "DECK", deckId, disciplinaId);
    }

    private static MontadorFila.FiltroFila todos() {
        return new MontadorFila.FiltroFila(EscopoFila.TODOS, null, null);
    }

    private void semEscopo(List<Flashcard> candidatos) {
        when(flashcards.buscarFilaPorEscopo(usuarioId, HOJE, "TODOS", null, null)).thenReturn(candidatos);
    }

    private void semConsumo() {
        lenient().when(consumoDiario.porDeck(usuarioId, INICIO_DIA)).thenReturn(List.of());
    }

    private List<Flashcard> candidatos(Deck deck, EstadoFlashcard primeiro, int quantidadePrimeiro,
            EstadoFlashcard segundo, int quantidadeSegundo) {
        var lista = new ArrayList<>(cards(deck, primeiro, quantidadePrimeiro));
        lista.addAll(cards(deck, segundo, quantidadeSegundo));
        return lista;
    }

    private List<Flashcard> cards(Deck deck, EstadoFlashcard estado, int quantidade) {
        var lista = new ArrayList<Flashcard>();
        for (int i = 0; i < quantidade; i++) {
            var instante = BASE.plusSeconds(sequencia.incrementAndGet());
            lista.add(card(deck, estado, instante, instante));
        }
        return lista;
    }

    private Flashcard card(Deck deck, EstadoFlashcard estado, Instant vencimento, Instant criadoEm) {
        var flashcard = mock(Flashcard.class);
        var agendamento = mock(AgendamentoFsrs.class);
        var registroTemporal = mock(RegistroTemporalFlashcard.class);
        lenient().when(agendamento.getEstado()).thenReturn(estado);
        lenient().when(agendamento.getProximaRevisaoEm()).thenReturn(vencimento);
        lenient().when(registroTemporal.getCriadoEm()).thenReturn(criadoEm);
        lenient().when(flashcard.getDeck()).thenReturn(deck);
        lenient().when(flashcard.getAgendamento()).thenReturn(agendamento);
        lenient().when(flashcard.getRegistroTemporal()).thenReturn(registroTemporal);
        return flashcard;
    }

    private Deck deck(short novosDia, short revisoesDia) {
        var deck = mock(Deck.class);
        when(deck.getId()).thenReturn(UUID.randomUUID());
        when(deck.getNovosDia()).thenReturn(novosDia);
        when(deck.getRevisoesDia()).thenReturn(revisoesDia);
        when(deck.getIntervaloMaxDias()).thenReturn((short) 180);
        when(deck.getRetencaoAlvo()).thenReturn(new BigDecimal("0.90"));
        return deck;
    }
}
