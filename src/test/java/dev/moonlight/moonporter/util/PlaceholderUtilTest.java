package dev.moonlight.moonporter.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaceholderUtilTest {

    @Test
    @DisplayName("Возвращает пустую строку для null и пустого текста")
    void returnsEmptyForNullOrEmpty() {
        assertEquals("", PlaceholderUtil.apply(null, null));
        assertEquals("", PlaceholderUtil.apply("", null));
    }

    @Test
    @DisplayName("Подставляет одиночный плейсхолдер")
    void appliesSinglePlaceholder() {
        assertEquals("Вы получили 15$",
                PlaceholderUtil.apply("Вы получили {amount}$", Map.of("amount", 15)));
    }

    @Test
    @DisplayName("Подставляет несколько плейсхолдеров в одной строке")
    void appliesMultiplePlaceholders() {

        String result = PlaceholderUtil.apply("{player} несёт {tier}",
                Map.of("player", "Ney", "tier", "LARGE"));

        assertEquals("Ney несёт LARGE", result);

    }

    @Test
    @DisplayName("Заменяет все вхождения одного плейсхолдера")
    void appliesRepeatedPlaceholder() {
        assertEquals("2-2", PlaceholderUtil.apply("{weight}-{weight}", Map.of("weight", 2)));
    }

    @Test
    @DisplayName("Оставляет текст без изменений при пустой карте")
    void keepsTextWhenNoPlaceholders() {
        assertEquals("Груз", PlaceholderUtil.apply("Груз", Map.of()));
        assertEquals("Груз", PlaceholderUtil.apply("Груз", null));
    }

    @Test
    @DisplayName("Неизвестный плейсхолдер остаётся в тексте")
    void keepsUnknownPlaceholder() {
        assertEquals("{unknown}", PlaceholderUtil.apply("{unknown}", Map.of("amount", 1)));
    }
}
