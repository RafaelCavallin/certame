package br.com.certame.importacao.lote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

class GeradorCodigoLoteTest {
    @Test
    void formataNumeroDaSequencia() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery("select nextval('lote_importacao_codigo_seq')")).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);

        assertThat(new GeradorCodigoLote(entityManager).gerar()).isEqualTo("IMP-00001");
    }
}
