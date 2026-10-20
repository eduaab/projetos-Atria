package br.com.atria.service.regra;

/** Mascara o CPF deixando visíveis só os dígitos 7 a 9: {@code ***.***.789-**}. */
public final class MascaradorCpf {
    private static final String MASCARA_VAZIA = "***.***.***-**";

    private MascaradorCpf() {
    }

    public static String mascarar(String cpf) {
        String digitos = NormalizadorTexto.somenteDigitos(cpf);
        if (digitos.length() != 11) return MASCARA_VAZIA;
        return "***.***." + digitos.substring(6, 9) + "-**";
    }
}
