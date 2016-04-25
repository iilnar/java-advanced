#!/usr/bin/env bash
case $1 in
walker)
java -cp "./artifacts/WalkTest.jar:./lib/*:./out/production/hw/" info.kgeorgiy.java.advanced.walk.Tester RecursiveWalk ru.ifmo.ctddev.sabirzyanov.walk.RecursiveWalk $2
;;
arrayset)
java -cp "./artifacts/ArraySetTest.jar:./lib/*:./out/production/hw/" info.kgeorgiy.java.advanced.arrayset.Tester NavigableSet ru.ifmo.ctddev.sabirzyanov.arrayset.ArraySet $2
;;
implementor)
java -cp "./artifacts/ImplementorTest.jar:./lib/*:./out/production/hw/" info.kgeorgiy.java.advanced.implementor.Tester class ru.ifmo.ctddev.sabirzyanov.implementor.Implementor $2
;;
jarimplementor)
java -cp "./artifacts/ImplementorTest.jar:./lib/*:./out/production/hw/" info.kgeorgiy.java.advanced.implementor.Tester jar-class ru.ifmo.ctddev.sabirzyanov.implementor.Implementor $2
;;
concurrent)
java -cp "./artifacts/IterativeParallelismTest.jar:./lib/*:./out/production/hw/" info.kgeorgiy.java.advanced.concurrent.Tester list ru.ifmo.ctddev.sabirzyanov.concurrent.Concurrent $2
;;
mapper)
java -cp "./artifacts/ParallelMapperTest.jar:./lib/*:./out/production/hw/" info.kgeorgiy.java.advanced.mapper.Tester scalar ru.ifmo.ctddev.sabirzyanov.mapper.ParallelMapperImpl,ru.ifmo.ctddev.sabirzyanov.concurrent.IterativeParallelism $2
;;
webcrawler)
java -cp "./artifacts/WebCrawlerTest.jar:./lib/*:./out/production/hw/" info.kgeorgiy.java.advanced.crawler.Tester hard ru.ifmo.ctddev.sabirzyanov.webcrawler.WebCrawler $2
;;
*)
echo "Usage: run.sh [walker, arrayset, implementor, jarimplementor, concurrent, mapper, webcrawler]"
;;
esac
