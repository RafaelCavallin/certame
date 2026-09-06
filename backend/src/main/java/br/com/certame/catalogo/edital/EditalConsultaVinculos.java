package br.com.certame.catalogo.edital;

import java.util.UUID;
import org.springframework.stereotype.Component;

public interface EditalConsultaVinculos {
    boolean disciplinaEmUso(UUID disciplinaId);
}

@Component
class EditalConsultaVinculosJpa implements EditalConsultaVinculos {
    private final EditalRepository editais;

    EditalConsultaVinculosJpa(EditalRepository editais) { this.editais = editais; }

    @Override
    public boolean disciplinaEmUso(UUID disciplinaId) {
        return editais.contarEditaisComDisciplina(disciplinaId) > 0;
    }
}
