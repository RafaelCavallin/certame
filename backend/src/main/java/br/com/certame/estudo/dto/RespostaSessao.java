package br.com.certame.estudo.dto;

import br.com.certame.estudo.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RespostaSessao(UUID sessaoId, ModoSessao modo, OrigemSessao origem, String origemRef,
                             int total, int indiceAtual, Instant iniciadaEm, int decorridoSegundos,
                             List<QuestaoDaSerie> questoes, List<RespostaRegistrada> respostas) {}
