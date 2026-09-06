package br.com.certame.catalogo.edital.dto;

import br.com.certame.catalogo.edital.StatusEdital;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EditalDetalhe(
        UUID id,
        String nome,
        StatusEdital status,
        Integer vagas,
        LocalDate prazoInscricao,
        String urlOficial,
        ItemCatalogoResumo banca,
        ItemCatalogoResumo orgao,
        List<DisciplinaExigida> disciplinas,
        Instant criadoEm,
        Instant atualizadoEm) {}
