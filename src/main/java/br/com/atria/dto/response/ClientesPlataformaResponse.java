package br.com.atria.dto.response;

public record ClientesPlataformaResponse(String slug, String nome, boolean conectada, long clientes) {
}
