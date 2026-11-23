@echo off
setlocal

rem ------------------------------------------------------------------
rem Gera o keystore do servidor, o certificado publico e o truststore
rem do cliente na pasta config.
rem Uso: scripts\gerar-certificados.cmd [senha]
rem Sem argumento, usa a senha de demonstracao "acalanto123".
rem A senha deve ter no minimo 6 caracteres, de preferencia so letras e numeros.
rem ------------------------------------------------------------------

cd /d "%~dp0.." || exit /b 1

set "SENHA=%~1"
if "%SENHA%"=="" set "SENHA=acalanto123"

set "KEYTOOL=keytool"
where keytool >nul 2>nul
if errorlevel 1 (
    if defined JAVA_HOME (
        set "KEYTOOL=%JAVA_HOME%\bin\keytool.exe"
    ) else (
        echo ERRO: keytool nao encontrado. Instale o JDK ou adicione a pasta bin do JDK ao PATH.
        exit /b 1
    )
)

if not exist config mkdir config
del /q config\servidor.p12 config\servidor.cer config\cliente-truststore.p12 2>nul

echo [1/3] Gerando par de chaves e certificado do servidor...
"%KEYTOOL%" -genkeypair -alias pousada-servidor -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -validity 365 -storetype PKCS12 -keystore config/servidor.p12 -storepass "%SENHA%" -keypass "%SENHA%" -dname "CN=localhost, OU=Projeto Integrador, O=Pousada Acalanto, C=BR" -ext "SAN=dns:localhost,ip:127.0.0.1" || goto :erro

echo [2/3] Exportando o certificado publico...
"%KEYTOOL%" -exportcert -alias pousada-servidor -keystore config/servidor.p12 -storepass "%SENHA%" -file config/servidor.cer -rfc || goto :erro

echo [3/3] Criando o truststore do cliente...
"%KEYTOOL%" -importcert -noprompt -alias pousada-servidor -file config/servidor.cer -keystore config/cliente-truststore.p12 -storetype PKCS12 -storepass "%SENHA%" || goto :erro

echo.
echo Certificados gerados em config\: servidor.p12, servidor.cer e cliente-truststore.p12
if not "%~1"=="" echo ATENCAO: ajuste a senha em config\servidor.properties e config\cliente.properties.
exit /b 0

:erro
echo.
echo ERRO: falha ao gerar os certificados.
exit /b 1