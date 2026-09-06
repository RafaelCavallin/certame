package br.com.certame.auth;

import br.com.certame.comum.excecao.MuitasTentativasException;
import br.com.certame.config.PropriedadesSeguranca;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class LimitadorTentativasMemoria implements LimitadorTentativas {
    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
    private final PropriedadesSeguranca propriedades;
    private final Clock clock;

    public LimitadorTentativasMemoria(PropriedadesSeguranca propriedades) { this(propriedades, Clock.systemUTC()); }
    @Autowired
    public LimitadorTentativasMemoria(PropriedadesSeguranca propriedades, Clock clock) { this.propriedades = propriedades; this.clock = clock; }

    @Override public void verificar(String chave) {
        Janela janela = janelas.get(chave);
        if (janela == null || expirou(janela)) return;
        if (janela.falhas >= propriedades.getLimiteTentativas()) {
            long segundos = Math.max(1, Duration.between(clock.instant(), janela.inicio.plus(propriedades.getJanelaTentativas())).toSeconds());
            throw new MuitasTentativasException(segundos);
        }
    }

    @Override public void registrarFalha(String chave) {
        Instant agora = clock.instant();
        janelas.compute(chave, (k, atual) -> atual == null || expirou(atual) ? new Janela(agora, 1) : new Janela(atual.inicio, atual.falhas + 1));
    }

    @Override public void registrarSucesso(String chave) { janelas.remove(chave); }
    private boolean expirou(Janela janela) { return !clock.instant().isBefore(janela.inicio.plus(propriedades.getJanelaTentativas())); }
    private record Janela(Instant inicio, int falhas) { }
}
