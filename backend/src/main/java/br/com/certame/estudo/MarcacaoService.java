package br.com.certame.estudo;

import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.estudo.dto.*;
import br.com.certame.questao.*;
import br.com.certame.usuario.UsuarioRepository;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarcacaoService {
    private final MarcacaoRepository marcacoes;
    private final QuestaoRepository questoes;
    private final UsuarioRepository usuarios;
    public MarcacaoService(MarcacaoRepository marcacoes, QuestaoRepository questoes, UsuarioRepository usuarios) { this.marcacoes = marcacoes; this.questoes = questoes; this.usuarios = usuarios; }
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public MarcacoesQuestao aplicar(UUID usuarioId, UUID questaoId, RequisicaoMarcacoes req) { var questao = questoes.findByIdAndExcluidoEmIsNull(questaoId).filter(q -> q.getStatus() == StatusQuestao.PUBLICADA).orElseThrow(RecursoNaoEncontradoException::new); var usuario = usuarios.findById(usuarioId).orElseThrow(RecursoNaoEncontradoException::new); aplicarTipo(usuario, questao, TipoMarcacao.FAVORITA, req.favorita()); aplicarTipo(usuario, questao, TipoMarcacao.ERRO_RECORRENTE, req.erroRecorrente()); return new MarcacoesQuestao(questaoId, req.favorita(), req.erroRecorrente()); }
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Map<UUID, MarcacoesQuestao> marcadasDa(UUID usuarioId, List<UUID> ids) { var resultado = new HashMap<UUID, MarcacoesQuestao>(); for (var id : ids) resultado.put(id, estado(usuarioId, id)); return resultado; }
    private void aplicarTipo(br.com.certame.usuario.Usuario usuario, Questao questao, TipoMarcacao tipo, boolean desejada) { var usuarioId = usuario.getId(); var existente = marcacoes.findByUsuarioIdAndQuestaoIdAndTipo(usuarioId, questao.getId(), tipo); if (desejada && existente.isEmpty()) marcacoes.save(new Marcacao(usuario, questao, tipo)); if (!desejada && existente.isPresent()) marcacoes.delete(existente.get()); }
    private MarcacoesQuestao estado(UUID usuarioId, UUID id) { var lista = marcacoes.findByUsuarioIdAndQuestaoId(usuarioId, id); return new MarcacoesQuestao(id, lista.stream().anyMatch(m -> m.getTipo() == TipoMarcacao.FAVORITA), lista.stream().anyMatch(m -> m.getTipo() == TipoMarcacao.ERRO_RECORRENTE)); }
}
