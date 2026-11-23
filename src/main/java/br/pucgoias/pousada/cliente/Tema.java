package br.pucgoias.pousada.cliente;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Centraliza a configuração visual da aplicação cliente.
 * Define a paleta de cores, instala o tema Dark do FlatLaf
 * e disponibiliza utilidades para estilização de componentes.
 */
final class Tema {

    // Cores de fundo dos componentes.
    static final Color FUNDO = Color.decode("#0F1727");         // fundo principal
    static final Color FUNDO_CARTAO = Color.decode("#182239");  // cards de resumo e tabela
    static final Color CAMPO = Color.decode("#242F48");         // busca, filtro, campos e botões
    static final Color LINHA_ALTERNADA = Color.decode("#1D2842");   // faixa alternada da tabela
    static final Color LINHA_HOVER = Color.decode("#26335A");       // linha sob o cursor
    static final Color DESTAQUE = Color.decode("#2F5BE0");          // piscar ao cadastrar/editar

    // Cores dos textos.
    static final Color TEXTO = Color.decode("#FBFCFD");
    static final Color TEXTO_SECUNDARIO = Color.decode("#B7BBC2");

    // Cores utilizadas para indicar estados e ações.
    static final Color VERMELHO = Color.decode("#F85262");      // erros, "Indisponível" e "Excluir"
    static final Color VERDE = Color.decode("#12F39E");         // "Disponível"
    static final Color PRIMARIO = Color.decode("#163CB8");      // Novo Produto, Aplicar, Salvar

    private Tema() {
    }

    /**
     * Chamado uma única vez, antes de criar qualquer janela.
     */
    static void instalar() {
        Map<String, String> p = new HashMap<>();

        // Variáveis-base do FlatLaf: quase tudo (painéis, diálogos, rótulos) deriva delas.
        p.put("@background", hex(FUNDO));
        p.put("@foreground", hex(TEXTO));
        p.put("@componentBackground", hex(CAMPO));
        p.put("@buttonBackground", hex(CAMPO));
        p.put("@accentColor", hex(PRIMARIO));

        // Bordas e botões
        p.put("Component.borderColor", hex(CAMPO));
        p.put("Button.borderColor", hex(CAMPO));
        p.put("Button.background", hex(CAMPO));
        p.put("Button.foreground", hex(TEXTO));
        p.put("ToggleButton.background", hex(CAMPO));
        p.put("ToggleButton.foreground", hex(TEXTO));
        p.put("ToggleButton.selectedBackground", hex(CAMPO));
        p.put("ToggleButton.selectedForeground", hex(TEXTO));
        p.put("Button.default.background", hex(PRIMARIO));
        p.put("Button.default.foreground", hex(TEXTO));
        p.put("Button.default.borderColor", hex(PRIMARIO));

        // Tabela
        p.put("Table.background", hex(FUNDO_CARTAO));
        p.put("Table.foreground", hex(TEXTO));
        p.put("Table.gridColor", hex(CAMPO));
        p.put("TableHeader.background", hex(FUNDO_CARTAO));
        p.put("TableHeader.foreground", hex(TEXTO));
        p.put("TableHeader.bottomSeparatorColor", hex(CAMPO));
        p.put("Table.alternateRowColor", hex(LINHA_ALTERNADA));

        // Cantos arredondados
        p.put("Button.arc", "10");
        p.put("Component.arc", "10");
        p.put("TextComponent.arc", "10");

        FlatLaf.setGlobalExtraDefaults(p);
        FlatDarkLaf.setup();
    }

    /**
     * Linha fina na cor de borda do tema, com espaço interno.
     */
    static Border bordaPainel(int vertical, int horizontal) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CAMPO),
                BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal));
    }

    /**
     * Botão de ação principal
     */
    static void botaoPrimario(JButton botao) {
        botao.putClientProperty(FlatClientProperties.STYLE,
                "background: " + hex(PRIMARIO) + "; foreground: " + hex(TEXTO) + ";"
                        + "hoverBackground: lighten(" + hex(PRIMARIO) + ", 8%);"
                        + "pressedBackground: darken(" + hex(PRIMARIO) + ", 8%)");
    }

    /**
     * Converte uma cor para sua representação hexadecimal.
     * <p>
     * O resultado segue o formato "#RRGGBB", utilizado
     * nas propriedades de estilo do FlatLaf.
     *
     * @param cor cor que será convertida
     * @return representação hexadecimal da cor
     */
    static String hex(Color cor) {
        return String.format("#%06X", cor.getRGB() & 0xFFFFFF);
    }
}