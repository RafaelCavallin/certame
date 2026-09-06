package br.com.certame.estudo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.util.Random;

@Configuration
public class EstudoConfig {
    @Bean
    ConferenciaGabarito conferenciaGabarito() { return new ConferenciaGabarito(); }
    @Bean
    TempoSessao tempoSessao(Clock clock) { return new TempoSessao(clock); }
    @Bean
    MontadorSerie montadorSerie(SerieRepository series) { return new MontadorSerie(series, new Random()); }
}
