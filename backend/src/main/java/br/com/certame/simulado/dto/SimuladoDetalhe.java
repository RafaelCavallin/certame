package br.com.certame.simulado.dto;

import br.com.certame.simulado.StatusSimulado;
import java.time.Instant;
import java.util.UUID;

public record SimuladoDetalhe(UUID id, String codigo, String nome, String descricao, UUID bancaId,
                              Short numeroQuestoes, Integer duracaoSegundos, Instant janelaAbertura,
                              Instant janelaEncerramento, StatusSimulado status, Instant publicadoEm,
                              ResumoComposicao composicao) {}
