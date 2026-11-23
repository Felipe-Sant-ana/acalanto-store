package br.pucgoias.pousada.cliente;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import javax.swing.JTextField;

/**
 * Restringe o que pode ser digitado (ou colado) em um campo de texto, por expressão regular.
 * <p>
 * É só uma comodidade de uso: a validação de verdade continua no formulário e no servidor,
 * pois o texto ainda pode ter formato inválido (ex.: "1,2,3").
 */
final class FiltroDigitacao extends DocumentFilter {

    private final String permitido;  // expressão regular que o texto inserido deve satisfazer
    private final int maximo;        // tamanho máximo do campo

    private FiltroDigitacao(String permitido, int maximo) {
        this.permitido = permitido;
        this.maximo = maximo;
    }

    /** Campo aceita somente dígitos (ex.: quantidade). */
    static void apenasDigitos(JTextField campo, int maximo) {
        instalar(campo, new FiltroDigitacao("\\d*", maximo));
    }

    /** Campo aceita dígitos, ponto e vírgula (ex.: preço). */
    static void apenasValor(JTextField campo, int maximo) {
        instalar(campo, new FiltroDigitacao("[\\d.,]*", maximo));
    }

    private static void instalar(JTextField campo, FiltroDigitacao filtro) {
        ((PlainDocument) campo.getDocument()).setDocumentFilter(filtro);
    }

    @Override
    public void insertString(FilterBypass fb, int posicao, String texto, AttributeSet atributos)
            throws BadLocationException {
        replace(fb, posicao, 0, texto, atributos);
    }

    @Override
    public void replace(FilterBypass fb, int posicao, int tamanho, String texto, AttributeSet atributos)
            throws BadLocationException {
        String novo = texto == null ? "" : texto;
        if (!novo.matches(permitido)) {
            return; // caractere recusado: nada é inserido
        }
        int tamanhoFinal = fb.getDocument().getLength() - tamanho + novo.length();
        if (tamanhoFinal > maximo) {
            return; // passaria do limite do campo
        }
        super.replace(fb, posicao, tamanho, novo, atributos);
    }
}