package br.com.atria.service;

import br.com.atria.dto.response.ClientesPlataformaResponse;
import br.com.atria.dto.response.ContagemResponse;
import br.com.atria.dto.response.DadoIdentificadoResponse;
import br.com.atria.dto.response.IndicadorProblemaResponse;
import br.com.atria.dto.response.MapeamentoResumoResponse;
import br.com.atria.mapper.DadoIdentificadoMapper;
import br.com.atria.model.DadoIdentificado;
import br.com.atria.model.Plataforma;
import br.com.atria.model.Titular;
import br.com.atria.model.enums.Prioridade;
import br.com.atria.model.enums.TipoDado;
import br.com.atria.repository.DadoIdentificadoRepository;
import br.com.atria.service.regra.NormalizadorTexto;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Mapeamento (identificação simulada): onde estão os dados dos clientes. Só contam os dados
 * de plataformas conectadas; ao desconectar, eles saem do resumo sem serem apagados.
 */
@Service
public class MapeamentoService {
    static final int MINIMO_PLATAFORMAS_EXPOSICAO = 4;

    private final DadoIdentificadoRepository dadoRepository;
    private final TitularService titularService;
    private final ConexaoService conexaoService;
    private final PlataformaService plataformaService;
    private final AcessoService acessoService;
    private final DadoIdentificadoMapper mapper;

    public MapeamentoService(DadoIdentificadoRepository dadoRepository, TitularService titularService,
                             ConexaoService conexaoService, PlataformaService plataformaService,
                             AcessoService acessoService, DadoIdentificadoMapper mapper) {
        this.dadoRepository = dadoRepository;
        this.titularService = titularService;
        this.conexaoService = conexaoService;
        this.plataformaService = plataformaService;
        this.acessoService = acessoService;
        this.mapper = mapper;
    }

    public MapeamentoResumoResponse resumo() {
        Set<Long> conectadas = conexaoService.idsPlataformasConectadas();
        List<Titular> titulares = titularService.ativos();
        List<DadoIdentificado> dados = dadosContabilizados(conectadas, titulares);
        Map<Long, Plataforma> plataformas = plataformaService.mapaPorId();

        List<ClientesPlataformaResponse> clientesPorPlataforma = plataformas.values().stream()
                .sorted(Comparator.comparing(Plataforma::getId))
                .map(p -> new ClientesPlataformaResponse(p.getSlug(), p.getNome(), conectadas.contains(p.getId()),
                        dados.stream().filter(d -> d.getPlataformaId().equals(p.getId())).map(DadoIdentificado::getTitularId).distinct().count()))
                .toList();
        Map<TipoDado, Long> porTipo = dados.stream().collect(Collectors.groupingBy(DadoIdentificado::getTipoDado, Collectors.counting()));
        List<ContagemResponse> dadosPorTipo = Arrays.stream(TipoDado.values())
                .map(tipo -> new ContagemResponse(tipo.name(), tipo.getDescricao(), porTipo.getOrDefault(tipo, 0L)))
                .toList();

        List<IndicadorProblemaResponse> indicadores = List.of(
                cpfEmCanalDeConversa(dados, plataformas),
                dadosEmMuitasPlataformas(dados),
                cadastrosDuplicados(titulares),
                acessosDeExFuncionarios());
        long mapeados = dados.stream().map(DadoIdentificado::getTitularId).distinct().count();
        return new MapeamentoResumoResponse(titulares.size(), mapeados, dados.size(), clientesPorPlataforma, dadosPorTipo, indicadores);
    }

    public List<DadoIdentificadoResponse> dados(String plataformaSlug, TipoDado tipo) {
        Long plataformaId = plataformaSlug == null || plataformaSlug.isBlank() ? null : plataformaService.buscarPorSlug(plataformaSlug).getId();
        Map<Long, Titular> titulares = titularService.ativos().stream().collect(Collectors.toMap(Titular::getId, Function.identity()));
        Map<Long, Plataforma> plataformas = plataformaService.mapaPorId();
        return dadosContabilizados(conexaoService.idsPlataformasConectadas(), titulares.values()).stream()
                .filter(d -> plataformaId == null || d.getPlataformaId().equals(plataformaId))
                .filter(d -> tipo == null || d.getTipoDado() == tipo)
                .sorted(Comparator.comparing(DadoIdentificado::getEncontradoEm).reversed())
                .map(d -> mapper.paraResponse(d, titulares.get(d.getTitularId()), plataformas.get(d.getPlataformaId())))
                .toList();
    }

