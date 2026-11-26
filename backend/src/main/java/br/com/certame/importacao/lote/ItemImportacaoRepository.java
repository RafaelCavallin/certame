package br.com.certame.importacao.lote;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemImportacaoRepository extends JpaRepository<ItemImportacao, UUID> {
}
