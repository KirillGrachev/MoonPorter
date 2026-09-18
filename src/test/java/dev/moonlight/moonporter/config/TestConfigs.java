package dev.moonlight.moonporter.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Фикстуры конфигурации для unit-тестов.
 * Читает YAML из test-resources без подъёма Bukkit-сервера.
 */
public final class TestConfigs {

    private static final Logger LOGGER = Logger.getLogger("TestConfigs");

    /**
     * Загружает менеджер поверх тестового ресурса.
     *
     * @param resource путь к ресурсу в test-resources, например "/test-config.yml"
     * @return менеджер с закэшированными значениями
     */
    public static @NotNull ConfigManager load(@NotNull String resource) {

        InputStream stream = TestConfigs.class.getResourceAsStream(resource);

        if (stream == null) {
            throw new IllegalStateException("Test resource not found: " + resource);
        }

        FileConfiguration configuration = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));

        return new ConfigManager(configuration, LOGGER);

    }

    /**
     * Менеджер поверх пустой конфигурации: проверяет значения по умолчанию.
     *
     * @return менеджер с дефолтами
     */
    public static @NotNull ConfigManager empty() {
        return new ConfigManager(new YamlConfiguration(), LOGGER);
    }
}
