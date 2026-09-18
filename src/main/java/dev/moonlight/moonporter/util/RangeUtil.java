package dev.moonlight.moonporter.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Утилита для работы с диапазонами вида "min-max".
 * Применяется к наградам из конфига (например, "5-10").
 */
public final class RangeUtil {

    /**
     * Разбирает строку диапазона.
     * Допускает одиночное значение ("10") и развёрнутый диапазон ("5-10").
     *
     * @param raw значение из конфига
     * @return пара [min, max] либо empty, если строка невалидна
     */
    public static Optional<int[]> parse(@Nullable String raw) {

        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String[] parts = raw.trim().split("-");

        try {

            if (parts.length == 1) {

                int single = Integer.parseInt(parts[0].trim());
                return Optional.of(new int[]{single, single});

            }

            if (parts.length != 2) {
                return Optional.empty();
            }

            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());

            if (min > max) {
                return Optional.of(new int[]{max, min});
            }

            return Optional.of(new int[]{min, max});

        } catch (NumberFormatException exception) {
            return Optional.empty();
        }

    }

    /**
     * Возвращает случайное значение внутри диапазона.
     *
     * @param range        пара [min, max]
     * @param defaultValue значение по умолчанию при невалидном диапазоне
     * @return случайное целое в пределах диапазона включительно
     */
    public static int random(@NotNull int[] range, int defaultValue) {

        if (range.length < 2 || range[0] < 0 || range[1] < range[0]) {
            return defaultValue;
        }

        if (range[0] == range[1]) {
            return range[0];
        }

        return ThreadLocalRandom.current().nextInt(range[0], range[1] + 1);

    }
}
