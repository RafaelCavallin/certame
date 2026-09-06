package br.com.certame.simulado;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface SimuladoRepository extends JpaRepository<Simulado, UUID> {
    Optional<Simulado> findByIdAndExcluidoEmIsNull(UUID id);
    boolean existsByCodigoAndExcluidoEmIsNull(String codigo);
    Page<Simulado> findByExcluidoEmIsNull(Pageable pageable);
    List<Simulado> findByStatusAndExcluidoEmIsNull(StatusSimulado status);

    @Query("""
            select s from Simulado s where s.excluidoEm is null
            and (:busca is null or lower(s.codigo) like lower(concat('%', :busca, '%'))
                 or lower(s.nome) like lower(concat('%', :busca, '%')))
            and (:bancaId is null or s.banca.id = :bancaId)
            and (:estado is null or
                 (:estado = 'RASCUNHO' and s.status = 'RASCUNHO') or
                 (:estado = 'AGENDADO' and s.status = 'PUBLICADO' and s.janelaAbertura > :agora) or
                 (:estado = 'DISPONIVEL' and s.status = 'PUBLICADO' and s.janelaAbertura <= :agora and s.janelaEncerramento > :agora) or
                 (:estado = 'ENCERRADO' and (s.status = 'ENCERRADO' or (s.status = 'PUBLICADO' and s.janelaEncerramento <= :agora))))
            order by s.atualizadoEm desc
            """)
    Page<Simulado> buscarAdministrativos(@Param("busca") String busca, @Param("estado") String estado,
            @Param("bancaId") UUID bancaId, @Param("agora") Instant agora, Pageable pageable);
}
