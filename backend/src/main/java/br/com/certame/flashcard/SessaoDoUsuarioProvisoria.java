package br.com.certame.flashcard;

import br.com.certame.estudo.SessaoEstudo;
import br.com.certame.estudo.SessaoEstudoRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SessaoDoUsuarioProvisoria implements SessaoDoUsuario {
    private final SessaoEstudoRepository sessoes;

    public SessaoDoUsuarioProvisoria(SessaoEstudoRepository sessoes) {
        this.sessoes = sessoes;
    }

    @Override
    public Optional<SessaoEstudo> buscar(UUID sessaoId, UUID usuarioId) {
        return sessoes.findByIdAndUsuarioId(sessaoId, usuarioId);
    }
}
