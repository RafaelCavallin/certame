package br.com.certame.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certame.auth.FinalidadeTokenSenha;
import br.com.certame.auth.RedefinicaoSenhaService;
import br.com.certame.auth.RefreshTokenService;
import br.com.certame.auth.TokenSenhaRepository;
import br.com.certame.comum.Paginacao;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.comum.excecao.EmailEmUsoException;
import br.com.certame.usuario.dto.FiltroUsuarios;
import br.com.certame.usuario.dto.RequisicaoCriarUsuario;
import br.com.certame.usuario.dto.RequisicaoEditarUsuario;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class GestaoUsuariosServiceTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RedefinicaoSenhaService redefinicaoSenha = mock(RedefinicaoSenhaService.class);
    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private final TokenSenhaRepository tokenSenhas = mock(TokenSenhaRepository.class);
    private final RegistradorAuditoria auditoria = mock(RegistradorAuditoria.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final SalvaguardaUsuarioService salvaguarda = new SalvaguardaUsuarioService(usuarios);
    private final LocalizadorUsuarioAdmin localizador = new LocalizadorUsuarioAdmin(usuarios);
    private final GeradorSenhaTemporaria geradorSenha = new GeradorSenhaTemporaria(encoder);
    private final GestaoUsuariosService service = new GestaoUsuariosService(usuarios, redefinicaoSenha, refreshTokens,
            tokenSenhas, auditoria, salvaguarda, localizador, geradorSenha);

    private Usuario usuario(PerfilUsuario perfil, StatusUsuario status) {
        return new Usuario("Pessoa Teste", "pessoa" + UUID.randomUUID() + "@example.com", "hash", perfil, status);
    }

    private void comId(Usuario usuario, UUID id) throws Exception {
        var campo = Usuario.class.getDeclaredField("id");
        campo.setAccessible(true);
        campo.set(usuario, id);
    }

    @Test
    void TU01_criaUsuarioComConviteForcandoStatusPendente() {
        when(usuarios.findByEmailAndExcluidoEmIsNull(anyString())).thenReturn(Optional.empty());
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        var requisicao = new RequisicaoCriarUsuario("Marina Duarte", "marina@example.com",
                PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO, true);

        var resumo = service.criar(requisicao);

        assertThat(resumo.status()).isEqualTo(StatusUsuario.CONVITE_PENDENTE);
        verify(redefinicaoSenha).emitirConvite(any(Usuario.class));
    }

    @Test
    void TU02_criaUsuarioSemConviteGerandoHashDescartavel() {
        when(usuarios.findByEmailAndExcluidoEmIsNull(anyString())).thenReturn(Optional.empty());
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        var requisicao = new RequisicaoCriarUsuario("Marina Duarte", "marina2@example.com",
                PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO, false);

        var resumo = service.criar(requisicao);

        assertThat(resumo.status()).isEqualTo(StatusUsuario.ATIVO);
        verify(redefinicaoSenha, never()).emitirConvite(any(Usuario.class));
    }

    @Test
    void TU03_recusaEmailDuplicadoInformandoOCampo() {
        when(usuarios.findByEmailAndExcluidoEmIsNull("duplicado@example.com"))
                .thenReturn(Optional.of(usuario(PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO)));
        var requisicao = new RequisicaoCriarUsuario("Pessoa Nova", "Duplicado@Example.com",
                PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO, true);

        assertThatThrownBy(() -> service.criar(requisicao))
                .isInstanceOf(EmailEmUsoException.class)
                .satisfies(excecao -> assertThat(((EmailEmUsoException) excecao).getFieldErrors())
                        .containsEntry("email", "Este e-mail já está em uso."));
    }

    @Test
    void TU04_editaPerfilPromovendoProfessorParaAdmin() throws Exception {
        var alvo = usuario(PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO);
        var alvoId = UUID.randomUUID();
        comId(alvo, alvoId);
        when(usuarios.findByIdAndExcluidoEmIsNull(alvoId)).thenReturn(Optional.of(alvo));
        when(usuarios.findByEmailAndExcluidoEmIsNull(anyString())).thenReturn(Optional.empty());
        var requisicao = new RequisicaoEditarUsuario(alvo.getNome(), alvo.getEmail(), PerfilUsuario.ADMIN, StatusUsuario.ATIVO);

        var resumo = service.editar(alvoId, requisicao, UUID.randomUUID());

        assertThat(resumo.perfil()).isEqualTo(PerfilUsuario.ADMIN);
        verify(refreshTokens).revogarTodosDoUsuario(alvoId);
    }

    @Test
    void TU05_bloqueiaAutoRebaixamentoOuDesativacao() throws Exception {
        var alvo = usuario(PerfilUsuario.SUPER_ADMIN, StatusUsuario.ATIVO);
        var alvoId = UUID.randomUUID();
        comId(alvo, alvoId);
        when(usuarios.findByIdAndExcluidoEmIsNull(alvoId)).thenReturn(Optional.of(alvo));
        when(usuarios.findByEmailAndExcluidoEmIsNull(anyString())).thenReturn(Optional.empty());
        var requisicao = new RequisicaoEditarUsuario(alvo.getNome(), alvo.getEmail(), PerfilUsuario.ADMIN, StatusUsuario.ATIVO);

        assertThatThrownBy(() -> service.editar(alvoId, requisicao, alvoId))
                .isInstanceOf(ConflitoException.class)
                .satisfies(excecao -> assertThat(((ConflitoException) excecao).getCode()).isEqualTo("AUTO_MODIFICACAO"));
    }

    @Test
    void TU06_bloqueiaUltimoSuperAdminNoRebaixarOuDesativar() throws Exception {
        var alvo = usuario(PerfilUsuario.SUPER_ADMIN, StatusUsuario.ATIVO);
        var alvoId = UUID.randomUUID();
        comId(alvo, alvoId);
        when(usuarios.findByIdAndExcluidoEmIsNull(alvoId)).thenReturn(Optional.of(alvo));
        when(usuarios.findByEmailAndExcluidoEmIsNull(anyString())).thenReturn(Optional.empty());
        when(usuarios.countByPerfilAndStatusAndExcluidoEmIsNull(PerfilUsuario.SUPER_ADMIN, StatusUsuario.ATIVO)).thenReturn(1L);
        var requisicao = new RequisicaoEditarUsuario(alvo.getNome(), alvo.getEmail(), PerfilUsuario.ADMIN, StatusUsuario.ATIVO);

        assertThatThrownBy(() -> service.editar(alvoId, requisicao, UUID.randomUUID()))
                .isInstanceOf(ConflitoException.class)
                .satisfies(excecao -> assertThat(((ConflitoException) excecao).getCode()).isEqualTo("ULTIMO_SUPER_ADMIN"));
    }

    @Test
    void TU07_excluiUsuarioLogicamenteRevogandoTokens() throws Exception {
        var alvo = usuario(PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO);
        var alvoId = UUID.randomUUID();
        comId(alvo, alvoId);
        when(usuarios.findByIdAndExcluidoEmIsNull(alvoId)).thenReturn(Optional.of(alvo));

        service.excluir(alvoId, UUID.randomUUID());

        assertThat(alvo.getExcluidoEm()).isNotNull();
        verify(refreshTokens).revogarTodosDoUsuario(alvoId);
        verify(tokenSenhas, times(FinalidadeTokenSenha.values().length))
                .invalidarPendentes(eq(alvoId), any(FinalidadeTokenSenha.class), any(Instant.class));
        verify(auditoria).registrar(eq("EXCLUIR"), eq("usuario"), eq(alvoId), any());
    }

    @Test
    void TU08_bloqueiaAutoexclusao() throws Exception {
        var alvo = usuario(PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO);
        var alvoId = UUID.randomUUID();
        comId(alvo, alvoId);
        when(usuarios.findByIdAndExcluidoEmIsNull(alvoId)).thenReturn(Optional.of(alvo));

        assertThatThrownBy(() -> service.excluir(alvoId, alvoId))
                .isInstanceOf(ConflitoException.class)
                .satisfies(excecao -> assertThat(((ConflitoException) excecao).getCode()).isEqualTo("AUTO_MODIFICACAO"));
    }

    @Test
    void TU11_rejeitaSortForaDaListaPermitida() {
        var filtro = new FiltroUsuarios(null, null);
        var paginacao = new Paginacao(0, 20, "nome,asc");

        assertThatThrownBy(() -> service.listar(filtro, paginacao))
                .isInstanceOf(DadosInvalidosException.class)
                .satisfies(excecao -> assertThat(((DadosInvalidosException) excecao).getFieldErrors()).containsKey("sort"));
    }

    @Test
    void TU11b_aceitaOrdenacaoPermitida() {
        var filtro = new FiltroUsuarios(PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO);
        var paginacao = new Paginacao(0, 20, "criadoEm,desc");
        when(usuarios.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(), PageRequest.of(0, 20), 0));

        var pagina = service.listar(filtro, paginacao);

        assertThat(pagina.totalElements()).isZero();
    }

    @Test
    void TU12_registraAuditoriaEmTodaOperacao() throws Exception {
        var alvo = usuario(PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO);
        var alvoId = UUID.randomUUID();
        comId(alvo, alvoId);
        when(usuarios.findByIdAndExcluidoEmIsNull(alvoId)).thenReturn(Optional.of(alvo));
        when(usuarios.findByEmailAndExcluidoEmIsNull(anyString())).thenReturn(Optional.empty());
        var requisicao = new RequisicaoEditarUsuario(alvo.getNome(), alvo.getEmail(), PerfilUsuario.ADMIN, StatusUsuario.ATIVO);

        service.editar(alvoId, requisicao, UUID.randomUUID());

        verify(auditoria).registrar(eq("EDITAR"), eq("usuario"), eq(alvoId), any());
    }
}
