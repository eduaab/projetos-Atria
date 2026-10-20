package br.com.atria.dto.response;

import br.com.atria.model.enums.OrigemAlerta;
import br.com.atria.model.enums.Prioridade;
import br.com.atria.model.enums.StatusAlerta;
import br.com.atria.model.enums.TipoAlerta;

import java.time.LocalDateTime;

/**
 * Alerta com o contexto necessário para a tela: plataforma, origem, titular afetado e,
 * quando o alerta é de acesso, o id do acesso que pode ser revogado em um clique.
 */
public record AlertaResponse(Long id, TipoAlerta tipo, String tipoDescricao, Prioridade prioridade,
                             StatusAlerta status, String statusDescricao, String titulo, String descricao,
                             String plataformaSlug, String plataformaNome,
                             OrigemAlerta tipoOrigem, String origemDescricao, Long origemId,
                             int clientesAfetados, Long titularId, String titularNome, String titularEmail,
                             Long acessoRevogavelId, Long diasRestantes,
                             LocalDateTime criadoEm, LocalDateTime resolvidoEm) {
}
