package br.com.certame.auth;

import br.com.certame.config.PropriedadesSeguranca;
import br.com.certame.usuario.Usuario;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final PropriedadesSeguranca propriedades;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, PropriedadesSeguranca propriedades) {
        this(encoder, null, propriedades, Clock.systemUTC());
    }

    public TokenService(JwtEncoder encoder, PropriedadesSeguranca propriedades, Clock clock) {
        this(encoder, null, propriedades, clock);
    }

    @Autowired
    public TokenService(JwtEncoder encoder, JwtDecoder decoder, PropriedadesSeguranca propriedades, Clock clock) {
        this.encoder = encoder; this.decoder = decoder;
        this.propriedades = propriedades;
        this.clock = clock;
    }

    public Jwt lerAccessToken(String valor) { if (decoder == null) throw new IllegalStateException("JwtDecoder não configurado"); return decoder.decode(valor); }

    public AccessToken emitirAccessToken(Usuario usuario) {
        Instant agora = clock.instant();
        Instant expiraEm = agora.plus(propriedades.getAccessTokenTtl());
        var claims = JwtClaimsSet.builder().issuer("certame").subject(usuario.getId().toString())
                .issuedAt(agora).expiresAt(expiraEm).claim("perfil", usuario.getPerfil().name()).build();
        var token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AccessToken(token, expiraEm);
    }

    public record AccessToken(String valor, Instant expiraEm) { }
}
