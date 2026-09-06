package br.com.certame.usuario;

import br.com.certame.auth.FinalidadeTokenSenha;
import br.com.certame.auth.RedefinicaoSenhaService;
import br.com.certame.auth.RefreshTokenService;
import br.com.certame.auth.TokenSenhaRepository;
import br.com.certame.comum.PaginaResposta;
import br.com.certame.comum.Paginacao;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.usuario.dto.FiltroUsuarios;
import br.com.certame.usuario.dto.RequisicaoCriarUsuario;
import br.com.certame.usuario.dto.RequisicaoEditarUsuario;
import br.com.certame.usuario.dto.UsuarioGestaoResumo;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class GestaoUsuariosService {
    private final UsuarioRepository usuarios;
    private final RedefinicaoSenhaService redefinicaoSenha;
    private final RefreshTokenService refreshTokens;
    private final TokenSenhaRepository tokenSenhas;
    private final RegistradorAuditoria auditoria;
    private final SalvaguardaUsuarioService salvaguarda;
    private final LocalizadorUsuarioAdmin localizador;
    private final GeradorSenhaTemporaria geradorSenha;

    public GestaoUsuariosService(UsuarioRepository usuarios, RedefinicaoSenhaService redefinicaoSenha,
            RefreshTokenService refreshTokens, TokenSenhaRepository tokenSenhas, RegistradorAuditoria auditoria,
            SalvaguardaUsuarioService salvaguarda, LocalizadorUsuarioAdmin localizador,
            GeradorSenhaTemporaria geradorSenha) {
        this.usuarios = usuarios;
        this.redefinicaoSenha = redefinicaoSenha;
        this.refreshTokens = refreshTokens;
        this.tokenSenhas = tokenSenhas;
        this.auditoria = auditoria;
        this.salvaguarda = salvaguarda;
        this.localizador = localizador;
        this.geradorSenha = geradorSenha;
    }

    @Transactional(readOnly = true)
    public PaginaResposta<UsuarioGestaoResumo> listar(FiltroUsuarios filtro, Paginacao paginacao) {
        var pagina = usuarios.findAll(UsuarioSpecifications.filtrar(filtro.perfil(), filtro.status()),
                PaginadorUsuarios.paginavel(paginacao));
        return new PaginaResposta<>(pagina.map(UsuarioGestaoResumo::de).getContent(),
                paginacao.page(), paginacao.size(), pagina.getTotalElements());
    }

    @Transactional
    public UsuarioGestaoResumo criar(RequisicaoCriarUsuario requisicao) {
        var email = localizador.normalizarEmail(requisicao.email());
        localizador.garantirEmailInedito(email, null);
        var status = requisicao.enviarConvite() ? StatusUsuario.CONVITE_PENDENTE : requisicao.status();
        var senhaHash = status == StatusUsuario.CONVITE_PENDENTE ? null : geradorSenha.gerarHash();
        var usuario = usuarios.saveAndFlush(
                new Usuario(requisicao.nome().trim(), email, senhaHash, requisicao.perfil(), status));
        auditoria.registrar("CRIAR", "usuario", usuario.getId(),
                Map.of("perfil", requisicao.perfil(), "status", status, "enviarConvite", requisicao.enviarConvite()));
        if (requisicao.enviarConvite()) redefinicaoSenha.emitirConvite(usuario);
        return UsuarioGestaoResumo.de(usuario);
    }

    @Transactional
    public UsuarioGestaoResumo editar(UUID usuarioId, RequisicaoEditarUsuario requisicao, UUID atorId) {
        var usuario = localizador.buscarAtivo(usuarioId);
        var email = localizador.normalizarEmail(requisicao.email());
        localizador.garantirEmailInedito(email, usuarioId);
        salvaguarda.garantirEdicao(usuario, usuarioId, atorId, requisicao.perfil(), requisicao.status());
        var antes = snapshot(usuario);
        usuario.editar(requisicao.nome().trim(), email, requisicao.perfil(), requisicao.status());
        usuarios.save(usuario);
        refreshTokens.revogarTodosDoUsuario(usuario.getId());
        auditoria.registrar("EDITAR", "usuario", usuario.getId(), Map.of("antes", antes, "depois", snapshot(usuario)));
        return UsuarioGestaoResumo.de(usuario);
    }

    @Transactional
    public void excluir(UUID usuarioId, UUID atorId) {
        var usuario = localizador.buscarAtivo(usuarioId);
        salvaguarda.garantirExclusao(usuario, usuarioId, atorId);
        usuario.excluir();
        usuarios.save(usuario);
        refreshTokens.revogarTodosDoUsuario(usuario.getId());
        var agora = Instant.now();
        for (var finalidade : FinalidadeTokenSenha.values()) tokenSenhas.invalidarPendentes(usuario.getId(), finalidade, agora);
        auditoria.registrar("EXCLUIR", "usuario", usuario.getId(),
                Map.of("perfil", usuario.getPerfil(), "status", usuario.getStatus()));
    }

    private Map<String, Object> snapshot(Usuario usuario) {
        return Map.of("nome", usuario.getNome(), "email", usuario.getEmail(),
                "perfil", usuario.getPerfil(), "status", usuario.getStatus());
    }
}
