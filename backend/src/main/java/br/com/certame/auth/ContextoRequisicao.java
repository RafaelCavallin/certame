package br.com.certame.auth;

import java.net.InetAddress;

public record ContextoRequisicao(InetAddress ip, String userAgent) {
    public static ContextoRequisicao vazio() { return new ContextoRequisicao(null, null); }
}
