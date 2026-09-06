package br.com.certame.estudo;

import br.com.certame.estudo.caderno.CadernoRepository;
import br.com.certame.estudo.caderno.ReferenciaCaderno;
import br.com.certame.estudo.caderno.TipoCadernoAutomatico;
import br.com.certame.simulado.SimuladoRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Nomeia uma sessão de estudo para exibição (ex.: card de "retome de onde parou"). */
@Component
public class TituloSessaoService {
    private final SessaoQuestaoRepository questoes;
    private final CadernoRepository cadernos;
    private final SimuladoRepository simulados;

    public TituloSessaoService(SessaoQuestaoRepository questoes, CadernoRepository cadernos,
            SimuladoRepository simulados) {
        this.questoes = questoes;
        this.cadernos = cadernos;
        this.simulados = simulados;
    }

    public String titulo(UUID sessaoId, OrigemSessao origem, String origemRef, UUID usuarioId) {
        return switch (origem) {
            case CADERNO -> tituloCaderno(usuarioId, origemRef);
            case SIMULADO -> tituloSimulado(origemRef);
            case LIVRE -> tituloLivre(sessaoId);
        };
    }

    private String tituloCaderno(UUID usuarioId, String origemRef) {
        try {
            var referencia = ReferenciaCaderno.de(origemRef);
            if (referencia.automatico()) {
                return "Caderno: " + nomeAutomatico(referencia.tipoAutomatico());
            }
            return cadernos.findByIdAndUsuarioIdAndExcluidoEmIsNull(referencia.manualId(), usuarioId)
                    .map(c -> "Caderno: " + c.getNome())
                    .orElse("Caderno");
        } catch (RuntimeException excecao) {
            return "Caderno";
        }
    }

    private String tituloSimulado(String origemRef) {
        try {
            return simulados.findByIdAndExcluidoEmIsNull(UUID.fromString(origemRef))
                    .map(s -> "Simulado: " + s.getNome())
                    .orElse("Simulado");
        } catch (IllegalArgumentException excecao) {
            return "Simulado";
        }
    }

    private String tituloLivre(UUID sessaoId) {
        List<String> disciplinas = questoes.disciplinasDaSessao(sessaoId);
        if (disciplinas.size() == 1) {
            return disciplinas.get(0);
        }
        return "Questões variadas";
    }

    private String nomeAutomatico(TipoCadernoAutomatico tipo) {
        return switch (tipo) {
            case ERRO_RECORRENTE -> "Erros recorrentes";
            case FAVORITA -> "Favoritas";
        };
    }
}
