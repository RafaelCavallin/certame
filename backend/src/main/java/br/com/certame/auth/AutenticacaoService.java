package br.com.certame.auth;

import br.com.certame.auth.dto.RespostaAutenticacao;
import br.com.certame.auth.dto.RequisicaoLogin;
import br.com.certame.auth.dto.RequisicaoCadastro;
import br.com.certame.auth.dto.UsuarioResumo;
import br.com.certame.comum.excecao.CredencialInvalidaException;
import br.com.certame.usuario.StatusUsuario;
import br.com.certame.usuario.Usuario;
import br.com.certame.usuario.UsuarioRepository;
import br.com.certame.usuario.UsuarioService;
import io.micrometer.core.instrument.Metrics;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AutenticacaoService {
    private static final Logger log = LoggerFactory.getLogger(AutenticacaoService.class);
    private static final String HASH_ISCA = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokens;
    private final LimitadorTentativas limitador;
    private final UsuarioService usuarioService;

    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, TokenService tokenService,
                               RefreshTokenService refreshTokens, LimitadorTentativas limitador, UsuarioService usuarioService) {
        this.usuarios = usuarios; this.passwordEncoder = passwordEncoder; this.tokenService = tokenService; this.refreshTokens = refreshTokens; this.limitador = limitador; this.usuarioService = usuarioService;
    }

    @Transactional
    public ResultadoAutenticacao autenticar(RequisicaoLogin requisicao, ContextoRequisicao contexto) {
        String email = normalizar(requisicao.email());
        try { limitador.verificar(email); }
        catch (br.com.certame.comum.excecao.MuitasTentativasException excecao) {
            Metrics.counter("certame.auth.login", "resultado", "bloqueado").increment();
            log.warn("Login bloqueado por excesso de tentativas");
            throw excecao;
        }
        Usuario usuario = usuarios.findByEmailAndExcluidoEmIsNull(email).orElse(null);
        String hash = usuario == null || usuario.getSenhaHash() == null ? HASH_ISCA : usuario.getSenhaHash();
        boolean senhaValida = passwordEncoder.matches(requisicao.senha(), hash);
        if (!senhaValida || usuario == null || usuario.getStatus() != StatusUsuario.ATIVO) {
            limitador.registrarFalha(email);
            Metrics.counter("certame.auth.login", "resultado", "credencial_invalida").increment();
            throw new CredencialInvalidaException();
        }
        limitador.registrarSucesso(email);
        Metrics.counter("certame.auth.login", "resultado", "sucesso").increment();
        log.info("Login realizado para usuarioId={}", usuario.getId());
        return criarResultado(usuario, contexto);
    }

    @Transactional
    public ResultadoAutenticacao cadastrarEstudante(RequisicaoCadastro requisicao, ContextoRequisicao contexto) {
        String email = normalizar(requisicao.email());
        limitador.verificar(email);
        try {
            Usuario usuario = usuarioService.cadastrarEstudante(requisicao);
            limitador.registrarSucesso(email);
            Metrics.counter("certame.auth.cadastro", "resultado", "sucesso").increment();
            log.info("Cadastro realizado para usuarioId={}", usuario.getId());
            return criarResultado(usuario, contexto);
        } catch (br.com.certame.comum.excecao.EmailEmUsoException excecao) {
            limitador.registrarFalha(email);
            Metrics.counter("certame.auth.cadastro", "resultado", "email_em_uso").increment();
            throw excecao;
        }
    }

    @Transactional
    public ResultadoAutenticacao renovar(String refreshTokenBruto, ContextoRequisicao contexto) {
        try {
            Usuario usuario = refreshTokens.consumirRotacionando(refreshTokenBruto);
            return criarResultado(usuario, contexto);
        } catch (br.com.certame.comum.excecao.SessaoExpiradaException excecao) {
            Metrics.counter("certame.auth.refresh.reuso").increment();
            throw excecao;
        }
    }

    public void encerrar(String refreshTokenBruto) { refreshTokens.revogar(refreshTokenBruto); }

    private ResultadoAutenticacao criarResultado(Usuario usuario, ContextoRequisicao contexto) {
        var access = tokenService.emitirAccessToken(usuario);
        var refresh = refreshTokens.emitir(usuario, contexto);
        return new ResultadoAutenticacao(new RespostaAutenticacao(access.valor(), access.expiraEm(), UsuarioResumo.de(usuario)), refresh);
    }
    private String normalizar(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    public record ResultadoAutenticacao(RespostaAutenticacao resposta, String refreshToken) { }
}
