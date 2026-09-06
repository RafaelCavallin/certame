package br.com.certame.questao;

import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.PublicacaoInvalidaException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.questao.dto.QuestaoDetalhe;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublicacaoService {
    private static final Logger LOG = LoggerFactory.getLogger(PublicacaoService.class);
    private final QuestaoRepository questoes;
    private final ValidadorPublicacao validador;
    private final RegistradorAuditoria auditoria;
    private final Clock clock;
    private final MetricasPublicacao metricas;

    public PublicacaoService(QuestaoRepository questoes, ValidadorPublicacao validador,
            RegistradorAuditoria auditoria, Clock clock, MetricasPublicacao metricas) {
        this.questoes = questoes;
        this.validador = validador;
        this.auditoria = auditoria;
        this.clock = clock;
        this.metricas = metricas;
    }

    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public QuestaoDetalhe enviarParaRevisao(UUID id) {
        var questao = obter(id);
        exigirStatus(questao, StatusQuestao.RASCUNHO);
        return transicionar(questao, StatusQuestao.EM_REVISAO, "ENVIAR_REVISAO");
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public QuestaoDetalhe publicar(UUID id) {
        var questao = obter(id);
        exigirStatus(questao, StatusQuestao.EM_REVISAO, StatusQuestao.RASCUNHO);
        var pendencias = validador.validar(questao);
        if (!pendencias.isEmpty()) {
            metricas.registrarRecusa(pendencias);
            LOG.warn("Publicação recusada para questão {} por pendências {}", questao.getCodigo(), pendencias.keySet());
            throw new PublicacaoInvalidaException(pendencias);
        }
        questao.setPublicadoEm(Instant.now(clock));
        var detalhe = transicionar(questao, StatusQuestao.PUBLICADA, "PUBLICAR");
        metricas.registrarPublicada(questao);
        return detalhe;
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public QuestaoDetalhe despublicar(UUID id) {
        var questao = obter(id);
        exigirStatus(questao, StatusQuestao.PUBLICADA);
        var detalhe = transicionar(questao, StatusQuestao.EM_REVISAO, "DESPUBLICAR");
        metricas.registrarDespublicada();
        return detalhe;
    }

    private Questao obter(UUID id) {
        return questoes.findByIdAndExcluidoEmIsNull(id).orElseThrow(RecursoNaoEncontradoException::new);
    }

    private void exigirStatus(Questao questao, StatusQuestao... permitidos) {
        for (var permitido : permitidos) if (questao.getStatus() == permitido) return;
        LOG.warn("Transição inválida para questão {} no status {}", questao.getCodigo(), questao.getStatus());
        throw new ConflitoException("TRANSICAO_INVALIDA", "A transição de status da questão não é válida.");
    }

    private QuestaoDetalhe transicionar(Questao questao, StatusQuestao novoStatus, String acao) {
        var anterior = questao.getStatus();
        questao.setStatus(novoStatus);
        var salva = questoes.save(questao);
        var payload = new LinkedHashMap<String, Object>();
        payload.put("statusAnterior", anterior);
        payload.put("statusNovo", novoStatus);
        auditoria.registrar(acao, "questao", salva.getId(), payload);
        LOG.info("Questão {} transicionada de {} para {}", salva.getCodigo(), anterior, novoStatus);
        return QuestaoDetalhe.de(salva);
    }
}
