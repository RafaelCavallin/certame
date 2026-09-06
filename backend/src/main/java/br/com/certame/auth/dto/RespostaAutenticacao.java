package br.com.certame.auth.dto;

import java.time.Instant;

public record RespostaAutenticacao(String accessToken, Instant expiraEm, UsuarioResumo usuario) {
}
