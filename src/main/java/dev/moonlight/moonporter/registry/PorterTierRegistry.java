package dev.moonlight.moonporter.registry;

import dev.moonlight.moonporter.config.MoonPorterConfig;
import dev.moonlight.moonporter.config.PorterTier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Реестр уровней груза.
 *
 * Уровни читаются из settings.porters конфига в порядке объявления.
 * Встроенных текстов в коде нет: если секция пуста, реестр остаётся
 * пустым, выдача груза отключается, а ConfigManager пишет об этом
 * warning в консоль при разборе конфигурации.
 */
public final class PorterTierRegistry {

    private final MoonPorterConfig config;
    private final Map<String, PorterTier> tiers = new LinkedHashMap<>();

    public PorterTierRegistry(@NotNull MoonPorterConfig config) {
        this.config = config;
        initializeTiers();
    }

    private void initializeTiers() {
        config.getPorterTiers().forEach(tier -> tiers.put(tier.id(), tier));
    }

    /**
     * Перечитывает уровни после перезагрузки конфигурации.
     */
    public void reloadRegistry() {
        tiers.clear();
        initializeTiers();
    }

    /**
     * Возвращает уровень по ключу конфига без учёта регистра.
     *
     * @param id ключ секции уровня
     * @return уровень либо null, если такого ключа нет
     */
    public @Nullable PorterTier getTier(@NotNull String id) {
        return tiers.get(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Возвращает случайный зарегистрированный уровень.
     * Общий экземпляр ThreadLocalRandom вместо new Random() на каждый вызов.
     *
     * @return случайный уровень либо null, если реестр пуст
     */
    public @Nullable PorterTier getRandomTier() {

        List<PorterTier> values = List.copyOf(tiers.values());

        if (values.isEmpty()) {
            return null;
        }

        return values.get(ThreadLocalRandom.current().nextInt(values.size()));

    }

    /**
     * Ключи всех зарегистрированных уровней в порядке объявления.
     *
     * @return неизменяемая коллекция ключей
     */
    public @NotNull Collection<String> getTierIds() {
        return Collections.unmodifiableCollection(tiers.keySet());
    }

    /**
     * Количество зарегистрированных уровней.
     *
     * @return число уровней
     */
    public int size() {
        return tiers.size();
    }
}
