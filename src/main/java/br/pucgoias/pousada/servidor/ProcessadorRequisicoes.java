package br.pucgoias.pousada.servidor;

import br.pucgoias.pousada.comum.*;

import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Regras de negócio do servidor: recebe uma {@link Requisicao} já lida do JSON e devolve
 * uma {@link Resposta}. Não conhece sockets, o que facilita testá-la isoladamente.
 * <p>
 * Segurança: toda entrada passa pelo {@link Validador}. Erros inesperados vão para o log
 * do servidor e o cliente recebe apenas uma mensagem genérica.
 */
public class ProcessadorRequisicoes {

    private static final Logger LOG = Logger.getLogger(ProcessadorRequisicoes.class.getName());

    private static final Collator COLATOR = Collator.getInstance(Locale.of("pt", "BR"));

    // Ordem da listagem: por categoria e, dentro dela, alfabética (com regras do português, ignorando acentos); o id desempata.
    private static final Comparator<Produto> ORDEM =
            Comparator.comparing(Produto::categoria)
                    .thenComparing(Produto::nome, COLATOR)
                    .thenComparingLong(Produto::id);

    private final ProdutoRepositorio repositorio;

    public ProcessadorRequisicoes(ProdutoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Executa a ação pedida. Nunca lança exceção: toda falha vira uma Resposta de erro.
     */
    public Resposta processar(Requisicao req) {
        try {
            return switch (Validador.acao(req)) {
                case LISTAR -> listar(req);
                case RESUMO -> resumo();
                case CRIAR -> criar(req);
                case EDITAR -> editar(req);
                case EXCLUIR -> excluir(req);
            };
        } catch (RequisicaoInvalidaException e) {
            return Resposta.erro(e.getMessage());
        } catch (RuntimeException e) {
            // Segurança: detalhes só no log; o cliente não vê stack trace.
            LOG.log(Level.SEVERE, "Erro inesperado ao processar requisição", e);
            return Resposta.erro("Erro interno do servidor.");
        }
    }

    // -=- Ações -=-

    /**
     * Aplica busca, filtros, ordenação e paginação, nesta ordem. Página fora do limite é ajustada para a última existente.
     */
    private Resposta listar(Requisicao req) throws RequisicaoInvalidaException {
        Validador.Consulta c = Validador.consulta(req);
        String termo = normalizar(c.busca());

        List<Produto> filtrados = repositorio.listarTodos().stream()
                .filter(p -> termo.isEmpty() || normalizar(p.nome()).contains(termo))
                .filter(p -> c.categorias().isEmpty() || c.categorias().contains(p.categoria()))
                .filter(p -> c.disponivel() == null || p.disponivel() == c.disponivel())
                .filter(p -> c.precoMin() == null || p.preco().compareTo(c.precoMin()) >= 0)
                .filter(p -> c.precoMax() == null || p.preco().compareTo(c.precoMax()) <= 0)
                .filter(p -> c.quantidadeMin() == null || p.quantidade() >= c.quantidadeMin())
                .filter(p -> c.quantidadeMax() == null || p.quantidade() <= c.quantidadeMax())
                .sorted(ORDEM)
                .toList();

        int total = filtrados.size();
        int tamanho = c.tamanhoPagina();
        int totalPaginas = Math.max(1, (total + tamanho - 1) / tamanho);
        int pagina = Math.min(c.pagina(), totalPaginas);
        int de = (pagina - 1) * tamanho;
        int ate = Math.min(de + tamanho, total);

        return Resposta.comPagina(new ArrayList<>(filtrados.subList(de, ate)), pagina, totalPaginas, total);
    }

    private Resposta resumo() {
        List<Produto> todos = repositorio.listarTodos();
        return Resposta.comResumo(new Resumo(
                todos.size(),
                contar(todos, Categoria.ALIMENTOS),
                contar(todos, Categoria.VESTUARIO),
                contar(todos, Categoria.DIVERSOS)));
    }

    private Resposta criar(Requisicao req) throws RequisicaoInvalidaException {
        Validador.DadosProduto d = Validador.dadosProduto(req);
        Produto novo = repositorio.inserir(d.nome(), d.categoria(), d.preco(), d.quantidade(), d.descricao());
        return Resposta.comProduto("Produto cadastrado.", novo);
    }

    /**
     * Devolve erro "Produto não encontrado." se outro cliente já removeu o item.
     */
    private Resposta editar(Requisicao req) throws RequisicaoInvalidaException {
        long id = Validador.id(req);
        Validador.DadosProduto d = Validador.dadosProduto(req);
        return repositorio.atualizar(id, d.nome(), d.categoria(), d.preco(), d.quantidade(), d.descricao())
                .map(p -> Resposta.comProduto("Produto atualizado.", p))
                .orElseGet(() -> Resposta.erro("Produto não encontrado."));
    }

    /**
     * Devolve erro "Produto não encontrado." se outro cliente já removeu o item.
     */
    private Resposta excluir(Requisicao req) throws RequisicaoInvalidaException {
        long id = Validador.id(req);
        return repositorio.excluir(id)
                ? Resposta.comMensagem("Produto excluído.")
                : Resposta.erro("Produto não encontrado.");
    }

    // -=- Auxiliares -=-

    private static int contar(List<Produto> produtos, Categoria categoria) {
        return (int) produtos.stream().filter(p -> p.categoria() == categoria).count();
    }

    /**
     * Minúsculas e sem acentos: "Calça" vira "calca".
     */
    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}