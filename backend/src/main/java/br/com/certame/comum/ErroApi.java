package br.com.certame.comum;

import java.util.Map;

public record ErroApi(String code, String message, Map<String, String> fieldErrors) {
}
