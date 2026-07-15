package br.com.analisador;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.analisador.analysis.SalesAnalyzer;
import br.com.analisador.config.ApplicationConfig;
import br.com.analisador.parser.ClientParser;
import br.com.analisador.parser.FileParser;
import br.com.analisador.parser.LineParser;
import br.com.analisador.parser.SaleParser;
import br.com.analisador.parser.SellerParser;
import br.com.analisador.processing.DirectoryWatcher;
import br.com.analisador.processing.FileProcessor;
import br.com.analisador.report.ReportFormatter;
import br.com.analisador.report.ReportWriter;

/**
 * Composition root. Wiring puro — nenhuma regra de negócio deve viver
 * aqui (§10, D13). É a única classe excluída da cobertura de código: testá-
 * la exercitaria o bootstrap da JVM, não lógica do sistema. Se qualquer
 * {@code if} de negócio aparecer nesta classe, está no lugar errado.
 */
public final class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private Application() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ApplicationConfig config = ApplicationConfig.load();

        Files.createDirectories(config.inputDirectory());
        Files.createDirectories(config.outputDirectory());

        FileProcessor processor = new FileProcessor(
                new FileParser(new LineParser(List.of(
                        new SellerParser(), new ClientParser(), new SaleParser()))),
                new SalesAnalyzer(),
                new ReportWriter(config.outputDirectory(), new ReportFormatter()));

        ExecutorService executor = Executors.newFixedThreadPool(config.workerThreads());
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));

        log.info("Monitorando diretório de entrada: {}", config.inputDirectory());
        log.info("Diretório de saída: {}", config.outputDirectory());

        new DirectoryWatcher(config.inputDirectory(), config.fileExtension())
                .watch(file -> executor.submit(() -> processor.process(file)));
    }
}
