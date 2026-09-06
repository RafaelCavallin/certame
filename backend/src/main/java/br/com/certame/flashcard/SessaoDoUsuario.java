package br.com.certame.flashcard;

import br.com.certame.estudo.SessaoEstudo;
import java.util.Optional;
import java.util.UUID;

public interface SessaoDoUsuario {
    Optional<SessaoEstudo> buscar(UUID sessaoId, UUID usuarioId);
}
