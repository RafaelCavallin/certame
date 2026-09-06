package br.com.certame.estudo.dto;

import java.util.UUID;

public record MarcacoesQuestao(UUID questaoId, boolean favorita, boolean erroRecorrente) {}
