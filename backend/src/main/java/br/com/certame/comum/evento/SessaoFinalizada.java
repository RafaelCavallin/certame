package br.com.certame.comum.evento;

import java.time.Instant;
import java.util.UUID;

public record SessaoFinalizada(UUID usuarioId, UUID sessaoId, Instant finalizadaEm) {}
