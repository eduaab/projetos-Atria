package br.com.atria.repository.memoria;

import br.com.atria.model.analise.Registros.ClienteResumo;
import br.com.atria.model.analise.Registros.EfeitoSemana;
import br.com.atria.model.analise.Registros.LinhaAtendimento;
import br.com.atria.model.analise.Registros.LinhaDiaria;
import br.com.atria.model.analise.Registros.LinhaReputacao;
import br.com.atria.model.analise.Registros.PedidoCliente;
import br.com.atria.model.analise.Registros.PostInstagram;
import br.com.atria.repository.AnaliseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Carrega os JSON de {@code classpath:/analise/} uma vez, na subida, e os mantém em memória. */
@Repository
public class AnaliseRepositoryMemoria implements AnaliseRepository {
    private final ObjectMapper json;
    private final List<LinhaDiaria> diario;
    private final List<LinhaAtendimento> atendimento;
    private final List<PostInstagram> posts;
    private final List<LinhaReputacao> reputacao;
    private final List<ClienteResumo> clientes;
    private final List<PedidoCliente> pedidosCliente;
    private final List<EfeitoSemana> efeitoSemanal;
    private final JsonNode efeitoDid;
    private final JsonNode pendencias;
    private final JsonNode modelos;
    private final JsonNode modeloConversa;
    private final JsonNode regressao;
    private final LocalDate dataReferencia;

    public AnaliseRepositoryMemoria(ObjectMapper json) {
        this.json = json;
        this.diario = ler("diario", new TypeReference<>() { });
        this.atendimento = ler("atendimento", new TypeReference<>() { });
        this.posts = ler("posts", new TypeReference<>() { });
        this.reputacao = ler("reputacao", new TypeReference<>() { });
        this.clientes = ler("clientes", new TypeReference<>() { });
        this.pedidosCliente = ler("pedidos_cliente", new TypeReference<>() { });
        this.efeitoSemanal = ler("efeito_semanal", new TypeReference<>() { });
        this.efeitoDid = ler("efeito_did", new TypeReference<>() { });
        this.pendencias = ler("pendencias", new TypeReference<>() { });
        this.modelos = ler("modelos", new TypeReference<>() { });
        this.modeloConversa = ler("modelo_conversa", new TypeReference<>() { });
        this.regressao = ler("regressao", new TypeReference<>() { });
        this.dataReferencia = diario.stream().map(l -> LocalDate.parse(l.d())).max(Comparator.naturalOrder())
                .orElseThrow(() -> new IllegalStateException("analise/diario.json está vazio"));
    }

    private <T> T ler(String nome, TypeReference<T> tipo) {
        try (InputStream entrada = new ClassPathResource("analise/" + nome + ".json").getInputStream()) {
            return json.readValue(entrada, tipo);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler analise/" + nome + ".json", e);
        }
    }

    @Override public List<LinhaDiaria> diario() { return diario; }
    @Override public List<LinhaAtendimento> atendimento() { return atendimento; }
    @Override public List<PostInstagram> posts() { return posts; }
    @Override public List<LinhaReputacao> reputacao() { return reputacao; }
    @Override public List<ClienteResumo> clientes() { return clientes; }
    @Override public List<PedidoCliente> pedidosCliente() { return pedidosCliente; }
    @Override public List<EfeitoSemana> efeitoSemanal() { return efeitoSemanal; }
    @Override public JsonNode efeitoDid() { return efeitoDid; }
    @Override public JsonNode pendencias() { return pendencias; }
    @Override public JsonNode modelos() { return modelos; }
    @Override public JsonNode modeloConversa() { return modeloConversa; }
    @Override public JsonNode regressao() { return regressao; }
    @Override public LocalDate dataReferencia() { return dataReferencia; }
}
