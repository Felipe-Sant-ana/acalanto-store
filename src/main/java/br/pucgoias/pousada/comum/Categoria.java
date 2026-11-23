package br.pucgoias.pousada.comum;

/**
 * Categorias de produto vendidas pela pousada.
 * <p>
 * O nome do valor ({@code VESTUARIO}) é o que vai no JSON; o rótulo ({@code "Vestuário"})
 * é só para exibir ao usuário.
 * <p>
 * A ordem de declaração é também a ordem de exibição na listagem.
 */
public enum Categoria {
    ALIMENTOS("Alimentos"),
    VESTUARIO("Vestuário"),
    DIVERSOS("Diversos");

    private final String rotulo;

    Categoria(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /**
     * Texto exibido pelos componentes Swing (ex.: JComboBox).
     */
    @Override
    public String toString() {
        return rotulo;
    }
}