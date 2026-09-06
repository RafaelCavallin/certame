package br.com.certame.catalogo.edital.dto;

import br.com.certame.catalogo.edital.StatusEdital;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EditalAdminResumo(
        UUID id,
        String nome,
        StatusEdital status,
        Integer vagas,
        LocalDate prazoInscricao,
        ItemCatalogoResumo banca,
        ItemCatalogoResumo orgao,
        int totalDisciplinas,
        boolean prazoVencido,
        Instant atualizadoEm) {}
