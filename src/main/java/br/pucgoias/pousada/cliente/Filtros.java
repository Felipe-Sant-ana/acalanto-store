package br.pucgoias.pousada.cliente;

import br.pucgoias.pousada.comum.Categoria;
import br.pucgoias.pousada.comum.Requisicao;

import java.math.BigDecimal;
import java.util.List;

/**
 * Filtros aplicados à listagem. {@code null} em um critério significa "sem filtro".
 * Registro imutável, copiado para a {@link br.pucgoias.pousada.comum.Requisicao} a cada consulta.
 */
record Filtros(List<Categoria> categorias, Boolean disponivel,
               BigDecimal precoMin, BigDecimal precoMax,
               Integer quantidadeMin, Integer quantidadeMax) {

    static final Filtros NENHUM = new Filtros(null, null, null, null, null, null);

    /**
     * Quantos grupos de filtro estão ativos (categoria, status, preço, quantidade).
     */
    int gruposAtivos() {
        return (categorias != null ? 1 : 0)
                + (disponivel != null ? 1 : 0)
                + (precoMin != null || precoMax != null ? 1 : 0)
                + (quantidadeMin != null || quantidadeMax != null ? 1 : 0);
    }

    void aplicarEm(Requisicao req) {
        req.categorias = categorias;
        req.disponivel = disponivel;
        req.precoMin = precoMin;
        req.precoMax = precoMax;
        req.quantidadeMin = quantidadeMin;
        req.quantidadeMax = quantidadeMax;
    }
}