package br.com.certame.desempenho.dto;

import java.util.UUID;

public record DesempenhoDisciplinaResumo(UUID disciplinaId, String disciplina,
                                         int volume, int pctAcerto) {}
