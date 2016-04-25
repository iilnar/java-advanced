#!/usr/bin/env bash
rm -rf doc
case "$1" in
implementor)
javadoc src/ru/ifmo/ctddev/sabirzyanov/implementor/Implementor.java src/info/kgeorgiy/java/advanced/implementor/* -d doc  -private -link http://docs.oracle.com/javase/8/docs/api/ -link http://commons.apache.org/proper/commons-compress/apidocs
;;
concurrent)
javadoc src/ru/ifmo/ctddev/sabirzyanov/concurrent/Concurrent.java src/info/kgeorgiy/java/advanced/concurrent/* -d doc  -private -link http://docs.oracle.com/javase/8/docs/api/ -link http://commons.apache.org/proper/commons-compress/apidocs
;;
esac