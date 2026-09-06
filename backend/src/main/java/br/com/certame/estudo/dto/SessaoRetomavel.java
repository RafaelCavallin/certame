package br.com.certame.estudo.dto;

import br.com.certame.estudo.*;
import java.time.Instant;
import java.util.UUID;

public record SessaoRetomavel(UUID sessaoId, ModoSessao modo, OrigemSessao origem,
                              String titulo, int indiceAtual, short total,
                              Instant ultimaAtividadeEm) {}
