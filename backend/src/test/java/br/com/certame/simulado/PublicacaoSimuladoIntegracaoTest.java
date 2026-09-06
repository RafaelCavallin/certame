package br.com.certame.simulado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certame.comum.excecao.ComposicaoInvalidaException;
import br.com.certame.simulado.dto.ItemComposicao;
import br.com.certame.simulado.dto.RequisicaoSimulado;
import br.com.certame.usuario.PerfilUsuario;
import br.com.certame.usuario.StatusUsuario;
import br.com.certame.usuario.Usuario;
import br.com.certame.usuario.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;

@SpringBootTest(properties = "certame.seguranca.jwt-segredo=chave-de-teste-local-com-mais-de-trinta-e-dois")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class PublicacaoSimuladoIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");
    @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SimuladoAdminService admin;
    @Autowired private PublicacaoSimuladoService publicacao;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private MockMvc mvc;

    @BeforeEach void autenticarAdmin() {
        var usuario = usuarios.save(new Usuario("Admin " + UUID.randomUUID(), UUID.randomUUID() + "@teste.local", "hash", PerfilUsuario.ADMIN, StatusUsuario.ATIVO));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(usuario.getId().toString(), null, java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test void TI01_publicaSimuladoECongelaSerie() {
        var disciplina = disciplina(); var autor = ator(); var questoes = questoesPublicadas(disciplina, autor, 10);
        var simulado = criar(10); admin.substituirComposicao(simulado.id(), java.util.List.of(new ItemComposicao(disciplina, null, (short) 10)));

        publicacao.publicar(simulado.id());

        assertThat(jdbc.queryForObject("select count(*) from simulado_questao where simulado_id = ?", Integer.class, simulado.id())).isEqualTo(10);
        assertThat(jdbc.queryForList("select ordem from simulado_questao where simulado_id = ? order by ordem", Short.class, simulado.id())).containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 10).mapToObj(i -> (short) i).toList());
        assertThat(jdbc.queryForObject("select count(*) from simulado_questao sq join questao q on q.id = sq.questao_id where sq.simulado_id = ? and q.status = 'PUBLICADA'", Integer.class, simulado.id())).isEqualTo(questoes.size());
    }

    @Test void TI02_naoEscreveNadaQuandoPublicacaoERecusada() {
        var simulado = criar(10);

        assertThatThrownBy(() -> publicacao.publicar(simulado.id())).isInstanceOf(RuntimeException.class);

        assertThat(jdbc.queryForObject("select status from simulado where id = ?", String.class, simulado.id())).isEqualTo("RASCUNHO");
        assertThat(jdbc.queryForObject("select count(*) from simulado_questao where simulado_id = ?", Integer.class, simulado.id())).isZero();
    }

    @Test void TI11_impedeAlteracaoDeComposicaoComTentativaFinalizada() {
        var disciplina = disciplina(); var simulado = criar(10); admin.substituirComposicao(simulado.id(), java.util.List.of(new ItemComposicao(disciplina, null, (short) 10)));
        var usuario = ator(); var sessao = UUID.randomUUID(); var tentativa = UUID.randomUUID();
        jdbc.update("insert into sessao_estudo (id, usuario_id, modo, origem, origem_ref, tamanho) values (?, ?, 'PROVA', 'SIMULADO', ?, 10)", sessao, usuario, simulado.id().toString());
        jdbc.update("insert into tentativa_simulado (id, simulado_id, usuario_id, sessao_id, status, nota, finalizada_em) values (?, ?, ?, ?, 'FINALIZADA', 80, now())", tentativa, simulado.id(), usuario, sessao);

        assertThatThrownBy(() -> admin.substituirComposicao(simulado.id(), java.util.List.of(new ItemComposicao(disciplina, null, (short) 9))))
                .hasMessageContaining("tentativa finalizada");
        assertThat(jdbc.queryForObject("select count(*) from simulado_composicao where simulado_id = ?", Integer.class, simulado.id())).isEqualTo(1);
    }

    @Test void TI12_registraAuditoriaDePublicacaoEDeJanela() {
        var disciplina = disciplina(); var autor = ator(); questoesPublicadas(disciplina, autor, 10);
        var simulado = criar(10); admin.substituirComposicao(simulado.id(), java.util.List.of(new ItemComposicao(disciplina, null, (short) 10)));
        publicacao.publicar(simulado.id());

        var auditorias = jdbc.queryForList("select acao, payload::text from auditoria where entidade_id = ? order by criado_em", simulado.id());
        assertThat(auditorias).extracting(linha -> linha.get("acao")).contains("SIMULADO_CRIADO", "SIMULADO_COMPOSICAO_ALTERADA", "SIMULADO_PUBLICADO");
        assertThat(auditorias.stream().map(linha -> linha.get("payload").toString()).filter(payload -> payload.contains("janelaAbertura")).count()).isGreaterThanOrEqualTo(2);
    }

    @Test void TI08_recusaOperacoesAdministrativasParaEstudante() throws Exception {
        var id = UUID.randomUUID();
        var estudante = user("estudante").roles("ESTUDANTE");
        var corpo = "{\"nome\":\"Simulado estudante\",\"numeroQuestoes\":10,\"duracaoSegundos\":600}";

        mvc.perform(get("/api/v1/admin/simulados").with(estudante)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/simulados").with(estudante).contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/simulados/" + id).with(estudante)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/admin/simulados/" + id).with(estudante).contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/admin/simulados/" + id).with(estudante)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/admin/simulados/" + id + "/composicao").with(estudante).contentType(MediaType.APPLICATION_JSON).content("{\"itens\":[]}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/simulados/" + id + "/publicar").with(estudante)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/simulados/" + id + "/despublicar").with(estudante)).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select count(*) from simulado where id = ?", Integer.class, id)).isZero();
    }

    private br.com.certame.simulado.dto.SimuladoDetalhe criar(int quantidade) { return admin.criar(new RequisicaoSimulado("Simulado " + UUID.randomUUID(), null, null, (short) quantidade, 600, Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600))); }
    private UUID disciplina() { var id = UUID.randomUUID(); jdbc.update("insert into disciplina (id, nome) values (?, ?)", id, "Disciplina " + id); return id; }
    private UUID ator() { var usuario = usuarios.save(new Usuario("Autor " + UUID.randomUUID(), UUID.randomUUID() + "@autor.local", "hash", PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO)); return usuario.getId(); }
    private java.util.List<UUID> questoesPublicadas(UUID disciplina, UUID autor, int total) { var ids = new java.util.ArrayList<UUID>(); for (var i = 0; i < total; i++) { var id = UUID.randomUUID(); ids.add(id); jdbc.update("insert into questao (id, tipo, enunciado, disciplina_id, gabarito, comentario, status, autor_id) values (?, 'CE', ?, ?, 'A', 'Comentário', 'PUBLICADA', ?)", id, "Questão " + id, disciplina, autor); } return ids; }
}
