package br.com.certame.desempenho;

import java.time.LocalDate;

public record SemanaEvolucao(LocalDate semana, String rotulo, int volume, Integer pctAcerto) {}
