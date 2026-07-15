# Analisador de Dados

[![CircleCI](https://dl.circleci.com/status-badge/img/gh/pedromartinsb/analisador-de-dados/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/pedromartinsb/analisador-de-dados/tree/main)
[![codecov](https://codecov.io/gh/pedromartinsb/analisador-de-dados/branch/main/graph/badge.svg)](https://codecov.io/gh/pedromartinsb/analisador-de-dados)

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

## Decisões de arquitetura

### Sem framework de injeção de dependência

O sistema é um watcher de diretório com um pipeline de parsing e agregação. Não há
ciclo de vida de container, escopo de request, proxy transacional ou configuração
dinâmica que justifique Spring. As dependências são instanciadas explicitamente no
composition root (`Application`) e o isolamento em testes é obtido com Mockito.

Trade-off assumido: abre-se mão do autowiring e da configuração declarativa. Em
troca, o grafo de dependências inteiro é legível lendo uma única classe, o startup
é imediato e o build não carrega dependências transitivas que o projeto não exerce.
Neste escopo, Spring seria complexidade sem contrapartida.

### Cobertura: gate local alinhado ao contador do Codecov

O `jacoco:check` usa o contador `LINE`, não `INSTRUCTION`. O Codecov reporta
cobertura de linha a partir do XML do JaCoCo; se o gate local usasse `INSTRUCTION`,
existiriam dois números distintos ambos chamados "80%", e o build local poderia
divergir do gate que efetivamente avalia o projeto. Um contador, um número.

O gate é redundante de propósito: `jacoco:check` falha o build localmente sem
depender de rede ou do Codecov, e o `codecov.yml` mantém o threshold no lado
remoto conforme exigido.

A classe `Application` está excluída da análise de cobertura por ser composition
root — só faz wiring e delega. Testá-la exercitaria o bootstrap da JVM, não lógica
do sistema. A exclusão é de código sem regra de negócio, não de código
inconveniente de testar.

### Envio de cobertura sem a orb do Codecov

O pipeline invoca o CLI do Codecov diretamente em vez de usar a orb
`codecov/codecov@5`.

A orb verifica a assinatura GPG do binário do CLI buscando a chave pública do
Codecov no keybase.io. Esse fetch retorna resposta inválida de forma consistente
no ambiente de CI (`gpg: no valid OpenPGP data found`, exit 2), quebrando o build
de forma reprodutível — confirmado como não transiente após rerun.

Solução: download direto do CLI a partir de `cli.codecov.io`, com verificação de
integridade via `sha256sum -c` contra o `SHA256SUM` publicado, e `--fail-on-error`
para que uma falha de upload derrube o build em vez de deixá-lo verde com
cobertura ausente.

Trade-off assumido: a verificação de **assinatura GPG** foi removida; a verificação
de **checksum** foi mantida. Como checksum e binário são servidos pelo mesmo host
sobre TLS, ela protege contra download corrompido ou truncado, mas não contra
comprometimento da origem — garantia estritamente mais fraca que a assinatura.

A justificativa é que uma verificação cuja disponibilidade depende do uptime de um
terceiro (keybase.io) é ponto único de falha externo inaceitável em um pipeline que
é gate de entrega. Em contexto de produção a resposta seria outra: mirror interno
do binário com hash pinado, eliminando tanto a dependência de disponibilidade
quanto a superfície de supply chain.

## Uso de IA

_(a completar na Fase 5)_