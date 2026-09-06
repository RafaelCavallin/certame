package br.com.certame.catalogo.edital.dto;

import java.util.List;

public record EditaisAgrupados(
        List<EditalResumo> abertos, List<EditalResumo> previstos, List<EditalResumo> encerrados) {}
