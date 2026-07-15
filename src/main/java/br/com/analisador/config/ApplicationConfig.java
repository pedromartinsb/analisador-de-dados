package br.com.analisador.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Carrega as configurações externalizadas e expande ${user.home},
 * evitando dependência de %HOMEPATH% (notação Windows) em ambiente Linux/CI.
 */
public final class ApplicationConfig {

    private static final String RESOURCE = "/application.properties";

    private final Properties properties;

    private ApplicationConfig(Properties properties) {
        this.properties = properties;
    }

    public static ApplicationConfig load() {
        return load(RESOURCE);
    }

    static ApplicationConfig load(String resource) {
        try (InputStream in = ApplicationConfig.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Arquivo não encontrado: " + resource);
            }
            Properties props = new Properties();
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return new ApplicationConfig(props);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao carregar " + resource, e);
        }
    }

    static ApplicationConfig of(Properties properties) {
        return new ApplicationConfig(properties);
    }

    public Path inputDirectory() {
        return Path.of(resolve("directory.in"));
    }

    public Path outputDirectory() {
        return Path.of(resolve("directory.out"));
    }

    public String fileExtension() {
        return resolve("file.extension");
    }

    public int workerThreads() {
        return Integer.parseInt(resolve("worker.threads"));
    }

    private String resolve(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Propriedade ausente: " + key);
        }
        return value.replace("${user.home}", System.getProperty("user.home"));
    }
}