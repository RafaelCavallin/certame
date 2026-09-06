package br.com.certame.auth;

import br.com.certame.auth.dto.RequisicaoLogin;
import br.com.certame.auth.dto.RequisicaoCadastro;
import br.com.certame.auth.dto.RequisicaoEsqueciSenha;
import br.com.certame.auth.dto.RequisicaoRedefinirSenha;
import br.com.certame.auth.dto.RespostaAutenticacao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.com.certame.config.PropriedadesSeguranca;

@RestController
@RequestMapping("/api/v1/auth")
public class AutenticacaoController {
    private static final String COOKIE_REFRESH = "certame_rt";
    private final AutenticacaoService service;
    private final RedefinicaoSenhaService redefinicaoSenha;
    private final PropriedadesSeguranca propriedades;

    public AutenticacaoController(AutenticacaoService service, PropriedadesSeguranca propriedades,
                                  RedefinicaoSenhaService redefinicaoSenha) {
        this.service = service; this.propriedades = propriedades; this.redefinicaoSenha = redefinicaoSenha;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody RequisicaoLogin requisicao, HttpServletRequest request) {
        return resposta(service.autenticar(requisicao, contexto(request)));
    }

    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastro(@Valid @RequestBody RequisicaoCadastro requisicao, HttpServletRequest request) {
        return respostaCriacao(service.cadastrarEstudante(requisicao, contexto(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(value = COOKIE_REFRESH, required = false) String cookie, HttpServletRequest request) {
        return resposta(service.renovar(cookie, contexto(request)));
    }

    @PostMapping("/senha/esqueci")
    public ResponseEntity<Void> esqueciSenha(@Valid @RequestBody RequisicaoEsqueciSenha requisicao) {
        redefinicaoSenha.solicitar(requisicao.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/senha/redefinir")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RequisicaoRedefinirSenha requisicao) {
        redefinicaoSenha.redefinir(requisicao.token(), requisicao.senha());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = COOKIE_REFRESH, required = false) String cookie) {
        service.encerrar(cookie);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookieLimpo().toString()).build();
    }

    private ResponseEntity<RespostaAutenticacao> resposta(AutenticacaoService.ResultadoAutenticacao resultado) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(resultado.refreshToken()).toString()).body(resultado.resposta());
    }

    private ResponseEntity<RespostaAutenticacao> respostaCriacao(AutenticacaoService.ResultadoAutenticacao resultado) {
        return ResponseEntity.status(201).header(HttpHeaders.SET_COOKIE, cookie(resultado.refreshToken()).toString()).body(resultado.resposta());
    }

    private ResponseCookie cookie(String valor) {
        return ResponseCookie.from(propriedades.getCookieNome(), valor).httpOnly(true).secure(propriedades.isCookieSecure())
                .sameSite(propriedades.getCookieSameSite()).path(propriedades.getCookiePath())
                .maxAge(propriedades.getRefreshTokenTtl()).build();
    }
    private ResponseCookie cookieLimpo() { return ResponseCookie.from(propriedades.getCookieNome(), "").httpOnly(true).secure(propriedades.isCookieSecure())
            .sameSite(propriedades.getCookieSameSite()).path(propriedades.getCookiePath()).maxAge(0).build(); }
    private ContextoRequisicao contexto(HttpServletRequest request) {
        InetAddress ip = null;
        try { ip = InetAddress.getByName(request.getRemoteAddr()); } catch (UnknownHostException ignored) { }
        return new ContextoRequisicao(ip, request.getHeader("User-Agent"));
    }
}
