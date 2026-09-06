package br.com.certame.estudo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import br.com.certame.estudo.dto.*;
import br.com.certame.questao.*;
import br.com.certame.usuario.Usuario;
import br.com.certame.usuario.UsuarioRepository;
import java.util.*;
import org.junit.jupiter.api.Test;

class MarcacaoServiceTest {
    private static final UUID USUARIO_ID = UUID.nameUUIDFromBytes("usuario".getBytes());
    private final MarcacaoRepository marcacoes = mock(MarcacaoRepository.class);
    private final QuestaoRepository questoes = mock(QuestaoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);

    @Test void TU29_ligaFavoritaEErroRecorrenteDeFormaIdempotente() {
        var q = questao(); var usuario = usuario(); var favorita = mock(Marcacao.class); var erro = mock(Marcacao.class);
        preparar(q, usuario);
        when(marcacoes.findByUsuarioIdAndQuestaoIdAndTipo(USUARIO_ID, q.getId(), TipoMarcacao.FAVORITA)).thenReturn(Optional.empty(), Optional.of(favorita));
        when(marcacoes.findByUsuarioIdAndQuestaoIdAndTipo(USUARIO_ID, q.getId(), TipoMarcacao.ERRO_RECORRENTE)).thenReturn(Optional.empty(), Optional.of(erro));
        var requisicao = new RequisicaoMarcacoes(true, true);
        var primeira = service().aplicar(USUARIO_ID, q.getId(), requisicao);
        var segunda = service().aplicar(USUARIO_ID, q.getId(), requisicao);
        assertThat(primeira).isEqualTo(segunda).isEqualTo(new MarcacoesQuestao(q.getId(), true, true));
        verify(marcacoes, times(2)).save(any(Marcacao.class));
    }

    @Test void TU30_desligaSoAMarcacaoPedida() {
        var q = questao(); var usuario = usuario(); var favorita = mock(Marcacao.class); var erro = mock(Marcacao.class);
        preparar(q, usuario);
        when(marcacoes.findByUsuarioIdAndQuestaoIdAndTipo(USUARIO_ID, q.getId(), TipoMarcacao.FAVORITA)).thenReturn(Optional.of(favorita));
        when(marcacoes.findByUsuarioIdAndQuestaoIdAndTipo(USUARIO_ID, q.getId(), TipoMarcacao.ERRO_RECORRENTE)).thenReturn(Optional.of(erro));
        var retorno = service().aplicar(USUARIO_ID, q.getId(), new RequisicaoMarcacoes(true, false));
        assertThat(retorno).isEqualTo(new MarcacoesQuestao(q.getId(), true, false));
        verify(marcacoes).delete(erro);
        verify(marcacoes, never()).delete(favorita);
        verify(marcacoes, never()).save(any());
    }

    private MarcacaoService service() { return new MarcacaoService(marcacoes, questoes, usuarios); }
    private void preparar(Questao q, Usuario usuario) { when(questoes.findByIdAndExcluidoEmIsNull(q.getId())).thenReturn(Optional.of(q)); when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario)); }
    private Questao questao() { var q = mock(Questao.class); when(q.getId()).thenReturn(UUID.nameUUIDFromBytes("questao".getBytes())); when(q.getStatus()).thenReturn(StatusQuestao.PUBLICADA); return q; }
    private Usuario usuario() { var u = mock(Usuario.class); when(u.getId()).thenReturn(USUARIO_ID); return u; }
}
