package br.pucgoias.pousada.comum;

/**
 * Operações que o cliente pode pedir ao servidor.
 * <p>
 * Faz parte do protocolo: o nome do valor viaja no JSON, no campo {@code "acao"}.
 * Um nome desconhecido é lido como {@code null} e o servidor o recusa.
 */
public enum Acao {
    /**
     * Lista produtos aplicando busca, filtros e paginação.
     */
    LISTAR,
    /**
     * Devolve os números dos cards (total e quantidade por categoria).
     */
    RESUMO,
    /**
     * Cadastra um produto novo.
     */
    CRIAR,
    /**
     * Altera os dados de um produto existente, identificado pelo {@code id}.
     */
    EDITAR,
    /**
     * Remove um produto, identificado pelo {@code id}.
     */
    EXCLUIR
}