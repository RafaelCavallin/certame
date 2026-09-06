package br.com.certame.auth;

import br.com.certame.comum.excecao.SessaoExpiradaException;
import br.com.certame.config.PropriedadesSeguranca;
import br.com.certame.usuario.Usuario;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final PropriedadesSeguranca propriedades;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, PropriedadesSeguranca propriedades) { this(repository, propriedades, Clock.systemUTC()); }
    @Autowired
    public RefreshTokenService(RefreshTokenRepository repository, PropriedadesSeguranca propriedades, Clock clock) {
        this.repository = repository; this.propriedades = propriedades; this.clock = clock;
    }

    @Transactional
    public String emitir(Usuario usuario, ContextoRequisicao contexto) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String bruto = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(new RefreshToken(usuario, hash(bruto), clock.instant().plus(propriedades.getRefreshTokenTtl()), contexto.ip(), contexto.userAgent()));
        return bruto;
    }

    @Transactional
    public Usuario consumirRotacionando(String tokenBruto) {
        if (tokenBruto == null || tokenBruto.isBlank()) throw new SessaoExpiradaException();
        var token = repository.findByTokenHash(hash(tokenBruto)).orElseThrow(SessaoExpiradaException::new);
        Instant agora = clock.instant();
        if (token.getUsadoEm() != null) {
            revogarTodosDoUsuario(token.getUsuario().getId());
            throw new SessaoExpiradaException();
        }
        if (token.getRevogadoEm() != null || !agora.isBefore(token.getExpiraEm())) throw new SessaoExpiradaException();
        token.setUsadoEm(agora);
        repository.save(token);
        return token.getUsuario();
    }

    @Transactional
    public void revogarTodosDoUsuario(java.util.UUID usuarioId) {
        Instant agora = clock.instant();
        repository.findAllByUsuarioId(usuarioId).forEach(token -> { if (token.getRevogadoEm() == null) token.setRevogadoEm(agora); });
        repository.flush();
    }

    @Transactional
    public void revogar(String tokenBruto) {
        if (tokenBruto == null || tokenBruto.isBlank()) return;
        repository.findByTokenHash(hash(tokenBruto)).ifPresent(token -> { token.setRevogadoEm(clock.instant()); repository.save(token); });
    }

    public static String hash(String valor) {
        try { return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException excecao) { throw new IllegalStateException(excecao); }
    }
}
