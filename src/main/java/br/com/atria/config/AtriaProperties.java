package br.com.atria.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades da Atria em {@code application.properties} (prefixo {@code atria}).
 *
 * @param lojistaAtualId lojista fixo atendido pela aplicação enquanto não há login
 * @param fusoHorario    fuso usado em todas as datas
 * @param prazoLgpdDias  prazo de atendimento de uma solicitação do titular
 */
@ConfigurationProperties(prefix = "atria")
public record AtriaProperties(Long lojistaAtualId, String fusoHorario, int prazoLgpdDias) {
}
