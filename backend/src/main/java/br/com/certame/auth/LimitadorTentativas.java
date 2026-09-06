package br.com.certame.auth;

public interface LimitadorTentativas {
    void registrarFalha(String chave);
    void verificar(String chave);
    default void registrarSucesso(String chave) { }
}
