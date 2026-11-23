package br.pucgoias.pousada.servidor;

import br.pucgoias.pousada.comum.Protocolo;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * Monta o {@code SSLServerSocket} a partir do keystore.
 * <p>
 * Papéis no Java: o {@code KeyStore} guarda a chave privada e o certificado do servidor;
 * o {@code KeyManagerFactory} os entrega ao {@code SSLContext}; o
 * {@code SSLContext} fabrica os sockets seguros.
 */
final class FabricaSslServidor {

    private FabricaSslServidor() {
    }

    /**
     * Cria o socket TLS na porta e no endereço informados. Segurança: habilita só TLS 1.3 e 1.2.
     */
    static SSLServerSocket criar(int porta, InetAddress endereco, Path keystore, char[] senha)
            throws IOException, GeneralSecurityException {

        // A mesma senha abre o arquivo e protege a chave privada (padrão do PKCS12).
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(keystore)) {
            ks.load(in, senha);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, senha);

        SSLContext contexto = SSLContext.getInstance("TLS");
        contexto.init(kmf.getKeyManagers(), null, null);

        SSLServerSocket socket = (SSLServerSocket) contexto.getServerSocketFactory()
                .createServerSocket(porta, 50, endereco);
        socket.setEnabledProtocols(Protocolo.protocolosTls());
        return socket;
    }
}