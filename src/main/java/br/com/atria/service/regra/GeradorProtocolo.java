package br.com.atria.service.regra;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

/** Gera protocolos no formato {@code ATR-XXXXXXXX} (8 caracteres hexadecimais). */
@Component
public class GeradorProtocolo {
    private final SecureRandom aleatorio = new SecureRandom();

    public String gerar() {
        byte[] bytes = new byte[4];
        aleatorio.nextBytes(bytes);
        return "ATR-" + HexFormat.of().withUpperCase().formatHex(bytes);
    }
}
