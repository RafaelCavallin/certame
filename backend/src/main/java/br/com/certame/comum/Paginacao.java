package br.com.certame.comum;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record Paginacao(@Min(0) int page, @Min(1) @Max(100) int size, String sort) {
    public Paginacao {
        sort = sort == null || sort.isBlank() ? "atualizadoEm,desc" : sort;
    }

    public Paginacao(int page, int size) {
        this(page, size, "atualizadoEm,desc");
    }
}
