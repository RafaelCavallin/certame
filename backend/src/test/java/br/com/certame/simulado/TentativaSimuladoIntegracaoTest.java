package br.com.certame.simulado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certame.estudo.dto.RespostaProva;
import br.com.certame.estudo.dto.RequisicaoFinalizacao;
import br.com.certame.simulado.dto.ItemComposicao;
import br.com.certame.simulado.dto.RequisicaoSimulado;
import br.com.certame.usuario.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(properties = "certame.seguranca.jwt-segredo=chave-de-teste-local-com-mais-de-trinta-e-dois")
@ActiveProfiles("test") @AutoConfigureMockMvc @Testcontainers
class TentativaSimuladoIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");
    @DynamicPropertySource static void datasource(DynamicPropertyRegistry r) { r.add("spring.datasource.url", postgres::getJdbcUrl); r.add("spring.datasource.username", postgres::getUsername); r.add("spring.datasource.password", postgres::getPassword); }
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private SimuladoAdminService admin;
    @Autowired private PublicacaoSimuladoService publicacao;
    @Autowired private SimuladoCatalogoService catalogo;
    @Autowired private TentativaSimuladoService tentativas;
    @Autowired private ExpiradorTentativaSimulado expirador;
    @Autowired private org.springframework.test.web.servlet.MockMvc mvc;
    private UUID adminId;

    @BeforeEach void autenticarAdmin() { var admin = novoUsuario(PerfilUsuario.ADMIN); adminId = admin.getId(); autenticar(adminId, "ADMIN"); }

    @Test void TI03_doisEstudantesRecebemAMesmaProva() {
        var simulado = publicado(); var primeiro = novoUsuario(PerfilUsuario.ESTUDANTE); var segundo = novoUsuario(PerfilUsuario.ESTUDANTE);
        autenticar(primeiro.getId(), "ESTUDANTE"); var serieUm = tentativas.iniciar(primeiro.getId(), simulado.id()).sessao().questoes().stream().map(q -> q.id()).toList();
        autenticar(segundo.getId(), "ESTUDANTE"); var serieDois = tentativas.iniciar(segundo.getId(), simulado.id()).sessao().questoes().stream().map(q -> q.id()).toList();
        assertThat(serieDois).containsExactlyElementsOf(serieUm);
    }

    @Test void TI04_aplicaSimuladoDoInicioAoResultado() {
        var simulado = publicado(); var estudante = novoUsuario(PerfilUsuario.ESTUDANTE); autenticar(estudante.getId(), "ESTUDANTE");
        var inicio = tentativas.iniciar(estudante.getId(), simulado.id()); var respostas = inicio.sessao().questoes().stream().limit(3).map(q -> new RespostaProva(q.id(), "CERTO", 100)).toList();
        var resultado = tentativas.finalizar(estudante.getId(), inicio.tentativaId(), new RequisicaoFinalizacao(respostas, 0));
        assertThat(resultado.total()).isEqualTo(10); assertThat(resultado.correcao().itens()).hasSize(10); assertThat(resultado.acertos() + resultado.erros() + resultado.naoRespondidas()).isEqualTo(10);
    }

    @Test void TI05_naoRegistraNotaDeTentativaAbandonada() {
        var simulado = publicado(); var estudante = novoUsuario(PerfilUsuario.ESTUDANTE); autenticar(estudante.getId(), "ESTUDANTE"); var inicio = tentativas.iniciar(estudante.getId(), simulado.id());
        jdbc.update("update sessao_estudo set expirada_em = now() where id = ?", inicio.sessao().sessaoId()); expirador.expirar();
        var lista = catalogo.listarDoEstudante(estudante.getId());
        assertThat(lista.concluidos()).isEmpty(); assertThat(jdbc.queryForObject("select status from tentativa_simulado where id = ?", String.class, inicio.tentativaId())).isEqualTo("ABANDONADA");
    }

    @Test void TI06_refeituraNaoAlteraNotaRegistrada() {
        var simulado = publicado(); var estudante = novoUsuario(PerfilUsuario.ESTUDANTE); autenticar(estudante.getId(), "ESTUDANTE"); var primeira = tentativas.iniciar(estudante.getId(), simulado.id());
        var original = tentativas.finalizar(estudante.getId(), primeira.tentativaId(), new RequisicaoFinalizacao(List.of(), 0)); var segunda = tentativas.iniciar(estudante.getId(), simulado.id());
        tentativas.finalizar(estudante.getId(), segunda.tentativaId(), new RequisicaoFinalizacao(List.of(), 0));
        assertThat(original.nota()).isEqualTo(0); assertThat(jdbc.queryForObject("select count(*) from tentativa_simulado where simulado_id = ? and usuario_id = ? and tipo = 'VALIDA' and status = 'FINALIZADA'", Integer.class, simulado.id(), estudante.getId())).isOne();
    }

    @Test void TI07_congelaTentativaFinalizada() {
        var simulado = publicado(); var estudante = novoUsuario(PerfilUsuario.ESTUDANTE); autenticar(estudante.getId(), "ESTUDANTE"); var inicio = tentativas.iniciar(estudante.getId(), simulado.id());
        tentativas.finalizar(estudante.getId(), inicio.tentativaId(), new RequisicaoFinalizacao(List.of(), 0)); jdbc.update("update tentativa_simulado set nota = 99 where id = ?", inicio.tentativaId());
        assertThat(jdbc.queryForObject("select nota from tentativa_simulado where id = ?", Short.class, inicio.tentativaId())).isZero();
    }

    @Test void TI09_derivaEstadoDaJanelaNaListaDoEstudante() {
        var disponivel = publicado(); var agendado = criarSimulado(Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200)); var disciplina = disciplina(); admin.substituirComposicao(agendado.id(), List.of(new ItemComposicao(disciplina, null, (short) 10))); questoes(disciplina, 10); publicar(agendado.id());
        var resposta = catalogo.listarDoEstudante(novoUsuario(PerfilUsuario.ESTUDANTE).getId());
        assertThat(resposta.disponiveis()).extracting("id").contains(disponivel.id()); assertThat(resposta.agendados()).extracting("id").contains(agendado.id());
    }

    @Test void TI10_encerramentoDaJanelaBloqueiaNovaEntradaMasNaoAFinalizacao() throws Exception {
        var simulado = publicado(); var estudante = novoUsuario(PerfilUsuario.ESTUDANTE); autenticar(estudante.getId(), "ESTUDANTE"); var inicio = tentativas.iniciar(estudante.getId(), simulado.id());
        jdbc.update("update simulado set janela_encerramento = now() - interval '1 second' where id = ?", simulado.id());
        var resultado = tentativas.finalizar(estudante.getId(), inicio.tentativaId(), new RequisicaoFinalizacao(List.of(), 0));
        assertThat(resultado.finalizadaEm()).isNotNull(); mvc.perform(post("/api/v1/simulados/" + simulado.id() + "/tentativas").with(user(novoUsuario(PerfilUsuario.ESTUDANTE).getId().toString()).roles("ESTUDANTE"))).andExpect(status().isConflict());
    }

    @Test void TI13_iniciarRetomaCom200ECriaCom201() throws Exception {
        var simulado = publicado(); var estudante = novoUsuario(PerfilUsuario.ESTUDANTE);
        var requisicao = user(estudante.getId().toString()).roles("ESTUDANTE");
        mvc.perform(post("/api/v1/simulados/" + simulado.id() + "/tentativas").with(requisicao)).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/simulados/" + simulado.id() + "/tentativas").with(requisicao)).andExpect(status().isOk());
    }

    private SimuladoId publicado() { var id = criarSimulado(Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600)); var disciplina = disciplina(); admin.substituirComposicao(id.id(), List.of(new ItemComposicao(disciplina, null, (short) 10))); questoes(disciplina, 10); publicar(id.id()); return id; }
    private void publicar(UUID id) { publicacao.publicar(id); }
    private SimuladoId criarSimulado(Instant abertura, Instant encerramento) { var criado = admin.criar(new RequisicaoSimulado("Simulado " + UUID.randomUUID(), null, null, (short) 10, 600, abertura, encerramento)); return new SimuladoId(criado.id()); }
    private UUID disciplina() { var id = UUID.randomUUID(); jdbc.update("insert into disciplina (id, nome) values (?, ?)", id, "Disciplina " + id); return id; }
    private void questoes(UUID disciplina, int quantidade) { for (var i = 0; i < quantidade; i++) jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, gabarito, comentario, status, autor_id) values (?, 'CE', ?, ?, 'CERTO', 'Comentário', 'PUBLICADA', ?)", UUID.randomUUID(), "Questão " + i, disciplina, adminId); }
    private Usuario novoUsuario(PerfilUsuario perfil) { return usuarios.save(new Usuario(perfil + " " + UUID.randomUUID(), UUID.randomUUID() + "@teste.local", "hash", perfil, StatusUsuario.ATIVO)); }
    private void autenticar(UUID id, String perfil) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_" + perfil)))); }
    private record SimuladoId(UUID id) {}
}
