package br.com.certame.usuario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AnonimizadorHistoricoUsuario {
    private static final Duration PRAZO_RETENCAO = Duration.ofDays(7);
    private static final String NOME_PLACEHOLDER = "Usuario anonimizado";
    private final UsuarioRepository usuarios;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public AnonimizadorHistoricoUsuario(UsuarioRepository usuarios, JdbcTemplate jdbc, Clock clock) {
        this.usuarios = usuarios;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void executar() {
        executarEm(Instant.now(clock));
    }

    @Transactional
    void executarEm(Instant agora) {
        var limite = agora.minus(PRAZO_RETENCAO);
        usuarios.findByExcluidoEmBeforeAndAnonimizadoEmIsNull(limite)
                .forEach(usuario -> anonimizar(usuario, agora));
    }

    private void anonimizar(Usuario usuario, Instant agora) {
        var usuarioId = usuario.getId();
        anonimizarConteudoLivre(usuarioId);
        anonimizarAuditorias(usuarioId);
        usuario.anonimizar(NOME_PLACEHOLDER, emailPlaceholder(usuarioId), agora);
        usuarios.save(usuario);
    }

    private void anonimizarConteudoLivre(UUID usuarioId) {
        jdbc.update("update caderno set nome = 'Caderno anonimizado ' || id, descricao = 'Conteudo anonimizado' where usuario_id = ?", usuarioId);
        jdbc.update("update deck set nome = 'Deck anonimizado ' || id, descricao = 'Conteudo anonimizado' where usuario_id = ?", usuarioId);
        jdbc.update("update flashcard set frente = 'Flashcard anonimizado', verso = 'Conteudo anonimizado', tags = '{}' where usuario_id = ?", usuarioId);
    }

    private void anonimizarAuditorias(UUID usuarioId) {
        jdbc.update("update auditoria set payload = cast(? as jsonb) where entidade_id = ? or ator_id = ?",
                "{\"anonimizado\":true}", usuarioId, usuarioId);
    }

    private String emailPlaceholder(UUID usuarioId) {
        return "anonimizado+" + usuarioId + "@anonimizado.invalid";
    }
}
