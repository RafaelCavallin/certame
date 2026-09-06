package br.com.certame.usuario;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certame.auth.RedefinicaoSenhaService;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.comum.excecao.StatusIncompativelException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AcessoUsuarioAdminServiceTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RedefinicaoSenhaService redefinicaoSenha = mock(RedefinicaoSenhaService.class);
    private final RegistradorAuditoria auditoria = mock(RegistradorAuditoria.class);
    private final LocalizadorUsuarioAdmin localizador = new LocalizadorUsuarioAdmin(usuarios);
    private final AcessoUsuarioAdminService service =
            new AcessoUsuarioAdminService(localizador, redefinicaoSenha, auditoria);

    private Usuario usuario(PerfilUsuario perfil, StatusUsuario status) {
        return new Usuario("Pessoa Teste", "pessoa" + UUID.randomUUID() + "@example.com", "hash", perfil, status);
    }

    private void comId(Usuario usuario, UUID id) throws Exception {
        var campo = Usuario.class.getDeclaredField("id");
        campo.setAccessible(true);
        campo.set(usuario, id);
    }

    @Test
    void TU01_redefineSenhaApenasEmUsuarioAtivo() throws Exception {
        var ativo = usuario(PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO);
        var ativoId = UUID.randomUUID();
        comId(ativo, ativoId);
        when(usuarios.findByIdAndExcluidoEmIsNull(ativoId)).thenReturn(Optional.of(ativo));

        service.redefinirSenha(ativoId);

        verify(redefinicaoSenha).emitirRedefinicao(ativo);
        var pendente = usuario(PerfilUsuario.ESTUDANTE, StatusUsuario.CONVITE_PENDENTE);
        var pendenteId = UUID.randomUUID();
        comId(pendente, pendenteId);
        when(usuarios.findByIdAndExcluidoEmIsNull(pendenteId)).thenReturn(Optional.of(pendente));

        assertThatThrownBy(() -> service.redefinirSenha(pendenteId))
                .isInstanceOf(StatusIncompativelException.class);
    }

    @Test
    void TU02_reenviaConviteApenasEmPendenteInvalidandoAnteriores() throws Exception {
        var pendente = usuario(PerfilUsuario.ESTUDANTE, StatusUsuario.CONVITE_PENDENTE);
        var pendenteId = UUID.randomUUID();
        comId(pendente, pendenteId);
        when(usuarios.findByIdAndExcluidoEmIsNull(pendenteId)).thenReturn(Optional.of(pendente));

        service.reenviarConvite(pendenteId);

        verify(redefinicaoSenha).emitirConvite(pendente);
        var ativo = usuario(PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO);
        var ativoId = UUID.randomUUID();
        comId(ativo, ativoId);
        when(usuarios.findByIdAndExcluidoEmIsNull(ativoId)).thenReturn(Optional.of(ativo));

        assertThatThrownBy(() -> service.reenviarConvite(ativoId))
                .isInstanceOf(StatusIncompativelException.class);
    }
}
