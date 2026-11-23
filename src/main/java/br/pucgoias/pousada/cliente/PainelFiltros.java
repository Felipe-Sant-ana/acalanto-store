package br.pucgoias.pousada.cliente;

import br.pucgoias.pousada.comum.Categoria;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Painel de filtros (categoria, status, preço e quantidade).
 * <p>
 * Os campos podem ser editados à vontade; o que vale para a consulta é o último conjunto
 * aplicado (botão Aplicar ou Enter), guardado em {@code aplicados}. Assim a paginação
 * continua usando os filtros em vigor, e não o que está pela metade nos campos.
 * <p>
 * Validação dupla: o painel valida para dar resposta imediata; o servidor valida de novo.
 */
final class PainelFiltros extends JPanel {

    private static final BigDecimal PRECO_MAX = new BigDecimal("999999.99");
    private static final int QUANTIDADE_MAX = 999_999;

    private static final class FiltroInvalidoException extends Exception {
        FiltroInvalidoException(String mensagem) {
            super(mensagem);
        }
    }

    private final Map<Categoria, JCheckBox> caixasCategoria = new EnumMap<>(Categoria.class);
    private final JCheckBox caixaDisponivel = new JCheckBox("Disponível", true);
    private final JCheckBox caixaIndisponivel = new JCheckBox("Indisponível", true);
    private final JTextField campoPrecoMin = new JTextField();
    private final JTextField campoPrecoMax = new JTextField();
    private final JTextField campoQtdMin = new JTextField();
    private final JTextField campoQtdMax = new JTextField();
    private final JLabel rotuloErro = new JLabel(" ");
    private final Runnable aoMudar;

    private Filtros aplicados = Filtros.NENHUM;

