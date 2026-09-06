package br.com.certame.usuario;

import br.com.certame.comum.Paginacao;
import br.com.certame.comum.PaginaResposta;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.usuario.dto.FiltroUsuarios;
import br.com.certame.usuario.dto.RequisicaoCriarUsuario;
import br.com.certame.usuario.dto.RequisicaoEditarUsuario;
import br.com.certame.usuario.dto.UsuarioGestaoResumo;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/super/usuarios")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UsuarioAdminController {
    private final GestaoUsuariosService gestao;

    public UsuarioAdminController(GestaoUsuariosService gestao) {
        this.gestao = gestao;
    }

    @GetMapping
    public ResponseEntity<PaginaResposta<UsuarioGestaoResumo>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "criadoEm,desc") String sort,
            @RequestParam(required = false) String perfil,
            @RequestParam(required = false) String status) {
        validarPaginacao(page, size);
        var filtro = new FiltroUsuarios(parsePerfil(perfil), parseStatus(status));
        var paginacao = new Paginacao(page, size, sort);
        return ResponseEntity.ok(gestao.listar(filtro, paginacao));
    }

    @PostMapping
    public ResponseEntity<UsuarioGestaoResumo> criar(@Valid @RequestBody RequisicaoCriarUsuario requisicao) {
        var resumo = gestao.criar(requisicao);
        return ResponseEntity.created(URI.create("/api/v1/super/usuarios/" + resumo.id())).body(resumo);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UsuarioGestaoResumo> editar(@PathVariable UUID id,
                                                      @Valid @RequestBody RequisicaoEditarUsuario requisicao,
                                                      Authentication auth) {
        var resumo = gestao.editar(id, requisicao, usuario(auth));
        return ResponseEntity.ok(resumo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id, Authentication auth) {
        gestao.excluir(id, usuario(auth));
        return ResponseEntity.noContent().build();
    }

    private PerfilUsuario parsePerfil(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return PerfilUsuario.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new DadosInvalidosException(Map.of("perfil", "Informe um perfil válido."));
        }
    }

    private StatusUsuario parseStatus(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return StatusUsuario.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new DadosInvalidosException(Map.of("status", "Informe um status válido."));
        }
    }

    private void validarPaginacao(int page, int size) {
        if (page < 0) throw new DadosInvalidosException(Map.of("page", "A página deve ser maior ou igual a zero."));
        if (size < 1 || size > 100) throw new DadosInvalidosException(Map.of("size", "O tamanho deve estar entre 1 e 100."));
    }

    private UUID usuario(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
