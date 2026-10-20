package br.com.atria.service.regra;

import br.com.atria.model.enums.Prioridade;

/** Define a prioridade de um alerta. Hoje é feito por regras; no futuro, por um modelo de ML. */
public interface ClassificadorPrioridade {
    Prioridade classificar(OcorrenciaRisco ocorrencia);
}
