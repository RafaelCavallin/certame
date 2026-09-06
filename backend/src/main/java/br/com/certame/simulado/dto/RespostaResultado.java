package br.com.certame.simulado.dto;

import br.com.certame.estudo.dto.RespostaCorrecao;
import br.com.certame.simulado.TipoTentativa;
import java.time.Instant;
import java.util.UUID;

public record RespostaResultado(UUID tentativaId, UUID simuladoId, String codigo, String nome,
                                TipoTentativa tipo, int nota, int acertos, int erros, int naoRespondidas,
                                int total, int tempoTotalSegundos, Instant finalizadaEm,
                                Integer notaRegistrada, RespostaCorrecao correcao) {}
