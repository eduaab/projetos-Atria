package br.com.atria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class RelogioConfig {

    @Bean
    public Clock clock(AtriaProperties propriedades) {
        return Clock.system(ZoneId.of(propriedades.fusoHorario()));
    }
}
