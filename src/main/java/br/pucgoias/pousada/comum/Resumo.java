package br.pucgoias.pousada.comum;

/**
 * Números dos 4 cards do topo da tela: total e quantidade por categoria.
 */
public record Resumo(int total, int alimentos, int vestuario, int diversos) {
}