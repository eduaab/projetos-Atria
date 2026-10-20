package br.com.atria.service.regra;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Formatos de data usados em textos de alerta, auditoria e PDFs. */
public final class FormatoData {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private FormatoData() {
    }

    public static String data(LocalDate data) {
        return data == null ? "-" : DATA.format(data);
    }

    public static String data(LocalDateTime dataHora) {
        return dataHora == null ? "-" : DATA.format(dataHora);
    }

    public static String dataHora(LocalDateTime dataHora) {
        return dataHora == null ? "-" : DATA_HORA.format(dataHora);
    }
}
