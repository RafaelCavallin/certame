package br.com.certame.simulado;

import br.com.certame.catalogo.BancaRepository;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.simulado.dto.RequisicaoSimulado;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Preenche um Simulado a partir do pedido de criação/edição, validando a janela e a banca. */
@Component
public class MontadorSimulado {
    private final BancaRepository bancas;

    public MontadorSimulado(BancaRepository bancas) {
        this.bancas = bancas;
    }

    public void preencher(Simulado simulado, RequisicaoSimulado r) {
        validarJanela(r);
        simulado.setNome(r.nome().trim());
        simulado.setDescricao(r.descricao());
        simulado.setNumeroQuestoes(r.numeroQuestoes());
        simulado.setDuracaoSegundos(r.duracaoSegundos());
        simulado.setJanelaAbertura(r.janelaAbertura());
        simulado.setJanelaEncerramento(r.janelaEncerramento());
        simulado.setBanca(r.bancaId() == null ? null
                : bancas.findById(r.bancaId()).orElseThrow(() -> new DadosInvalidosException(Map.of("bancaId", "A banca informada não existe."))));
    }

    private void validarJanela(RequisicaoSimulado r) {
        if (r.janelaAbertura() != null && r.janelaEncerramento() != null && !r.janelaEncerramento().isAfter(r.janelaAbertura())) {
            throw new DadosInvalidosException(Map.of("janelaEncerramento", "A janela de encerramento deve ser posterior à abertura."));
        }
    }
}
