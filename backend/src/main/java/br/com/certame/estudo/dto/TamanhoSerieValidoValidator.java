package br.com.certame.estudo.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TamanhoSerieValidoValidator implements ConstraintValidator<TamanhoSerieValido, Integer> {
    private static final int TAMANHO_PEQUENO = 10;
    private static final int TAMANHO_MEDIO = 30;
    private static final int TAMANHO_GRANDE = 50;

    @Override
    public boolean isValid(Integer valor, ConstraintValidatorContext contexto) {
        return valor == null || valor == TAMANHO_PEQUENO || valor == TAMANHO_MEDIO || valor == TAMANHO_GRANDE;
    }
}
