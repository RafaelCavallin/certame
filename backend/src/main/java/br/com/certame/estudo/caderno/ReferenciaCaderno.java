package br.com.certame.estudo.caderno;

import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import java.util.UUID;

public final class ReferenciaCaderno {
    private static final String PREFIXO_AUTOMATICO = "auto:";
    private final TipoCadernoAutomatico tipo;
    private final UUID manualId;

    private ReferenciaCaderno(TipoCadernoAutomatico tipo, UUID manualId) {
        this.tipo = tipo;
        this.manualId = manualId;
    }

    public static ReferenciaCaderno de(String id) {
        if (id == null || id.isBlank()) {
            throw new RecursoNaoEncontradoException("CADERNO_NAO_ENCONTRADO");
        }
        if (id.startsWith(PREFIXO_AUTOMATICO)) {
            return automatico(id.substring(PREFIXO_AUTOMATICO.length()));
        }
        return manual(id);
    }

    public boolean automatico() { return tipo != null; }
    public TipoCadernoAutomatico tipoAutomatico() { return tipo; }
    public UUID manualId() { return manualId; }

    private static ReferenciaCaderno automatico(String valor) {
        try {
            return new ReferenciaCaderno(TipoCadernoAutomatico.valueOf(valor), null);
        } catch (IllegalArgumentException excecao) {
            throw new RecursoNaoEncontradoException("CADERNO_NAO_ENCONTRADO");
        }
    }

    private static ReferenciaCaderno manual(String valor) {
        try {
            return new ReferenciaCaderno(null, UUID.fromString(valor));
        } catch (IllegalArgumentException excecao) {
            throw new RecursoNaoEncontradoException("CADERNO_NAO_ENCONTRADO");
        }
    }
}
