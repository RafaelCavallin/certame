package br.com.certame.estudo.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TamanhoSerieValidoValidator.class)
public @interface TamanhoSerieValido {
    String message() default "Escolha 10, 30 ou 50 questões.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
