package br.com.certame.questao;

import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.flashcard.QuestaoPublicada;
import br.com.certame.questao.dto.QuestaoDetalhe;
import br.com.certame.questao.dto.RequisicaoQuestao;
import br.com.certame.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QuestaoService implements QuestaoPublicada {
    private static final Logger LOG = LoggerFactory.getLogger(QuestaoService.class);
    private final QuestaoRepository questoes;
    private final QuestaoEstatisticaRepository estatisticas;
    private final UsuarioRepository usuarios;
    private final GeradorCodigoQuestao gerador;
    private final MontadorQuestao montador;
    private final RegistradorAuditoria auditoria;
    private final ValidadorPublicacao validador;
    private final Clock clock;

    public QuestaoService(QuestaoRepository questoes, QuestaoEstatisticaRepository estatisticas,
            UsuarioRepository usuarios, GeradorCodigoQuestao gerador, MontadorQuestao montador,
            RegistradorAuditoria auditoria, ValidadorPublicacao validador, Clock clock) {
        this.questoes = questoes;
        this.estatisticas = estatisticas;
        this.usuarios = usuarios;
        this.gerador = gerador;
        this.montador = montador;
        this.auditoria = auditoria;
        this.validador = validador;
        this.clock = clock;
    }
    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public QuestaoDetalhe criar(RequisicaoQuestao requisicao, UUID autorId) {
        montador.validarAlternativas(requisicao);
        var autor = usuarios.findById(autorId).orElseThrow(RecursoNaoEncontradoException::new);
        var questao = new Questao(requisicao.tipo(), requisicao.enunciado().trim(), autor);
        questao.setCodigo(gerador.gerar());
        montador.preencher(questao, requisicao);
        var salva = Optional.ofNullable(questoes.save(questao)).orElse(questao);
        estatisticas.save(new QuestaoEstatistica(salva));
        registrar("CRIAR", salva);
        return QuestaoDetalhe.de(salva);
    }

    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public QuestaoDetalhe atualizar(UUID id, RequisicaoQuestao requisicao) {
        montador.validarAlternativas(requisicao);
        var questao = obter(id);
        var tipoAnterior = questao.getTipo();
        montador.preencher(questao, requisicao);
        if (tipoAnterior == TipoQuestao.ME && requisicao.tipo() == TipoQuestao.CE) questao.setGabarito(null);
        if (questao.getStatus() == StatusQuestao.PUBLICADA) validador.garantirPublicavel(questao);
        var salva = Optional.ofNullable(questoes.save(questao)).orElse(questao);
        registrar("EDITAR", salva);
        return QuestaoDetalhe.de(salva);
    }
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public QuestaoDetalhe buscarPorId(UUID id) {
        return QuestaoDetalhe.de(obter(id));
    }
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public long contarDisponiveis(UUID disciplinaId) {
        return questoes.countByDisciplinaIdAndStatusAndExcluidoEmIsNull(disciplinaId, StatusQuestao.PUBLICADA);
    }
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public long contarDisponiveis(UUID disciplinaId, Dificuldade dificuldade) {
        return questoes.countByDisciplinaIdAndDificuldadeAndStatusAndExcluidoEmIsNull(disciplinaId, dificuldade, StatusQuestao.PUBLICADA);
    }
    @Override
    @Transactional(readOnly = true)
    public Optional<Questao> buscarPublicada(UUID id) {
        return questoes.findByIdAndStatusAndExcluidoEmIsNull(id, StatusQuestao.PUBLICADA);
    }
    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public void excluir(UUID id) {
        var questao = obter(id);
        if (questoes.countRespostas(id) > 0) throw new ConflitoException("QUESTAO_EM_USO", "A questão possui respostas e não pode ser excluída.");
        questao.setExcluidoEm(Instant.now(clock));
        questoes.save(questao);
        registrar("EXCLUIR", questao);
    }
    private Questao obter(UUID id) {
        return questoes.findByIdAndExcluidoEmIsNull(id).orElseThrow(RecursoNaoEncontradoException::new);
    }
    private void registrar(String acao, Questao q) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("id", q.getId());
        payload.put("codigo", q.getCodigo());
        payload.put("acao", acao);
        auditoria.registrar(acao, "questao", q.getId(), payload);
        LOG.info("Questão {} {} por ator", q.getCodigo(), acao.toLowerCase());
    }
}
