package br.com.atria.seed;

/** Gera CPFs fictícios em formato válido (dígitos verificadores corretos). */
final class CpfFicticio {

    private CpfFicticio() {
    }

    static String aPartirDe(long base) {
        String digitos = String.format("%09d", base % 1_000_000_000L);
        digitos += digitoVerificador(digitos, 10);
        digitos += digitoVerificador(digitos, 11);
        return digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "." + digitos.substring(6, 9) + "-" + digitos.substring(9);
    }

    private static int digitoVerificador(String digitos, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < digitos.length(); i++) {
            soma += Character.getNumericValue(digitos.charAt(i)) * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
