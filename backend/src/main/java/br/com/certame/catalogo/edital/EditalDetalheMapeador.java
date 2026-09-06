package br.com.certame.catalogo.edital;

import br.com.certame.catalogo.Banca;
import br.com.certame.catalogo.Disciplina;
import br.com.certame.catalogo.Orgao;
import br.com.certame.catalogo.edital.dto.DisciplinaExigida;
import br.com.certame.catalogo.edital.dto.EditalAdminResumo;
import br.com.certame.catalogo.edital.dto.EditalDetalhe;
import br.com.certame.catalogo.edital.dto.EditalResumo;
import br.com.certame.catalogo.edital.dto.ItemCatalogoResumo;
import java.util.Comparator;
import java.util.List;

final class EditalDetalheMapeador {
    private EditalDetalheMapeador() {}

    static EditalDetalhe detalhar(Edital edital) {
        return new EditalDetalhe(edital.getId(), edital.getNome(), edital.getStatus(), edital.getVagas(),
                edital.getPrazoInscricao(), edital.getUrlOficial(), item(edital.getBanca()),
                item(edital.getOrgao()), disciplinasExigidas(edital), edital.getCriadoEm(),
                edital.getAtualizadoEm());
    }

    static EditalResumo resumir(Edital edital) {
        return new EditalResumo(edital.getId(), edital.getNome(), edital.getStatus(), edital.getVagas(),
                edital.getPrazoInscricao(), edital.getUrlOficial(), item(edital.getBanca()),
                item(edital.getOrgao()), disciplinasExigidas(edital));
    }

    static EditalAdminResumo resumirParaAdministracao(Edital edital, boolean prazoVencido) {
        return new EditalAdminResumo(edital.getId(), edital.getNome(), edital.getStatus(), edital.getVagas(),
                edital.getPrazoInscricao(), item(edital.getBanca()), item(edital.getOrgao()),
                edital.getDisciplinas().size(), prazoVencido, edital.getAtualizadoEm());
    }

    private static List<DisciplinaExigida> disciplinasExigidas(Edital edital) {
        return edital.getDisciplinas().stream()
                .sorted(Comparator.comparing(Disciplina::getNome))
                .map(d -> new DisciplinaExigida(d.getId(), d.getNome()))
                .toList();
    }

    private static ItemCatalogoResumo item(Banca banca) {
        return banca == null ? null : new ItemCatalogoResumo(banca.getId(), banca.getNome(), banca.getSigla());
    }

    private static ItemCatalogoResumo item(Orgao orgao) {
        return orgao == null ? null : new ItemCatalogoResumo(orgao.getId(), orgao.getNome(), orgao.getSigla());
    }
}
