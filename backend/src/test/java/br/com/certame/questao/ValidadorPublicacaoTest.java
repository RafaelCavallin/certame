package br.com.certame.questao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValidadorPublicacaoTest {
    private final ValidadorPublicacao validador = new ValidadorPublicacao();

    @Test
    void aceitaMultiplaEscolhaCompleta() {
        var questao = questao(TipoQuestao.ME, "B", "Comentário", 2);
        assertThat(validador.validar(questao)).isEmpty();
    }

    @Test
    void retornaTodasAsPendencias() {
        var pendencias = validador.validar(questao(TipoQuestao.ME, null, " ", 1));
        assertThat(pendencias).containsKeys("gabarito", "comentario", "alternativas");
    }

    @Test
    void retornaDisciplinaComoPendenciaQuandoAusente() {
        var questao = questao(TipoQuestao.ME, "B", "Comentário", 2);
        questao.setDisciplina(null);

        assertThat(validador.validar(questao))
                .containsEntry("disciplina", "Selecione uma disciplina antes de publicar.");
    }

    @Test
    void aceitaCertoErradoSemAlternativas() {
        assertThat(validador.validar(questao(TipoQuestao.CE, "CERTO", "Comentário", 0))).isEmpty();
    }

    @Test
    void rejeitaGabaritoInvalidoEmCertoErrado() {
        assertThat(validador.validar(questao(TipoQuestao.CE, "B", "Comentário", 0)))
                .containsEntry("gabarito", "Marque uma opção válida antes de publicar.");
    }

    private Questao questao(TipoQuestao tipo, String gabarito, String comentario, int alternativas) {
        var questao = new Questao(tipo, "Enunciado suficientemente longo", null);
        questao.setDisciplina(new br.com.certame.catalogo.Disciplina("Direito Constitucional"));
        questao.setGabarito(gabarito);
        questao.setComentario(comentario);
        for (short ordem = 1; ordem <= alternativas; ordem++) {
            questao.adicionarAlternativa(new Alternativa("Z", "Texto", ordem));
        }
        return questao;
    }
}
