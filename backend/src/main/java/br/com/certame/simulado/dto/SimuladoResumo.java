package br.com.certame.simulado.dto;

import br.com.certame.simulado.EstadoSimulado;
import java.time.Instant;
import java.util.UUID;

public record SimuladoResumo(UUID id, String codigo, String nome, String banca, Short numeroQuestoes,
                             Integer duracaoSegundos, EstadoSimulado estado, Instant janelaAbertura,
                             Instant janelaEncerramento, UUID tentativaEmAndamentoId) {}
