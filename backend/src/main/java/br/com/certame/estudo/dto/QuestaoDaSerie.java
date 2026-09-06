package br.com.certame.estudo.dto;

import br.com.certame.questao.*;
import java.util.List;
import java.util.UUID;

public record QuestaoDaSerie(UUID id, short ordem, String codigo, TipoQuestao tipo, String enunciado,
                             String disciplina, String assunto, String banca, String orgao, Short ano,
                             Dificuldade dificuldade, List<AlternativaDaSerie> alternativas,
                             boolean favorita, boolean erroRecorrente) {}
