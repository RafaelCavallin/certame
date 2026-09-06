package br.com.certame.comum.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import br.com.certame.usuario.Usuario;
import br.com.certame.usuario.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RegistradorAuditoriaTest {
    @AfterEach void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test void TU02_persisteAuditoriaComAtorEPayloadSerializado() {
        var auditorias = mock(AuditoriaRepository.class);
        var usuarios = mock(UsuarioRepository.class);
        var atorId = UUID.randomUUID();
        var ator = mock(Usuario.class);
        when(usuarios.findById(atorId)).thenReturn(Optional.of(ator));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(atorId.toString(), null, java.util.List.of()));
        var registrador = new RegistradorAuditoria(auditorias, usuarios, new ObjectMapper());

        registrador.registrar("EDITAR", "questao", atorId, java.util.Map.of("status", "PUBLICADA"));

        var captor = org.mockito.ArgumentCaptor.forClass(Auditoria.class);
        verify(auditorias).save(captor.capture());
        assertThat(captor.getValue().getAtor()).isSameAs(ator);
        assertThat(captor.getValue().getPayload()).isEqualTo("{\"status\":\"PUBLICADA\"}");
    }
}
