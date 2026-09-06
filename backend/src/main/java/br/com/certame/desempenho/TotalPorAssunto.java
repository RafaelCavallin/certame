package br.com.certame.desempenho;

import java.util.UUID;

public record TotalPorAssunto(UUID assuntoId, String assunto, UUID disciplinaId,
                              String disciplina, int volume, int pctAcerto) {}
