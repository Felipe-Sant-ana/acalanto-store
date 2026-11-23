package br.pucgoias.pousada.cliente;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Ícone que reduz uma imagem com boa qualidade. A imagem é pré-reduzida (de metade em metade)
 * até ficar com 3x a altura exibida, e na pintura é reduzida com interpolação bicúbica.
 * O fator 3 deixa pixels de sobra para telas com escala de 125% a 300%.
 */
final class IconeImagem implements Icon {

    private final BufferedImage imagem;
    private final int largura;
    private final int altura;

    private IconeImagem(BufferedImage imagem, int altura) {
        this.imagem = imagem;
        this.altura = altura;
        this.largura = Math.max(1, imagem.getWidth() * altura / imagem.getHeight());
    }

    /**
     * Retorna null se o recurso não existir ou não for uma imagem válida.
     */
    static IconeImagem carregar(String recurso, int altura) {
        try (InputStream in = IconeImagem.class.getResourceAsStream(recurso)) {
            if (in == null) {
                return null;
            }
            BufferedImage original = ImageIO.read(in);
            if (original == null || original.getHeight() <= 0) {
                return null;
            }
            return new IconeImagem(preReduzir(original, altura * 3), altura);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Reduz pela metade enquanto a imagem continuar maior que o limite. Reduzir de uma vez só causa serrilhado.
     */
    private static BufferedImage preReduzir(BufferedImage origem, int alturaLimite) {
        BufferedImage atual = origem;
        while (atual.getHeight() / 2 >= alturaLimite) {
            int w = Math.max(1, atual.getWidth() / 2);
            int h = atual.getHeight() / 2;
            BufferedImage metade = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = metade.createGraphics();
            qualidade(g);
            g.drawImage(atual, 0, 0, w, h, null);
            g.dispose();
            atual = metade;
        }
        return atual;
    }

    private static void qualidade(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            qualidade(g2);
            g2.drawImage(imagem, x, y, largura, altura, null);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return largura;
    }

    @Override
    public int getIconHeight() {
        return altura;
    }
}