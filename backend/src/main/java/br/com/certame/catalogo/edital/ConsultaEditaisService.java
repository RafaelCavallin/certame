package br.com.certame.catalogo.edital;

import br.com.certame.catalogo.edital.dto.EditaisAgrupados;
import br.com.certame.catalogo.edital.dto.EditalAdminResumo;
import br.com.certame.catalogo.edital.dto.EditalDetalhe;
import br.com.certame.catalogo.edital.dto.EditalResumo;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConsultaEditaisService {
    private final EditalRepository editais;
    private final VerificadorPrazoEdital verificadorPrazo;

    public ConsultaEditaisService(EditalRepository editais, VerificadorPrazoEdital verificadorPrazo) {
        this.editais = editais;
        this.verificadorPrazo = verificadorPrazo;
    }

    public EditaisAgrupados agrupados() {
        var abertos = resumir(editais.findByStatusAndExcluidoEmIsNullOrderByPrazoInscricaoAsc(StatusEdital.ABERTO));
        var previstos = resumir(editais.findByStatusAndExcluidoEmIsNullOrderByNomeAsc(StatusEdital.PREVISTO));
        var encerrados = resumir(editais.findPorStatusOrdenadosPorPrazoDescendente(StatusEdital.ENCERRADO));
        return new EditaisAgrupados(abertos, previstos, encerrados);
    }

    public List<EditalAdminResumo> listarParaAdministracao() {
        return editais.findByExcluidoEmIsNull().stream()
                .sorted(comparadorAdministracao())
                .map(this::resumirParaAdministracao)
                .toList();
    }

    public EditalDetalhe buscarParaEdicao(UUID id) {
        var edital = editais.findByIdAndExcluidoEmIsNull(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("EDITAL_NAO_ENCONTRADO"));
        return EditalDetalheMapeador.detalhar(edital);
    }

    private List<EditalResumo> resumir(List<Edital> editais) {
        return editais.stream().map(EditalDetalheMapeador::resumir).toList();
    }

    private EditalAdminResumo resumirParaAdministracao(Edital edital) {
        var prazoVencido = verificadorPrazo.vencido(edital.getStatus(), edital.getPrazoInscricao());
        return EditalDetalheMapeador.resumirParaAdministracao(edital, prazoVencido);
    }

    private Comparator<Edital> comparadorAdministracao() {
        return Comparator.comparing(Edital::getStatus)
                .thenComparing(Edital::getPrazoInscricao, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Edital::getNome);
    }
}
