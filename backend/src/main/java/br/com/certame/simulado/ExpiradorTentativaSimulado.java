package br.com.certame.simulado;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpiradorTentativaSimulado {
    private final TentativaSimuladoRepository tentativas;

    public ExpiradorTentativaSimulado(TentativaSimuladoRepository tentativas) {
        this.tentativas = tentativas;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expirar() {
        tentativas.findByStatus(StatusTentativa.EM_ANDAMENTO).stream()
                .filter(tentativa -> tentativa.getSessao().getExpiradaEm() != null)
                .forEach(tentativa -> tentativa.setStatus(StatusTentativa.ABANDONADA));
    }
}
