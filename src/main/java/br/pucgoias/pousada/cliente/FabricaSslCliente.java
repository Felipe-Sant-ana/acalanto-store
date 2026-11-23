package br.pucgoias.pousada.cliente;

import br.pucgoias.pousada.comum.Protocolo;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * Abre a conexão TLS do cliente.
 * <p>
 * Segurança: o cliente confia somente nos certificados do truststore (o do nosso
 * servidor), e não em todas as autoridades do Java, e confere se o nome do servidor
 * consta no certificado (SAN). Isso impede aceitar um certificado válido emitido para outro.
 */
final class FabricaSslCliente {

    private FabricaSslCliente() {
    }

    static SSLSocket conectar(String host, int porta, Path truststore, char[] senha, int timeoutMs)
            throws IOException, GeneralSecurityException {

        KeyStore ts = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(truststore)) {
            ts.load(in, senha);
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        SSLContext contexto = SSLContext.getInstance("TLS");
        contexto.init(null, tmf.getTrustManagers(), null);

        Socket base = new Socket();
        try {
            base.connect(new InetSocketAddress(host, porta), timeoutMs);
            base.setSoTimeout(timeoutMs);

            // A conexão TCP é criada antes (com timeout) e depois "embrulhada" em TLS informando o nome do servidor.
            SSLSocket socket = (SSLSocket) contexto.getSocketFactory().createSocket(base, host, porta, true);
            socket.setEnabledProtocols(Protocolo.protocolosTls());

            // Ativa a verificação: o nome do servidor precisa constar no certificado (SAN).
            SSLParameters parametros = socket.getSSLParameters();
            parametros.setEndpointIdentificationAlgorithm("HTTPS"); // Ativa a verificação do nome: sem isto, um certificado válido de outro servidor seria aceito.
            socket.setSSLParameters(parametros);

            socket.startHandshake();
            return socket;
        } catch (IOException e) {
            base.close();
            throw e;
        }
    }
}