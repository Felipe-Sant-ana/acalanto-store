package br.pucgoias.pousada.servidor;

import br.pucgoias.pousada.comum.Categoria;
import br.pucgoias.pousada.comum.Produto;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Guarda os produtos em memória e os persiste em um arquivo JSON.
 * <p>
 * Concorrência: todos os métodos públicos são {@code synchronized}, então só um cliente
 * por vez lê ou altera o estado. Isso impede corrupção do arquivo e códigos duplicados.
 * <p>
 * A lista em memória é imutável: cada alteração monta uma lista nova, grava no disco e
 * só então a adota. Se a gravação falhar, a memória continua igual ao arquivo.
 */
public class ProdutoRepositorio {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path arquivo;
    private List<Produto> produtos; // sempre uma lista imutável

    public ProdutoRepositorio(Path arquivo) {
        this.arquivo = arquivo;
        this.produtos = carregar();
    }

    public synchronized List<Produto> listarTodos() {
        return produtos; // imutável: seguro de devolver
    }

    public synchronized Optional<Produto> buscarPorId(long id) {
        return produtos.stream()
                .filter(p -> p.id() == id)
                .findFirst();
    }

    /**
     * Cadastra um produto com id novo (maior id existente + 1).
     */
    public synchronized Produto inserir(String nome, Categoria categoria,
                                        BigDecimal preco, int quantidade, String descricao) {
        Produto novo = new Produto(gerarId(), nome, categoria, preco, quantidade, descricao);
        List<Produto> nova = new ArrayList<>(produtos);
        nova.add(novo);
        confirmar(nova);
        return novo;
    }

    /**
     * O id não muda; todo o resto pode mudar. Vazio se o id não existir.
     */
    public synchronized Optional<Produto> atualizar(long id, String nome, Categoria categoria,
                                                    BigDecimal preco, int quantidade, String descricao) {
        if (buscarPorId(id).isEmpty()) {
            return Optional.empty();
        }
        Produto editado = new Produto(id, nome, categoria, preco, quantidade, descricao);
        List<Produto> nova = new ArrayList<>(produtos);
        nova.replaceAll(p -> p.id() == id ? editado : p);
        confirmar(nova);
        return Optional.of(editado);
    }

    /**
     * Remove o produto. Retorna false se o id não existir.
     */
    public synchronized boolean excluir(long id) {
        List<Produto> nova = new ArrayList<>(produtos);
        boolean removeu = nova.removeIf(p -> p.id() == id);
        if (removeu) {
            confirmar(nova);
        }
        return removeu;
    }

    // -=- Internos -=-

    // Aproximação de uma chave gerada por banco. Após excluir o último, o número é reaproveitado.
    private long gerarId() {
        return produtos.stream().mapToLong(Produto::id).max().orElse(0) + 1;
    }

    /**
     * Grava primeiro no disco; só depois adota a nova lista em memória.
     */
    private void confirmar(List<Produto> nova) {
        salvar(nova);
        this.produtos = List.copyOf(nova);
    }

    private List<Produto> carregar() {
        if (!Files.exists(arquivo)) {
            List<Produto> vazia = List.of();
            salvar(vazia);
            return vazia;
        }
        try {
            String json = Files.readString(arquivo, StandardCharsets.UTF_8);
            Produto[] lidos = GSON.fromJson(json, Produto[].class);
            if (lidos == null) {
                return List.of();
            }
            List<Produto> lista = List.copyOf(Arrays.asList(lidos));
            verificarIntegridade(lista);
            return lista;
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + arquivo, e);
        } catch (JsonParseException e) {
            throw new IllegalStateException("Arquivo JSON inválido: " + arquivo, e);
        }
    }

    /**
     * O Gson devolve null/0 para campos ausentes ou desconhecidos (ex.: categoria escrita errada). Recusa subir do que falhar depois.
     */
    private void verificarIntegridade(List<Produto> lista) {
        Set<Long> ids = new HashSet<>();
        for (Produto p : lista) {
            if (p.id() <= 0 || p.nome() == null || p.categoria() == null || p.preco() == null) {
                throw new IllegalStateException("Produto com dados ausentes ou inválidos em " + arquivo
                        + " (o arquivo está no formato antigo, com 'codigo'?): " + p);
            }
            if (!ids.add(p.id())) {
                throw new IllegalStateException("Id repetido em " + arquivo + ": " + p.id());
            }
        }
    }

    // Grava em arquivo temporário e move para o definitivo: uma queda no meio da escrita não deixa o JSON pela metade.
    private void salvar(List<Produto> lista) {
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            Files.createDirectories(pasta);
            Path temporario = pasta.resolve(arquivo.getFileName() + ".tmp");
            Files.writeString(temporario, GSON.toJson(lista), StandardCharsets.UTF_8);
            Files.move(temporario, arquivo,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gravar " + arquivo, e);
        }
    }
}