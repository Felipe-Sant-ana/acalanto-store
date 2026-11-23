package br.pucgoias.pousada.servidor;

import br.pucgoias.pousada.comum.Acao;
import br.pucgoias.pousada.comum.Categoria;
import br.pucgoias.pousada.comum.Requisicao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.Set;

/**
 * Validação de tudo o que chega do cliente.
 * <p>
 * Segurança: o servidor nunca confia no cliente. Usa lista de permissões: só aceita o que
 * reconhece como válido (tipo, obrigatoriedade, tamanho, faixa numérica, casas decimais
 * do preço e caracteres de controle). Qualquer falha lança
 * {@link RequisicaoInvalidaException}, cuja mensagem é segura para o usuário.
 */
final class Validador {

    // Limites: iguais aos do formulário do cliente (que valida só para dar resposta imediata).
    static final int NOME_MAX = 100;
    static final int DESCRICAO_MAX = 500;
    static final int BUSCA_MAX = 100;
    static final BigDecimal PRECO_MAX = new BigDecimal("999999.99");
    static final int QUANTIDADE_MAX = 999_999;
    static final int TAMANHO_PAGINA_PADRAO = 6;
    static final int TAMANHO_PAGINA_MAX = 50;


    /**
     * Parâmetros de uma listagem, já validados e com valores padrão aplicados.
     */
    record Consulta(String busca, Set<Categoria> categorias, Boolean disponivel,
                    BigDecimal precoMin, BigDecimal precoMax,
                    Integer quantidadeMin, Integer quantidadeMax,
                    int pagina, int tamanhoPagina) {
    }

    /**
     * Dados de um produto, já validados e normalizados (textos sem espaços nas pontas).
     */
    record DadosProduto(String nome, Categoria categoria, BigDecimal preco,
                        int quantidade, String descricao) {
    }

    private Validador() {
    }

    static Acao acao(Requisicao r) throws RequisicaoInvalidaException {
        if (r.acao == null) {
            throw new RequisicaoInvalidaException("Ação ausente ou desconhecida.");
        }
        return r.acao;
    }

    static long id(Requisicao r) throws RequisicaoInvalidaException {
        if (r.id == null || r.id < 1) {
            throw new RequisicaoInvalidaException("Identificador do produto inválido.");
        }
        return r.id;
    }

    static DadosProduto dadosProduto(Requisicao r) throws RequisicaoInvalidaException {
        String nome = texto(r.nome, "nome", NOME_MAX, true, false);
        if (r.categoria == null) {
            throw new RequisicaoInvalidaException("Categoria ausente ou inválida.");
        }
        BigDecimal preco = preco(r.preco);
        int quantidade = quantidade(r.quantidade);
        String descricao = texto(r.descricao, "descrição", DESCRICAO_MAX, false, true);
        return new DadosProduto(nome, r.categoria, preco, quantidade, descricao);
    }

    static Consulta consulta(Requisicao r) throws RequisicaoInvalidaException {
        String busca = texto(r.busca, "busca", BUSCA_MAX, false, false);

        Set<Categoria> categorias = EnumSet.noneOf(Categoria.class);
        if (r.categorias != null) {
            for (Categoria c : r.categorias) {
                if (c == null) {
                    throw new RequisicaoInvalidaException("Categoria inválida no filtro.");
                }
                categorias.add(c);
            }
        }

        BigDecimal precoMin = faixaPreco(r.precoMin, "preço mínimo");
        BigDecimal precoMax = faixaPreco(r.precoMax, "preço máximo");
        if (precoMin != null && precoMax != null && precoMin.compareTo(precoMax) > 0) {
            throw new RequisicaoInvalidaException("O preço mínimo não pode ser maior que o máximo.");
        }

        Integer qtdMin = faixaQuantidade(r.quantidadeMin, "quantidade mínima");
        Integer qtdMax = faixaQuantidade(r.quantidadeMax, "quantidade máxima");
        if (qtdMin != null && qtdMax != null && qtdMin > qtdMax) {
            throw new RequisicaoInvalidaException("A quantidade mínima não pode ser maior que a máxima.");
        }

        int pagina = r.pagina == null ? 1 : r.pagina;
        if (pagina < 1) {
            throw new RequisicaoInvalidaException("A página deve ser maior ou igual a 1.");
        }
        int tamanho = r.tamanhoPagina == null ? TAMANHO_PAGINA_PADRAO : r.tamanhoPagina;
        if (tamanho < 1 || tamanho > TAMANHO_PAGINA_MAX) {
            throw new RequisicaoInvalidaException(
                    "O tamanho da página deve estar entre 1 e " + TAMANHO_PAGINA_MAX + ".");
        }

        return new Consulta(busca, categorias, r.disponivel, precoMin, precoMax, qtdMin, qtdMax, pagina, tamanho);
    }

    // -=- Auxiliares -=-

    /**
     * Valida e normaliza um texto. Recusa caracteres de controle; a quebra de linha só é aceita onde permitida (descrição).
     */
    private static String texto(String valor, String campo, int max,
                                boolean obrigatorio, boolean permiteQuebraLinha)
            throws RequisicaoInvalidaException {
        String t = valor == null ? "" : valor.strip();
        if (t.isEmpty()) {
            if (obrigatorio) {
                throw new RequisicaoInvalidaException("O campo " + campo + " é obrigatório.");
            }
            return "";
        }
        if (t.length() > max) {
            throw new RequisicaoInvalidaException(
                    "O campo " + campo + " deve ter no máximo " + max + " caracteres.");
        }
        for (char c : t.toCharArray()) {
            if (Character.isISOControl(c) && !(permiteQuebraLinha && c == '\n')) {
                throw new RequisicaoInvalidaException("O campo " + campo + " contém caracteres inválidos.");
            }
        }
        return t;
    }

    /**
     * Aceita 0,00 a 999999,99 com no máximo 2 casas e devolve sempre com escala 2.
     */
    private static BigDecimal preco(BigDecimal valor) throws RequisicaoInvalidaException {
        if (valor == null) {
            throw new RequisicaoInvalidaException("O preço é obrigatório.");
        }
        if (valor.signum() < 0 || valor.compareTo(PRECO_MAX) > 0) {
            throw new RequisicaoInvalidaException("O preço deve estar entre 0,00 e 999999,99.");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new RequisicaoInvalidaException("O preço deve ter no máximo 2 casas decimais.");
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static int quantidade(Integer valor) throws RequisicaoInvalidaException {
        if (valor == null) {
            throw new RequisicaoInvalidaException("A quantidade é obrigatória.");
        }
        if (valor < 0 || valor > QUANTIDADE_MAX) {
            throw new RequisicaoInvalidaException("A quantidade deve estar entre 0 e " + QUANTIDADE_MAX + ".");
        }
        return valor;
    }

    private static BigDecimal faixaPreco(BigDecimal valor, String campo) throws RequisicaoInvalidaException {
        if (valor != null && (valor.signum() < 0 || valor.compareTo(PRECO_MAX) > 0)) {
            throw new RequisicaoInvalidaException("Valor inválido para " + campo + ".");
        }
        return valor;
    }

    private static Integer faixaQuantidade(Integer valor, String campo) throws RequisicaoInvalidaException {
        if (valor != null && (valor < 0 || valor > QUANTIDADE_MAX)) {
            throw new RequisicaoInvalidaException("Valor inválido para " + campo + ".");
        }
        return valor;
    }
}