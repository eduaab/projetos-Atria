package br.com.atria.service.regra;

import org.springframework.stereotype.Component;

/**
 * Índice de conformidade de 0 a 100 usado nos relatórios. Parte de 100 e desconta:
 * 15 pontos por solicitação atrasada, 10 por acesso ativo de ex-funcionário
 * e 5 por alerta de prioridade alta em aberto.
 */
@Component
public class CalculadoraIndiceConformidade {
    private static final int PESO_ATRASADA = 15;
    private static final int PESO_ACESSO = 10;
    private static final int PESO_ALERTA_ALTO = 5;

    public int calcular(long solicitacoesAtrasadas, long acessosIndevidos, long alertasAltaAbertos) {
        long desconto = solicitacoesAtrasadas * PESO_ATRASADA + acessosIndevidos * PESO_ACESSO + alertasAltaAbertos * PESO_ALERTA_ALTO;
        return (int) Math.max(0, 100 - desconto);
    }
}
