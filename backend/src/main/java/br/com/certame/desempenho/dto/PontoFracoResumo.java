package br.com.certame.desempenho.dto;

import java.util.UUID;

public record PontoFracoResumo(UUID assuntoId, String assunto, UUID disciplinaId,
                         String disciplina, int volume, int pctAcerto) {}