    private List<DadoIdentificado> dadosContabilizados(Set<Long> conectadas, Collection<Titular> titulares) {
        Set<Long> titularIds = titulares.stream().map(Titular::getId).collect(Collectors.toSet());
        return dadoRepository.findAll().stream()
                .filter(d -> conectadas.contains(d.getPlataformaId()) && titularIds.contains(d.getTitularId()))
                .toList();
    }

    private IndicadorProblemaResponse cpfEmCanalDeConversa(List<DadoIdentificado> dados, Map<Long, Plataforma> plataformas) {
        List<Long> titularIds = dados.stream()
                .filter(d -> d.getTipoDado() == TipoDado.CPF && plataformas.get(d.getPlataformaId()).isCanalDeConversa())
                .map(DadoIdentificado::getTitularId).distinct().sorted().toList();
        return new IndicadorProblemaResponse("CPF_EM_CANAL_CONVERSA", "CPF em canal de conversa",
                "CPFs trocados por WhatsApp ou Instagram ficam expostos no histórico das conversas. Oriente a equipe a não pedir CPF por mensagem.",
                titularIds.size(), Prioridade.ALTA, titularIds);
    }

    private IndicadorProblemaResponse dadosEmMuitasPlataformas(List<DadoIdentificado> dados) {
        List<Long> titularIds = dados.stream()
                .collect(Collectors.groupingBy(DadoIdentificado::getTitularId,
                        Collectors.mapping(DadoIdentificado::getPlataformaId, Collectors.toSet())))
                .entrySet().stream()
                .filter(entrada -> entrada.getValue().size() >= MINIMO_PLATAFORMAS_EXPOSICAO)
                .map(Map.Entry::getKey).sorted().toList();
        return new IndicadorProblemaResponse("DADOS_EM_MUITAS_PLATAFORMAS", "Cliente em 4 ou mais plataformas",
                "Quanto mais plataformas guardam os dados de um cliente, maior a superfície de exposição.",
                titularIds.size(), Prioridade.MEDIA, titularIds);
    }

    /** Cadastros diferentes com o mesmo e-mail ou telefone. A quantidade é o número de casos. */
    private IndicadorProblemaResponse cadastrosDuplicados(List<Titular> titulares) {
        List<List<Long>> casos = Stream.concat(
                        agruparDuplicados(titulares, t -> NormalizadorTexto.normalizar(t.getEmail())),
                        agruparDuplicados(titulares, t -> NormalizadorTexto.somenteDigitos(t.getTelefone())))
                .distinct().toList();
        List<Long> titularIds = casos.stream().flatMap(List::stream).distinct().sorted().toList();
        return new IndicadorProblemaResponse("CADASTRO_DUPLICADO", "Possível cadastro duplicado",
                "Clientes diferentes com o mesmo e-mail ou telefone. Confira se são a mesma pessoa antes de atender uma solicitação.",
                casos.size(), Prioridade.MEDIA, titularIds);
    }

    private Stream<List<Long>> agruparDuplicados(List<Titular> titulares, Function<Titular, String> chave) {
        return titulares.stream()
                .filter(t -> !chave.apply(t).isEmpty())
                .collect(Collectors.groupingBy(chave, Collectors.mapping(Titular::getId, Collectors.toList())))
                .values().stream()
                .filter(ids -> ids.size() > 1)
                .map(ids -> ids.stream().sorted().toList());
    }

    private IndicadorProblemaResponse acessosDeExFuncionarios() {
        long quantidade = acessoService.acessosIndevidos().size();
        return new IndicadorProblemaResponse("ACESSO_EX_FUNCIONARIO", "Acesso ativo de ex-funcionário",
                "Funcionários desligados que ainda acessam canais com dados de clientes.", quantidade, Prioridade.ALTA, List.of());
    }
}
