package br.com.certame.simulado.dto;

import br.com.certame.estudo.dto.RespostaSessao;
import br.com.certame.simulado.TipoTentativa;
import java.time.Instant;
import java.util.UUID;

public record RespostaTentativa(UUID tentativaId, UUID simuladoId, String codigo, String nome,
                                TipoTentativa tipo, Integer duracaoSegundos, Instant iniciadaEm,
                                RespostaSessao sessao) {}
