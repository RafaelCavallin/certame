package br.com.certame.simulado.dto;

import java.time.Instant;
import java.util.UUID;

public record SimuladoConcluido(UUID id, String codigo, String nome, String banca, Short numeroQuestoes,
                                Integer duracaoSegundos, UUID tentativaId, Short nota, Short acertos,
                                Short erros, Short naoRespondidas, Integer tempoTotalSegundos,
                                Instant concluidoEm, boolean podeRefazer) {}
