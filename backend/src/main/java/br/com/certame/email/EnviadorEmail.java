package br.com.certame.email;

import br.com.certame.usuario.Usuario;

public interface EnviadorEmail {
    void enviarRedefinicaoSenha(Usuario usuario, String linkAbsoluto);
    void enviarConvite(Usuario usuario, String linkAbsoluto);
}
