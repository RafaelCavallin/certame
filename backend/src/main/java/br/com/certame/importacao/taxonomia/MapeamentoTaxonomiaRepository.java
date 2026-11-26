package br.com.certame.importacao.taxonomia;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapeamentoTaxonomiaRepository extends JpaRepository<MapeamentoTaxonomia, UUID> {
}
