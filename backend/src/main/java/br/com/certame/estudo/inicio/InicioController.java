package br.com.certame.estudo.inicio;

import br.com.certame.estudo.dto.ResumoInicio;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/estudo/inicio")
public class InicioController {
    private final ResumoDiaService resumo;
    public InicioController(ResumoDiaService resumo) { this.resumo = resumo; }
    @GetMapping
    public ResponseEntity<ResumoInicio> consultar(Authentication auth) { return ResponseEntity.ok(resumo.doDia(UUID.fromString(auth.getName()), null)); }
}
