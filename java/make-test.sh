#!/bin/bash

echo Purge...
rm -rf out/bin/*
mkdir -p out/bin

echo Compile...
javac -d out/bin modulo/srl/connector/DynConnector.java test/Test.java

if [ "$?" -eq 0 ]; then
    echo Execute...
    cd out/bin
    java Test
    cd ../../
fi
