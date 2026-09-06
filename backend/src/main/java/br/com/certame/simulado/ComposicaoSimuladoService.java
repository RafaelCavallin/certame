package br.com.certame.simulado;

import br.com.certame.catalogo.DisciplinaRepository;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.comum.excecao.ComposicaoInvalidaException;
import br.com.certame.simulado.dto.ItemComposicao;
import br.com.certame.simulado.dto.ResumoComposicao;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Substitui a composição (disciplina + quantidade de questões) de um simulado, validando os itens. */
@Service
@Transactional
public class ComposicaoSimuladoService {
    private final SimuladoComposicaoRepository composicoes;
    private final DisciplinaRepository disciplinas;
    private final RegistradorAuditoria auditoria;
    private final ConsultaSimuladoAdmin consulta;

    public ComposicaoSimuladoService(SimuladoComposicaoRepository composicoes, DisciplinaRepository disciplinas,
            RegistradorAuditoria auditoria, ConsultaSimuladoAdmin consulta) {
        this.composicoes = composicoes;
        this.disciplinas = disciplinas;
        this.auditoria = auditoria;
        this.consulta = consulta;
    }

    public ResumoComposicao substituir(Simulado simulado, List<ItemComposicao> itens) {
        validarItens(itens);
        var entidades = itens.stream().map(item -> novaComposicao(simulado, item)).toList();
        composicoes.deleteAll(composicoes.findBySimuladoId(simulado.getId()));
        composicoes.flush();
        composicoes.saveAll(entidades);
        auditoria.registrar("SIMULADO_COMPOSICAO_ALTERADA", "simulado", simulado.getId(), itens);
        return consulta.resumoComposicao(simulado, entidades);
    }

    private void validarItens(List<ItemComposicao> itens) {
        var chaves = itens.stream().map(i -> i.disciplinaId() + "|" + i.dificuldade()).toList();
        if (chaves.size() != new HashSet<>(chaves).size()) {
            throw new ComposicaoInvalidaException(
                    Map.of("itens", "Cada combinação de disciplina e dificuldade pode aparecer uma única vez."));
        }
        for (var item : itens) {
            disciplinas.findByIdAndExcluidoEmIsNull(item.disciplinaId()).filter(d -> d.getArquivadoEm() == null)
                    .orElseThrow(() -> new ComposicaoInvalidaException(Map.of("itens", "A disciplina informada não existe ou está arquivada.")));
        }
    }

    private SimuladoComposicao novaComposicao(Simulado s, ItemComposicao item) {
        return new SimuladoComposicao(s, disciplinas.findByIdAndExcluidoEmIsNull(item.disciplinaId()).orElseThrow(),
                item.dificuldade(), item.quantidade());
    }
}
