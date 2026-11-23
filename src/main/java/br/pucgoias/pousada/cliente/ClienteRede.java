package br.pucgoias.pousada.cliente;

import br.pucgoias.pousada.comum.Configuracao;
import br.pucgoias.pousada.comum.Protocolo;
import br.pucgoias.pousada.comum.Requisicao;
import br.pucgoias.pousada.comum.Resposta;
import com.google.gson.JsonParseException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

/**
 * Camada de comunicação do cliente. Cada chamada a {@link #enviar} abre uma conexão TLS,
 * troca uma mensagem e fecha: o custo do handshake em rede local é desprezível e não
 * sobra conexão esquecida. Não toca na interface, então pode ser usada em qualquer thread.
 * <p>
 * Todas as falhas viram {@code IOException} com mensagem pronta para o usuário.
 */
public final class ClienteRede {

    private static final int TIMEOUT_MS = 10_000; // Tempo máximo para conectar e para aguardar a resposta.

    private final String host;
    private final int porta;
    private final Path truststore;
    private final char[] senha;

    private ClienteRede(String host, int porta, Path truststore, char[] senha) {
        this.host = host;
        this.porta = porta;
        this.truststore = truststore;
        this.senha = senha;
    }

    public static ClienteRede daConfiguracao(Path arquivo) throws IOException {
        Configuracao cfg = Configuracao.carregar(arquivo);
        String host = cfg.opcional("servidor.host", "localhost");
        int porta = Integer.parseInt(cfg.opcional("servidor.porta", String.valueOf(Protocolo.PORTA_PADRAO)));
        Path truststore = Path.of(cfg.obrigatorio("truststore.arquivo"));
        char[] senha = cfg.obrigatorio("truststore.senha").toCharArray();
        return new ClienteRede(host, porta, truststore, senha);
    }

    public Resposta enviar(Requisicao requisicao) throws IOException {
        try (SSLSocket socket = FabricaSslCliente.conectar(host, porta, truststore, senha, TIMEOUT_MS);
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter saida = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            saida.write(Protocolo.GSON.toJson(requisicao));
            saida.write('\n');
            saida.flush();

            String linha = entrada.readLine();
            if (linha == null) {
                throw new IOException("O servidor encerrou a conexão sem responder.");
            }
            Resposta resposta = Protocolo.GSON.fromJson(linha, Resposta.class);
            if (resposta == null) {
                throw new IOException("Resposta vazia do servidor.");
            }
            return resposta;

        } catch (SocketTimeoutException e) {
            // O servidor demorou para conectar ou enviar a resposta.
            throw new IOException("O servidor demorou demais para responder.", e);
        } catch (ConnectException e) {
            // Servidor desligado ou porta errada.
            throw new IOException("Não foi possível conectar ao servidor. Ele está em execução?", e);
        } catch (SSLException e) {
            // Falha durante o estabelecimento ou uso da conexão TLS.
            throw new IOException("Falha na conexão segura (TLS): " + e.getMessage(), e);
        } catch (GeneralSecurityException e) {
            // Erro ao configurar os recursos de segurança da conexão.
            throw new IOException("Falha ao preparar a conexão segura: " + e.getMessage(), e);
        } catch (JsonParseException e) {
            // A resposta recebida não está em um formato JSON válido.
            throw new IOException("Resposta inválida do servidor.", e);
        }
    }
}