    PainelFiltros(Runnable aoMudar) {
        super(new BorderLayout(0, 12));
        this.aoMudar = aoMudar;
        setBorder(Tema.bordaPainel(12, 14));

        List<JComponent> itensCategoria = new ArrayList<>();
        for (Categoria c : Categoria.values()) {
            JCheckBox caixa = new JCheckBox(c.getRotulo(), true);
            caixasCategoria.put(c, caixa);
            itensCategoria.add(caixa);
        }

        dica(campoPrecoMin, "Mínimo (R$)");
        dica(campoPrecoMax, "Máximo (R$)");
        dica(campoQtdMin, "Mínimo");
        dica(campoQtdMax, "Máximo");

        FiltroDigitacao.apenasValor(campoPrecoMin, 12);
        FiltroDigitacao.apenasValor(campoPrecoMax, 12);
        FiltroDigitacao.apenasDigitos(campoQtdMin, 6);
        FiltroDigitacao.apenasDigitos(campoQtdMax, 6);

        JPanel grupos = new JPanel(new GridLayout(1, 4, 24, 0));
        grupos.add(grupo("Categoria", itensCategoria.toArray(new JComponent[0])));
        grupos.add(grupo("Status", caixaDisponivel, caixaIndisponivel));
        grupos.add(grupo("Preço", campoPrecoMin, campoPrecoMax));
        grupos.add(grupo("Quantidade", campoQtdMin, campoQtdMax));

        JButton botaoLimpar = new JButton("Limpar filtros");
        JButton botaoAplicar = new JButton("Aplicar filtros");
        botaoLimpar.addActionListener(e -> limpar());
        botaoAplicar.addActionListener(e -> aplicar());
        Tema.botaoPrimario(botaoAplicar);

        // Enter dentro de qualquer campo também aplica.
        for (JTextField campo : List.of(campoPrecoMin, campoPrecoMax, campoQtdMin, campoQtdMax)) {
            campo.addActionListener(e -> aplicar());
        }

        rotuloErro.setForeground(Tema.VERMELHO);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.add(botaoLimpar);
        botoes.add(botaoAplicar);
        JPanel rodape = new JPanel(new BorderLayout(12, 0));
        rodape.add(rotuloErro, BorderLayout.WEST);
        rodape.add(botoes, BorderLayout.EAST);

        add(grupos, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
    }

    /**
     * Filtros em vigor na consulta (não o que está digitado nos campos).
     */
    Filtros getAplicados() {
        return aplicados;
    }


    // -=- Ações -=-

    private void aplicar() {
        try {
            aplicados = lerCampos();
            rotuloErro.setText(" ");
            aoMudar.run();
        } catch (FiltroInvalidoException e) {
            rotuloErro.setText(e.getMessage()); // fica no painel; a tabela não muda
        }
    }

    private void limpar() {
        caixasCategoria.values().forEach(c -> c.setSelected(true));
        caixaDisponivel.setSelected(true);
        caixaIndisponivel.setSelected(true);
        for (JTextField campo : List.of(campoPrecoMin, campoPrecoMax, campoQtdMin, campoQtdMax)) {
            campo.setText("");
        }
        rotuloErro.setText(" ");
        aplicados = Filtros.NENHUM;
        aoMudar.run();
    }

    // -=- Leitura e validação -=-


    private Filtros lerCampos() throws FiltroInvalidoException {
        List<Categoria> marcadas = new ArrayList<>();
        // Tudo marcado equivale a "sem filtro": nada é enviado ao servidor para aquele grupo.
        caixasCategoria.forEach((categoria, caixa) -> {
            if (caixa.isSelected()) {
                marcadas.add(categoria);
            }
        });
        if (marcadas.isEmpty()) {
            throw new FiltroInvalidoException("Selecione ao menos uma categoria.");
        }
        List<Categoria> categorias = marcadas.size() == Categoria.values().length ? null : marcadas;

        boolean disp = caixaDisponivel.isSelected();
        boolean indisp = caixaIndisponivel.isSelected();
        if (!disp && !indisp) {
            throw new FiltroInvalidoException("Selecione ao menos um status.");
        }
        Boolean disponivel = (disp && indisp) ? null : disp;

        BigDecimal precoMin = lerPreco(campoPrecoMin, "preço mínimo");
        BigDecimal precoMax = lerPreco(campoPrecoMax, "preço máximo");
        if (precoMin != null && precoMax != null && precoMin.compareTo(precoMax) > 0) {
            throw new FiltroInvalidoException("O preço mínimo não pode ser maior que o máximo.");
        }

        Integer qtdMin = lerQuantidade(campoQtdMin, "quantidade mínima");
        Integer qtdMax = lerQuantidade(campoQtdMax, "quantidade máxima");
        if (qtdMin != null && qtdMax != null && qtdMin > qtdMax) {
            throw new FiltroInvalidoException("A quantidade mínima não pode ser maior que a máxima.");
        }

        return new Filtros(categorias, disponivel, precoMin, precoMax, qtdMin, qtdMax);
    }

    /**
     * Aceita "xx,xx" e "xx.xx". Campo vazio = sem limite.
     */
    private static BigDecimal lerPreco(JTextField campo, String nome) throws FiltroInvalidoException {
        String texto = campo.getText().replace("R$", "").strip().replace(',', '.');
        if (texto.isEmpty()) {
            return null;
        }
        try {
            BigDecimal valor = new BigDecimal(texto);
            if (valor.signum() < 0 || valor.compareTo(PRECO_MAX) > 0) {
                throw new FiltroInvalidoException("O " + nome + " deve estar entre 0,00 e 999999,99.");
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new FiltroInvalidoException("O " + nome + " é inválido.");
        }
    }

    private static Integer lerQuantidade(JTextField campo, String nome) throws FiltroInvalidoException {
        String texto = campo.getText().strip();
        if (texto.isEmpty()) {
            return null;
        }
        try {
            int valor = Integer.parseInt(texto);
            if (valor < 0 || valor > QUANTIDADE_MAX) {
                throw new FiltroInvalidoException("A " + nome + " deve estar entre 0 e " + QUANTIDADE_MAX + ".");
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new FiltroInvalidoException("A " + nome + " é inválida (use número inteiro).");
        }
    }

    // -=- Montagem -=-

    private static void dica(JTextField campo, String texto) {
        campo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, texto);
    }

    /**
     * Título em negrito seguido dos componentes, empilhados na vertical.
     */
    private static JPanel grupo(String titulo, JComponent... itens) {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));

        JLabel rotulo = new JLabel(titulo);
        rotulo.setFont(rotulo.getFont().deriveFont(Font.BOLD));
        rotulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        rotulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        painel.add(rotulo);

        for (JComponent item : itens) {
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            painel.add(item);
            painel.add(javax.swing.Box.createVerticalStrut(4));
        }
        return painel;
    }
}