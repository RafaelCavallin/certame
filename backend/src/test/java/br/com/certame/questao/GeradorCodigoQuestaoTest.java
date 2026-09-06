package br.com.certame.questao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

class GeradorCodigoQuestaoTest {
    @Test
    void formataNumeroDaSequencia() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery("select nextval('questao_codigo_seq')")).thenReturn(query);
        when(query.getSingleResult()).thenReturn(10000L);

        assertThat(new GeradorCodigoQuestao(entityManager).gerar()).isEqualTo("Q-10000");
    }
}
