package br.com.certame.estudo.resposta;

import br.com.certame.desempenho.FonteRespostas;
import br.com.certame.estudo.RespostaEstatisticaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RespostaAgregadoAdapter implements FonteRespostas {
    private final RespostaEstatisticaRepository respostas;

    public RespostaAgregadoAdapter(RespostaEstatisticaRepository respostas) {
        this.respostas = respostas;
    }

    @Override
    public List<TotalPorChave<LocalDate>> porSemana(UUID usuarioId) {
        return respostas.porSemana(usuarioId).stream()
                .map(total -> new TotalPorChave<>(total.semana(), total.volume(), total.acertos())).toList();
    }

    @Override
    public List<TotalPorDisciplina> porDisciplina(UUID usuarioId) {
        return respostas.porDisciplina(usuarioId).stream()
                .map(total -> new TotalPorDisciplina(total.disciplinaId(), total.volume(), total.acertos())).toList();
    }

    @Override
    public List<TotalPorAssunto> porAssunto(UUID usuarioId) {
        return respostas.porAssunto(usuarioId).stream()
                .map(total -> new TotalPorAssunto(total.assuntoId(), total.disciplinaId(), total.volume(), total.acertos())).toList();
    }
}
