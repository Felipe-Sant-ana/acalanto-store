package br.pucgoias.pousada.cliente;

import br.pucgoias.pousada.comum.*;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Formulário de produto, usado no cadastro e na edição.
 * <p>
 * Com um {@code Produto} recebido, funciona como edição (campos preenchidos); sem ele,
 * como cadastro. Valida no cliente (resposta imediata) e envia ao servidor por
 * {@code SwingWorker}. Se o servidor recusar ou a rede falhar, a mensagem aparece dentro
 * do diálogo sem perder o que foi digitado; ele só fecha em caso de sucesso.
 */
final class DialogoProduto extends JDialog {

    private static final BigDecimal PRECO_MAX = new BigDecimal("999999.99");
    private static final int QUANTIDADE_MAX = 999_999;
    private static final int NOME_MAX = 100;
    private static final int DESCRICAO_MAX = 500;

    private static final class EntradaInvalidaException extends Exception {
        EntradaInvalidaException(String mensagem) {
            super(mensagem);
        }
    }

    private final ClienteRede rede;
    private final Produto produto; // null = cadastro
    private final Consumer<Produto> aoSalvar;

    private final JTextField campoNome = new JTextField();
    private final JComboBox<Categoria> comboCategoria = new JComboBox<>(Categoria.values());
    private final JTextField campoPreco = new JTextField();
    private final JTextField campoQuantidade = new JTextField();
    private final JTextArea campoDescricao = new JTextArea(4, 30);
    private final JLabel rotuloErro = new JLabel(" ");
    private final JButton botaoSalvar = new JButton("Salvar Produto");
    private final JButton botaoCancelar = new JButton("Cancelar");

    private boolean salvando = false; // Impede dois envios simultâneos (duplo clique ou Enter repetido).

    DialogoProduto(Frame dono, ClienteRede rede, Produto produto, Consumer<Produto> aoSalvar) {
        super(dono, produto == null ? "Novo Produto" : "Editar Produto", true);
        this.rede = rede;
        this.produto = produto;
        this.aoSalvar = aoSalvar;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        configurarCampos();
        preencher();
        setContentPane(montar());
        Tema.botaoPrimario(botaoSalvar);

        botaoSalvar.addActionListener(e -> salvar());
        botaoCancelar.addActionListener(e -> cancelar());

        getRootPane().setDefaultButton(botaoSalvar); // Enter aciona Salvar Produto.
        getRootPane().registerKeyboardAction(e -> cancelar(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                campoNome.requestFocusInWindow();
            }
        });

