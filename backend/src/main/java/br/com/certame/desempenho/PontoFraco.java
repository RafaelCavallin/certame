package br.com.certame.desempenho;

import java.util.UUID;

public record PontoFraco(UUID assuntoId, String assunto, UUID disciplinaId,
                         String disciplina, int volume, int pctAcerto) {}
