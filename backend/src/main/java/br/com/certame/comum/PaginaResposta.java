package br.com.certame.comum;

import java.util.List;

public record PaginaResposta<T>(List<T> content, int page, int size, long totalElements) {
    public PaginaResposta {
        content = List.copyOf(content);
    }
}
