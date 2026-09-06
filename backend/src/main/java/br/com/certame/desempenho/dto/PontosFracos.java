package br.com.certame.desempenho.dto;

import java.util.List;

public record PontosFracos(Integer pctMediaUsuario, int minimoRespostas,
                           List<PontoFracoResumo> itens) {}
