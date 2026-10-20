package br.com.atria;

import br.com.atria.config.AtriaProperties;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Relógio fixo dos testes: 09/10/2026 às 10h em Fortaleza. */
public final class RelogioFixo {
    public static final ZoneId FUSO = ZoneId.of("America/Fortaleza");
    public static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 9, 10, 0);
    public static final Clock CLOCK = Clock.fixed(AGORA.atZone(FUSO).toInstant(), FUSO);
    public static final AtriaProperties PROPRIEDADES = new AtriaProperties(1L, "America/Fortaleza", 15);

    private RelogioFixo() {
    }
}
