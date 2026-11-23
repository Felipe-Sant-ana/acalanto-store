#!/usr/bin/env bash
# Gera o keystore do servidor, o certificado público e o truststore do cliente na pasta config.
# Uso: bash scripts/gerar-certificados.sh [senha]
# Sem argumento, usa a senha de demonstração "acalanto123".
# A senha deve ter no mínimo 6 caracteres, de preferência só letras e números.
set -euo pipefail

cd "$(dirname "$0")/.."

SENHA="${1:-acalanto123}"

if command -v keytool >/dev/null 2>&1; then
    KEYTOOL=keytool
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/keytool" ]; then
    KEYTOOL="$JAVA_HOME/bin/keytool"
else
    echo "ERRO: keytool não encontrado. Instale o JDK ou adicione a pasta bin do JDK ao PATH." >&2
    exit 1
fi

mkdir -p config
rm -f config/servidor.p12 config/servidor.cer config/cliente-truststore.p12

echo "[1/3] Gerando par de chaves e certificado do servidor..."
"$KEYTOOL" -genkeypair -alias pousada-servidor -keyalg RSA -keysize 2048 -sigalg SHA256withRSA \
    -validity 365 -storetype PKCS12 -keystore config/servidor.p12 \
    -storepass "$SENHA" -keypass "$SENHA" \
    -dname "CN=localhost, OU=Projeto Integrador, O=Pousada Acalanto, C=BR" \
    -ext "SAN=dns:localhost,ip:127.0.0.1"

echo "[2/3] Exportando o certificado público..."
"$KEYTOOL" -exportcert -alias pousada-servidor -keystore config/servidor.p12 \
    -storepass "$SENHA" -file config/servidor.cer -rfc

echo "[3/3] Criando o truststore do cliente..."
"$KEYTOOL" -importcert -noprompt -alias pousada-servidor -file config/servidor.cer \
    -keystore config/cliente-truststore.p12 -storetype PKCS12 -storepass "$SENHA"

echo
echo "Certificados gerados em config/: servidor.p12, servidor.cer e cliente-truststore.p12"
if [ "$#" -gt 0 ]; then
    echo "ATENÇÃO: ajuste a senha em config/servidor.properties e config/cliente.properties."
fi