package br.com.certame.desempenho.dto;

public record ResumoDesempenho(int totalRespostas, int acertos, int erros,
                               Integer pctAproveitamento, boolean possuiHistorico) {}
