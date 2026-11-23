package br.pucgoias.pousada.comum;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Constantes e ferramentas compartilhadas por cliente e servidor: porta, limite de tamanho
 * das mensagens, serialização JSON e versões de TLS aceitas.
 * <p>
 * Formato das mensagens: uma linha de JSON por mensagem, terminada em {@code '\n'}.
 */
public final class Protocolo {

    /**
     * Segurança: apenas TLS 1.3 e 1.2. Versões antigas ficam desabilitadas. Devolve uma cópia nova a cada chamada.
     */
    public static String[] protocolosTls() {
        return new String[]{"TLSv1.3", "TLSv1.2"};
    }

    /**
     * Porta usada quando não há outra nos arquivos de configuração.
     */
    public static final int PORTA_PADRAO = 5000;

    /**
     * Segurança: tamanho máximo de uma mensagem (uma linha), em caracteres. Evita esgotar a memória do servidor.
     */
    public static final int TAMANHO_MAXIMO_MENSAGEM = 16 * 1024;

    /**
     * Gson compacto, sem pretty printing: cada mensagem precisa caber em uma única linha. É thread-safe.
     */
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private Protocolo() {
    }
}