package br.com.atria.controller;

import br.com.atria.relatorio.ArquivoPdf;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Resposta HTTP de download de PDF ({@code Content-Disposition: attachment}). */
final class RespostaPdf {

    private RespostaPdf() {
    }

    static ResponseEntity<byte[]> de(ArquivoPdf arquivo) {
        ContentDisposition disposicao = ContentDisposition.attachment().filename(arquivo.nomeArquivo()).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(arquivo.conteudo());
    }
}
