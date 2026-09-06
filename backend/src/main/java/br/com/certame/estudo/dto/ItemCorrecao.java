package br.com.certame.estudo.dto;

import java.util.UUID;

public record ItemCorrecao(short ordem, UUID questaoId, String codigo, String enunciadoResumo,
                           String disciplina, String gabarito, String marcada, Boolean correta,
                           String comentario) {}
