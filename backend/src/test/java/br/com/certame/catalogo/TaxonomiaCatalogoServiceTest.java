package br.com.certame.catalogo;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.certame.catalogo.edital.EditalConsultaVinculos;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import java.util.*;
import org.junit.jupiter.api.Test;

class TaxonomiaCatalogoServiceTest {
    private final DisciplinaRepository disciplinas = mock(DisciplinaRepository.class);
    private final AssuntoRepository assuntos = mock(AssuntoRepository.class);
    private final BancaRepository bancas = mock(BancaRepository.class);
    private final OrgaoRepository orgaos = mock(OrgaoRepository.class);
    private final RegistradorAuditoria auditoria = mock(RegistradorAuditoria.class);
    private final EditalConsultaVinculos editalVinculos = mock(EditalConsultaVinculos.class);
    private final ConsultaArvoreTaxonomia consultaArvore = new ConsultaArvoreTaxonomia(disciplinas);
    private final TaxonomiaService taxonomia = new TaxonomiaService(consultaArvore,
            new DisciplinaTaxonomiaService(disciplinas, auditoria, editalVinculos, consultaArvore),
            new AssuntoTaxonomiaService(disciplinas, assuntos, auditoria, consultaArvore));
    private final CatalogoService catalogo = new CatalogoService(bancas, orgaos, auditoria);

    @Test void TU23_excluirAssuntoEmUsoRecusaComContagem() {
        var id = UUID.randomUUID(); var assunto = mock(Assunto.class);
        when(assuntos.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(assunto)); when(assuntos.countQuestoesAtivas(id)).thenReturn(3L);
        assertThatThrownBy(() -> taxonomia.excluirAssunto(id)).isInstanceOf(ConflitoException.class).hasMessageContaining("3");
    }
    @Test void TU24_excluirAssuntoSemVinculoMarcaExcluido() {
        var id = UUID.randomUUID(); var assunto = mock(Assunto.class); when(assuntos.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(assunto));
        verifyExclusaoAssunto(id, assunto);
    }
    @Test void TU25_arquivarAssuntoAtualizaData() {
        var id = UUID.randomUUID(); var assunto = mock(Assunto.class); when(assuntos.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(assunto));
        taxonomia.arquivarAssunto(id); verify(assunto).setArquivadoEm(any());
    }
    @Test void TU26_renomearAssuntoAlteraNome() {
        var id = UUID.randomUUID(); var disciplina = mock(Disciplina.class); var assunto = mock(Assunto.class); var disciplinaId = UUID.randomUUID(); when(assunto.getNome()).thenReturn("Antigo"); when(assunto.getDisciplina()).thenReturn(disciplina); when(disciplina.getId()).thenReturn(disciplinaId);
        var linha = linhaAssunto(id, "Novo");
        when(assuntos.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(assunto)); when(disciplinas.contarArvore(true)).thenReturn(List.of(linha));
        taxonomia.renomearAssunto(id, "Novo"); verify(assunto).setNome("Novo");
    }
    @Test void TU27_nomeDeAssuntoDuplicadoRecusa() {
        var disciplina = mock(Disciplina.class); var id = UUID.randomUUID(); when(disciplina.getId()).thenReturn(id); when(disciplinas.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(disciplina)); when(assuntos.existsByDisciplinaIdAndNomeIgnoreCaseAndExcluidoEmIsNull(id, "Direito")).thenReturn(true);
        assertThatThrownBy(() -> taxonomia.criarAssunto(id, "Direito")).isInstanceOf(ConflitoException.class).hasMessageContaining("assunto");
    }
    @Test void TU36_criarBancaPersisteNomeESigla() {
        var id = UUID.randomUUID(); var banca = mock(Banca.class); when(banca.getId()).thenReturn(id); when(banca.getNome()).thenReturn("FCC"); when(banca.getSigla()).thenReturn("FCC"); when(bancas.save(any())).thenReturn(banca);
        assertThat(catalogo.criarBanca("FCC", "FCC")).isEqualTo(new br.com.certame.catalogo.dto.ItemCatalogo(id, "FCC", "FCC"));
    }
    @Test void TU37_excluirBancaEmUsoRecusa() { var id=UUID.randomUUID(); var banca=mock(Banca.class); when(bancas.findById(id)).thenReturn(Optional.of(banca)); when(bancas.countQuestoesAtivas(id)).thenReturn(1L); assertThatThrownBy(() -> catalogo.excluirBanca(id)).isInstanceOf(ConflitoException.class).hasMessageContaining("1"); }
    @Test void TU38_criarOrgaoPersisteNomeESigla() { var id=UUID.randomUUID(); var orgao=mock(Orgao.class); when(orgao.getId()).thenReturn(id); when(orgao.getNome()).thenReturn("TCU"); when(orgao.getSigla()).thenReturn("TCU"); when(orgaos.save(any())).thenReturn(orgao); assertThat(catalogo.criarOrgao("TCU", "TCU").sigla()).isEqualTo("TCU"); }
    @Test void TU39_excluirOrgaoEmUsoRecusa() { var id=UUID.randomUUID(); var orgao=mock(Orgao.class); when(orgaos.findById(id)).thenReturn(Optional.of(orgao)); when(orgaos.countQuestoesAtivas(id)).thenReturn(2L); assertThatThrownBy(() -> catalogo.excluirOrgao(id)).isInstanceOf(ConflitoException.class).hasMessageContaining("2"); }
    @Test void TU40_nomeDeBancaDuplicadoRecusa() { when(bancas.existsByNomeIgnoreCase("FCC")).thenReturn(true); assertThatThrownBy(() -> catalogo.criarBanca("FCC", null)).isInstanceOf(ConflitoException.class); }

