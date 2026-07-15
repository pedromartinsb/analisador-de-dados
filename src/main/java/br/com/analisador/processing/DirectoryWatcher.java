package br.com.analisador.processing;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitora um diretório por arquivos com a extensão configurada, invocando
 * um handler para cada um — tanto os já presentes na inicialização
 * ({@link #scanExisting()}) quanto os que chegarem depois via
 * {@link WatchService} (§10). Bloqueia até interrupção.
 *
 * <p><strong>Importante:</strong> o diretório monitorado e o diretório de
 * saída dos relatórios devem ser distintos. Se coincidirem, cada
 * {@code .done.dat} escrito dispara um novo {@code ENTRY_CREATE} no mesmo
 * diretório observado, e — como o sufixo de saída termina em
 * {@code .dat} — o watcher tentaria reprocessar seu próprio relatório como
 * se fosse entrada nova, criando um ciclo de retroalimentação. A
 * configuração real ({@code directory.in} / {@code directory.out}) já
 * mantém os diretórios separados; isso é uma armadilha de configuração,
 * não um caso tratado internamente por esta classe.
 *
 * <p>{@code ENTRY_CREATE} dispara quando o arquivo é criado, não quando
 * termina de ser escrito. {@link #awaitStable(Path)} mitiga isso aguardando
 * o tamanho do arquivo estabilizar entre duas checagens sucessivas — é
 * mitigação pragmática, não garantia formal: um processo que escreve mais
 * devagar que o intervalo entre duas amostras estáveis ainda pode ser lido
 * parcialmente. Não há solução 100% confiável para "arquivo terminou de
 * ser escrito" via NIO puro, sem depender de convenções externas (ex.
 * escrever em um nome temporário e renomear ao final).
 */
public class DirectoryWatcher {

    private static final Logger log = LoggerFactory.getLogger(DirectoryWatcher.class);

    private static final long STABILITY_CHECK_INTERVAL_MS = 100;
    private static final int MAX_STABILITY_CHECKS = 10;

    private final Path directory;
    private final String extension;

    public DirectoryWatcher(Path directory, String extension) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.extension = Objects.requireNonNull(extension, "extension");
    }

    public void watch(Consumer<Path> handler) throws IOException, InterruptedException {
        Objects.requireNonNull(handler, "handler");

        for (Path existing : scanExisting()) {
            dispatch(existing, handler);
        }

        try (WatchService watchService = directory.getFileSystem().newWatchService()) {
            directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    Path fileName = (Path) event.context();
                    Path file = directory.resolve(fileName);
                    if (isTarget(file)) {
                        dispatch(file, handler);
                    }
                }
                if (!key.reset()) {
                    log.error("Diretório {} tornou-se inacessível; encerrando monitoramento", directory);
                    break;
                }
            }
        }
    }

    private void dispatch(Path file, Consumer<Path> handler) throws InterruptedException {
        awaitStable(file);
        handler.accept(file);
    }

    List<Path> scanExisting() throws IOException {
        List<Path> found = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (isTarget(path)) {
                    found.add(path);
                }
            }
        }
        found.sort(Comparator.comparing(Path::toString));
        return found;
    }

    boolean isTarget(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(extension);
    }

    void awaitStable(Path path) throws InterruptedException {
        long previousSize = -1;
        for (int attempt = 0; attempt < MAX_STABILITY_CHECKS; attempt++) {
            long currentSize;
            try {
                currentSize = Files.size(path);
            } catch (IOException e) {
                // Arquivo sumiu ou ficou inacessível entre a detecção e a
                // leitura do tamanho; deixa o parser lidar com isso adiante,
                // não é responsabilidade desta checagem de estabilidade.
                return;
            }
            if (currentSize == previousSize) {
                return;
            }
            previousSize = currentSize;
            Thread.sleep(STABILITY_CHECK_INTERVAL_MS);
        }
        log.warn("Arquivo {} não estabilizou após {} tentativas; prosseguindo mesmo assim",
                path, MAX_STABILITY_CHECKS);
    }
}
