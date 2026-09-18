package dev.moonlight.moonporter.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Утилита для подстановки плейсхолдеров вида {key} в строки.
 * Используется для сообщений конфига: {player}, {amount}, {seconds} и т.д.
 */
public final class PlaceholderUtil {

    /**
     * Заменяет плейсхолдеры в строке.
     *
     * @param text         исходная строка
     * @param placeholders карта замен (ключ — имя плейсхолдера без скобок)
     * @return строка с подставленными значениями
     */
    public static @NotNull String apply(@Nullable String text,
                                        @Nullable Map<String, ?> placeholders) {

        if (text == null || text.isEmpty()) {
            return "";
        }

        if (placeholders == null || placeholders.isEmpty()) {
            return text;
        }

        String result = text;

        for (Map.Entry<String, ?> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }

        return result;

    }
}
