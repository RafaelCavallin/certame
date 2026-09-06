package br.com.certame.comum.excecao;

public class CadernoAutomaticoImutavelException extends ConflitoException {
    public CadernoAutomaticoImutavelException() {
        super("CADERNO_AUTOMATICO_IMUTAVEL", "Cadernos automáticos não podem ser alterados.");
    }
}
