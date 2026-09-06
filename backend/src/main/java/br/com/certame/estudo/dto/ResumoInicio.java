package br.com.certame.estudo.dto;

import java.time.LocalDate;
import java.util.List;

public record ResumoInicio(LocalDate dia, int questoesResolvidas, Integer pctAcerto,
                           int tempoEstudoSegundos, int tempoQuestoesSegundos,
                           int tempoFlashcardsSegundos, List<SessaoRetomavel> sessoesRetomaveis) {}
