package br.com.certame.usuario;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {
    Optional<Usuario> findByEmailAndExcluidoEmIsNull(String email);
    Optional<Usuario> findByIdAndExcluidoEmIsNull(UUID id);
    boolean existsByEmail(String email);
    long countByPerfilAndStatusAndExcluidoEmIsNull(PerfilUsuario perfil, StatusUsuario status);
    List<Usuario> findByExcluidoEmBeforeAndAnonimizadoEmIsNull(Instant limite);
}
