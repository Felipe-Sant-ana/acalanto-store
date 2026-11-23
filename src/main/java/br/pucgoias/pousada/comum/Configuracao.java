package br.pucgoias.pousada.comum;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Leitura de um arquivo {@code .properties} com mensagens de erro claras.
 * Usada tanto pelo servidor (keystore) quanto pelo cliente (truststore e endereço).
 */
public final class Configuracao {

    private final Path arquivo;
    private final Properties props;

    private Configuracao(Path arquivo, Properties props) {
        this.arquivo = arquivo;
        this.props = props;
    }

    /**
     * Lê o arquivo (UTF-8). Lança IOException com o caminho completo se ele não existir.
     */
    public static Configuracao carregar(Path arquivo) throws IOException {
        if (!Files.isRegularFile(arquivo)) {
            throw new IOException("Arquivo de configuração não encontrado: " + arquivo.toAbsolutePath());
        }
        Properties props = new Properties();
        try (Reader leitor = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            props.load(leitor);
        }
        return new Configuracao(arquivo, props);
    }

    /**
     * Valor da chave; falha com mensagem clara se estiver ausente ou vazia.
     */
    public String obrigatorio(String chave) throws IOException {
        String valor = props.getProperty(chave);
        if (valor == null || valor.isBlank()) {
            throw new IOException("A chave '" + chave + "' está ausente em " + arquivo);
        }
        return valor.strip();
    }

    /**
     * Valor da chave ou, se ausente, o padrão informado.
     */
    public String opcional(String chave, String padrao) {
        String valor = props.getProperty(chave);
        return (valor == null || valor.isBlank()) ? padrao : valor.strip();
    }
}