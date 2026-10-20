package br.com.atria.controller;

import br.com.atria.RelogioFixo;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@TestConfiguration
public class ConfiguracaoTesteWeb {

    @Bean
    Clock clock() {
        return RelogioFixo.CLOCK;
    }
}
