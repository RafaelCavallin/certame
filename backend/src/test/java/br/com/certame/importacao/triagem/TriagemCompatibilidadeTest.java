package br.com.certame.importacao.triagem;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certame.importacao.AlternativaExterna;
import br.com.certame.importacao.QuestaoExterna;
import br.com.certame.importacao.ResultadoCandidata;
import java.util.List;
import org.junit.jupiter.api.Test;

class TriagemCompatibilidadeTest {
    private final TriagemCompatibilidade triagem = new TriagemCompatibilidade(
            new DetectorTipo(), new ValidadorEstrutura(), new ValidadorLimites(),
            new DetectorRecursoNaoSuportado(), new AvaliadorComentario());

    @Test
    void TU03_recusaCombinacaoAmbiguaComoTipoNaoSuportado() {
        var questao = questao(List.of(alt("A", "Única opção preenchida")), "X");

        var resultado = triagem.avaliar(questao);

        assertThat(resultado.aprovada()).isFalse();
        assertThat(resultado.resultado()).isEqualTo(ResultadoCandidata.TIPO_NAO_SUPORTADO);
    }

    @Test
    void TU04_descartaAlternativasComLacunaSemCompactarLetras() {
        var alternativas = List.of(alt("A", "Texto A"), alt("B", "Texto B"), alt("C", ""), alt("D", "Texto D"), alt("E", "Texto E"));
        var questao = questao(alternativas, "D");

        var resultado = triagem.avaliar(questao);

        assertThat(resultado.aprovada()).isFalse();
        assertThat(resultado.resultado()).isEqualTo(ResultadoCandidata.INCOMPLETA);
    }

    @Test
    void TU05_descartaGabaritoSemAlternativaCorrespondente() {
        var questao = questao(List.of(alt("A", "Texto A"), alt("B", "Texto B")), "Z");

        var resultado = triagem.avaliar(questao);

        assertThat(resultado.aprovada()).isFalse();
        assertThat(resultado.resultado()).isEqualTo(ResultadoCandidata.INCOMPLETA);
    }

    @Test
    void TU06_recusaEnunciadoForaDosLimitesDoModelo() {
        var questao = new QuestaoExterna(null, "id-1", "Curto", List.of(alt("A", "Texto A"), alt("B", "Texto B")),
                "A", comentarioValido(), null, null, null, null, null, null, null, null);

        var resultado = triagem.avaliar(questao);

        assertThat(resultado.aprovada()).isFalse();
        assertThat(resultado.resultado()).isEqualTo(ResultadoCandidata.INCOMPLETA);
    }

    private AlternativaExterna alt(String letra, String texto) {
        return new AlternativaExterna(letra, texto, (short) (letra.charAt(0) - 'A' + 1));
    }

    private String comentarioValido() {
        return "Comentário explicando detalhadamente a resposta correta desta questão de teste.";
    }

    private QuestaoExterna questao(List<AlternativaExterna> alternativas, String gabarito) {
        return new QuestaoExterna(null, "id-1",
                "Enunciado de teste com conteúdo suficiente para passar nos limites do modelo.",
                alternativas, gabarito, comentarioValido(), null, null, null, null, null, null, null, null);
    }
}
