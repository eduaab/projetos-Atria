package br.com.atria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AtriaApplication {
    public static void main(String[] args) {
        SpringApplication.run(AtriaApplication.class, args);
    }
}
