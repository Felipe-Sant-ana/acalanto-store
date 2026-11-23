package br.pucgoias.pousada.cliente;

import br.pucgoias.pousada.comum.Acao;
import br.pucgoias.pousada.comum.Produto;
import br.pucgoias.pousada.comum.Requisicao;
import br.pucgoias.pousada.comum.Resposta;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Tela principal: cards, busca, filtros, tabela, paginação e rodapé de status.
 * <p>
 * Regra do Swing seguida em todo o cliente: a rede nunca roda na Event Dispatch Thread
 * (EDT), senão a janela congela. Cada consulta usa um {@code SwingWorker}:
 * {@code doInBackground()} fala com o servidor e {@code done()} volta à EDT para
 * atualizar a tela. Por isso os campos de estado abaixo dispensam sincronização.
 */
public class JanelaPrincipal extends JFrame {

    private static final int TAMANHO_PAGINA = 15;

    private final ClienteRede rede;
    private final JTextField campoBusca = new JTextField(); // Propriedades de cliente do FlatLaf configuram o campo sem criar subclasse.
    private final JToggleButton botaoFiltros = new JToggleButton("Filtros");
    private final JButton botaoNovo = new JButton("+ Novo Produto");
    private final PainelFiltros painelFiltros = new PainelFiltros(this::aoMudarFiltros);
    private final PainelResumo painelResumo = new PainelResumo();
    private final TabelaProdutos tabela = new TabelaProdutos();
    private final JLabel rotuloInfo = new JLabel(" ");
    private final JLabel rotuloPagina = new JLabel(" ");
    private final JButton botaoAnterior = new JButton("‹ Anterior");
    private final JButton botaoProxima = new JButton("Próxima ›");
    private final Timer atrasoBusca; // Debounce: a consulta só sai 300 ms depois da última tecla, não a cada letra.

    // Só são acessados pela EDT (eventos e done() do SwingWorker), então não precisam de sincronização.
    private int paginaAtual = 1;
    private int sequencia = 0;  // Número da consulta mais recente. Resposta de consulta antiga é descartada em done().
    private boolean resumoPendente = false; // Verdadeiro quando os cards falharam e devem ser recarregados na próxima listagem bem-sucedida.
    private String avisoPendente = null; // Aviso mostrado uma vez no rodapé após cadastrar, editar ou excluir.
    private Long idParaDestacar = null; // produto a piscar quando a próxima listagem chegar

    public JanelaPrincipal(ClienteRede rede) {
        super("Pousada Acalanto - Produtos");
        this.rede = rede;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        definirIcone();

        atrasoBusca = new Timer(300, e -> {
            paginaAtual = 1;
            carregar();
        });
        atrasoBusca.setRepeats(false);

        tabela.setAcoes(this::abrirEdicao, this::confirmarExclusao);

        JPanel raiz = new JPanel(new BorderLayout(0, 16));
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel parteSuperior = new JPanel(new BorderLayout(0, 16));
        parteSuperior.add(new PainelCabecalho(), BorderLayout.NORTH);
        parteSuperior.add(painelResumo, BorderLayout.CENTER);
        raiz.add(parteSuperior, BorderLayout.NORTH);
        raiz.add(criarPainelProdutos(), BorderLayout.CENTER);
        setContentPane(raiz);

        setMinimumSize(new Dimension(760, 560));
        setSize(960, 860);
        setLocationRelativeTo(null);

        atualizarBotaoFiltros();
        carregar();
        carregarResumo();
    }

    // -=- Montagem da tela -=-

    /** Ícone da janela e da barra de tarefas. Se o arquivo não existir, mantém o padrão do Java. */
    private void definirIcone() {
        java.net.URL url = JanelaPrincipal.class.getResource("/imagens/icone.png");
        if (url != null) {
            setIconImage(new javax.swing.ImageIcon(url).getImage());
        }
    }

    private JPanel criarPainelProdutos() {
        // Propriedades de cliente do FlatLaf: configuram o campo sem criar subclasse.
        campoBusca.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por nome...");
        campoBusca.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSearchIcon());
        campoBusca.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        campoBusca.getDocument().addDocumentListener(new OuvinteTexto(atrasoBusca::restart));

        botaoFiltros.addActionListener(e -> {
            painelFiltros.setVisible(botaoFiltros.isSelected());
            atualizarBotaoFiltros();
            getContentPane().revalidate(); // recalcula o layout para o painel abrir/fechar
            getContentPane().repaint();
        });
        painelFiltros.setVisible(false);

