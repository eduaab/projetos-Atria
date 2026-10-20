package br.com.atria.relatorio;

import br.com.atria.service.regra.FormatoData;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.core.io.ClassPathResource;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Montador de PDFs com a identidade da Atria: cabeçalho com a marca, título, período,
 * blocos de indicadores, tabelas e rodapé com a data de geração e o número da página.
 */
public class DocumentoPdf {
    private static final Color ROXO = new Color(0x4b, 0x3f, 0x5c);
    private static final Color LILAS = new Color(0xa1, 0x92, 0xb3);
    private static final Color CINZA = new Color(0xe6, 0xe6, 0xe6);
    private static final Color CINZA_TEXTO = new Color(0x66, 0x66, 0x66);
    private static final String LOGO = "static/img/Logo.png";

    private final ByteArrayOutputStream saida = new ByteArrayOutputStream();
    private final Document documento = new Document(PageSize.A4, 40, 40, 40, 50);

    public DocumentoPdf(String titulo, String subtitulo, String periodo, LocalDateTime geradoEm) {
        PdfWriter escritor = PdfWriter.getInstance(documento, saida);
        escritor.setPageEvent(new Rodape(geradoEm));
        documento.addTitle(titulo);
        documento.addCreator("Atria");
        documento.open();
        cabecalho(titulo, subtitulo, periodo, geradoEm);
    }

    public DocumentoPdf secao(String titulo) {
        Paragraph paragrafo = new Paragraph(titulo, fonte(13, Font.BOLD, ROXO));
        paragrafo.setSpacingBefore(14);
        paragrafo.setSpacingAfter(6);
        adicionar(paragrafo);
        return this;
    }

    public DocumentoPdf paragrafo(String texto) {
        Paragraph paragrafo = new Paragraph(texto, fonte(10, Font.NORMAL, Color.BLACK));
        paragrafo.setSpacingAfter(4);
        paragrafo.setLeading(14);
        adicionar(paragrafo);
        return this;
    }

    /** Blocos de indicador em grade de 3 colunas: cada item é {rótulo, valor}. */
    public DocumentoPdf indicadores(List<String[]> itens) {
        PdfPTable tabela = new PdfPTable(3);
        tabela.setWidthPercentage(100);
        tabela.setSpacingAfter(4);
        for (String[] item : itens) {
            PdfPCell celula = new PdfPCell();
            celula.setBackgroundColor(CINZA);
            celula.setBorderColor(Color.WHITE);
            celula.setBorderWidth(3);
            celula.setPadding(8);
            celula.addElement(new Paragraph(item[1], fonte(15, Font.BOLD, ROXO)));
            celula.addElement(new Paragraph(item[0], fonte(8.5f, Font.NORMAL, CINZA_TEXTO)));
            tabela.addCell(celula);
        }
        tabela.completeRow();
        adicionar(tabela);
        return this;
    }

    public DocumentoPdf tabela(String[] cabecalho, List<String[]> linhas, float[] larguras) {
        if (linhas.isEmpty()) {
            return paragrafo("Nenhum registro no período.");
        }
        PdfPTable tabela = new PdfPTable(larguras);
        tabela.setWidthPercentage(100);
        tabela.setHeaderRows(1);
        for (String titulo : cabecalho) {
            PdfPCell celula = new PdfPCell(new Phrase(titulo, fonte(9, Font.BOLD, Color.WHITE)));
            celula.setBackgroundColor(LILAS);
            celula.setBorderColor(LILAS);
            celula.setPadding(5);
            tabela.addCell(celula);
        }
        boolean zebra = false;
        for (String[] linha : linhas) {
            for (String valor : linha) {
                PdfPCell celula = new PdfPCell(new Phrase(valor == null ? "-" : valor, fonte(8.5f, Font.NORMAL, Color.BLACK)));
                celula.setBorderColor(CINZA);
                celula.setPadding(4);
                if (zebra) celula.setBackgroundColor(new Color(0xf6, 0xf6, 0xf9));
                tabela.addCell(celula);
            }
            zebra = !zebra;
        }
        adicionar(tabela);
        return this;
    }

    public byte[] concluir() {
        documento.close();
        return saida.toByteArray();
    }

    private void cabecalho(String titulo, String subtitulo, String periodo, LocalDateTime geradoEm) {
        PdfPTable tabela = new PdfPTable(new float[]{1.2f, 4f});
        tabela.setWidthPercentage(100);

        PdfPCell marca = logo();
        marca.setBorder(Rectangle.NO_BORDER);
        marca.setBackgroundColor(LILAS);
        marca.setPadding(8);
        marca.setHorizontalAlignment(Element.ALIGN_CENTER);
        marca.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabela.addCell(marca);

        PdfPCell textos = new PdfPCell();
        textos.setBorder(Rectangle.NO_BORDER);
        textos.setPaddingLeft(12);
        textos.addElement(new Paragraph(titulo, fonte(16, Font.BOLD, ROXO)));
        textos.addElement(new Paragraph(subtitulo, fonte(10, Font.NORMAL, CINZA_TEXTO)));
        textos.addElement(new Paragraph("Período: " + periodo + "   |   Gerado em " + FormatoData.dataHora(geradoEm),
                fonte(9, Font.NORMAL, CINZA_TEXTO)));
        tabela.addCell(textos);
        adicionar(tabela);
        Paragraph linha = new Paragraph(new Chunk(new LineSeparator(2f, 100, LILAS, Element.ALIGN_CENTER, -6)));
        linha.setSpacingAfter(10);
        adicionar(linha);
    }

    private PdfPCell logo() {
        try (InputStream entrada = new ClassPathResource(LOGO).getInputStream()) {
            Image imagem = Image.getInstance(entrada.readAllBytes());
            imagem.scaleToFit(90, 40);
            return new PdfPCell(imagem, false);
        } catch (IOException | RuntimeException e) {
            return new PdfPCell(new Phrase("atria", fonte(22, Font.BOLD, Color.WHITE)));
        }
    }

    private void adicionar(Element elemento) {
        try {
            documento.add(elemento);
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao montar o PDF", e);
        }
    }

    private static Font fonte(float tamanho, int estilo, Color cor) {
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.CP1252, tamanho, estilo, cor);
    }

    private static class Rodape extends PdfPageEventHelper {
        private final String texto;

        Rodape(LocalDateTime geradoEm) {
            this.texto = "Atria – governança de dados para o seu comércio   |   Gerado em " + FormatoData.dataHora(geradoEm);
        }

        @Override
        public void onEndPage(PdfWriter escritor, Document documento) {
            Phrase frase = new Phrase(texto + "   |   Página " + escritor.getPageNumber(), fonte(8, Font.NORMAL, CINZA_TEXTO));
            ColumnText.showTextAligned(escritor.getDirectContent(), Element.ALIGN_CENTER, frase,
                    (documento.left() + documento.right()) / 2, documento.bottom() - 25, 0);
        }
    }
}
