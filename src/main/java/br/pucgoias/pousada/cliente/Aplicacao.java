package br.pucgoias.pousada.cliente;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Ponto de entrada do cliente. Instala o tema, lê {@code config/cliente.properties} e
 * abre a janela principal. O servidor precisa estar em execução.
 */
public class Aplicacao {

    // O tema precisa ser instalado antes de qualquer componente existir.
    public static void main(String[] args) {
        javax.swing.JFrame.setDefaultLookAndFeelDecorated(true);
        javax.swing.JDialog.setDefaultLookAndFeelDecorated(true);
        Tema.instalar();
        SwingUtilities.invokeLater(Aplicacao::iniciar); // A interface é criada e alterada somente na Event Dispatch Thread.
    }

    private static void iniciar() {
        ClienteRede rede;
        try {
            rede = ClienteRede.daConfiguracao(Path.of("config", "cliente.properties"));
        } catch (IOException | RuntimeException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(),
                    "Erro de configuração", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        new JanelaPrincipal(rede).setVisible(true);
    }
}