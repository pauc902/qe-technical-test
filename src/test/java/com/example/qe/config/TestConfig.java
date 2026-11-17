package com.example.qe.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Reads settings. The first place that has a value wins:
 * 1. a command-line property, e.g. -Dorangehrm.password=...
 * 2. an environment variable, e.g. ORANGEHRM_PASSWORD
 * 3. src/test/resources/config.properties
 * So the tests run with no set-up, while a pipeline can still supply its own values.
 */
public final class TestConfig {

    private static final Properties DEFAULTS = load();

    private TestConfig() {
    }

    public static String get(String key) {
        String fromCommandLine = System.getProperty(key);
        if (fromCommandLine != null && !fromCommandLine.isBlank()) {
            return fromCommandLine;
        }
        String fromEnvironment = System.getenv(key.toUpperCase().replace('.', '_'));
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment;
        }
        String fromFile = DEFAULTS.getProperty(key);
        if (fromFile == null) {
            throw new IllegalStateException("Missing setting: " + key);
        }
        return fromFile;
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = TestConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return props;
    }
}
