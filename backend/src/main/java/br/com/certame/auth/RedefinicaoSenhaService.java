package br.com.certame.auth;

import br.com.certame.comum.excecao.TokenInvalidoException;
import br.com.certame.usuario.StatusUsuario;
import br.com.certame.usuario.Usuario;
import br.com.certame.usuario.UsuarioRepository;
import br.com.certame.usuario.UsuarioService;
import io.micrometer.core.instrument.Metrics;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RedefinicaoSenhaService {
    private final UsuarioRepository usuarios;
    private final TokenSenhaRepository tokens;
    private final UsuarioService usuarioService;
    private final RefreshTokenService refreshTokens;
    private final LimitadorTentativas limitador;
    private final EmissorTokenSenha emissor;
    private final Clock clock;

    @Autowired
    public RedefinicaoSenhaService(UsuarioRepository usuarios, TokenSenhaRepository tokens,
            UsuarioService usuarioService, RefreshTokenService refreshTokens, LimitadorTentativas limitador,
            EmissorTokenSenha emissor) {
        this(usuarios, tokens, usuarioService, refreshTokens, limitador, emissor, Clock.systemUTC());
    }

    public RedefinicaoSenhaService(UsuarioRepository usuarios, TokenSenhaRepository tokens,
            UsuarioService usuarioService, RefreshTokenService refreshTokens, LimitadorTentativas limitador,
            EmissorTokenSenha emissor, Clock clock) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.usuarioService = usuarioService;
        this.refreshTokens = refreshTokens;
        this.limitador = limitador;
        this.emissor = emissor;
        this.clock = clock;
    }

    @Transactional
    public void solicitar(String email) {
        String normalizado = email.trim().toLowerCase(Locale.ROOT);
        limitador.verificar(normalizado);
        Metrics.counter("certame.auth.senha.pedido").increment();
        Usuario usuario = usuarios.findByEmailAndExcluidoEmIsNull(normalizado).orElse(null);
        if (usuario == null || usuario.getStatus() != StatusUsuario.ATIVO) return;
        emissor.emitir(usuario, FinalidadeTokenSenha.REDEFINICAO, false);
    }

    @Transactional
    public void redefinir(String tokenBruto, String novaSenha) {
        TokenSenha token = tokens.findByTokenHash(RefreshTokenService.hash(tokenBruto))
                .orElseThrow(TokenInvalidoException::new);
        Instant agora = clock.instant();
        if (token.getUsadoEm() != null || !agora.isBefore(token.getExpiraEm())) {
            throw new TokenInvalidoException();
        }
        Usuario usuario = token.getUsuario();
        usuarioService.trocarSenha(usuario, novaSenha);
        token.setUsadoEm(agora);
        tokens.save(token);
        if (token.getFinalidade() == FinalidadeTokenSenha.CONVITE
                && usuario.getStatus() == StatusUsuario.CONVITE_PENDENTE) {
            usuario.setStatus(StatusUsuario.ATIVO);
        }
        refreshTokens.revogarTodosDoUsuario(usuario.getId());
        Metrics.counter("certame.auth.senha.redefinida").increment();
    }

    @Scheduled(fixedDelayString = "${certame.seguranca.expurgo-token-senha-ms:3600000}")
    @Transactional
    public void expurgarTokensExpirados() {
        tokens.deleteByExpiraEmBefore(clock.instant());
    }

    @Transactional
    public void emitirRedefinicao(Usuario usuario) {
        emissor.emitir(usuario, FinalidadeTokenSenha.REDEFINICAO, false);
    }

    @Transactional
    public void emitirConvite(Usuario usuario) {
        emissor.emitir(usuario, FinalidadeTokenSenha.CONVITE, true);
    }
}
