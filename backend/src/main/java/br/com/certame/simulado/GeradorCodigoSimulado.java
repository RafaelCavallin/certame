package br.com.certame.simulado;

import br.com.certame.comum.excecao.CodigoEmUsoException;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class GeradorCodigoSimulado {
    private final EntityManager entityManager;

    public GeradorCodigoSimulado(EntityManager entityManager) { this.entityManager = entityManager; }

    public String gerar() {
        try {
            var proximo = (Number) entityManager.createNativeQuery("select nextval('simulado_codigo_seq')").getSingleResult();
            return "SM-%03d".formatted(proximo.longValue());
        } catch (DataIntegrityViolationException excecao) {
            throw new CodigoEmUsoException();
        }
    }
}
