package br.pucgoias.pousada.comum;

import java.math.BigDecimal;

/**
 * Produto da pousada. Registro imutável: para "editar", o servidor cria um novo objeto.
 * <p>
 * O {@code id} é um identificador técnico gerado pelo servidor e nunca exibido ao usuário
 * (com um banco de dados, seria a chave primária). O status de disponibilidade não é
 * guardado: é calculado pela quantidade.
 *
 * @param id         identificador único, maior que zero
 * @param nome       nome exibido e pesquisado
 * @param categoria  categoria do produto
 * @param preco      preço em reais; BigDecimal evita erros de arredondamento do double
 * @param quantidade unidades em estoque
 * @param descricao  texto opcional (nunca nulo)
 */
public record Produto(
        long id,
        String nome,
        Categoria categoria,
        BigDecimal preco,
        int quantidade,
        String descricao) {

    // Garante que a descrição nunca seja nula, mesmo se o JSON a omitir.
    public Produto {
        if (descricao == null) {
            descricao = "";
        }
    }

    /**
     * Disponível quando há ao menos uma unidade. Derivado, portanto não vai para o JSON.
     */
    public boolean disponivel() {
        return quantidade > 0;
    }
}