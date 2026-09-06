package br.com.certame.catalogo.dto;

import java.util.UUID;

public record AssuntoArvore(UUID id, String nome, long totalQuestoes, long totalPublicadas, boolean arquivado) {}
