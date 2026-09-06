package br.com.certame.catalogo.edital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultaEditaisServiceTest {
    private static final Clock HOJE_FIXO = Clock.fixed(
            LocalDate.of(2026, 9, 1).atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private final EditalRepository editais = mock(EditalRepository.class);
    private final ConsultaEditaisService service =
            new ConsultaEditaisService(editais, new VerificadorPrazoEdital(HOJE_FIXO));

    private Edital edital(String nome, StatusEdital status, LocalDate prazo) {
        var edital = new Edital(nome, status);
        edital.setPrazoInscricao(prazo);
        return edital;
    }

    @Test void TU18_agrupadosDistribuiOsTresStatus() {
        // Arrange
        var aberto = edital("TRF1", StatusEdital.ABERTO, LocalDate.of(2026, 10, 1));
        var previsto = edital("SEFAZ-RS", StatusEdital.PREVISTO, null);
        var encerrado = edital("TCU", StatusEdital.ENCERRADO, LocalDate.of(2026, 1, 1));
        when(editais.findByStatusAndExcluidoEmIsNullOrderByPrazoInscricaoAsc(StatusEdital.ABERTO))
                .thenReturn(List.of(aberto));
        when(editais.findByStatusAndExcluidoEmIsNullOrderByNomeAsc(StatusEdital.PREVISTO))
                .thenReturn(List.of(previsto));
        when(editais.findPorStatusOrdenadosPorPrazoDescendente(StatusEdital.ENCERRADO))
                .thenReturn(List.of(encerrado));

        // Act
        var agrupados = service.agrupados();

        // Assert
        assertThat(agrupados.abertos()).extracting("nome").containsExactly("TRF1");
        assertThat(agrupados.previstos()).extracting("nome").containsExactly("SEFAZ-RS");
        assertThat(agrupados.encerrados()).extracting("nome").containsExactly("TCU");
    }

    @Test void TU19_ordenaOsAbertosPeloPrazoMaisProximo() {
        // Arrange
        var maisProximo = edital("Mais próximo", StatusEdital.ABERTO, LocalDate.of(2026, 9, 10));
        var maisDistante = edital("Mais distante", StatusEdital.ABERTO, LocalDate.of(2026, 12, 1));
        when(editais.findByStatusAndExcluidoEmIsNullOrderByPrazoInscricaoAsc(StatusEdital.ABERTO))
                .thenReturn(List.of(maisProximo, maisDistante));
        when(editais.findByStatusAndExcluidoEmIsNullOrderByNomeAsc(StatusEdital.PREVISTO)).thenReturn(List.of());
        when(editais.findPorStatusOrdenadosPorPrazoDescendente(StatusEdital.ENCERRADO)).thenReturn(List.of());

        // Act
        var agrupados = service.agrupados();

        // Assert
        assertThat(agrupados.abertos()).extracting("nome").containsExactly("Mais próximo", "Mais distante");
    }

    @Test void TU20_ordenaPrevistosPorNomeEEncerradosPeloPrazoMaisRecente() {
        // Arrange
        var previstoB = edital("Beta", StatusEdital.PREVISTO, null);
        var previstoA = edital("Alfa", StatusEdital.PREVISTO, null);
        var encerradoRecente = edital("Recente", StatusEdital.ENCERRADO, LocalDate.of(2026, 8, 1));
        var encerradoAntigo = edital("Antigo", StatusEdital.ENCERRADO, LocalDate.of(2026, 1, 1));
        var encerradoSemPrazo = edital("Sem prazo", StatusEdital.ENCERRADO, null);
        when(editais.findByStatusAndExcluidoEmIsNullOrderByPrazoInscricaoAsc(StatusEdital.ABERTO)).thenReturn(List.of());
        when(editais.findByStatusAndExcluidoEmIsNullOrderByNomeAsc(StatusEdital.PREVISTO))
                .thenReturn(List.of(previstoA, previstoB));
        when(editais.findPorStatusOrdenadosPorPrazoDescendente(StatusEdital.ENCERRADO))
                .thenReturn(List.of(encerradoRecente, encerradoAntigo, encerradoSemPrazo));

        // Act
        var agrupados = service.agrupados();

        // Assert
        assertThat(agrupados.previstos()).extracting("nome").containsExactly("Alfa", "Beta");
        assertThat(agrupados.encerrados()).extracting("nome").containsExactly("Recente", "Antigo", "Sem prazo");
    }

    @Test void TU21_agrupadosSemNenhumEdital() {
        // Arrange
        when(editais.findByStatusAndExcluidoEmIsNullOrderByPrazoInscricaoAsc(StatusEdital.ABERTO)).thenReturn(List.of());
        when(editais.findByStatusAndExcluidoEmIsNullOrderByNomeAsc(StatusEdital.PREVISTO)).thenReturn(List.of());
        when(editais.findPorStatusOrdenadosPorPrazoDescendente(StatusEdital.ENCERRADO)).thenReturn(List.of());

        // Act
        var agrupados = service.agrupados();

        // Assert
        assertThat(agrupados.abertos()).isEmpty();
        assertThat(agrupados.previstos()).isEmpty();
        assertThat(agrupados.encerrados()).isEmpty();
    }

    @Test void TU22_listarParaAdministracaoMarcaOPrazoVencido() {
        // Arrange
        var vencido = edital("Vencido", StatusEdital.ABERTO, LocalDate.of(2026, 8, 20));
        var emDia = edital("Em dia", StatusEdital.ABERTO, LocalDate.of(2026, 9, 20));
        when(editais.findByExcluidoEmIsNull()).thenReturn(List.of(vencido, emDia));

        // Act
        var resumo = service.listarParaAdministracao();

        // Assert
        assertThat(resumo).extracting("nome", "prazoVencido")
                .containsExactly(tuple("Vencido", true), tuple("Em dia", false));
    }

    @Test void TU22b_buscarParaEdicaoRecusaIdExcluidoOuInexistente() {
        // Arrange
        var id = UUID.randomUUID();
        when(editais.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.buscarParaEdicao(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
