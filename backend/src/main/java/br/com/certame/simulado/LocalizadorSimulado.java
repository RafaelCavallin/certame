package br.com.certame.simulado;

import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Localiza um simulado ativo e garante que não tenha tentativa finalizada antes de alterações. */
@Component
public class LocalizadorSimulado {
    private final SimuladoRepository simulados;
    private final TentativaSimuladoRepository tentativas;

    public LocalizadorSimulado(SimuladoRepository simulados, TentativaSimuladoRepository tentativas) {
        this.simulados = simulados;
        this.tentativas = tentativas;
    }

    public Simulado obter(UUID id) {
        return simulados.findByIdAndExcluidoEmIsNull(id).orElseThrow(() -> new RecursoNaoEncontradoException("SIMULADO_NAO_ENCONTRADO"));
    }

    public Simulado obterAlteravel(UUID id) {
        var simulado = obter(id);
        if (tentativas.existsBySimuladoIdAndStatus(id, StatusTentativa.FINALIZADA)) {
            throw new ConflitoException("SIMULADO_EM_USO", "O simulado possui tentativa finalizada e não pode ser alterado.");
        }
        return simulado;
    }
}
