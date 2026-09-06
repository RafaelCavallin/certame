package br.com.certame.desempenho;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/desempenho")
public class DesempenhoController {
    private final ResumoDesempenhoService resumo;
    private final EvolucaoSemanalService evolucao;
    private final DisciplinaDesempenhoService disciplinas;
    private final PontosFracosService pontosFracos;

    public DesempenhoController(ResumoDesempenhoService resumo, EvolucaoSemanalService evolucao,
                                DisciplinaDesempenhoService disciplinas, PontosFracosService pontosFracos) {
        this.resumo = resumo;
        this.evolucao = evolucao;
        this.disciplinas = disciplinas;
        this.pontosFracos = pontosFracos;
    }

    @GetMapping("/resumo")
    public ResponseEntity<?> resumo(Authentication auth) { return ResponseEntity.ok(resumo.consultar(usuario(auth))); }

    @GetMapping("/evolucao")
    public ResponseEntity<?> evolucao(Authentication auth) { return ResponseEntity.ok(evolucao.consultar(usuario(auth))); }

    @GetMapping("/disciplinas")
    public ResponseEntity<?> disciplinas(Authentication auth) { return ResponseEntity.ok(disciplinas.consultar(usuario(auth))); }

    @GetMapping("/pontos-fracos")
    public ResponseEntity<?> pontosFracos(Authentication auth) { return ResponseEntity.ok(pontosFracos.consultar(usuario(auth))); }

    private UUID usuario(Authentication auth) { return UUID.fromString(auth.getName()); }
}
