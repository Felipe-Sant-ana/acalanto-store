package br.pucgoias.pousada.servidor;

import br.pucgoias.pousada.comum.Protocolo;
import br.pucgoias.pousada.comum.Requisicao;
import br.pucgoias.pousada.comum.Resposta;
import com.google.gson.JsonParseException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Atende uma conexão de cliente, em uma thread própria do pool.
 * <p>
 * Ciclo: negocia o TLS, lê uma linha JSON, processa, responde com uma linha JSON e repete
 * até o cliente fechar a conexão. Esta classe só cuida da comunicação: as regras de
 * negócio ficam em {@link ProcessadorRequisicoes}.
 * <p>
 * Segurança: timeout de handshake (10 s) e de inatividade (60 s), limite de tamanho por
 * mensagem e respostas de erro sem detalhes internos.
 */
public class TratadorCliente implements Runnable {

    private static final Logger LOG = Logger.getLogger(TratadorCliente.class.getName());
    private static final int TIMEOUT_OCIOSO_MS = 60_000;    // Conexão parada além disso é encerrada.
    private static final int TIMEOUT_HANDSHAKE_MS = 10_000; // Curto: quem conecta e não negocia não pode ocupar uma thread por muito tempo.

    private final Socket socket;
    private final ProcessadorRequisicoes processador;

    public TratadorCliente(Socket socket, ProcessadorRequisicoes processador) {
        this.socket = socket;
        this.processador = processador;
    }

    /**
     * Executa o ciclo de atendimento. O handshake é feito aqui, e não no accept(), para que um cliente lento não trave o servidor inteiro.
     */
    @Override
    public void run() {
        String origem = String.valueOf(socket.getRemoteSocketAddress());
        LOG.info("Conexão aberta: " + origem);

        try (Socket s = socket;
             Reader entrada = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             Writer saida = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))) {

            if (s instanceof SSLSocket ssl) {
                s.setSoTimeout(TIMEOUT_HANDSHAKE_MS);
                ssl.startHandshake();
                SSLSession sessao = ssl.getSession();
                LOG.info("TLS negociado com " + origem + ": "
                        + sessao.getProtocol() + " / " + sessao.getCipherSuite());
            }
            s.setSoTimeout(TIMEOUT_OCIOSO_MS);

            while (true) {
                String linha;
                try {
                    linha = lerLinha(entrada);
                } catch (MensagemGrandeException e) {
                    enviar(saida, Resposta.erro("Mensagem excede o tamanho máximo permitido."));
                    LOG.warning("Mensagem acima do limite de " + origem + "; conexão encerrada.");
                    break;
                }
                if (linha == null) {
                    break; // cliente fechou a conexão
                }
                enviar(saida, processarLinha(linha));
            }
            // SSLException e SocketTimeoutException são subclasses de IOException: devem vir antes dela.
        } catch (SocketTimeoutException e) {
            LOG.info("Conexão encerrada por inatividade: " + origem);
            return;
        } catch (SSLException e) {
            LOG.warning("Falha TLS com " + origem + ": " + e.getMessage());
            return;
        } catch (IOException e) {
            LOG.warning("Falha de comunicação com " + origem + ": " + e.getMessage());
            return;
        }
        LOG.info("Conexão encerrada: " + origem);
    }

    /**
     * Converte a linha em Requisicao e a entrega ao processador. JSON inválido vira resposta de erro, sem derrubar a conexão.
     */
    private Resposta processarLinha(String linha) {
        if (linha.isBlank()) {
            return Resposta.erro("Mensagem vazia.");
        }
        Requisicao req;
        try {
            req = Protocolo.GSON.fromJson(linha, Requisicao.class);
        } catch (JsonParseException e) {
            LOG.fine("JSON inválido recebido: " + e.getMessage());
            return Resposta.erro("Mensagem JSON inválida.");
        }
        if (req == null) {
            return Resposta.erro("Mensagem JSON inválida.");
        }
        return processador.processar(req);
    }

    /**
     * Escreve a resposta como uma linha JSON e descarrega o buffer.
     */
    private static void enviar(Writer saida, Resposta resposta) throws IOException {
        saida.write(Protocolo.GSON.toJson(resposta));
        saida.write('\n');
        saida.flush();
    }

    /**
     * Lê até o '\n'. Diferente de BufferedReader.readLine(), recusa linhas acima do limite,
     * para que um cliente malicioso não consiga esgotar a memória do servidor.
     * Retorna null no fim da conexão.
     */
    private static String lerLinha(Reader entrada) throws IOException, MensagemGrandeException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = entrada.read()) != -1) {
            if (c == '\n') {
                return sb.toString();
            }
            if (c == '\r') {
                continue;
            }
            if (sb.length() >= Protocolo.TAMANHO_MAXIMO_MENSAGEM) {
                throw new MensagemGrandeException();
            }
            sb.append((char) c);
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private static class MensagemGrandeException extends Exception {
        // Sinaliza mensagem acima do limite. A conexão é encerrada, pois não dá para saber onde começa a próxima.
    }
}