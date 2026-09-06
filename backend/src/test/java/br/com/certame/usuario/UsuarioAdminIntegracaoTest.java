package br.com.certame.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certame.comum.auditoria.AuditoriaRepository;
import br.com.certame.email.EnviadorEmail;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {"certame.seguranca.jwt-segredo=chave-de-teste-local-com-mais-de-trinta-e-dois"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class UsuarioAdminIntegracaoTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired AuditoriaRepository auditorias;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean EnviadorEmail enviador;

    @BeforeEach
    void limparMensagens() { reset(enviador); }

    @Test
    void TI01_listaUsuariosComFiltroDePerfilEStatus() throws Exception {
        var superAdmin = superAdmin();
        var agora = Instant.now();
        var maisAntigo = professor("ATIVO", agora.minus(3, ChronoUnit.MINUTES));
        var maisRecente = professor("ATIVO", agora.minus(1, ChronoUnit.MINUTES));
        var pendente = professor("CONVITE_PENDENTE", agora.minus(2, ChronoUnit.MINUTES));
        var estudante = estudante();

        var resultado = mvc.perform(get("/api/v1/super/usuarios").param("perfil", "PROFESSOR")
                        .param("status", "ATIVO").param("sort", "criadoEm,desc").param("size", "100")
                        .with(superAdmin(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(100))
                .andReturn();
        var ids = idsOrdenados(resultado);

        assertThat(ids).contains(maisRecente, maisAntigo).doesNotContain(pendente, estudante);
        assertThat(ids.indexOf(maisRecente)).isLessThan(ids.indexOf(maisAntigo));
    }

    private java.util.List<UUID> idsOrdenados(org.springframework.test.web.servlet.MvcResult resultado) throws Exception {
        var content = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("content");
        var ids = new java.util.ArrayList<UUID>();
        content.forEach(item -> ids.add(UUID.fromString(item.get("id").asText())));
        return ids;
    }

    @Test
    void TI02_criaUsuarioComConviteEFluxoCompletoDeAtivacao() throws Exception {
        var superAdmin = superAdmin();
        var email = "convidado-" + UUID.randomUUID() + "@example.com";
        var corpo = """
                {"nome":"Marina Duarte","email":"%s","perfil":"PROFESSOR","status":"ATIVO","enviarConvite":true}
                """.formatted(email);

        var resultado = mvc.perform(post("/api/v1/super/usuarios").with(superAdmin(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONVITE_PENDENTE"))
                .andReturn();
        var criadoId = UUID.fromString(objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText());

        var pendentes = jdbc.queryForObject(
                "select count(*) from token_senha where usuario_id = ? and finalidade = 'CONVITE' and usado_em is null",
                Integer.class, criadoId);
        assertThat(pendentes).isEqualTo(1);

        var link = ArgumentCaptor.forClass(String.class);
        verify(enviador).enviarConvite(any(), link.capture());
        var token = link.getValue().substring(link.getValue().lastIndexOf('/') + 1);

        mvc.perform(post("/api/v1/auth/senha/redefinir").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"senha\":\"senhaDefinida123\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"senhaDefinida123\"}"))
                .andExpect(status().isOk());
        var ativado = usuarios.findByIdAndExcluidoEmIsNull(criadoId).orElseThrow();
        assertThat(ativado.getStatus()).isEqualTo(StatusUsuario.ATIVO);
    }

    @Test
    void TI04_editaUsuarioRevogandoRefreshTokens() throws Exception {
        var superAdmin = superAdmin();
        var senhaOriginal = "senhaOriginal123";
        var email = "editado-" + UUID.randomUUID() + "@example.com";
        var editadoId = usuarioAtivo(email, senhaOriginal, PerfilUsuario.PROFESSOR);
        var refresh = login(email, senhaOriginal);

        var corpo = """
                {"nome":"Marina Editada","email":"%s","perfil":"ADMIN","status":"ATIVO"}
                """.formatted(email);
        mvc.perform(patch("/api/v1/super/usuarios/" + editadoId).with(superAdmin(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Marina Editada"))
                .andExpect(jsonPath("$.perfil").value("ADMIN"));

        var editado = usuarios.findByIdAndExcluidoEmIsNull(editadoId).orElseThrow();
        assertThat(editado.getNome()).isEqualTo("Marina Editada");
        assertThat(editado.getPerfil()).isEqualTo(PerfilUsuario.ADMIN);

        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("certame_rt", refresh)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSAO_EXPIRADA"));
    }

    @Test
    void regressao_usuarioEditadoParaInativoRecebeCredencialInvalidaGenericaNoLogin() throws Exception {
        var superAdmin = superAdmin();
        var senha = "senhaOriginal123";
        var email = "inativado-" + UUID.randomUUID() + "@example.com";
        var usuarioId = usuarioAtivo(email, senha, PerfilUsuario.PROFESSOR);

        var corpo = """
                {"nome":"Marina Inativada","email":"%s","perfil":"PROFESSOR","status":"INATIVO"}
                """.formatted(email);
        mvc.perform(patch("/api/v1/super/usuarios/" + usuarioId).with(superAdmin(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENCIAL_INVALIDA"))
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
    }

    @Test
    void TI05_excluindoUsuarioOracleEncerraAcesso() throws Exception {
        var superAdmin = superAdmin();
        var senha = "senhaOriginal123";
        var email = "excluido-" + UUID.randomUUID() + "@example.com";
        var excluidoId = usuarioAtivo(email, senha, PerfilUsuario.PROFESSOR);

        mvc.perform(delete("/api/v1/super/usuarios/" + excluidoId).with(superAdmin(superAdmin)))
                .andExpect(status().isNoContent());

        var corpoEdicao = """
                {"nome":"Qualquer","email":"%s","perfil":"PROFESSOR","status":"ATIVO"}
                """.formatted(email);
        mvc.perform(patch("/api/v1/super/usuarios/" + excluidoId).with(superAdmin(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoEdicao))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
        mvc.perform(delete("/api/v1/super/usuarios/" + excluidoId).with(superAdmin(superAdmin)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/super/usuarios/" + excluidoId + "/redefinir-senha").with(superAdmin(superAdmin)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/super/usuarios/" + excluidoId + "/reenviar-convite").with(superAdmin(superAdmin)))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/super/usuarios").param("size", "100").with(superAdmin(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + excluidoId + "')]").isEmpty());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENCIAL_INVALIDA"));
    }

    @Test
    void TI06_auditoriaPersistidaComAtorEEntidade() throws Exception {
        var superAdmin = superAdmin();

        var emailCriado = "auditoria-criar-" + UUID.randomUUID() + "@example.com";
        var corpoCriacao = """
                {"nome":"Auditoria Criar","email":"%s","perfil":"PROFESSOR","status":"ATIVO","enviarConvite":true}
                """.formatted(emailCriado);
        var criacao = mvc.perform(post("/api/v1/super/usuarios").with(superAdmin(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoCriacao))
                .andExpect(status().isCreated()).andReturn();
        var criadoId = UUID.fromString(objectMapper.readTree(criacao.getResponse().getContentAsString()).get("id").asText());
        var auditoriaCriar = ultimaAuditoria(criadoId);
        assertThat(auditoriaCriar.getAtor().getId()).isEqualTo(superAdmin);
        assertThat(auditoriaCriar.getAcao()).isEqualTo("CRIAR");
        assertThat(auditoriaCriar.getEntidade()).isEqualTo("usuario");
        var payloadCriar = objectMapper.readTree(auditoriaCriar.getPayload());
        assertThat(payloadCriar.get("perfil").asText()).isEqualTo("PROFESSOR");
        assertThat(payloadCriar.get("enviarConvite").asBoolean()).isTrue();

        var editadoId = usuarioAtivo("auditoria-editar-" + UUID.randomUUID() + "@example.com", "senhaOriginal123", PerfilUsuario.PROFESSOR);
        var emailEditado = usuarios.findByIdAndExcluidoEmIsNull(editadoId).orElseThrow().getEmail();
        var corpoEdicao = """
                {"nome":"Editado Auditoria","email":"%s","perfil":"ADMIN","status":"ATIVO"}
                """.formatted(emailEditado);
        mvc.perform(patch("/api/v1/super/usuarios/" + editadoId).with(superAdmin(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoEdicao))
                .andExpect(status().isOk());
        var auditoriaEditar = ultimaAuditoria(editadoId);
        assertThat(auditoriaEditar.getAtor().getId()).isEqualTo(superAdmin);
        assertThat(auditoriaEditar.getAcao()).isEqualTo("EDITAR");
        var payloadEditar = objectMapper.readTree(auditoriaEditar.getPayload());
        assertThat(payloadEditar.get("antes").get("perfil").asText()).isEqualTo("PROFESSOR");
        assertThat(payloadEditar.get("depois").get("perfil").asText()).isEqualTo("ADMIN");

        mvc.perform(post("/api/v1/super/usuarios/" + editadoId + "/redefinir-senha").with(superAdmin(superAdmin)))
                .andExpect(status().isNoContent());
        var auditoriaRedefinir = ultimaAuditoria(editadoId);
        assertThat(auditoriaRedefinir.getAcao()).isEqualTo("REDEFINIR_SENHA");
        assertThat(auditoriaRedefinir.getPayload()).isNull();

        var convidadoId = usuarioConvitePendente("auditoria-convite-" + UUID.randomUUID() + "@example.com");
        mvc.perform(post("/api/v1/super/usuarios/" + convidadoId + "/reenviar-convite").with(superAdmin(superAdmin)))
                .andExpect(status().isNoContent());
        var auditoriaReenvio = ultimaAuditoria(convidadoId);
        assertThat(auditoriaReenvio.getAcao()).isEqualTo("REENVIAR_CONVITE");
        assertThat(auditoriaReenvio.getPayload()).isNull();

        var excluidoId = usuarioAtivo("auditoria-excluir-" + UUID.randomUUID() + "@example.com", "senhaOriginal123", PerfilUsuario.PROFESSOR);
        mvc.perform(delete("/api/v1/super/usuarios/" + excluidoId).with(superAdmin(superAdmin)))
                .andExpect(status().isNoContent());
        var auditoriaExcluir = ultimaAuditoria(excluidoId);
        assertThat(auditoriaExcluir.getAtor().getId()).isEqualTo(superAdmin);
        assertThat(auditoriaExcluir.getAcao()).isEqualTo("EXCLUIR");
        assertThat(auditoriaExcluir.getEntidade()).isEqualTo("usuario");
    }

    private br.com.certame.comum.auditoria.Auditoria ultimaAuditoria(UUID entidadeId) {
        return auditorias.findByEntidadeAndEntidadeIdOrderByCriadoEmDesc("usuario", entidadeId).get(0);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor superAdmin(UUID id) {
        return user(id.toString()).roles("SUPER_ADMIN");
    }

    private UUID superAdmin() {
        var id = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Super', ?, ?, 'SUPER_ADMIN', 'ATIVO')",
                id, id + "@super-admin.local", passwordEncoder.encode("senhaSegura123"));
        return id;
    }

    private UUID professor(String status, Instant criadoEm) {
        var id = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status, criado_em) values (?, 'Professor', ?, ?, 'PROFESSOR', ?, ?)",
                id, id + "@professor.local", "CONVITE_PENDENTE".equals(status) ? null : passwordEncoder.encode("senhaSegura123"),
                status, java.sql.Timestamp.from(criadoEm));
        return id;
    }

    private UUID estudante() {
        var id = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Estudante', ?, ?, 'ESTUDANTE', 'ATIVO')",
                id, id + "@estudante.local", passwordEncoder.encode("senhaSegura123"));
        return id;
    }

    private UUID usuarioAtivo(String email, String senha, PerfilUsuario perfil) {
        var id = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, senha_hash, perfil, status) values (?, 'Pessoa Teste', ?, ?, ?, 'ATIVO')",
                id, email, passwordEncoder.encode(senha), perfil.name());
        return id;
    }

    private UUID usuarioConvitePendente(String email) {
        var id = UUID.randomUUID();
        jdbc.update("insert into usuario (id, nome, email, perfil, status) values (?, 'Pessoa Convidada', ?, 'PROFESSOR', 'CONVITE_PENDENTE')",
                id, email);
        return id;
    }

    private String login(String email, String senha) throws Exception {
        var resultado = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return resultado.getResponse().getCookie("certame_rt").getValue();
    }
}
