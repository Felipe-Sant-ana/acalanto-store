package br.pucgoias.pousada.cliente;

import br.pucgoias.pousada.comum.Resumo;

import javax.swing.*;
import java.awt.*;

/**
 * Os 4 cards do topo: total e contagem por categoria.
 */
final class PainelResumo extends JPanel {

    private final Cartao total = new Cartao("Total de Produtos");
    private final Cartao alimentos = new Cartao("Alimentos");
    private final Cartao vestuario = new Cartao("Vestuários");
    private final Cartao diversos = new Cartao("Diversos");

    PainelResumo() {
        super(new GridLayout(1, 4, 12, 0));
        add(total);
        add(alimentos);
        add(vestuario);
        add(diversos);
    }

    void mostrar(Resumo r) {
        total.definir(String.valueOf(r.total()));
        alimentos.definir(String.valueOf(r.alimentos()));
        vestuario.definir(String.valueOf(r.vestuario()));
        diversos.definir(String.valueOf(r.diversos()));
    }

    /**
     * Mostra "–" nos 4 cards. Usado quando o servidor está indisponível.
     */
    void limpar() {
        total.definir("–");
        alimentos.definir("–");
        vestuario.definir("–");
        diversos.definir("–");
    }

    private static final class Cartao extends JPanel {
        private final JLabel numero = new JLabel("–");

        /**
         * Um card: número grande e legenda.
         */
        Cartao(String texto) {
            super(new GridLayout(2, 1, 0, 2));
            setBackground(Tema.FUNDO_CARTAO);
            setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

            numero.setFont(numero.getFont().deriveFont(Font.BOLD, 26f));
            numero.setForeground(Tema.TEXTO);
            JLabel legenda = new JLabel(texto);
            legenda.setForeground(Tema.TEXTO);

            add(numero);
            add(legenda);
        }

        void definir(String valor) {
            numero.setText(valor);
        }
    }
}