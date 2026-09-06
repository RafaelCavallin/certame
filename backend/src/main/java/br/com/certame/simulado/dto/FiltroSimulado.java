package br.com.certame.simulado.dto;

import java.util.UUID;

public record FiltroSimulado(String busca, String estado, UUID bancaId) {}
