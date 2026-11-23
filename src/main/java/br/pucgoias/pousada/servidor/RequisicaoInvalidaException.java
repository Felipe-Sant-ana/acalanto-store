package br.pucgoias.pousada.servidor;

/**
 * Erro de validação causado por dado do cliente. A mensagem é escrita para o usuário
 * ler: nunca coloque nela detalhes internos (caminhos, stack traces).
 */
public class RequisicaoInvalidaException extends Exception {
    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}