package br.com.atria.service;

import br.com.atria.dto.response.ContadoresResponse;
import br.com.atria.dto.response.DashboardResponse;
import br.com.atria.model.Alerta;
import br.com.atria.model.enums.Prioridade;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {
    private static final int ALERTAS_NA_HOME = 5;

    private final LojistaAtualService lojistaAtual;
    private final TitularService titularService;
    private final ConexaoService conexaoService;
    private final SolicitacaoService solicitacaoService;
    private final AlertaService alertaService;
    private final RiscoService riscoService;

    public DashboardService(LojistaAtualService lojistaAtual, TitularService titularService, ConexaoService conexaoService,
                            SolicitacaoService solicitacaoService, AlertaService alertaService, RiscoService riscoService) {
        this.lojistaAtual = lojistaAtual;
        this.titularService = titularService;
        this.conexaoService = conexaoService;
        this.solicitacaoService = solicitacaoService;
        this.alertaService = alertaService;
        this.riscoService = riscoService;
    }

    public DashboardResponse obter() {
        List<Alerta> abertos = alertaService.abertos();
        ContadoresResponse contadores = new ContadoresResponse(
                titularService.ativos().size(),
                conexaoService.idsPlataformasConectadas().size(),
                solicitacaoService.abertas().size(),
                abertos.size(),
                abertos.stream().filter(a -> a.getPrioridade() == Prioridade.ALTA).count());
        return new DashboardResponse(lojistaAtual.obter().getNomeLoja(), contadores, alertaService.fraseResumo(),
                alertaService.prioritarios(ALERTAS_NA_HOME), solicitacaoService.listarAbertas(),
                riscoService.statusAnpd(), riscoService.statusMarketplace());
    }
}
