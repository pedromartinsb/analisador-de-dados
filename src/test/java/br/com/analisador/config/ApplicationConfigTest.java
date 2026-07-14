package br.com.analisador.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationConfigTest {

    @Test
    void deveExpandirUserHomeNosDiretorios() {
        Properties props = new Properties();
        props.setProperty("directory.in", "${user.home}/data/in");
        props.setProperty("directory.out", "${user.home}/data/out");

        ApplicationConfig config = ApplicationConfig.of(props);
        String home = System.getProperty("user.home");

        assertEquals(Path.of(home, "data", "in"), config.inputDirectory());
        assertEquals(Path.of(home, "data", "out"), config.outputDirectory());
    }

    @Test
    void deveFalharQuandoPropriedadeAusente() {
        ApplicationConfig config = ApplicationConfig.of(new Properties());
        assertThrows(IllegalStateException.class, config::inputDirectory);
    }

    @Test
    void deveCarregarDoClasspath() {
        ApplicationConfig config = ApplicationConfig.load();
        assertEquals(".dat", config.fileExtension());
        assertEquals(4, config.workerThreads());
    }
}