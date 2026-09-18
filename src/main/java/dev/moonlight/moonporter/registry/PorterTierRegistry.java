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
 * Если секция отсутствует или пуста, регистрируется встроенный набор
 * по умолчанию (low, normal, large) — плагин работоспособен без настройки.
 */
public final class PorterTierRegistry {

    private final MoonPorterConfig config;
    private final Map<String, PorterTier> tiers = new LinkedHashMap<>();

    public PorterTierRegistry(@NotNull MoonPorterConfig config) {
        this.config = config;
        initializeTiers();
    }

    private void initializeTiers() {

        List<PorterTier> configured = config.getPorterTiers();

        if (configured.isEmpty()) {

            registerDefaultTiers();
            return;

        }

        configured.forEach(tier -> tiers.put(tier.id(), tier));

    }

    /**
     * Встроенный набор уровней: повторяет значения оригинального плагина.
     */
    private void registerDefaultTiers() {

        tiers.put("low", new PorterTier("low", "&eBarrel with weight {weight}", 5, 10, 1));
        tiers.put("normal", new PorterTier("normal", "&6Barrel with weight {weight}", 10, 25, 2));
        tiers.put("large", new PorterTier("large", "&cBarrel with weight {weight}", 25, 40, 3));

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
     * Проверяет наличие уровня в реестре.
     *
     * @param id ключ секции уровня
     * @return true если уровень зарегистрирован
     */
    public boolean isRegistered(@NotNull String id) {
        return tiers.containsKey(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Возвращает случайный зарегистрированный уровень.
     * Общий экземпляр ThreadLocalRandom вместо new Random() на каждый вызов.
     *
     * @return случайный уровень
     */
    public @NotNull PorterTier getRandomTier() {

        List<PorterTier> values = List.copyOf(tiers.values());

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
