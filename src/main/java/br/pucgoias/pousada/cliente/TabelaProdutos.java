package br.pucgoias.pousada.cliente;

import br.pucgoias.pousada.comum.Produto;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.awt.geom.RoundRectangle2D;

/**
 * Tabela de produtos. O modelo guarda os dados; os renderizadores só desenham as células.
 * <p>
 * A coluna Ações não usa botões: ela apenas desenha "Editar" e "Excluir", e o clique é
 * tratado pela própria tabela (metade esquerda edita, metade direita exclui). Isso evita
 * o editor de célula.
 */
public class TabelaProdutos extends JTable {

    private static final int COLUNA_ACOES = 4;  // Índice da coluna Ações (a última).
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    // Produto que deve receber o destaque animado.
    private long idDestaque = -1;

    // Intensidade do destaque: 0 = normal, 1 = destaque máximo.
    private float intensidade = 0f;

    // Timer da animação, executado na Event Dispatch Thread.
    private Timer animacao;

    // Controla as transições da animação.
    private int passo;

    private static final int LARGURA_BOTAO = 64;
    private static final int ALTURA_BOTAO = 24;
    private static final int ESPACO_BOTOES = 8;

    // Índices da linha e do botão sob o cursor.
    private int linhaHover = -1;
    private int botaoHover = -1;       // 0 = Editar, 1 = Excluir, -1 = nenhum

    private final ModeloProdutos modelo;
    private Consumer<Produto> aoEditar;
    private Consumer<Produto> aoExcluir;

    public TabelaProdutos() {
        this(new ModeloProdutos());
    }

    /** Retângulos dos botões Editar e Excluir, centralizados na célula. Índice 0 = Editar, 1 = Excluir. */
    private static Rectangle[] retangulosAcoes(Rectangle celula) {
        int total = LARGURA_BOTAO * 2 + ESPACO_BOTOES;
        int x = celula.x + (celula.width - total) / 2;
        int y = celula.y + (celula.height - ALTURA_BOTAO) / 2;
        return new Rectangle[]{
                new Rectangle(x, y, LARGURA_BOTAO, ALTURA_BOTAO),
                new Rectangle(x + LARGURA_BOTAO + ESPACO_BOTOES, y, LARGURA_BOTAO, ALTURA_BOTAO)
        };
    }

    /** Fundo da linha: piscar > hover > faixa alternada > padrão. */
    Color fundoDaLinha(int linha) {
        Color base = (linha == linhaHover)
                ? Tema.LINHA_HOVER
                : (linha % 2 == 0 ? getBackground() : Tema.LINHA_ALTERNADA);
        if (intensidade > 0f && idDestaque >= 0 && linha < getRowCount()
                && produtoNaLinha(linha).id() == idDestaque) {
            return misturar(base, Tema.DESTAQUE, intensidade);
        }
        return base;
    }

