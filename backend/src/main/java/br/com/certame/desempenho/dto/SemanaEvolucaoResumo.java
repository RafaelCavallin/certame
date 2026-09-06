package br.com.certame.desempenho.dto;

import java.time.LocalDate;

public record SemanaEvolucaoResumo(LocalDate semana, String rotulo, int volume, Integer pctAcerto) {}
