package br.com.certame.usuario;

import org.springframework.data.jpa.domain.Specification;

public final class UsuarioSpecifications {

    private UsuarioSpecifications() {
    }

    public static Specification<Usuario> filtrar(PerfilUsuario perfil, StatusUsuario status) {
        return (raiz, consulta, construtor) -> {
            var predicados = construtor.conjunction();
            predicados = construtor.and(predicados, construtor.isNull(raiz.get("excluidoEm")));
            if (perfil != null) {
                predicados = construtor.and(predicados, construtor.equal(raiz.get("perfil"), perfil));
            }
            if (status != null) {
                predicados = construtor.and(predicados, construtor.equal(raiz.get("status"), status));
            }
            return predicados;
        };
    }
}
