#!/bin/bash

echo Purge...
rm -rf out/bin/*
mkdir -p out/bin

echo Compile...
javac -d out/bin -cp test/TestSOAP.java modulo/srl/soap/DynSOAPConnector.java modulo/srl/soap/DynSOAPClient.java modulo/srl/utils/DynXMLUtils.java test/TestSOAP.java

if [ "$?" -eq 0 ]; then
    echo Execute...
    cd out/bin
    java TestSOAP
    cd ../../
fi
