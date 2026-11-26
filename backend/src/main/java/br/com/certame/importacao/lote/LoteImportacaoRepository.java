package br.com.certame.importacao.lote;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoteImportacaoRepository extends JpaRepository<LoteImportacao, UUID> {
}
