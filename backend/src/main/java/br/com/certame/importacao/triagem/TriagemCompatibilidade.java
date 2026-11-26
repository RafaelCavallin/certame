package br.com.certame.importacao.triagem;

import br.com.certame.importacao.QuestaoExterna;
import br.com.certame.importacao.ResultadoCandidata;
import br.com.certame.questao.TipoQuestao;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Orquestra a triagem na ordem tipo → estrutura → limites → recurso → comentário. */
@Component
public class TriagemCompatibilidade {
    private static final String MOTIVO_TIPO_AMBIGUO =
            "Não foi possível determinar se a questão é de múltipla escolha ou de certo/errado.";
    private static final String PENDENCIA_SEM_COMENTARIO = "SEM_COMENTARIO";

    private final DetectorTipo detectorTipo;
    private final ValidadorEstrutura validadorEstrutura;
    private final ValidadorLimites validadorLimites;
    private final DetectorRecursoNaoSuportado detectorRecurso;
    private final AvaliadorComentario avaliadorComentario;

    public TriagemCompatibilidade(DetectorTipo detectorTipo, ValidadorEstrutura validadorEstrutura,
            ValidadorLimites validadorLimites, DetectorRecursoNaoSuportado detectorRecurso,
            AvaliadorComentario avaliadorComentario) {
        this.detectorTipo = detectorTipo;
        this.validadorEstrutura = validadorEstrutura;
        this.validadorLimites = validadorLimites;
        this.detectorRecurso = detectorRecurso;
        this.avaliadorComentario = avaliadorComentario;
    }

    public ResultadoTriagem avaliar(QuestaoExterna questao) {
        var tipo = detectorTipo.detectar(questao);
        if (tipo.isEmpty()) {
            return ResultadoTriagem.reprovada(ResultadoCandidata.TIPO_NAO_SUPORTADO, MOTIVO_TIPO_AMBIGUO);
        }
        var estrutura = validadorEstrutura.validar(questao, tipo.get());
        if (!estrutura.valida()) {
            return ResultadoTriagem.reprovada(ResultadoCandidata.INCOMPLETA, estrutura.motivo());
        }
        var motivoLimites = validadorLimites.validar(questao);
        if (motivoLimites.isPresent()) {
            return ResultadoTriagem.reprovada(ResultadoCandidata.INCOMPLETA, motivoLimites.get());
        }
        var recurso = detectorRecurso.primeiraOcorrencia(camposTextuais(questao));
        if (recurso.isPresent()) {
            return ResultadoTriagem.reprovada(ResultadoCandidata.RECURSO_NAO_SUPORTADO,
                    "O conteúdo contém um recurso que o Certame não exibe: " + recurso.get() + ".");
        }
        return aprovar(questao, tipo.get(), estrutura.gabaritoNormalizado());
    }

    private ResultadoTriagem aprovar(QuestaoExterna questao, TipoQuestao tipo, String gabaritoNormalizado) {
        var comentarioReprovado = avaliadorComentario.avaliar(questao.comentario(), questao.enunciado(), questao.gabarito());
        var pendencias = comentarioReprovado ? List.of(PENDENCIA_SEM_COMENTARIO) : List.<String>of();
        return ResultadoTriagem.aprovada(tipo, gabaritoNormalizado, pendencias);
    }

    private String[] camposTextuais(QuestaoExterna questao) {
        var textos = new ArrayList<String>();
        textos.add(questao.enunciado());
        textos.add(questao.comentario());
        if (questao.alternativas() != null) {
            questao.alternativas().forEach(a -> textos.add(a.texto()));
        }
        return textos.toArray(new String[0]);
    }
}
