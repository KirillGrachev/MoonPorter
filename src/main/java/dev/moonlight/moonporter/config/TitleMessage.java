package dev.moonlight.moonporter.config;

import org.jetbrains.annotations.NotNull;

/**
 * Неизменяемый титул из конфига: флаг включения и две строки.
 * Тайминги показа общие для всех титулов и живут в settings.title.
 *
 * @param enabled  включён ли титул
 * @param title    основной текст титула
 * @param subtitle текст подзаголовка
 */
public record TitleMessage(boolean enabled,
                           @NotNull String title,
                           @NotNull String subtitle) {

    public static @NotNull TitleMessage empty() {
        return new TitleMessage(false, "", "");
    }

    /**
     * Отключён ли титул (флаг выключен или обе строки пустые).
     *
     * @return true если отправлять титул не нужно
     */
    public boolean isDisabled() {
        return !enabled || (title.isEmpty() && subtitle.isEmpty());
    }
}
