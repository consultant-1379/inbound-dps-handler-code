#!/bin/sh
rm -f application.keystore
keytool -genkey -keyalg RSA -keystore 'application.keystore' -storetype pkcs12 -storepass 'password' -keypass 'password' -validity 7300 -alias 'server' -dname "cn=Server Administrator,o=Acme,c=US"
