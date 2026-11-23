package br.pucgoias.pousada.comum;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mensagem enviada pelo cliente ao servidor, em formato JSON.
 * <p>
 * Um único formato serve a todas as ações: cada ação usa só os campos que lhe interessam
 * e os demais ficam {@code null} (e nem vão no JSON). Tipos "caixa" ({@code Integer},
 * {@code Boolean}) permitem distinguir "não informado" de zero ou falso.
 * <p>
 * Segurança: nada aqui é confiável. O servidor valida todos os campos em {@code Validador}.
 *
 * <pre>
 * Ação      Campos usados
 * LISTAR    busca, categorias, disponivel, precoMin/Max, quantidadeMin/Max, pagina, tamanhoPagina
 * RESUMO    (nenhum)
 * CRIAR     nome, categoria, preco, quantidade, descricao
 * EDITAR    id, nome, categoria, preco, quantidade, descricao
 * EXCLUIR   id
 * </pre>
 */
public class Requisicao {
    public Acao acao;

    // LISTAR
    public String busca;    // LISTAR: texto procurado no nome (vazio = sem busca)
    public List<Categoria> categorias;  // vazio ou ausente = todas as categorias
    public Boolean disponivel;  // true, false ou ausente (todos)
    public BigDecimal precoMin;
    public BigDecimal precoMax;
    public Integer quantidadeMin;
    public Integer quantidadeMax;
    public Integer pagina;  // começa em 1; padrão 1
    public Integer tamanhoPagina;   // itens por página; padrão 6, máximo 50

    // CRIAR / EDITAR / EXCLUIR
    public Long id; //EDITAR e EXCLUIR: identifica o produto
    public String nome;
    public Categoria categoria;
    public BigDecimal preco;
    public Integer quantidade;
    public String descricao;
}