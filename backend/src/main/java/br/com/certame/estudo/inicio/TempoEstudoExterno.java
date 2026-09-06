package br.com.certame.estudo.inicio;

import java.time.LocalDate;
import java.util.UUID;

public interface TempoEstudoExterno {
    int segundosNoDia(UUID usuarioId, LocalDate dia);
}
