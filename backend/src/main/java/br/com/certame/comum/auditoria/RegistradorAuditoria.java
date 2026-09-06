package br.com.certame.comum.auditoria;

import br.com.certame.usuario.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistradorAuditoria {
    private final AuditoriaRepository auditorias;
    private final UsuarioRepository usuarios;
    private final ObjectMapper objectMapper;

    public RegistradorAuditoria(AuditoriaRepository auditorias, UsuarioRepository usuarios,
                                ObjectMapper objectMapper) {
        this.auditorias = auditorias;
        this.usuarios = usuarios;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void registrar(String acao, String entidade, UUID entidadeId, Object payload) {
        var ator = buscarAtor();
        var auditoria = new Auditoria(ator, acao, entidade, entidadeId, serializar(payload));
        auditorias.save(auditoria);
    }

    private br.com.certame.usuario.Usuario buscarAtor() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) return null;
        try {
            return usuarios.findById(UUID.fromString(autenticacao.getName())).orElse(null);
        } catch (IllegalArgumentException excecao) {
            return null;
        }
    }

    private String serializar(Object payload) {
        if (payload == null) return null;
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException excecao) {
            throw new IllegalArgumentException("Payload de auditoria inválido.", excecao);
        }
    }
}