        pack();
        setSize(Math.max(getWidth(), 520), getHeight());
        setResizable(false);
        setLocationRelativeTo(dono);
    }

    // -=- Montagem -=-

    private void configurarCampos() {
        campoNome.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ex.: Água Mineral");
        campoPreco.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0,00");
        campoQuantidade.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0");

        campoDescricao.setLineWrap(true);
        campoDescricao.setWrapStyleWord(true);

        // Sem isto, a tecla Tab inseriria tabulação em vez de ir ao próximo campo.
        campoDescricao.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        campoDescricao.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        FiltroDigitacao.apenasValor(campoPreco, 12);        // ex.: 999.999,99
        FiltroDigitacao.apenasDigitos(campoQuantidade, 6);  // máximo 999999

        // Sem seleção, o combo pede para desenhar "null": usamos isso para exibir "Selecione a categoria".
        comboCategoria.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                          boolean selecionado, boolean foco) {
                Component c = super.getListCellRendererComponent(lista, valor, indice, selecionado, foco);
                if (valor == null) {
                    setText("Selecione a categoria");
                    if (!selecionado) {
                        setForeground(UIManager.getColor("Label.disabledForeground"));
                    }
                }
                return c;
            }
        });
        comboCategoria.setSelectedIndex(-1);

        rotuloErro.setForeground(Tema.VERMELHO);
    }

    private void preencher() {
        if (produto == null) {
            return;
        }
        campoNome.setText(produto.nome());
        comboCategoria.setSelectedItem(produto.categoria());
        campoPreco.setText(produto.preco().toPlainString().replace('.', ','));
        campoQuantidade.setText(String.valueOf(produto.quantidade()));
        campoDescricao.setText(produto.descricao());
        campoDescricao.setCaretPosition(0);
    }

    private JPanel montar() {
        JPanel form = new JPanel(new GridBagLayout());
        colocar(form, rotulo("Nome do Produto", true), 0, 0, 3, 1, 0, 0, 4);
        colocar(form, campoNome, 0, 1, 3, 1, 0, 0, 12);

        colocar(form, rotulo("Categoria", true), 0, 2, 1, 0.5, 0, 12, 4);
        colocar(form, rotulo("Preço (R$)", true), 1, 2, 1, 0.25, 0, 12, 4);
        colocar(form, rotulo("Quantidade", true), 2, 2, 1, 0.25, 0, 0, 4);
        colocar(form, comboCategoria, 0, 3, 1, 0.5, 0, 12, 12);
        colocar(form, campoPreco, 1, 3, 1, 0.25, 0, 12, 12);
        colocar(form, campoQuantidade, 2, 3, 1, 0.25, 0, 0, 12);

        colocar(form, rotulo("Descrição (opcional)", false), 0, 4, 3, 1, 0, 0, 4);
        colocar(form, new JScrollPane(campoDescricao), 0, 5, 3, 1, 1, 0, 8);
        colocar(form, rotuloErro, 0, 6, 3, 1, 0, 0, 0);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.add(botaoCancelar);
        botoes.add(botaoSalvar);

        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        raiz.add(form, BorderLayout.CENTER);
        raiz.add(botoes, BorderLayout.SOUTH);
        return raiz;
    }

    private static JLabel rotulo(String texto, boolean obrigatorio) {
        return new JLabel("<html>" + texto + (obrigatorio ? "<font color='#DC2626'> *</font>" : "") + "</html>");
    }

    /**
     * Atalho para o GridBagLayout: posição, largura, pesos de esticamento e margens.
     */
    private static void colocar(JPanel painel, JComponent componente, int x, int y, int largura,
                                double pesoX, double pesoY, int margemDireita, int margemBaixo) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.gridwidth = largura;
        g.weightx = pesoX;
        g.weighty = pesoY;
        g.fill = pesoY > 0 ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(0, 0, margemBaixo, margemDireita);
        painel.add(componente, g);
    }

    // -=- Ações -=-

    private void cancelar() {
        if (!salvando) {
            dispose();
        }
    }

    private void salvar() {
        if (salvando) {
            return;
        }
        Requisicao req;
        try {
            req = montarRequisicao();
        } catch (EntradaInvalidaException e) {
            rotuloErro.setText(e.getMessage());
            return;
        }

        rotuloErro.setText(" ");
        definirSalvando(true);

        new SwingWorker<Resposta, Void>() {
            @Override
            protected Resposta doInBackground() throws Exception {
                return rede.enviar(req); // thread de fundo
            }

            @Override
            protected void done() { // de volta à EDT
                try {
                    Resposta r = get();
                    if (r.ok() && r.produto() != null) {
                        dispose();
                        aoSalvar.accept(r.produto());
                    } else {
                        definirSalvando(false);
                        rotuloErro.setText(r.ok() ? "Resposta inesperada do servidor." : r.mensagem());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    definirSalvando(false);
                } catch (ExecutionException e) {
                    definirSalvando(false);
                    Throwable causa = e.getCause();
                    rotuloErro.setText(causa != null && causa.getMessage() != null
                            ? causa.getMessage() : "Falha inesperada na comunicação.");
                }
            }
        }.execute();
    }

    private void definirSalvando(boolean valor) {
        salvando = valor;
        campoNome.setEnabled(!valor);
        campoPreco.setEnabled(!valor);
        campoQuantidade.setEnabled(!valor);
        campoDescricao.setEnabled(!valor);
        comboCategoria.setEnabled(!valor);
        botaoSalvar.setEnabled(!valor);
        botaoCancelar.setEnabled(!valor);
        botaoSalvar.setText(valor ? "Salvando..." : "Salvar Produto");
    }

    // -=- Leitura e validação -=-

    private Requisicao montarRequisicao() throws EntradaInvalidaException {
        String nome = limpar(campoNome.getText());
        if (nome.isEmpty()) {
            throw new EntradaInvalidaException("O nome do produto é obrigatório.");
        }
        if (nome.length() > NOME_MAX) {
            throw new EntradaInvalidaException("O nome deve ter no máximo " + NOME_MAX + " caracteres.");
        }

        Categoria categoria = (Categoria) comboCategoria.getSelectedItem();
        if (categoria == null) {
            throw new EntradaInvalidaException("Selecione a categoria.");
        }

        BigDecimal preco = lerPreco();
        int quantidade = lerQuantidade();

        String descricao = campoDescricao.getText().replace('\t', ' ').strip();
        if (descricao.length() > DESCRICAO_MAX) {
            throw new EntradaInvalidaException(
                    "A descrição deve ter no máximo " + DESCRICAO_MAX + " caracteres (agora: " + descricao.length() + ").");
        }

        Requisicao req = new Requisicao();
        if (produto == null) {
            req.acao = Acao.CRIAR;
        } else {
            req.acao = Acao.EDITAR;
            req.id = produto.id();
        }
        req.categoria = categoria;
        req.nome = nome;
        req.preco = preco;
        req.quantidade = quantidade;
        req.descricao = descricao;
        return req;
    }

    private static String limpar(String texto) {
        return texto.replace('\t', ' ').strip();
    }

    /**
     * Aceita "12,50", "12.50", "R$ 12,50" e "1.234,50".
     */
    private BigDecimal lerPreco() throws EntradaInvalidaException {
        String texto = campoPreco.getText().replace("R$", "").strip();
        if (texto.isEmpty()) {
            throw new EntradaInvalidaException("O preço é obrigatório.");
        }
        if (texto.contains(",")) {
            texto = texto.replace(".", "").replace(',', '.'); // vírgula é o separador decimal
        }
        try {
            BigDecimal valor = new BigDecimal(texto);
            if (valor.signum() < 0 || valor.compareTo(PRECO_MAX) > 0) {
                throw new EntradaInvalidaException("O preço deve estar entre 0,00 e 999.999,99.");
            }
            if (valor.stripTrailingZeros().scale() > 2) {
                throw new EntradaInvalidaException("O preço deve ter no máximo 2 casas decimais.");
            }
            return valor.setScale(2);
        } catch (NumberFormatException e) {
            throw new EntradaInvalidaException("O preço é inválido. Exemplo: 12,50");
        }
    }

    private int lerQuantidade() throws EntradaInvalidaException {
        String texto = campoQuantidade.getText().strip();
        if (texto.isEmpty()) {
            throw new EntradaInvalidaException("A quantidade é obrigatória.");
        }
        try {
            int valor = Integer.parseInt(texto);
            if (valor < 0 || valor > QUANTIDADE_MAX) {
                throw new EntradaInvalidaException("A quantidade deve estar entre 0 e " + QUANTIDADE_MAX + ".");
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new EntradaInvalidaException("A quantidade deve ser um número inteiro.");
        }
    }
}