    /** Mistura duas cores: fracao 0 = só a primeira, 1 = só a segunda. */
    private static Color misturar(Color a, Color b, float fracao) {
        int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * fracao);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * fracao);
        int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * fracao);
        return new Color(r, g, bl);
    }

    /** Pede para a linha do produto piscar. Se ele não estiver na página exibida, nada acontece. */
    public void destacar(long id) {
        if (!estaNaPagina(id)) {
            return;
        }
        pararAnimacao();
        idDestaque = id;
        passo = 0;
        // 3 piscadas = 6 transições (sobe, desce, sobe, desce...) a cada 150 ms ≈ 0,9 s no total
        animacao = new Timer(150, e -> {
            passo++;
            intensidade = (passo % 2 == 1) ? 1f : 0f;
            if (passo >= 6) {
                pararAnimacao();
            }
            repaint();
        });
        animacao.start();
    }

    private boolean estaNaPagina(long id) {
        for (int i = 0; i < getRowCount(); i++) {
            if (produtoNaLinha(i).id() == id) {
                return true;
            }
        }
        return false;
    }

    private void pararAnimacao() {
        if (animacao != null) {
            animacao.stop();
            animacao = null;
        }
        intensidade = 0f;
        idDestaque = -1;
    }

    /**
     * Atualiza a linha destacada somente quando ela muda.
     */
    private void definirLinhaHover(int linha) {
        if (linha != linhaHover) {
            linhaHover = linha;
            repaint();
        }
    }

    /**
     * Identifica qual botão está sob o cursor.
     * Retorna 0 para Editar, 1 para Excluir ou -1.
     */
    private int botaoNoPonto(Point ponto) {
        int linha = rowAtPoint(ponto);
        int coluna = columnAtPoint(ponto);
        if (linha < 0 || coluna != COLUNA_ACOES) {
            return -1;
        }
        Rectangle[] botoes = retangulosAcoes(getCellRect(linha, coluna, false));
        for (int i = 0; i < botoes.length; i++) {
            if (botoes[i].contains(ponto)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Atualiza o botão destacado somente quando ele muda.
     */
    private void definirBotaoHover(int botao) {
        if (botao != botaoHover) {
            botaoHover = botao;
            repaint();
        }
    }

    private TabelaProdutos(ModeloProdutos modelo) {
        super(modelo);
        this.modelo = modelo;

        setRowHeight(34);
        setRowSelectionAllowed(false); // As ações não dependem de linha selecionada, e o destaque atrapalharia as cores.
        setFillsViewportHeight(true);
        setShowHorizontalLines(false);
        setShowVerticalLines(false);
        setIntercellSpacing(new Dimension(0, 1));
        getTableHeader().setReorderingAllowed(false);
        // Linhas zebradas: o FlatLaf alterna a cor de fundo usando "Table.alternateRowColor".
        putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines: false");

        TableColumnModel colunas = getColumnModel();
        DefaultTableCellRenderer centralizado = new RenderizadorCentralizado();
        colunas.getColumn(1).setCellRenderer(new RenderizadorMoeda());
        colunas.getColumn(2).setCellRenderer(centralizado);
        colunas.getColumn(3).setCellRenderer(new RenderizadorStatus());
        colunas.getColumn(COLUNA_ACOES).setCellRenderer(new RenderizadorAcoes());
        colunas.getColumn(0).setCellRenderer(centralizado);

        colunas.getColumn(0).setPreferredWidth(440);
        colunas.getColumn(1).setPreferredWidth(100);
        colunas.getColumn(2).setPreferredWidth(120);
        colunas.getColumn(3).setPreferredWidth(110);
        colunas.getColumn(COLUNA_ACOES).setPreferredWidth(170);

        instalarCliqueNasAcoes();
    }

    public void setProdutos(List<Produto> produtos) {
        linhaHover = -1;
        pararAnimacao();
        modelo.definir(produtos);
    }

    /**
     * Define o que acontece ao clicar em "Editar" e em "Excluir".
     */
    public void setAcoes(Consumer<Produto> aoEditar, Consumer<Produto> aoExcluir) {
        this.aoEditar = aoEditar;
        this.aoExcluir = aoExcluir;
    }

    public Produto produtoNaLinha(int linhaVisivel) {
        return modelo.get(convertRowIndexToModel(linhaVisivel));
    }

    /**
     * A coluna Ações só é desenhada (o renderizador não recebe cliques). Por isso o clique é
     * tratado aqui: metade esquerda da célula = Editar, metade direita = Excluir.
     */
    private void instalarCliqueNasAcoes() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                int botao = botaoNoPonto(e.getPoint());
                if (botao < 0) {
                    return; // clique fora dos botões
                }
                Produto produto = produtoNaLinha(rowAtPoint(e.getPoint()));
                Consumer<Produto> acao = botao == 0 ? aoEditar : aoExcluir;
                if (acao != null) {
                    acao.accept(produto);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                definirLinhaHover(-1);
                definirBotaoHover(-1);
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int linha = rowAtPoint(e.getPoint());
                definirLinhaHover(linha);
                int botao = botaoNoPonto(e.getPoint());
                definirBotaoHover(botao);
                setCursor(botao >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
    }

    // -=- Modelo -=-

    private static final class ModeloProdutos extends AbstractTableModel {
        private static final String[] COLUNAS = {"Nome", "Preço", "Qtd. Disponível", "Status", "Ações"};

        private List<Produto> produtos = List.of();

        void definir(List<Produto> novos) {
            this.produtos = List.copyOf(novos);
            fireTableDataChanged(); // avisa a JTable para se redesenhar
        }

        Produto get(int indice) {
            return produtos.get(indice);
        }

        @Override
        public int getRowCount() {
            return produtos.size();
        }

        @Override
        public int getColumnCount() {
            return COLUNAS.length;
        }

        @Override
        public String getColumnName(int coluna) {
            return COLUNAS[coluna];
        }

        @Override
        public Object getValueAt(int linha, int coluna) {
            Produto p = produtos.get(linha);
            return switch (coluna) {
                case 0 -> p.nome();
                case 1 -> p.preco();
                case 2 -> p.quantidade();
                case 3 -> p.disponivel();
                default -> null; // coluna Ações não tem valor: é só desenho
            };
        }
        // isCellEditable() não é sobrescrito: por padrão nenhuma célula é editável.
    }

    // -=- Renderizadores -=-

    /** Base dos renderizadores de texto: aplica o fundo da linha (hover ou faixa). */
    private static class RenderizadorCentralizado extends DefaultTableCellRenderer {
        RenderizadorCentralizado() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionada,
                                                       boolean foco, int linha, int coluna) {
            Component c = super.getTableCellRendererComponent(tabela, valor, selecionada, foco, linha, coluna);
            setBackground(((TabelaProdutos) tabela).fundoDaLinha(linha));
            return c;
        }
    }

    private static class RenderizadorMoeda extends RenderizadorCentralizado {
        @Override
        protected void setValue(Object valor) {
            setText(valor instanceof BigDecimal b ? MOEDA.format(b) : "");
        }
    }

    /**
     * Mostra Disponível (verde) ou Indisponível (vermelho), em negrito.
     */
    private static class RenderizadorStatus extends RenderizadorCentralizado {
        @Override
        public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionada,
                                                       boolean foco, int linha, int coluna) {
            Component c = super.getTableCellRendererComponent(tabela, valor, selecionada, foco, linha, coluna);
            boolean disponivel = Boolean.TRUE.equals(valor);
            setText(disponivel ? "Disponível" : "Indisponível");
            setFont(getFont().deriveFont(Font.BOLD));
            if (!selecionada) {
                setForeground(disponivel ? Tema.VERDE : Tema.VERMELHO);
            }
            return c;
        }
    }

    /** Desenha "Editar" e "Excluir" como botões arredondados. O clique é tratado pela tabela. */
    private static final class RenderizadorAcoes extends JPanel implements TableCellRenderer {
        private TabelaProdutos tabela;
        private int linha;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean selecionada,
                                                       boolean foco, int linha, int coluna) {
            this.tabela = (TabelaProdutos) t;
            this.linha = linha;
            setOpaque(true);
            setBackground(tabela.fundoDaLinha(linha));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // pinta o fundo da linha
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Rectangle[] botoes = retangulosAcoes(new Rectangle(0, 0, getWidth(), getHeight()));
                boolean linhaComHover = tabela != null && linha == tabela.linhaHover;
                desenhar(g2, botoes[0], "Editar", Tema.TEXTO, Tema.CAMPO, Tema.CAMPO,
                        linhaComHover && tabela.botaoHover == 0);
                desenhar(g2, botoes[1], "Excluir", Tema.VERMELHO, null, Tema.VERMELHO,
                        linhaComHover && tabela.botaoHover == 1);
            } finally {
                g2.dispose();
            }
        }

        /** Desenha um botão: fundo (opcional), borda e texto centralizado. Com hover, fica mais claro. */
        private void desenhar(Graphics2D g2, Rectangle r, String texto, Color corTexto,
                              Color corFundo, Color corBorda, boolean hover) {
            RoundRectangle2D forma = new RoundRectangle2D.Double(r.x + 0.5, r.y + 0.5, r.width - 1, r.height - 1, 8, 8);
            if (corFundo != null) {
                g2.setColor(hover ? corFundo.brighter() : corFundo);
                g2.fill(forma);
            } else if (hover) {
                g2.setColor(new Color(corBorda.getRed(), corBorda.getGreen(), corBorda.getBlue(), 40));
                g2.fill(forma); // botão vazado ganha um véu da própria cor ao passar o mouse
            }
            g2.setColor(corBorda);
            g2.draw(forma);

            g2.setColor(corTexto);
            FontMetrics fm = g2.getFontMetrics(getFont());
            int tx = r.x + (r.width - fm.stringWidth(texto)) / 2;
            int ty = r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent();
            g2.setFont(getFont());
            g2.drawString(texto, tx, ty);
        }
    }
}