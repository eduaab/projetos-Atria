package br.com.atria.service.regra;

import br.com.atria.exception.RegraNegocioException;
import br.com.atria.model.Titular;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Anonimização do titular: apaga somente os dados pessoais que a própria Atria guarda
 * (nome, CPF, e-mail, telefone e CEP). Nada é alterado nas plataformas de origem.
 */
@Component
public class AnonimizadorTitular {

    public List<CampoPessoal> camposQueSeraoApagados(Titular titular) {
        exigirNaoAnonimizado(titular);
        return List.of(
                new CampoPessoal("Nome", titular.getNome()),
                new CampoPessoal("CPF", titular.getCpf()),
                new CampoPessoal("E-mail", titular.getEmail()),
                new CampoPessoal("Telefone", titular.getTelefone()),
                new CampoPessoal("CEP", titular.getCep()));
    }

    /** Anonimiza o titular e devolve o CPF mascarado, que é preservado no comprovante. */
    public String anonimizar(Titular titular, LocalDateTime instante) {
        exigirNaoAnonimizado(titular);
        String cpfMascarado = MascaradorCpf.mascarar(titular.getCpf());
        titular.anonimizar(instante);
        return cpfMascarado;
    }

    private void exigirNaoAnonimizado(Titular titular) {
        if (titular.isAnonimizado()) {
            throw new RegraNegocioException("Os dados deste titular já foram excluídos em " + FormatoData.data(titular.getAnonimizadoEm()) + ".");
        }
    }
}
