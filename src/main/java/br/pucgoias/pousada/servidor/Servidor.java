package br.pucgoias.pousada.servidor;

import br.pucgoias.pousada.comum.Configuracao;
import br.pucgoias.pousada.comum.Protocolo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Ponto de entrada do servidor.
 * <p>
 * Carrega os produtos, abre um {@code SSLServerSocket} e entrega cada conexão recebida
 * a uma thread de um pool de tamanho fixo ({@link TratadorCliente}).
 * <p>
 * Segurança: o número de clientes simultâneos é limitado, para que o servidor não crie
 * threads sem limite. A configuração (keystore, endereço, porta) vem de
 * {@code config/servidor.properties}. Por padrão escuta só em 127.0.0.1.
 */
public class Servidor {

    private static final Logger LOG = Logger.getLogger(Servidor.class.getName());
    private static final int MAX_CLIENTES_SIMULTANEOS = 16; // Os demais clientes esperam na fila até uma thread ficar livre.

    public static void main(String[] args) {
        ProdutoRepositorio repositorio;

        // Se o JSON estiver inválido, o servidor recusa subir.
        try {
            repositorio = new ProdutoRepositorio(Path.of("dados", "produtos.json"));
        } catch (RuntimeException e) {
            LOG.severe("Não foi possível iniciar o servidor: " + e.getMessage());
            return;
        }

        ProcessadorRequisicoes processador = new ProcessadorRequisicoes(repositorio);
        ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTES_SIMULTANEOS);

        try (ServerSocket servidor = criarServerSocket()) {
            LOG.info("Servidor TLS aguardando conexões em " + servidor.getLocalSocketAddress());
            while (true) {
                Socket cliente = servidor.accept();
                pool.execute(new TratadorCliente(cliente, processador));
            }
        } catch (IOException | GeneralSecurityException | RuntimeException e) {
            LOG.severe("Falha no servidor: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }

    /**
     * Lê a configuração e cria o socket protegido por TLS. O endereço padrão é loopback; a porta padrão vem de {@link Protocolo}.
     */
    private static ServerSocket criarServerSocket() throws IOException, GeneralSecurityException {
        Configuracao cfg = Configuracao.carregar(Path.of("config", "servidor.properties"));
        Path keystore = Path.of(cfg.obrigatorio("keystore.arquivo"));
        char[] senha = cfg.obrigatorio("keystore.senha").toCharArray();
        InetAddress endereco = InetAddress.getByName(cfg.opcional("endereco", "127.0.0.1"));
        int porta = Integer.parseInt(cfg.opcional("porta", String.valueOf(Protocolo.PORTA_PADRAO)));
        return FabricaSslServidor.criar(porta, endereco, keystore, senha);
    }
}