package br.com.certame.desempenho.dto;

import java.util.List;

public record EvolucaoSemanal(List<SemanaEvolucaoResumo> semanas, Integer pctMediaPeriodo,
                              SemanaEvolucaoResumo melhorSemana, int volumeTotal) {}
