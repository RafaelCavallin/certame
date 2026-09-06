package br.com.certame.estudo.caderno;

import br.com.certame.estudo.caderno.dto.CadernoResumo;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaCadernosService {
    private final CadernoAutomaticoService automaticos;
    private final CadernoRepository cadernos;
    private final ResumoCadernoService resumoService;
    private final CadernoObservabilidade observabilidade;

    public ConsultaCadernosService(CadernoAutomaticoService automaticos, CadernoRepository cadernos,
            ResumoCadernoService resumoService, CadernoObservabilidade observabilidade) {
        this.automaticos = automaticos;
        this.cadernos = cadernos;
        this.resumoService = resumoService;
        this.observabilidade = observabilidade;
    }

    @Transactional(readOnly = true)
    public List<CadernoResumo> listar(UUID usuarioId) {
        var resposta = new ArrayList<>(automaticos.projetar(usuarioId));
        var contagens = cadernos.contarQuestoes(usuarioId).stream().collect(java.util.stream.Collectors.toMap(
                CadernoRepository.Contagem::getCadernoId, c -> new long[]{c.getTotalQuestoes(), c.getQuestoesRespondidas()}));
        resposta.addAll(cadernos.findByUsuarioIdAndExcluidoEmIsNullOrderByAtualizadoEmDesc(usuarioId).stream().map(c -> {
            var contagem = contagens.getOrDefault(c.getId(), new long[]{0, 0});
            return resumoService.resumo(c, (int) contagem[0], (int) contagem[1]);
        }).toList());
        resposta.forEach(c -> observabilidade.progresso(c.progressoPercentual()));
        return resposta;
    }
}
