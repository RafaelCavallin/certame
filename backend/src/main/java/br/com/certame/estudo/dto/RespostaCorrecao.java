package br.com.certame.estudo.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RespostaCorrecao(UUID sessaoId, int pctAcerto, int acertos, int erros,
                               int naoRespondidas, int total, int tempoTotalSegundos,
                               Instant finalizadaEm, List<ItemCorrecao> itens) {}
