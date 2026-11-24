# Acalanto Store

Aplicação desktop cliente-servidor para consulta e gestão de produtos da Pousada Acalanto, desenvolvida em Java. Este projeto faz parte do Projeto Integrador III-A do curso de Análise e Desenvolvimento de Sistemas da Pontifícia Universidade Católica de Goiás (PUC Goiás).

<img width="1920" height="1080" alt="Demonstracao" src="https://github.com/user-attachments/assets/465c1b7a-7251-4ed5-aaac-59ec34246ff6" />

---

## Sumário

1. [Sobre o projeto](#1-sobre-o-projeto)
2. [Arquitetura do projeto](#2-arquitetura-do-projeto)
3. [Tecnologias utilizadas](#3-tecnologias-utilizadas)
4. [Como executar o projeto](#4-como-executar-o-projeto)
5. [Dados](#5-dados)
6. [Solução de Problemas](#6-solução-de-problemas)
7. [Limitações](#7-limitações)
8. [Autoria](#8-autoria)

---

## 1. Sobre o projeto

A aplicação permite consultar, cadastrar, editar e excluir os produtos vendidos pela Pousada Acalanto (alimentos, vestuário e diversos). O sistema segue a arquitetura cliente-servidor: o **servidor** guarda os dados em arquivo JSON, valida e processa as solicitações, e o **cliente** (interface gráfica em Swing com FlatLaf) consulta o servidor pela rede. Toda a comunicação entre os dois é protegida por **SSL/TLS**.

### 1.1 Funcionalidades

- Listagem de produtos em ordem de categoria e, dentro dela, alfabética
- Busca pelo nome do produto, sem diferenciar acentos ou maiúsculas
- Filtros por categoria, disponibilidade, faixa de preço e faixa de quantidade
- Paginação, com busca, filtros e paginação executados no servidor
- Cards de resumo (total de produtos e quantidade por categoria)
- Cadastro, edição e exclusão com confirmação
- Status de disponibilidade calculado a partir da quantidade
- Registro de conexões e falhas no console do servidor

### 1.2 Segurança

- **TLS 1.3 e 1.2** (`SSLServerSocket` e `SSLSocket`): comunicação criptografada
- **Autenticação do servidor:** o cliente confia somente no certificado do truststore e confere o nome do servidor (SAN)
- **Validação no servidor:** tipo, obrigatoriedade, tamanho, faixa numérica e caracteres de todos os campos recebidos
- **Tratamento de erros:** erros do cliente geram mensagens claras; erros internos ficam no log do servidor e o cliente recebe uma mensagem genérica
- **Limites:** tamanho máximo de mensagem, timeout de inatividade e de handshake, e número máximo de clientes simultâneos
- **Chaves e certificados fora do repositório:** o keystore (chave privada) e o truststore não são versionados (ver `.gitignore`); cada pessoa gera os seus localmente. A senha padrão deste projeto (`acalanto123`) é **apenas de demonstração**, pois protege somente certificados autoassinados gerados na própria máquina

---

## 2. Arquitetura do projeto

```
acalanto-store/
├── config/
│   ├── servidor.properties           # Configuração do servidor (keystore)
│   └── cliente.properties            # Configuração do cliente (truststore e endereço)
├── dados/
│   └── produtos.json                 # Armazenamento dos produtos
├── scripts/
│   ├── gerar-certificados.cmd        # Gera os certificados (Windows)
│   └── gerar-certificados.sh         # Gera os certificados (Linux e macOS)
├── pom.xml                           # Dependências e configuração do Maven
└── src/main/
    ├── java/br/pucgoias/pousada/
    │   ├── cliente/                  # Lado do cliente (interface)
    │   │   ├── Aplicacao.java                 # Ponto de entrada
    │   │   ├── ClienteRede.java               # Comunicação com o servidor
    │   │   ├── DialogoProduto.java            # Formulário de cadastro e edição
    │   │   ├── FabricaSslCliente.java         # Criação do SSLSocket
    │   │   ├── FiltroDigitacao.java           # Filtro de digitação
    │   │   ├── Filtros.java                   # Filtros aplicados
    │   │   ├── IconeImagem.java               # Redução da logo com qualidade
    │   │   ├── JanelaPrincipal.java           # Tela principal
    │   │   ├── PainelCabecalho.java           # Logo e saudação
    │   │   ├── PainelFiltros.java             # Painel de filtros
    │   │   ├── PainelResumo.java              # Cards de resumo
    │   │   ├── TabelaProdutos.java            # Tabela de produtos e ações
    │   │   └── Tema.java                      # Paleta de cores e tema Dark do FlatLaf
    │   ├── comum/                    # Modelos e protocolo compartilhados
    │   │   ├── Acao.java                      # Operações: LISTAR, RESUMO, CRIAR, EDITAR, EXCLUIR
    │   │   ├── Categoria.java                 # Alimentos, Vestuário, Diversos
    │   │   ├── Configuracao.java              # Leitura de arquivos .properties
    │   │   ├── Produto.java                   # Modelo de dados do produto
    │   │   ├── Protocolo.java                 # Constantes, Gson e versões de TLS
    │   │   ├── Requisicao.java                # Mensagem enviada pelo cliente
    │   │   ├── Resposta.java                  # Mensagem devolvida pelo servidor
    │   │   └── Resumo.java                    # Números dos cards
    │   └── servidor/                 # Lado do servidor
    │       ├── FabricaSslServidor.java        # Criação do SSLServerSocket
    │       ├── ProcessadorRequisicoes.java    # Busca, filtros, paginação e CRUD
    │       ├── ProdutoRepositorio.java        # Leitura e gravação do JSON
    │       ├── RequisicaoInvalidaException.java
    │       ├── Servidor.java                  # Ponto de entrada (aceita conexões)
    │       ├── TratadorCliente.java           # Atende uma conexão      
    │       └── Validador.java                 # Validação dos dados recebidos
    └── resources/imagens/            # Logo e ícone
```

### 2.1 Comunicação

O cliente envia uma linha JSON (por exemplo `{"acao":"LISTAR","busca":"calca"}`) e o servidor responde com uma linha JSON (`"status":"OK"` com os dados, ou `"status":"ERRO"` com a mensagem):

```
┌──────────────────┐   TLS 1.3/1.2 (porta 5000)   ┌───────────────────┐
│     CLIENTE      │ ◄──────────────────────────► │     SERVIDOR      │
│ Swing + FlatLaf  │   mensagens JSON (1 linha)   │ SSLServerSocket   │
└──────────────────┘                              │ Validação + regras│
                                                  └─────────┬─────────┘
                                                            │
                                                    dados/produtos.json
```

---

## 3. Tecnologias utilizadas

- **Linguagem:** Java (desenvolvido com JDK 24, compatível com Java 21)
- **Interface Gráfica:** Java Swing + FlatLaf 3.5.4
- **Comunicação:** `Socket` e `ServerSocket` com SSL/TLS (`javax.net.ssl`)
- **Armazenamento:** arquivo JSON, com a biblioteca Gson 2.11.0
- **Gerenciador de dependências:** Maven
- **IDE:** IntelliJ IDEA

---

## 4. Como executar o projeto

Siga os passos abaixo para configurar e rodar a aplicação na sua máquina local:

### 4.1 Pré-requisitos

- Java JDK 21 ou superior instalado (o `keytool`, usado no passo 4.4, já vem com o JDK)
- Maven (já incluído no IntelliJ IDEA; só é necessário instalar à parte para executar pelo terminal)
- Uma IDE de sua preferência: **IntelliJ IDEA (recomendada)**, **VS Code** com o *Extension Pack for Java* ou qualquer outra IDE.

### 4.2 Clone o repositório (ou baixe os arquivos)

```bash
git clone https://github.com/Felipe-Sant-ana/acalanto-store.git
```

Ou baixe o ZIP clicando em **Code → Download ZIP** e extraia a pasta.

### 4.3 Abrir o projeto e carregar as dependências

As bibliotecas (FlatLaf e Gson) são baixadas automaticamente pelo Maven, não sendo necessário baixá-las separadamente.

<details>
<summary><strong>IntelliJ IDEA (recomendada)</strong></summary>

1. Acesse **File → Open** e selecione a pasta do projeto (ou o arquivo `pom.xml`)
2. Aguarde o IntelliJ importar o projeto e baixar as dependências (barra de progresso no canto inferior)
3. Se aparecer um ícone do Maven no editor, clique em **Load Maven Changes**
4. Confirme em **External Libraries** se `com.formdev:flatlaf` e `com.google.code.gson:gson` estão listadas

</details>

<details>
<summary><strong>Eclipse</strong></summary>

1. Acesse **File → Import → Maven → Existing Maven Projects**
2. Selecione a pasta do projeto e clique em **Finish**
3. Aguarde o download das dependências

</details>

<details>
<summary><strong>VS Code</strong></summary>

1. Acesse **File → Open Folder** e selecione a pasta do projeto
2. O *Extension Pack for Java* reconhece o `pom.xml` e baixa as dependências automaticamente
3. Confira na aba **JAVA PROJECTS → Maven Dependencies**

</details>

### 4.4 Gerar os certificados de segurança

Por segurança, a chave privada e os certificados **não** estão no repositório, então precisam ser gerados uma única vez em cada máquina. Abra um terminal **na raiz do projeto** (no IntelliJ: **Alt+F12**) e execute o script correspondente ao seu sistema:

**Windows** (Prompt de Comando ou PowerShell):

```bash
.\scripts\gerar-certificados.cmd
```

**Windows com Git Bash, Linux e macOS:**

```bash
bash scripts/gerar-certificados.sh
```

Os arquivos `config/servidor.properties` e `config/cliente.properties` já vêm configurados com a senha de demonstração `acalanto123`, que o script utiliza por padrão. Não é preciso digitar nem editar nenhuma senha.

Ao final, a pasta `config/` deve conter `servidor.p12`, `servidor.cer` e `cliente-truststore.p12`, além dos dois arquivos `.properties`. Se executar o script novamente, ele substitui os três arquivos gerados.

<details>
<summary><strong>Gerar os certificados manualmente (sem o script)</strong></summary>

Execute os três comandos abaixo, **na ordem**, na raiz do projeto.

**1. Criar o par de chaves e o certificado do servidor** (comando em uma linha só):

```bash
keytool -genkeypair -alias pousada-servidor -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -validity 365 -storetype PKCS12 -keystore config/servidor.p12 -storepass acalanto123 -keypass acalanto123 -dname "CN=localhost, OU=Projeto Integrador, O=Pousada Acalanto, C=BR" -ext "SAN=dns:localhost,ip:127.0.0.1"
```

> Se ocorrer erro de sintaxe ao colar, digite as aspas manualmente (aspas retas `"`), pois alguns editores as trocam por aspas tipográficas.

**2. Exportar o certificado público:**

```bash
keytool -exportcert -alias pousada-servidor -keystore config/servidor.p12 -storepass acalanto123 -file config/servidor.cer -rfc
```

**3. Criar o truststore do cliente** (o `-noprompt` dispensa a pergunta de confiança no certificado):

```bash
keytool -importcert -noprompt -alias pousada-servidor -file config/servidor.cer -keystore config/cliente-truststore.p12 -storetype PKCS12 -storepass acalanto123
```

Se o terminal informar que `keytool` não é reconhecido, use o caminho completo do JDK, por exemplo `"C:\Program Files\Java\jdk-24\bin\keytool.exe"`.

</details>

<details>
<summary><strong>Usar uma senha própria (em vez da senha de demonstração)</strong></summary>

1. Gere os certificados informando a sua senha como argumento do script (mínimo de 6 caracteres; prefira apenas letras e números):

   ```bash
   .\scripts\gerar-certificados.cmd MinhaSenha123
   ```
   ```bash
   bash scripts/gerar-certificados.sh MinhaSenha123
   ```

2. Troque `acalanto123` pela sua senha nos dois arquivos de configuração:
    - `config/servidor.properties` → `keystore.senha`
    - `config/cliente.properties` → `truststore.senha`

3. Reinicie o servidor e o cliente.

Evite barra invertida (`\`) na senha, pois o formato `.properties` a trata como caractere de escape. **Não envie** os arquivos `.properties` com a sua senha para o repositório.

Em uma instalação real, o `servidor.p12` (chave privada) deve ficar **somente no computador do servidor**, e os clientes precisam apenas do `cliente-truststore.p12`. Se cliente e servidor ficarem em máquinas diferentes, o IP do servidor precisa constar no SAN do certificado.

</details>

### 4.5 Executar a aplicação

Execute sempre com o **diretório de trabalho na raiz do projeto**. O **servidor deve ser iniciado antes do cliente**.

**4.5.1 Iniciar o servidor**

1. Abra o arquivo `src/main/java/br/pucgoias/pousada/servidor/Servidor.java`
2. Clique no botão **Run** (ícone verde ao lado do `main`)
3. O console deve exibir `Servidor TLS aguardando conexões em localhost/127.0.0.1:5000`. As linhas de log aparecem em vermelho no IntelliJ, e isso é normal

**4.5.2 Iniciar o cliente**

1. Abra o arquivo `src/main/java/br/pucgoias/pousada/cliente/Aplicacao.java`
2. Clique em **Run**. O servidor continua em execução na outra aba do console; **não** clique em Run novamente na classe `Servidor`, pois isso encerraria o servidor em execução. Se o IntelliJ perguntar o que fazer com o processo em execução, escolha **Run on separate instance** (ou "Allow parallel run")
3. Opcional: para silenciar um aviso do FlatLaf no Java 24, adicione a opção de VM `--enable-native-access=ALL-UNNAMED` em **Run → Edit Configurations → Modify options → Add VM options**

Realizado todos esses passos corretamente, a janela principal da aplicação irá aparecer em sua tela com os produtos cadastrados.

### 4.6 Execução pelo terminal (alternativa)

Requer o Maven instalado (o `exec-maven-plugin` já está configurado no `pom.xml`). Execute cada comando em um terminal separado, sempre na raiz do projeto e iniciando pelo servidor:

```bash
mvn compile exec:java -Dexec.mainClass=br.pucgoias.pousada.servidor.Servidor
mvn compile exec:java -Dexec.mainClass=br.pucgoias.pousada.cliente.Aplicacao
```

---

## 5. Dados

Os produtos ficam em `dados/produtos.json`, que acompanha o repositório com 9 produtos de exemplo. O servidor grava nesse arquivo todas as alterações (cadastro, edição e exclusão). Para voltar aos dados de exemplo, descarte as alterações locais desse arquivo com `git restore --source=HEAD -- dados/produtos.json` (isso substitui as alterações feitas nele). Sem Git, baixe novamente o arquivo do repositório.

---

## 6. Solução de Problemas

Caso encontre algum problema ao executar a aplicação, verifique as situações abaixo:

<details>
<summary><strong>"keytool" não é reconhecido / "keytool não encontrado"</strong></summary>

O JDK não está no PATH do sistema. Verifique:

- Se o JDK (e não apenas o JRE) está instalado
- Se a variável `JAVA_HOME` aponta para a pasta do JDK (o script a utiliza automaticamente)
- Em último caso, execute o `keytool` com o caminho completo, por exemplo `"C:\Program Files\Java\jdk-24\bin\keytool.exe"`

</details>

<details>
<summary><strong>"Permission denied" ao executar o script no Linux ou macOS</strong></summary>

Execute o script com `bash scripts/gerar-certificados.sh` (não exige permissão de execução) ou conceda a permissão com `chmod +x scripts/gerar-certificados.sh`.

</details>

<details>
<summary><strong>"Arquivo de configuração não encontrado"</strong></summary>

O programa não achou os arquivos de configuração. Verifique:

- Se existem `config/servidor.properties` e `config/cliente.properties` (eles acompanham o repositório)
- Se o diretório de trabalho da execução é a **raiz do projeto**. No IntelliJ: **Run → Edit Configurations** e confira o campo **Working directory**

</details>

<details>
<summary><strong>"NoSuchFileException" ou "FileNotFoundException" citando <code>config/servidor.p12</code> ou <code>config/cliente-truststore.p12</code></strong></summary>

Os certificados ainda não foram gerados. Execute o script do passo 4.4.

</details>

<details>
<summary><strong>"Não foi possível conectar ao servidor. Ele está em execução?"</strong></summary>

O cliente não encontrou o servidor. Verifique:

- Se o `Servidor` foi iniciado **antes** e continua em execução
- Se a porta em `cliente.properties` (`servidor.porta`) é a mesma do servidor (padrão `5000`)
- Se outro programa não está usando a porta 5000

</details>

<details>
<summary><strong>"Address already in use" (ao iniciar o servidor)</strong></summary>

Já existe um servidor em execução ou outro programa está usando a porta 5000. Encerre o processo anterior (botão vermelho de parar no IntelliJ) e tente novamente.

</details>

<details>
<summary><strong>"keystore password was incorrect"</strong></summary>

A senha dos arquivos `.properties` não confere com a usada ao gerar os certificados. Verifique:

- Se `keystore.senha` e `truststore.senha` são `acalanto123` (a senha padrão do script)
- Se você alterou a senha nos `.properties` sem gerar os certificados novamente com a mesma senha
- Em último caso, execute o script do passo 4.4 novamente (ele substitui os certificados) e reinicie o servidor

</details>

<details>
<summary><strong>"PKIX path building failed" ou "unable to find valid certification path"</strong></summary>

O cliente não reconhece o certificado do servidor. Verifique:

- Se o `cliente-truststore.p12` foi criado a partir do **mesmo** `servidor.p12` que o servidor está usando
- Se o certificado foi gerado novamente com o servidor em execução. Nesse caso, execute o script do passo 4.4 e **reinicie o servidor**

</details>

<details>
<summary><strong>"No subject alternative names matching ..."</strong></summary>

O nome usado pelo cliente não consta no certificado. Verifique:

- Se `servidor.host` em `cliente.properties` é `localhost`
- Se os certificados foram gerados pelo script do passo 4.4 (ele inclui `localhost` e `127.0.0.1` no certificado)

</details>

<details>
<summary><strong>O servidor se recusa a iniciar com "Produto com dados ausentes ou inválidos"</strong></summary>

O `dados/produtos.json` está em formato inválido ou desatualizado. Verifique:

- Se cada produto tem `id`, `nome`, `categoria` e `preco`
- Se a categoria é `ALIMENTOS`, `VESTUARIO` ou `DIVERSOS` (em maiúsculas e sem acento)
- Para restaurar o arquivo de exemplo: `git restore --source=HEAD -- dados/produtos.json` (isso descarta as alterações locais nesse arquivo)

</details>

<details>
<summary><strong>Aviso "A restricted method in java.lang.System has been called"</strong></summary>

É apenas um aviso do Java 24 sobre o FlatLaf e não afeta o funcionamento. Para eliminá-lo, adicione a opção de VM `--enable-native-access=ALL-UNNAMED` na configuração de execução do cliente.

</details>

<details>
<summary><strong>A janela abre mas a tabela aparece vazia ou com erro em vermelho</strong></summary>

A interface abriu, mas não conseguiu obter os dados. Verifique:

- A mensagem em vermelho no rodapé indica a causa (servidor fora do ar, falha de TLS etc.)
- O log do servidor no console, que registra cada conexão e falha
- Se `dados/produtos.json` existe e contém os produtos

</details>

<details>
<summary><strong>O script .cmd falha com caminho estranho ou "comando não reconhecido"</strong></summary>

O terminal pode não ser o esperado:

- No **Git Bash**, use `bash scripts/gerar-certificados.sh`
- No **Prompt de Comando** ou **PowerShell**, use `.\scripts\gerar-certificados.cmd`
- O nome do terminal aparece no canto direito do painel do VS Code; para trocar, use a seta ao lado do botão **+**

</details>

<details>
<summary><strong>Erros de Java/JDK ao executar o script ou o projeto</strong></summary>

Verifique no terminal:

```bash
java -version
keytool -help
```

Ambos devem funcionar e indicar o JDK 21 ou superior. Se `keytool` não for reconhecido ou a versão estiver errada, confira a variável `JAVA_HOME` (deve apontar para a pasta do JDK, não para `bin`) e o `PATH`. No VS Code, o JDK usado pelo projeto também pode ser escolhido em **Java: Configure Java Runtime**.

</details>

---

## 7. Limitações

O Acalanto Store foi desenvolvido como um MVP (Minimum Viable Product), com foco nas funcionalidades essenciais de gerenciamento de produtos. Atualmente, apresenta as seguintes limitações:

- **Sem autenticação de usuários:** a aplicação não possui tela de login ou controle de acesso individual.
- **Execução local:** a configuração padrão escuta apenas em `localhost`. O uso em rede exige alterar o `endereco` no `servidor.properties` e gerar o certificado com o IP do servidor no SAN.
- **Armazenamento em JSON:** os dados são persistidos em um arquivo JSON, sem utilização de um banco de dados.
- **Certificado autoassinado e senha de demonstração:** adequados à demonstração local. Em produção, seriam usados um certificado emitido por uma autoridade certificadora e uma senha secreta, fora dos arquivos versionados.

---

## 8. Autoria

**Aluno:** Felipe Oliveira Sant'ana  
**Curso:** Análise e Desenvolvimento de Sistemas  
**Instituição:** Pontifícia Universidade Católica de Goiás (PUC Goiás)  
**Disciplina:** Projeto Integrador III-A  
**Professor Orientador:** Thalles Bruno G. N. dos Santos
