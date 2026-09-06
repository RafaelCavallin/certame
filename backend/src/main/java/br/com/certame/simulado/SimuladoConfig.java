package br.com.certame.simulado;

import br.com.certame.questao.QuestaoRepository;
import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimuladoConfig {
    @Bean
    MontadorProvaSimulado montadorProvaSimulado(QuestaoRepository questoes) {
        return new MontadorProvaSimulado(questoes, new Random());
    }
}
