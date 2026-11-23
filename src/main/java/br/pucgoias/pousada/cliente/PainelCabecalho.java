package br.pucgoias.pousada.cliente;

import javax.swing.*;
import java.awt.*;

/**
 * Logo e saudação.
 */
final class PainelCabecalho extends JPanel {

    private static final int ALTURA_LOGO = 48;

    PainelCabecalho() {
        super(new BorderLayout(16, 0));

        IconeImagem logo = IconeImagem.carregar("/imagens/logo-dark.png", ALTURA_LOGO);
        if (logo != null) {
            add(new JLabel(logo), BorderLayout.WEST);
        }

        JLabel titulo = new JLabel("Olá, proprietário!");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));
        JLabel subtitulo = new JLabel("Gerencie os produtos da sua pousada.");
        subtitulo.setForeground(Tema.TEXTO_SECUNDARIO);

        JPanel textos = new JPanel(new GridLayout(0, 1));
        textos.add(titulo);
        textos.add(subtitulo);
        add(textos, BorderLayout.CENTER);
    }
}