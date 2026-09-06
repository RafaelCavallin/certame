package br.com.certame.simulado;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ValidadorPublicacaoSimulado {
    public Map<String, String> validar(Simulado simulado, List<SimuladoComposicao> composicao,
                                       Map<UUID, Long> acervo) {
        var pendencias = new LinkedHashMap<String, String>();
        validarJanela(simulado, pendencias);
        validarSoma(simulado, composicao, pendencias);
        validarAcervo(composicao, acervo, pendencias);
        return pendencias;
    }

    private void validarJanela(Simulado simulado, Map<String, String> pendencias) {
        if (simulado.getJanelaAbertura() == null || simulado.getJanelaEncerramento() == null) {
            pendencias.put("janelaAbertura", "Defina a janela de aplicação antes de publicar.");
        }
    }

    private void validarSoma(Simulado simulado, List<SimuladoComposicao> composicao,
                             Map<String, String> pendencias) {
        var soma = composicao.stream().mapToInt(item -> item.getQuantidade()).sum();
        if (soma != simulado.getNumeroQuestoes()) {
            pendencias.put("composicao", "A composição soma %d questões, mas o simulado declara %d."
                    .formatted(soma, simulado.getNumeroQuestoes()));
        }
    }

    private void validarAcervo(List<SimuladoComposicao> composicao, Map<UUID, Long> acervo,
                               Map<String, String> pendencias) {
        for (var item : composicao) {
            var disponiveis = acervo.getOrDefault(item.getDisciplina().getId(), 0L);
            if (disponiveis < item.getQuantidade()) adicionarPendenciaAcervo(item, disponiveis, pendencias);
        }
    }

    private void adicionarPendenciaAcervo(SimuladoComposicao item, long disponiveis,
                                          Map<String, String> pendencias) {
        var disciplina = item.getDisciplina();
        pendencias.put("composicao." + disciplina.getNome(), "A disciplina exige " + item.getQuantidade()
                + " questões e há apenas " + disponiveis + " publicadas.");
    }
}
