package br.com.certame.catalogo.edital.dto;

import br.com.certame.catalogo.edital.StatusEdital;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EditalResumo(
        UUID id,
        String nome,
        StatusEdital status,
        Integer vagas,
        LocalDate prazoInscricao,
        String urlOficial,
        ItemCatalogoResumo banca,
        ItemCatalogoResumo orgao,
        List<DisciplinaExigida> disciplinas) {}
