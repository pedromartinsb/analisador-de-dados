# Analisador de Dados

[![CircleCI](https://dl.circleci.com/status-badge/img/gh/pedrombcampos/analisador-de-dados/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/pedrombcampos/analisador-de-dados/tree/main)
[![codecov](https://codecov.io/gh/pedrombcampos/analisador-de-dados/branch/main/graph/badge.svg)](https://codecov.io/gh/pedrombcampos/analisador-de-dados)

Sistema de análise de dados que monitora um diretório de entrada, processa
arquivos `.dat` em lote e gera relatórios automaticamente.

> Em construção — este commit estabelece build, cobertura e pipeline.

## Requisitos

Java 17 e Maven 3.9+.

## Build e execução

    mvn clean package
    java -jar target/analisador-de-dados-1.0.0-SNAPSHOT.jar

## Testes

    mvn test          # testes
    mvn verify        # testes + cobertura + gate de 80%

## Decisões técnicas

_(a completar na Fase 5)_

## Uso de IA

_(a completar na Fase 5)_