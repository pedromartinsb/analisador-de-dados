package br.com.analisador;

import br.com.analisador.config.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        ApplicationConfig config = ApplicationConfig.load();
        log.info("Diretório de entrada: {}", config.inputDirectory());
        log.info("Diretório de saída: {}", config.outputDirectory());
    }
}