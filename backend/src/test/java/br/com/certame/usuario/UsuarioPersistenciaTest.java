package br.com.certame.usuario;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UsuarioPersistenciaTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UsuarioRepository repository;

    @Test
    @DisplayName("TI-14 rejeita e-mail duplicado entre usuários ativos")
    void deveRejeitarEmailDuplicado() {
        repository.saveAndFlush(new Usuario(
                "Pessoa Teste", "pessoa@example.com", "hash", PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO));

        assertThatThrownBy(() -> repository.saveAndFlush(new Usuario(
                "Outra Pessoa", "pessoa@example.com", "hash", PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO)))
                .isInstanceOf(Exception.class);

    }

    @Test
    @DisplayName("TI-14 permite reutilizar e-mail após exclusão lógica")
    void devePermitirReusoDoEmailExcluido() {
        var usuario = repository.saveAndFlush(new Usuario(
                "Pessoa Teste", "pessoa@example.com", "hash", PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO));
        usuario.setExcluidoEm(Instant.now());
        repository.saveAndFlush(usuario);

        var reusado = repository.saveAndFlush(new Usuario(
                "Outra Pessoa", "pessoa@example.com", "hash", PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO));
        assertThat(reusado.getId()).isNotNull();
    }

    @Test
    @DisplayName("editar altera nome, e-mail, perfil e status da entidade")
    void deveEditarDadosDoUsuario() {
        var usuario = new Usuario("Pessoa Teste", "editar@example.com", "hash",
                PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO);

        usuario.editar("Novo Nome", "novo@example.com", PerfilUsuario.PROFESSOR, StatusUsuario.INATIVO);

        assertThat(usuario.getNome()).isEqualTo("Novo Nome");
        assertThat(usuario.getEmail()).isEqualTo("novo@example.com");
        assertThat(usuario.getPerfil()).isEqualTo(PerfilUsuario.PROFESSOR);
        assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.INATIVO);
    }

    @Test
    @DisplayName("excluir preenche excluidoEm")
    void deveExcluirLogicamenteOUsuario() {
        var usuario = new Usuario("Pessoa Teste", "excluir@example.com", "hash",
                PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO);

        usuario.excluir();

        assertThat(usuario.getExcluidoEm()).isNotNull();
    }

    @Test
    @DisplayName("findByIdAndExcluidoEmIsNull ignora usuário excluído")
    void deveIgnorarUsuarioExcluidoAoBuscarPorId() {
        var usuario = repository.saveAndFlush(new Usuario(
                "Pessoa Teste", "busca@example.com", "hash", PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO));
        usuario.setExcluidoEm(Instant.now());
        repository.saveAndFlush(usuario);

        assertThat(repository.findByIdAndExcluidoEmIsNull(usuario.getId())).isEmpty();
    }

    @Test
    @DisplayName("countByPerfilAndStatusAndExcluidoEmIsNull conta apenas super-admins ativos não excluídos")
    void deveContarSuperAdminsAtivos() {
        repository.saveAndFlush(new Usuario(
                "Super Um", "super1@example.com", "hash", PerfilUsuario.SUPER_ADMIN, StatusUsuario.ATIVO));
        var superExcluido = repository.saveAndFlush(new Usuario(
                "Super Dois", "super2@example.com", "hash", PerfilUsuario.SUPER_ADMIN, StatusUsuario.ATIVO));
        superExcluido.setExcluidoEm(Instant.now());
        repository.saveAndFlush(superExcluido);
        repository.saveAndFlush(new Usuario(
                "Admin Comum", "admin@example.com", "hash", PerfilUsuario.ADMIN, StatusUsuario.ATIVO));

        var total = repository.countByPerfilAndStatusAndExcluidoEmIsNull(PerfilUsuario.SUPER_ADMIN, StatusUsuario.ATIVO);

        assertThat(total).isEqualTo(1L);
    }

    @Test
    @DisplayName("Specification de filtro combina perfil, status e exclusão lógica")
    void deveFiltrarPorPerfilEStatusExcluindoLogicos() {
        repository.saveAndFlush(new Usuario(
                "Professor Ativo", "professor.ativo@example.com", "hash", PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO));
        repository.saveAndFlush(new Usuario(
                "Professor Inativo", "professor.inativo@example.com", "hash", PerfilUsuario.PROFESSOR, StatusUsuario.INATIVO));
        var professorExcluido = repository.saveAndFlush(new Usuario(
                "Professor Excluido", "professor.excluido@example.com", "hash", PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO));
        professorExcluido.setExcluidoEm(Instant.now());
        repository.saveAndFlush(professorExcluido);

        var pagina = repository.findAll(
                UsuarioSpecifications.filtrar(PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO),
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(pagina.getContent())
                .extracting(Usuario::getEmail)
                .containsExactly("professor.ativo@example.com");
    }
}
