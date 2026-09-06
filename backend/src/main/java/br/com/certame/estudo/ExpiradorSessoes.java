package br.com.certame.estudo;

import java.time.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ExpiradorSessoes {
    private static final Duration LIMITE_LEITURA = Duration.ofDays(7);
    private static final Duration LIMITE_PROVA = Duration.ofHours(24);
    private final SessaoEstudoRepository sessoes;
    private final Clock clock;
    private final EstudoObservabilidade observabilidade;
    public ExpiradorSessoes(SessaoEstudoRepository sessoes, Clock clock) { this(sessoes, clock, null); }
    @Autowired
    public ExpiradorSessoes(SessaoEstudoRepository sessoes, Clock clock, EstudoObservabilidade observabilidade) { this.sessoes = sessoes; this.clock = clock; this.observabilidade = observabilidade; }
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expirar() {
        var agora = clock.instant();
        sessoes.buscarRetomaveisExpiradas(agora.minus(LIMITE_PROVA)).stream()
                .filter(sessao -> expirado(sessao, agora)).forEach(sessao -> { sessao.setExpiradaEm(agora); if (observabilidade != null) observabilidade.sessaoExpirada(sessao.getModo()); });
    }
    private boolean expirado(SessaoEstudo sessao, Instant agora) {
        var limite = sessao.getModo() == ModoSessao.PROVA ? LIMITE_PROVA : LIMITE_LEITURA;
        return sessao.getUltimaAtividadeEm().isBefore(agora.minus(limite));
    }
}