        Tema.botaoPrimario(botaoNovo);
        botaoNovo.addActionListener(e -> abrirCadastro());

        JPanel botoesBusca = new JPanel(new GridLayout(1, 2, 8, 0));
        botoesBusca.add(botaoFiltros);
        botoesBusca.add(botaoNovo);

        JPanel linhaBusca = new JPanel(new BorderLayout(8, 0));
        linhaBusca.add(campoBusca, BorderLayout.CENTER);
        linhaBusca.add(botoesBusca, BorderLayout.EAST);

        JPanel topoTabela = new JPanel(new BorderLayout(0, 8));
        topoTabela.add(linhaBusca, BorderLayout.NORTH);
        topoTabela.add(painelFiltros, BorderLayout.CENTER);

        JPanel painel = new JPanel(new BorderLayout(0, 12));
        painel.add(topoTabela, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);
        painel.add(criarRodape(), BorderLayout.SOUTH);
        return painel;
    }

    private JPanel criarRodape() {
        botaoAnterior.setEnabled(false);
        botaoProxima.setEnabled(false);
        botaoAnterior.addActionListener(e -> {
            paginaAtual = Math.max(1, paginaAtual - 1);
            carregar();
        });
        botaoProxima.addActionListener(e -> {
            paginaAtual++;
            carregar();
        });

        JPanel navegacao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        navegacao.add(botaoAnterior);
        navegacao.add(rotuloPagina);
        navegacao.add(botaoProxima);

        JPanel rodape = new JPanel(new BorderLayout(12, 0));
        rodape.add(rotuloInfo, BorderLayout.WEST);
        rodape.add(navegacao, BorderLayout.EAST);
        return rodape;
    }

    // Texto do botão: "Filtros"
    private void atualizarBotaoFiltros() {
        int ativos = painelFiltros.getAplicados().gruposAtivos();
        String texto = "Filtros" + (ativos > 0 ? " (" + ativos + ")" : "");
        botaoFiltros.setText(texto + (botaoFiltros.isSelected() ? " ▴" : " ▾"));
    }

    private void aoMudarFiltros() {
        paginaAtual = 1;
        atualizarBotaoFiltros();
        carregar();
    }

    // -=- Cadastro, edição e exclusão -=-

    private void abrirCadastro() {
        new DialogoProduto(this, rede, null, p -> {
            idParaDestacar = p.id();
            aposOperacao("Produto \"" + p.nome() + "\" cadastrado.");
        }).setVisible(true);
    }

    private void abrirEdicao(Produto produto) {
        new DialogoProduto(this, rede, produto, p -> {
            idParaDestacar = p.id();
            aposOperacao("Produto \"" + p.nome() + "\" atualizado.");
        }).setVisible(true);
    }

    // "Cancelar" é a opção padrão, para evitar exclusão por Enter acidental.
    private void confirmarExclusao(Produto produto) {
        Object[] opcoes = {"Excluir", "Cancelar"};
        int escolha = JOptionPane.showOptionDialog(this,
                "Excluir o produto \"" + produto.nome() + "\"?\n"
                        + "Esta ação não pode ser desfeita.",
                "Confirmar exclusão", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, opcoes, opcoes[1]); // "Cancelar" é a opção padrão
        if (escolha != 0) {
            return;
        }

        Requisicao req = new Requisicao();
        req.acao = Acao.EXCLUIR;
        req.id = produto.id();

        new SwingWorker<Resposta, Void>() {
            @Override
            protected Resposta doInBackground() throws Exception {
                return rede.enviar(req);
            }

            @Override
            protected void done() {
                try {
                    Resposta r = get();
                    if (r.ok()) {
                        aposOperacao("Produto \"" + produto.nome() + "\" excluído.");
                    } else {
                        // Ex.: outro cliente já excluiu. Avisa e atualiza a lista mesmo assim.
                        JOptionPane.showMessageDialog(JanelaPrincipal.this, r.mensagem(),
                                "Não foi possível excluir", JOptionPane.ERROR_MESSAGE);
                        aposOperacao(null);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    JOptionPane.showMessageDialog(JanelaPrincipal.this,
                            causa != null && causa.getMessage() != null
                                    ? causa.getMessage() : "Falha inesperada na comunicação.",
                            "Não foi possível excluir", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Depois de qualquer alteração, recarrega a lista (com busca e filtros atuais) e os cards. O aviso explica se o item caiu em outra página ou saiu do filtro.
     */
    private void aposOperacao(String aviso) {
        avisoPendente = aviso;
        carregar();
        carregarResumo();
    }

    // -=- Consultas ao servidor -=-

    /**
     * Pede ao servidor a página atual com a busca e os filtros aplicados.
     */
    private void carregar() {
        final int minhaSequencia = ++sequencia;
        botaoAnterior.setEnabled(false);
        botaoProxima.setEnabled(false);

        Requisicao req = new Requisicao();
        req.acao = Acao.LISTAR;
        req.busca = campoBusca.getText();
        req.pagina = paginaAtual;
        req.tamanhoPagina = TAMANHO_PAGINA;
        painelFiltros.getAplicados().aplicarEm(req);

        new SwingWorker<Resposta, Void>() {
            @Override
            protected Resposta doInBackground() throws Exception {
                return rede.enviar(req); // thread de fundo
            }

            @Override
            protected void done() { // de volta à EDT
                if (minhaSequencia != sequencia) {
                    return; // chegou uma consulta mais nova: esta resposta está velha
                }
                try {
                    mostrar(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    mostrarErro(causa != null && causa.getMessage() != null
                            ? causa.getMessage() : "Falha inesperada na comunicação.");
                }
            }
        }.execute();
    }

    /**
     * Atualiza os 4 cards.
     */
    void carregarResumo() {
        resumoPendente = false;
        Requisicao req = new Requisicao();
        req.acao = Acao.RESUMO;

        new SwingWorker<Resposta, Void>() {
            @Override
            protected Resposta doInBackground() throws Exception {
                return rede.enviar(req);
            }

            @Override
            protected void done() {
                try {
                    Resposta r = get();
                    if (r.ok() && r.resumo() != null) {
                        painelResumo.mostrar(r.resumo());
                    } else {
                        resumoPendente = true;
                        painelResumo.limpar();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    resumoPendente = true; // o erro já aparece na listagem; aqui só marca para tentar depois
                    painelResumo.limpar();
                }
            }
        }.execute();
    }

    /**
     * Exibe a página recebida. O servidor pode ter ajustado o número da página pedida.
     */
    private void mostrar(Resposta r) {
        if (!r.ok()) {
            mostrarErro(r.mensagem());
            return;
        }
        List<Produto> produtos = r.produtos() == null ? List.of() : r.produtos();
        int pagina = r.pagina() == null ? 1 : r.pagina();
        int totalPaginas = r.totalPaginas() == null ? 1 : r.totalPaginas();
        int totalItens = r.totalItens() == null ? produtos.size() : r.totalItens();

        paginaAtual = pagina;
        tabela.setProdutos(produtos);
        if (idParaDestacar != null) {
            tabela.destacar(idParaDestacar); // só pisca se o produto estiver nesta página
            idParaDestacar = null;
        }

        String info;
        if (totalItens == 0) {
            info = "Nenhum produto encontrado.";
        } else {
            int de = (pagina - 1) * TAMANHO_PAGINA + 1;
            int ate = de + produtos.size() - 1;
            info = "Mostrando " + de + " a " + ate + " de " + totalItens
                    + (totalItens == 1 ? " produto" : " produtos");
        }
        if (avisoPendente != null) {
            info += "   •   " + avisoPendente;
            avisoPendente = null;
        }
        rotuloInfo.setForeground(Tema.TEXTO);
        rotuloInfo.setText(info);

        rotuloPagina.setText("Página " + pagina + " de " + totalPaginas);
        botaoAnterior.setEnabled(pagina > 1);
        botaoProxima.setEnabled(pagina < totalPaginas);

        if (resumoPendente) {
            carregarResumo(); // o servidor voltou: recupera os cards
        }
    }

    /**
     * Limpa tabela e cards e mostra a mensagem em vermelho no rodapé. A próxima consulta bem-sucedida recupera tudo.
     */
    private void mostrarErro(String mensagem) {
        avisoPendente = null;
        idParaDestacar = null;
        tabela.setProdutos(List.of());
        painelResumo.limpar();
        resumoPendente = true;
        rotuloInfo.setForeground(Tema.VERMELHO);
        rotuloInfo.setText(mensagem == null ? "Erro desconhecido." : mensagem);
        rotuloPagina.setText(" ");
        botaoAnterior.setEnabled(false);
        botaoProxima.setEnabled(false);
    }

    // -=- Auxiliar -=-

    /**
     * DocumentListener tem 3 métodos; para a busca os três significam "o texto mudou".
     */
    private static final class OuvinteTexto implements DocumentListener {
        private final Runnable acao;

        OuvinteTexto(Runnable acao) {
            this.acao = acao;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            acao.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            acao.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            acao.run();
        }
    }
}