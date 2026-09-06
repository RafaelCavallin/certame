package br.com.certame.catalogo.edital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import br.com.certame.catalogo.Banca;
import br.com.certame.catalogo.BancaRepository;
import br.com.certame.catalogo.Disciplina;
import br.com.certame.catalogo.DisciplinaRepository;
import br.com.certame.catalogo.Orgao;
import br.com.certame.catalogo.OrgaoRepository;
import br.com.certame.catalogo.edital.dto.RequisicaoEdital;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EditalServiceTest {
    private final EditalRepository editais = mock(EditalRepository.class);
    private final BancaRepository bancas = mock(BancaRepository.class);
    private final OrgaoRepository orgaos = mock(OrgaoRepository.class);
    private final DisciplinaRepository disciplinas = mock(DisciplinaRepository.class);
    private final RegistradorAuditoria auditoria = mock(RegistradorAuditoria.class);
    private final EditalService service = new EditalService(
            editais, bancas, orgaos, new ResolvedorDisciplinasEdital(disciplinas), auditoria);

    private RequisicaoEdital requisicaoCompleta(String nome, StatusEdital status, LocalDate prazo) {
        return new RequisicaoEdital(nome, status, 100, null, null, prazo,
                "https://banca.exemplo.com.br/edital", Set.of());
    }

    @Test void TU05_criaEditalComTodosOsCamposPreenchidos() {
        // Arrange
        var bancaId = UUID.randomUUID();
        var orgaoId = UUID.randomUUID();
        var banca = new Banca("Cebraspe", "CB");
        var orgao = new Orgao("TRF1", "TRF1");
        when(bancas.findById(bancaId)).thenReturn(Optional.of(banca));
        when(orgaos.findById(orgaoId)).thenReturn(Optional.of(orgao));
        var requisicao = new RequisicaoEdital("TRF 1ª Região", StatusEdital.ABERTO, 320, bancaId, orgaoId,
                LocalDate.of(2026, 8, 30), "https://cebraspe.org.br/trf1", Set.of());

        // Act
        var detalhe = service.criar(requisicao);

        // Assert
        assertThat(detalhe.nome()).isEqualTo("TRF 1ª Região");
        assertThat(detalhe.status()).isEqualTo(StatusEdital.ABERTO);
        assertThat(detalhe.vagas()).isEqualTo(320);
        assertThat(detalhe.banca().nome()).isEqualTo("Cebraspe");
        assertThat(detalhe.orgao().nome()).isEqualTo("TRF1");
        verify(auditoria).registrar("CRIAR", "edital", detalhe.id(), EditalAuditoria.payload(requisicao));
    }

    @Test void TU06_recusaEditalAbertoSemPrazo() {
        // Arrange
        var requisicao = requisicaoCompleta("Edital sem prazo", StatusEdital.ABERTO, null);

        // Act & Assert
        assertThatThrownBy(() -> service.criar(requisicao))
                .isInstanceOfSatisfying(DadosInvalidosException.class, excecao ->
                        assertThat(excecao.getFieldErrors())
                                .containsEntry("prazoInscricao",
                                        "Informe o prazo de inscrição para um edital aberto."));
        verify(editais, never()).save(any());
    }

    @Test void TU07_recusaNomeJaUsado() {
        // Arrange
        var requisicao = requisicaoCompleta("SEFAZ-RS", StatusEdital.PREVISTO, null);
        when(editais.existsByNomeIgnoreCaseAndExcluidoEmIsNull("SEFAZ-RS")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.criar(requisicao))
                .isInstanceOf(ConflitoException.class)
                .hasFieldOrPropertyWithValue("code", "NOME_EM_USO");
        verify(editais, never()).save(any());
    }

    @Test void TU08_recusaBancaInexistente() {
        // Arrange
        var bancaId = UUID.randomUUID();
        when(bancas.findById(bancaId)).thenReturn(Optional.empty());
        var requisicao = new RequisicaoEdital("Edital com banca inválida", StatusEdital.PREVISTO, null,
                bancaId, null, null, null, Set.of());

        // Act & Assert
        assertThatThrownBy(() -> service.criar(requisicao))
                .isInstanceOfSatisfying(DadosInvalidosException.class, excecao ->
                        assertThat(excecao.getFieldErrors())
                                .containsEntry("bancaId", "A banca selecionada não existe."));
    }

    @Test void TU09_recusaUrlSemEsquemaHttp() {
        // Arrange
        var requisicao = new RequisicaoEdital("Edital com URL inválida", StatusEdital.PREVISTO, null,
                null, null, null, "ftp://banca.exemplo.com.br", Set.of());

        // Act & Assert
        assertThatThrownBy(() -> service.criar(requisicao))
                .isInstanceOfSatisfying(DadosInvalidosException.class, excecao ->
                        assertThat(excecao.getFieldErrors()).containsKey("urlOficial"));
        verify(editais, never()).save(any());
    }

    @Test void TU10_atualizaMudandoStatusPrevistoParaAberto() {
        // Arrange
        var id = UUID.randomUUID();
        var edital = new Edital("SEFAZ-RS", StatusEdital.PREVISTO);
        when(editais.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(edital));
        when(editais.existsByNomeIgnoreCaseAndExcluidoEmIsNullAndIdNot("SEFAZ-RS", id)).thenReturn(false);
        var requisicao = requisicaoCompleta("SEFAZ-RS", StatusEdital.ABERTO, LocalDate.of(2026, 10, 15));

        // Act
        var detalhe = service.atualizar(id, requisicao);

        // Assert
        assertThat(detalhe.status()).isEqualTo(StatusEdital.ABERTO);
        assertThat(detalhe.prazoInscricao()).isEqualTo(LocalDate.of(2026, 10, 15));
        verify(editais).save(edital);
        verify(auditoria).registrar("ATUALIZAR", "edital", id, EditalAuditoria.payload(requisicao));
    }

    @Test void TU11_atualizaTrocandoOConjuntoDeDisciplinas() {
        // Arrange
        var id = UUID.randomUUID();
        var antiga = new Disciplina("Direito Administrativo");
        var edital = new Edital("Edital", StatusEdital.PREVISTO);
        edital.setDisciplinas(Set.of(antiga));
        when(editais.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(edital));
        var novaId = UUID.randomUUID();
        var nova = new Disciplina("Direito Constitucional");
        when(disciplinas.findByIdInAndExcluidoEmIsNull(Set.of(novaId))).thenReturn(java.util.List.of(nova));
        var requisicao = new RequisicaoEdital("Edital", StatusEdital.PREVISTO, null, null, null, null,
                null, Set.of(novaId));

        // Act
        var detalhe = service.atualizar(id, requisicao);

        // Assert
        assertThat(detalhe.disciplinas()).extracting("nome").containsExactly("Direito Constitucional");
    }

    @Test void TU12_excluiEditalExistenteMarcandoExcluidoEm() {
        // Arrange
        var id = UUID.randomUUID();
        var edital = new Edital("Edital", StatusEdital.PREVISTO);
        when(editais.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(edital));

        // Act
        service.excluir(id);

        // Assert
        assertThat(edital.getExcluidoEm()).isNotNull();
        verify(editais).save(edital);
        verify(auditoria).registrar("EXCLUIR", "edital", id, null);
    }

    @Test void TU13_excluirIdInexistenteRecusa() {
        // Arrange
        var id = UUID.randomUUID();
        when(editais.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(editais, never()).save(any());
    }
}
