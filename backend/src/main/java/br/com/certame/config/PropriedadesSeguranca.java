package br.com.certame.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "certame.seguranca")
public class PropriedadesSeguranca {
    @NotBlank
    @Size(min = 32)
    private String jwtSegredo;
    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofDays(14);
    private Duration tokenSenhaTtl = Duration.ofMinutes(60);
    private String cookieNome = "certame_rt";
    private String cookiePath = "/api/v1/auth";
    private boolean cookieSecure = true;
    private String cookieSameSite = "Strict";
    @Positive
    private int limiteTentativas = 5;
    private Duration janelaTentativas = Duration.ofMinutes(15);
    private String frontendUrl = "http://localhost:4200";

    @AssertTrue(message = "certame.seguranca.jwt-segredo deve ter ao menos 32 bytes")
    boolean segredoTemTamanhoMinimoEmBytes() {
        return jwtSegredo == null || jwtSegredo.getBytes(StandardCharsets.UTF_8).length >= 32;
    }

    public String getJwtSegredo() { return jwtSegredo; }
    public void setJwtSegredo(String valor) { this.jwtSegredo = valor; }
    public Duration getAccessTokenTtl() { return accessTokenTtl; }
    public void setAccessTokenTtl(Duration valor) { this.accessTokenTtl = valor; }
    public Duration getRefreshTokenTtl() { return refreshTokenTtl; }
    public void setRefreshTokenTtl(Duration valor) { this.refreshTokenTtl = valor; }
    public Duration getTokenSenhaTtl() { return tokenSenhaTtl; }
    public void setTokenSenhaTtl(Duration valor) { this.tokenSenhaTtl = valor; }
    public String getCookieNome() { return cookieNome; }
    public void setCookieNome(String valor) { this.cookieNome = valor; }
    public String getCookiePath() { return cookiePath; }
    public void setCookiePath(String valor) { this.cookiePath = valor; }
    public boolean isCookieSecure() { return cookieSecure; }
    public void setCookieSecure(boolean valor) { this.cookieSecure = valor; }
    public String getCookieSameSite() { return cookieSameSite; }
    public void setCookieSameSite(String valor) { this.cookieSameSite = valor; }
    public int getLimiteTentativas() { return limiteTentativas; }
    public void setLimiteTentativas(int valor) { this.limiteTentativas = valor; }
    public Duration getJanelaTentativas() { return janelaTentativas; }
    public void setJanelaTentativas(Duration valor) { this.janelaTentativas = valor; }
    public String getFrontendUrl() { return frontendUrl; }
    public void setFrontendUrl(String valor) { this.frontendUrl = valor; }
}
