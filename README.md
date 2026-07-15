# Analisador de Dados

[![CircleCI](https://dl.circleci.com/status-badge/img/gh/pedromartinsb/analisador-de-dados/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/pedromartinsb/analisador-de-dados/tree/main)
[![codecov](https://codecov.io/gh/pedromartinsb/analisador-de-dados/branch/main/graph/badge.svg)](https://codecov.io/gh/pedromartinsb/analisador-de-dados)

Sistema de análise de dados que monitora um diretório de entrada, processa em
lote arquivos `.dat` com layout posicional (vendedores, clientes e vendas) e
gera um relatório por arquivo no diretório de saída.

## Requisitos

Java 17 e Maven 3.9+.

## Build e execução

```bash
mvn clean package
java -jar target/analisador-de-dados-1.0.0-SNAPSHOT.jar
```

O jar é auto-contido (fat jar via `maven-shade-plugin` — ver [Decisões de
arquitetura](#decisões-de-arquitetura)) e roda sem classpath adicional.

Os diretórios monitorados são criados automaticamente na primeira execução:

- Entrada: `~/data/in`
- Saída: `~/data/out`

Solte um arquivo `.dat` em `~/data/in` com a aplicação rodando; o
`.done.dat` correspondente aparece em `~/data/out` assim que o arquivo
estabilizar.

## Testes

```bash
mvn test          # testes unitários e de integração
mvn verify         # testes + cobertura (JaCoCo) + gate de 80%
```

Testes de integração (`@Tag("integration")`) usam `@TempDir` e, no caso do
`DirectoryWatcher`, o `WatchService` real do sistema de arquivos — não são
instantâneos por natureza (a suíte completa leva alguns segundos, não
milissegundos, por causa disso).

> Se `SellerParserTest` imprimir um stack trace de
> `IllegalClassFormatException: ... Unsupported class file major version 70`
> ao rodar localmente, é ruído inofensivo do agente JaCoCo tentando
> instrumentar classes internas do JDK ao rodar sob um runtime mais novo que
> o `release` alvo (17) — não indica falha de teste e não ocorre no
> CircleCI, que roda `cimg/openjdk:17.0`. Ver detalhe na seção de
> decisões abaixo.

## Estrutura do projeto

```
br.com.analisador
├── Application.java     composition root; wiring e nada mais
├── config/               configuração externalizada (diretórios, extensão, threads)
├── domain/               modelo; zero dependência de I/O ou framework
├── parser/               texto → domínio
├── analysis/             domínio → resultado agregado
├── report/                resultado → texto → arquivo
└── processing/           detecção de arquivos, concorrência, orquestração
```

Os pacotes seguem o sentido do fluxo de dados. `domain` não depende de
ninguém; `parser`, `analysis` e `report` dependem só de `domain`;
`processing` orquestra os três. Não há dependência circular. Adicionar um
tipo de registro toca `domain` + `parser`; adicionar uma análise toca só
`analysis`.

## Decisões de arquitetura

### Sem framework de injeção de dependência

O sistema é um watcher de diretório com um pipeline de parsing e agregação.
Não há ciclo de vida de container, escopo de request ou proxy transacional
que justifique Spring. As dependências são instanciadas explicitamente no
composition root (`Application`) e o isolamento em testes é obtido com
Mockito onde necessário.

Trade-off assumido: abre-se mão de autowiring e configuração declarativa. Em
troca, o grafo de dependências inteiro é legível lendo uma única classe, o
startup é imediato, e o build não carrega dependências transitivas que o
projeto não exerce.

### O delimitador `ç` e o problema de nomes acentuados

O formato usa `ç` como separador de campos sem definir escaping. Nomes
próprios brasileiros contêm `ç` naturalmente (ex. "Conceição"), o que quebra
o `split` em campos extras — nenhum valor de `limit` corrige isso, porque a
quebra espúria acontece no meio do registro, não no fim.

Decisão: `split("ç", -1)` seguido de validação de contagem exata de campos.
Divergência → linha rejeitada como malformada, com log do número da linha e
do motivo (nunca do conteúdo — ver PII abaixo). É falha do formato de
entrada, não da implementação; descartar com rastro é preferível a corromper
em silêncio.

### Dinheiro em `BigDecimal`, sempre construído de `String`

Salários e preços usam `BigDecimal`, nunca `double` — `double` acumula erro
de arredondamento no somatório de itens de venda, e a corretude dos cálculos
é critério explícito de avaliação.

### UTF-8 explícito em toda leitura e escrita

`StandardCharsets.UTF_8` é passado explicitamente em todo `Files.lines` e
`Files.writeString`. O charset default da JVM varia por sistema operacional
(no Windows tende a ser `Cp1252`), e o delimitador `ç` é multi-byte.

### Diretórios via `${user.home}`, não `%HOMEPATH%`

O enunciado referencia `%HOMEPATH%`, notação Windows. O CI roda Linux
(`cimg/openjdk:17.0`), e a resolução via `System.getProperty("user.home")`,
externalizada em `application.properties`, funciona nos dois ambientes.

### Integridade referencial: venda com vendedor inexistente

Vendas cujo `Nome do Vendedor` não corresponde a nenhum registro `001` são
descartadas antes do cálculo de qualquer métrica — inclusive "venda mais
cara": se a venda é inválida, é inválida para tudo.

A resolução roda em duas passagens: o conjunto de vendedores conhecidos é
montado a partir de todos os registros `001` do arquivo antes de qualquer
venda ser avaliada, porque nada garante que os registros `001` precedam os
`003` no arquivo — o exemplo do enunciado ordena, mas isso não é contrato do
formato.

**Nota de fronteira:** o enunciado autoriza descartar "linhas com formato
inválido ou tipo desconhecido". Vendedor inexistente não é nenhum dos dois —
é uma extensão deliberada da regra de descarte, pelo mesmo princípio de não
interromper o processamento.

**Limitação conhecida:** a referência de venda para vendedor é feita por
**nome**, não por CPF — é o único identificador que o registro `003` carrega.
Dois vendedores homônimos com CPFs diferentes colidiriam nessa checagem; o
formato do enunciado não oferece meio de desambiguar isso.

### Vendedor sem nenhuma venda: incluído, faturamento zero

Os registros `001` declaram o universo de vendedores do arquivo. Quem não
vendeu tem faturamento zero — literalmente o menor volume possível. Excluí-lo
responderia "pior entre os que venderam", que é outra pergunta.

**Risco assumido conscientemente:** uma implementação de referência baseada
em `groupingBy` sobre as vendas nunca enxergaria esses vendedores, e
produziria resultado diferente em arquivo com vendedor ocioso. Optou-se pela
leitura literal do requisito em vez de espelhar o comportamento acidental de
uma implementação que agrupa apenas o que existe do lado das vendas. Por
esse motivo, o cálculo de faturamento por vendedor parte da lista de
`Seller`, nunca de um agrupamento sobre `Sale`.

### Contagem de clientes e vendedores: distintos por documento

Duas linhas `001` com o mesmo CPF, ou duas `002` com o mesmo CNPJ, contam
como um único vendedor/cliente. Documento é a chave natural da entidade.

### Nome do arquivo de saída: extensão original removida

`{nome_do_arquivo_original}.done.dat` é ambíguo entre manter a extensão
original (`dados.dat.done.dat`) ou removê-la (`dados.done.dat`). Adotada a
segunda opção — `.done.dat` já carrega o sufixo. Para nomes com múltiplos
pontos (`relatorio.vendas.dat`), apenas a última extensão é removida
(`relatorio.vendas.done.dat`).

### Ausência de dado: `Optional` no domínio, `N/A` na apresentação

Arquivo sem vendas não tem "venda mais cara"; sem vendedores, não tem "pior
vendedor". O domínio (`AnalysisResult`) expressa isso com `Optional`; a
decisão de renderizar como `"N/A"` pertence ao `ReportFormatter`, não ao
modelo — apresentação é responsabilidade de apresentação.

### Desempate: primeira ocorrência no arquivo

Empate na venda mais cara ou no pior vendedor é resolvido pela primeira
ocorrência no arquivo. `Stream.max`/`.min` sobre um stream sequencial já
garantem isso — `BinaryOperator.maxBy`/`minBy` preservam o primeiro elemento
em caso de empate. Determinismo é pré-requisito de testabilidade; por isso
essas chamadas nunca devem virar `.parallelStream()`.

### `FileProcessor` como fronteira de contenção

`FileProcessor.process` captura `Exception` (não `Throwable`) e loga `ERROR`
com o nome do arquivo, nunca propaga — nem para dado malformado, nem para
falha real de I/O (arquivo apagado entre a detecção e a leitura, por
exemplo). Capturar `Throwable` mascararia erros de JVM (`OutOfMemoryError`)
como se fossem falha de processamento de arquivo, o que seria pior que
deixar propagar.

Deliberadamente não há validação de argumento nulo no início de `process()`:
se o `Path` for nulo, a falha ocorre dentro do próprio `try` e é capturada
como qualquer outra — a fronteira de contenção vale inclusive para erro do
chamador.

### `DirectoryWatcher`: varredura inicial, `awaitStable` e o risco de ciclo

`watch()` varre os arquivos já presentes no diretório antes de entrar no
loop do `WatchService`, para não perder arquivos que chegaram antes da
aplicação subir.

`ENTRY_CREATE` dispara quando o arquivo é **criado**, não quando termina de
ser escrito. `awaitStable` mitiga isso aguardando o tamanho do arquivo
estabilizar entre checagens sucessivas (100ms de intervalo, até 10
tentativas). **É mitigação pragmática, não garantia formal**: um processo
que escreve mais devagar que o intervalo entre duas amostras estáveis ainda
pode ser lido parcialmente. Não existe solução 100% confiável para "arquivo
terminou de ser escrito" via NIO puro sem depender de convenção externa
(escrever em nome temporário e renomear ao final, por exemplo).

**Risco de configuração:** se o diretório de entrada e o de saída
coincidirem, cada `.done.dat` escrito dispara um novo `ENTRY_CREATE` no
mesmo diretório observado e — como o sufixo termina em `.dat` — o watcher
tentaria reprocessar seu próprio relatório como entrada nova, criando um
ciclo de retroalimentação. A configuração real (`directory.in` /
`directory.out`) mantém os diretórios separados; isso é uma armadilha de
configuração documentada, não um caso tratado internamente pela classe.

### Log de auditoria e PII

Linhas rejeitadas são logadas com número da linha e motivo, **nunca** com o
conteúdo — uma linha malformada pode conter CPF ou CNPJ, e logar o conteúdo
cru seria vazamento de dado pessoal. O mesmo princípio se estende à
descartada de venda por vendedor inexistente: o log cita o ID da venda, não
o nome do vendedor fantasma, porque nome de pessoa também é dado pessoal.

### Cobertura: gate local alinhado ao contador do Codecov

`jacoco:check` usa o contador `LINE`, não `INSTRUCTION`. O Codecov reporta
cobertura de linha a partir do XML do JaCoCo; usar contadores diferentes
localmente e remotamente produziria dois números diferentes ambos chamados
"80%". A classe `Application` é excluída da análise por ser composition
root — testá-la exercitaria o bootstrap da JVM, não lógica de negócio; a
exclusão é de código sem regra, não de código inconveniente de testar.

### Testes de integração: `@Tag("integration")`, não Failsafe

Testes de integração convivem com os unitários no Surefire, marcados com
`@Tag("integration")`, em vez de separados via `maven-failsafe-plugin` com
sufixo `*IT`. Um único `jacoco.exec`, sem necessidade de merge de execution
data entre duas execuções do agente — Failsafe adicionaria complexidade real
sem ganho neste escopo, já que o enunciado não exige fases de build
separadas para testes de integração.

### Envio de cobertura ao Codecov sem a orb oficial

O pipeline usa o CLI do Codecov baixado diretamente, em vez da orb
`codecov/codecov@5`. A orb verifica a assinatura GPG do binário buscando a
chave pública no keybase.io, que retornou resposta inválida de forma
reprodutível no ambiente de CI (`gpg: no valid OpenPGP data found`, exit 2),
confirmado como não transiente após rerun.

Trade-off assumido: a verificação de **assinatura GPG** foi removida; a
verificação de **checksum** (`sha256sum -c` contra o `SHA256SUM` publicado)
foi mantida — protege contra download corrompido ou truncado, mas não
contra comprometimento da origem, garantia estritamente mais fraca que a
assinatura. Justificativa: uma verificação cuja disponibilidade depende do
uptime de um terceiro (keybase.io) é ponto único de falha externo
inaceitável num pipeline que é gate de entrega.

### Jar executável: `maven-shade-plugin`

`maven-jar-plugin` sozinho produz um jar fino, sem as dependências
empacotadas — `java -jar` falhava com `NoClassDefFoundError` para
`org.slf4j.LoggerFactory`. Descoberto ao validar manualmente a execução via
linha de comando (exigida pelo enunciado), não pelo `mvn verify`: os testes
rodam com o classpath resolvido pelo próprio Maven, então esse tipo de falha
de empacotamento não aparece na suíte automatizada nem no CircleCI, que só
executa `mvn verify`. `maven-shade-plugin` substitui o jar fino pelo jar
completo na mesma fase e no mesmo caminho de saída, sem alterar o comando de
execução documentado.

## Uso de inteligência artificial

Claude (Anthropic) foi usado em três papéis diferentes ao longo do projeto,
cada um com um propósito distinto:

**Design da arquitetura e da especificação técnica.** A estrutura de
pacotes, os contratos de cada classe, as decisões sobre ambiguidades do
enunciado (integridade referencial, vendedor sem venda, nome do arquivo de
saída, contagem de registros duplicados) e a tabela de casos de teste foram
discutidos e travados antes de qualquer linha de código, num documento de
especificação que serviu de contrato para a implementação. Cada decisão de
ambiguidade foi meu julgamento explícito sobre o trade-off, não escolha
automática da IA — está documentado seção por seção acima, incluindo os
riscos assumidos.

**Implementação, fatiada por camada.** O código de cada pacote
(`domain`+`parser`, `analysis`, `report`, `processing/FileProcessor`,
`DirectoryWatcher`+`Application`) foi gerado em cinco etapas sequenciais,
cada uma restrita ao escopo da especificação e revisada antes de avançar
para a próxima — nunca uma geração monolítica do projeto inteiro. Cada
fatia foi validada por mim rodando `mvn clean verify` localmente antes do
commit; a IA nunca teve acesso de execução ao meu ambiente.

**Depuração de configuração de build/CI.** A IA ajudou a diagnosticar
falhas reais que apareceram rodando localmente: a regra do JaCoCo aplicada
no elemento errado (contando `Application` na cobertura), o agente do
JaCoCo pulando silenciosamente por falta do `prepare-agent`, a falha de
verificação GPG da orb do Codecov, o `Missing Base Commit` por ausência de
histórico, e o jar fino sem dependências. Em todos os casos, o diagnóstico
partiu de um erro real que eu colei, não de execução direta da IA sobre o
projeto.

**Revisão.** Todo código gerado foi lido, compilado e testado por mim antes
do commit. As decisões de design documentadas acima — inclusive quando
divergem da leitura mais óbvia do enunciado — são minhas, e eu as defendo.