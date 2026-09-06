package br.com.certame.catalogo.edital;

import br.com.certame.catalogo.edital.dto.EditaisAgrupados;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/editais")
public class EditalController {
    private final ConsultaEditaisService consulta;

    public EditalController(ConsultaEditaisService consulta) {
        this.consulta = consulta;
    }

    @GetMapping
    public ResponseEntity<EditaisAgrupados> listar() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(consulta.agrupados());
    }
}