    @Test void listaBancasEmOrdemDeNome() {
        var id = UUID.randomUUID(); var banca = mock(Banca.class); when(banca.getId()).thenReturn(id); when(banca.getNome()).thenReturn("Cebraspe"); when(banca.getSigla()).thenReturn("CB");
        when(bancas.findAllByOrderByNomeAsc()).thenReturn(List.of(banca));

        assertThat(catalogo.listarBancas()).containsExactly(new br.com.certame.catalogo.dto.ItemCatalogo(id, "Cebraspe", "CB"));
    }
    @Test void listaOrgaosEmOrdemDeNome() {
        var id = UUID.randomUUID(); var orgao = mock(Orgao.class); when(orgao.getId()).thenReturn(id); when(orgao.getNome()).thenReturn("TRF-1"); when(orgao.getSigla()).thenReturn("TRF1");
        when(orgaos.findAllByOrderByNomeAsc()).thenReturn(List.of(orgao));

        assertThat(catalogo.listarOrgaos()).containsExactly(new br.com.certame.catalogo.dto.ItemCatalogo(id, "TRF-1", "TRF1"));
    }
    @Test void renomeiaBancaAtualizandoNomeESigla() {
        var id = UUID.randomUUID(); var banca = mock(Banca.class); when(banca.getNome()).thenReturn("FCC"); when(bancas.findById(id)).thenReturn(Optional.of(banca));

        catalogo.renomearBanca(id, "  Fundação Carlos Chagas  ", " FCC ");

        verify(banca).setNome("Fundação Carlos Chagas"); verify(banca).setSigla("FCC"); verify(bancas).save(banca);
    }
    @Test void renomearBancaParaNomeDeOutraRecusa() {
        var id = UUID.randomUUID(); var banca = mock(Banca.class); when(banca.getNome()).thenReturn("FCC"); when(bancas.findById(id)).thenReturn(Optional.of(banca)); when(bancas.existsByNomeIgnoreCase("Cebraspe")).thenReturn(true);

        assertThatThrownBy(() -> catalogo.renomearBanca(id, "Cebraspe", null)).isInstanceOf(ConflitoException.class).hasMessageContaining("banca");
        verify(bancas, never()).save(any());
    }
    @Test void excluiBancaSemQuestoesVinculadas() {
        var id = UUID.randomUUID(); var banca = mock(Banca.class); when(bancas.findById(id)).thenReturn(Optional.of(banca)); when(bancas.countQuestoesAtivas(id)).thenReturn(0L);

        catalogo.excluirBanca(id);

        verify(bancas).delete(banca); verify(auditoria).registrar("EXCLUIR", "banca", id, null);
    }
    @Test void renomeiaOrgaoAtualizandoNomeELimpandoSiglaEmBranco() {
        var id = UUID.randomUUID(); var orgao = mock(Orgao.class); when(orgao.getNome()).thenReturn("TCU"); when(orgaos.findById(id)).thenReturn(Optional.of(orgao));

        catalogo.renomearOrgao(id, "Tribunal de Contas da União", "   ");

        verify(orgao).setNome("Tribunal de Contas da União"); verify(orgao).setSigla(null); verify(orgaos).save(orgao);
    }
    @Test void excluiOrgaoSemQuestoesVinculadas() {
        var id = UUID.randomUUID(); var orgao = mock(Orgao.class); when(orgaos.findById(id)).thenReturn(Optional.of(orgao)); when(orgaos.countQuestoesAtivas(id)).thenReturn(0L);

        catalogo.excluirOrgao(id);

        verify(orgaos).delete(orgao); verify(auditoria).registrar("EXCLUIR", "orgao", id, null);
    }
    @Test void excluirBancaInexistenteRecusa() {
        var id = UUID.randomUUID(); when(bancas.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogo.excluirBanca(id)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
    @Test void nomeDeOrgaoDuplicadoRecusa() {
        when(orgaos.existsByNomeIgnoreCase("TCU")).thenReturn(true);

        assertThatThrownBy(() -> catalogo.criarOrgao("TCU", null)).isInstanceOf(ConflitoException.class).hasMessageContaining("órgão");
        verify(orgaos, never()).save(any());
    }

    @Test void arvorePadraoAgregaTotaisDaDisciplinaEDosAssuntos() {
        var disciplinaId = UUID.randomUUID(); var assuntoId = UUID.randomUUID();
        var linhas = List.of(linhaDisciplina(disciplinaId, "Direito Administrativo", 12L, 7L),
                linhaAssuntoDe(disciplinaId, assuntoId, "Licitações", 5L, 3L));
        when(disciplinas.contarArvore(false)).thenReturn(linhas);

        var arvore = taxonomia.arvore();

        assertThat(arvore).singleElement().satisfies(no -> {
            assertThat(no.totalQuestoes()).isEqualTo(12L); assertThat(no.totalPublicadas()).isEqualTo(7L); assertThat(no.arquivado()).isFalse();
            assertThat(no.assuntos()).singleElement().satisfies(a -> { assertThat(a.id()).isEqualTo(assuntoId); assertThat(a.totalQuestoes()).isEqualTo(5L); assertThat(a.totalPublicadas()).isEqualTo(3L); });
        });
    }
    @Test void criaDisciplinaComNomeAparadoEDevolveONoDaArvore() {
        var id = UUID.randomUUID(); var disciplina = mock(Disciplina.class); when(disciplina.getId()).thenReturn(id);
        var linhas = List.of(linhaDisciplina(id, "Direito Penal", 0L, 0L));
        when(disciplinas.save(any())).thenReturn(disciplina); when(disciplinas.contarArvore(true)).thenReturn(linhas);

        var criada = taxonomia.criarDisciplina("  Direito Penal  ");

        assertThat(criada.nome()).isEqualTo("Direito Penal"); assertThat(criada.id()).isEqualTo(id);
        verify(auditoria).registrar("CRIAR", "disciplina", id, Map.of("nome", "Direito Penal"));
    }
    @Test void nomeDeDisciplinaDuplicadoRecusa() {
        when(disciplinas.existsByNomeIgnoreCaseAndExcluidoEmIsNull("Direito Penal")).thenReturn(true);

        assertThatThrownBy(() -> taxonomia.criarDisciplina("Direito Penal")).isInstanceOf(ConflitoException.class).hasMessageContaining("disciplina");
        verify(disciplinas, never()).save(any());
    }
    @Test void renomeiaDisciplinaAlterandoONome() {
        var id = UUID.randomUUID(); var disciplina = mock(Disciplina.class); when(disciplina.getNome()).thenReturn("Antigo");
        var linhas = List.of(linhaDisciplina(id, "Novo", 0L, 0L));
        when(disciplinas.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(disciplina)); when(disciplinas.contarArvore(true)).thenReturn(linhas);

        assertThat(taxonomia.renomearDisciplina(id, "Novo").nome()).isEqualTo("Novo");

        verify(disciplina).setNome("Novo"); verify(disciplinas).save(disciplina);
    }
    @Test void renomearDisciplinaInexistenteRecusa() {
        var id = UUID.randomUUID(); when(disciplinas.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taxonomia.renomearDisciplina(id, "Novo")).isInstanceOf(RecursoNaoEncontradoException.class);
    }
    @Test void criaAssuntoVinculadoADisciplina() {
        var disciplinaId = UUID.randomUUID(); var assuntoId = UUID.randomUUID();
        var disciplina = mock(Disciplina.class); when(disciplina.getId()).thenReturn(disciplinaId);
        var assunto = mock(Assunto.class); when(assunto.getId()).thenReturn(assuntoId);
        var linhas = List.of(linhaAssuntoDe(disciplinaId, assuntoId, "Licitações", 0L, 0L));
        when(disciplinas.findByIdAndExcluidoEmIsNull(disciplinaId)).thenReturn(Optional.of(disciplina));
        when(assuntos.save(any())).thenReturn(assunto); when(disciplinas.contarArvore(true)).thenReturn(linhas);

        var criado = taxonomia.criarAssunto(disciplinaId, "  Licitações  ");

        assertThat(criado.id()).isEqualTo(assuntoId); assertThat(criado.nome()).isEqualTo("Licitações");
        verify(auditoria).registrar("CRIAR", "assunto", assuntoId, Map.of("nome", "Licitações", "disciplinaId", disciplinaId));
    }
    @Test void criarAssuntoSemDisciplinaRecusa() {
        assertThatThrownBy(() -> taxonomia.criarAssunto(new br.com.certame.catalogo.dto.RequisicaoNoTaxonomia("Licitações", null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("disciplinaId");
    }
    @Test void excluirDisciplinaEmUsoRecusaComContagem() {
        var id = UUID.randomUUID(); var disciplina = mock(Disciplina.class);
        when(disciplinas.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(disciplina)); when(disciplinas.countQuestoesAtivas(id)).thenReturn(4L);

        assertThatThrownBy(() -> taxonomia.excluirDisciplina(id)).isInstanceOf(ConflitoException.class).hasMessageContaining("4");
        verify(disciplina, never()).setExcluidoEm(any());
    }
    @Test void excluirDisciplinaVinculadaAEditalRecusa() {
        var id = UUID.randomUUID(); var disciplina = mock(Disciplina.class);
        when(disciplinas.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(disciplina)); when(disciplinas.countQuestoesAtivas(id)).thenReturn(0L);
        when(editalVinculos.disciplinaEmUso(id)).thenReturn(true);

        assertThatThrownBy(() -> taxonomia.excluirDisciplina(id)).isInstanceOf(ConflitoException.class).hasMessageContaining("edital");
        verify(disciplina, never()).setExcluidoEm(any());
    }
    @Test void excluiDisciplinaSemVinculoMarcandoExcluida() {
        var id = UUID.randomUUID(); var disciplina = mock(Disciplina.class);
        when(disciplinas.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(disciplina)); when(disciplinas.countQuestoesAtivas(id)).thenReturn(0L);

        taxonomia.excluirDisciplina(id);

        verify(disciplina).setExcluidoEm(any()); verify(disciplinas).save(disciplina); verify(auditoria).registrar("EXCLUIR", "disciplina", id, null);
    }
    @Test void desarquivarAssuntoLimpaAData() {
        var id = UUID.randomUUID(); var assunto = mock(Assunto.class); when(assuntos.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(assunto));

        taxonomia.desarquivarAssunto(id);

        verify(assunto).setArquivadoEm(null); verify(auditoria).registrar("DESARQUIVAR", "assunto", id, null);
    }
    @Test void arquivarDisciplinaAtualizaData() {
        var id = UUID.randomUUID(); var disciplina = mock(Disciplina.class); when(disciplinas.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(disciplina));

        taxonomia.arquivarDisciplina(id);

        verify(disciplina).setArquivadoEm(any()); verify(auditoria).registrar("ARQUIVAR", "disciplina", id, null);
    }
    @Test void desarquivarDisciplinaLimpaAData() {
        var id = UUID.randomUUID(); var disciplina = mock(Disciplina.class); when(disciplinas.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(disciplina));

        taxonomia.desarquivarDisciplina(id);

        verify(disciplina).setArquivadoEm(null); verify(auditoria).registrar("DESARQUIVAR", "disciplina", id, null);
    }
    @Test void arvoreComArquivadosMarcaODisciplinaComoArquivada() {
        var id = UUID.randomUUID();
        var linha = linhaDisciplina(id, "Arquivada", 0L, 0L); when(linha.getDisciplinaArquivadaEm()).thenReturn(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        var linhas = List.of(linha);
        when(disciplinas.contarArvore(true)).thenReturn(linhas);

        assertThat(taxonomia.arvore(true)).singleElement().extracting(no -> no.arquivado()).isEqualTo(true);
    }


    private void verifyExclusaoAssunto(UUID id, Assunto assunto) { taxonomia.excluirAssunto(id); verify(assunto).setExcluidoEm(any()); verify(assuntos).save(assunto); }
    private DisciplinaRepository.ContagemArvore linhaAssunto(UUID id, String nome) { var linha=mock(DisciplinaRepository.ContagemArvore.class); var disciplinaId=UUID.randomUUID(); when(linha.getDisciplinaId()).thenReturn(disciplinaId); when(linha.getDisciplinaNome()).thenReturn("Disciplina"); when(linha.getAssuntoId()).thenReturn(id); when(linha.getAssuntoNome()).thenReturn(nome); when(linha.getTotalQuestoes()).thenReturn(0L); when(linha.getTotalPublicadas()).thenReturn(0L); return linha; }

    private DisciplinaRepository.ContagemArvore linhaDisciplina(UUID disciplinaId, String nome, long total, long publicadas) {
        var linha = mock(DisciplinaRepository.ContagemArvore.class);
        when(linha.getDisciplinaId()).thenReturn(disciplinaId); when(linha.getDisciplinaNome()).thenReturn(nome);
        when(linha.getAssuntoId()).thenReturn(null); when(linha.getTotalQuestoes()).thenReturn(total); when(linha.getTotalPublicadas()).thenReturn(publicadas);
        return linha;
    }
    private DisciplinaRepository.ContagemArvore linhaAssuntoDe(UUID disciplinaId, UUID assuntoId, String nome, long total, long publicadas) {
        var linha = mock(DisciplinaRepository.ContagemArvore.class);
        when(linha.getDisciplinaId()).thenReturn(disciplinaId); when(linha.getDisciplinaNome()).thenReturn("Disciplina");
        when(linha.getAssuntoId()).thenReturn(assuntoId); when(linha.getAssuntoNome()).thenReturn(nome);
        when(linha.getTotalQuestoes()).thenReturn(total); when(linha.getTotalPublicadas()).thenReturn(publicadas);
        return linha;
    }
}
