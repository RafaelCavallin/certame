package br.com.certame.estudo.dto;

import br.com.certame.estudo.*;
import jakarta.validation.Valid;

public record RequisicaoSessao(ModoSessao modo, @TamanhoSerieValido Integer tamanho, OrigemSessao origem,
                                String origemRef, @Valid FiltroSerie filtro) {
    public ModoSessao modoEfetivo() { return modo == null ? ModoSessao.FOCO : modo; }
    public OrigemSessao origemEfetiva() { return origem == null ? OrigemSessao.LIVRE : origem; }
    public FiltroSerie filtroEfetivo() { return filtro == null ? new FiltroSerie(null, null, null, null, null, false, false) : filtro; }
}
