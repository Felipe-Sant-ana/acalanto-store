package br.pucgoias.pousada.comum;

import java.util.List;

/**
 * Mensagem devolvida pelo servidor, em formato JSON.
 * <p>
 * {@code status} é {@code "OK"} ou {@code "ERRO"}. Em caso de erro, {@code mensagem} traz um
 * texto seguro para o usuário. Os demais campos dependem da ação e ficam {@code null}
 * (fora do JSON) quando não se aplicam. Use as fábricas estáticas em vez do construtor.
 */
public record Resposta(
        String status,          // "OK" ou "ERRO".
        String mensagem,
        List<Produto> produtos, // LISTAR.
        Produto produto,        // CRIAR / EDITAR.
        Integer pagina,         // LISTAR: página devolvida.
        Integer totalPaginas,
        Integer totalItens,     // itens que atendem à busca e aos filtros, somando todas as páginas.
        Resumo resumo) {

    public static final String OK = "OK";
    public static final String ERRO = "ERRO";

    /**
     * Resposta de falha. A mensagem é escrita para o usuário ler,
     * sem detalhes internos.
     */
    public static Resposta erro(String mensagem) {
        return new Resposta(ERRO, mensagem, null, null, null, null, null, null);
    }

    /**
     * Resposta de sucesso contendo uma mensagem.
     */
    public static Resposta comMensagem(String mensagem) {
        return new Resposta(OK, mensagem, null, null, null, null, null, null);
    }

    /**
     * Resposta de sucesso contendo um produto.
     */
    public static Resposta comProduto(String mensagem, Produto produto) {
        return new Resposta(OK, mensagem, null, produto, null, null, null, null);
    }

    /**
     * Resposta de sucesso contendo uma página de produtos e informações de paginação.
     */
    public static Resposta comPagina(List<Produto> produtos, int pagina, int totalPaginas, int totalItens) {
        return new Resposta(OK, null, produtos, null, pagina, totalPaginas, totalItens, null);
    }

    /**
     * Resposta de sucesso contendo o resumo dos produtos.
     */
    public static Resposta comResumo(Resumo resumo) {
        return new Resposta(OK, null, null, null, null, null, null, resumo);
    }

    /**
     * Verdadeiro quando a operação foi concluída com sucesso.
     */
    public boolean ok() {
        return OK.equals(status);
    }
}