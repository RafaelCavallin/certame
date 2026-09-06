package br.com.certame.estudo.caderno.dto;

import java.util.UUID;

public record QuestaoCadernoResumo(UUID questaoId, String codigo, String enunciadoResumo,
                                   String disciplina, String assunto, boolean respondida) {}
