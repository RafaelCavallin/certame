package br.com.certame.importacao.triagem;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certame.importacao.triagem.DetectorRecursoNaoSuportado.RecursoNaoSuportado;
import org.junit.jupiter.api.Test;

class DetectorRecursoNaoSuportadoTest {
    private final DetectorRecursoNaoSuportado detector = new DetectorRecursoNaoSuportado();
    private static final String CARACTERE_CONTROLE = "";

    @Test
    void TU07_descartaMarcacaoHtmlEmQualquerCampo() {
        assertThat(detector.primeiraOcorrencia("Um enunciado", "<b>negrito</b>", "comentário", "texto-base"))
                .contains(RecursoNaoSuportado.HTML);
    }

    @Test
    void TU08_descartaDelimitadorLatex() {
        assertThat(detector.primeiraOcorrencia("A fórmula $x^2 + y^2 = z^2$ é famosa."))
                .contains(RecursoNaoSuportado.LATEX);
    }

    @Test
    void TU09_descartaReferenciaImagemOuAnexo() {
        assertThat(detector.primeiraOcorrencia("Observe o anexo do processo antes de responder."))
                .contains(RecursoNaoSuportado.IMAGEM_OU_ANEXO);
    }

    @Test
    void TU10_descartaTabelaECaractereDeControle() {
        assertThat(detector.primeiraOcorrencia("| Coluna A | Coluna B |")).contains(RecursoNaoSuportado.TABELA);
        assertThat(detector.primeiraOcorrencia("Texto com caractere" + CARACTERE_CONTROLE + " de controle"))
                .contains(RecursoNaoSuportado.CARACTERE_CONTROLE);
    }

    @Test
    void naoDetectaRecursoEmTextoComumSemMarcacao() {
        assertThat(detector.primeiraOcorrencia("Um enunciado inteiramente comum, sem nenhum recurso especial."))
                .isEmpty();
    }
}
