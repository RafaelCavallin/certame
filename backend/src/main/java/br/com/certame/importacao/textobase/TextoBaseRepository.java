package br.com.certame.importacao.textobase;

import br.com.certame.questao.Fonte;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TextoBaseRepository extends JpaRepository<TextoBase, UUID> {
    Optional<TextoBase> findByFonteAndIdentificadorExterno(Fonte fonte, String identificadorExterno);
}
