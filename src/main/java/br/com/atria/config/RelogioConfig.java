package br.com.atria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class RelogioConfig {

    @Bean
    public Clock clock(AtriaProperties propriedades) {
        String fuso = propriedades.fusoHorario() != null
                ? propriedades.fusoHorario()
                : "America/Sao_Paulo";
        return Clock.system(ZoneId.of(fuso));
    }
}
