package br.com.certame.simulado.dto;

import java.util.List;

public record RespostaSimulados(SimuladoResumo proximo, List<SimuladoResumo> disponiveis,
                                List<SimuladoResumo> agendados, List<SimuladoConcluido> concluidos) {}
