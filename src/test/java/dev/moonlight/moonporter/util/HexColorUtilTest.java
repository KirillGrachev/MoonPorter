package dev.moonlight.moonporter.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HexColorUtilTest {

    @Test
    @DisplayName("Возвращает пустую строку для null и пустого текста")
    void returnsEmptyForNullOrEmpty() {
        assertEquals("", HexColorUtil.color(null));
        assertEquals("", HexColorUtil.color(""));
    }

    @Test
    @DisplayName("Преобразует HEX-код в legacy-формат &x&R&R&G&G&B&B")
    void convertsHexCode() {
        String result = HexColorUtil.color("#ff0000Груз");

        assertTrue(result.contains("&xff0000") || result.contains("\u00A7x\u00A7f\u00A7f\u00A70\u00A70\u00A70"),
                "HEX-код не преобразован: " + result);
        assertTrue(result.endsWith("Груз"));
    }

    @Test
    @DisplayName("Оставляет текст без изменений, если HEX-кодов нет")
    void keepsPlainText() {
        assertEquals("Обычный текст", HexColorUtil.color("Обычный текст"));
    }

    @Test
    @DisplayName("Не трогает невалидный HEX-код")
    void keepsInvalidHexCode() {
        assertEquals("#zzzzzz", HexColorUtil.color("#zzzzzz"));
    }

    @Test
    @DisplayName("Обрабатывает несколько HEX-кодов в одной строке")
    void convertsSeveralHexCodes() {
        String result = HexColorUtil.color("#ff0000A#00ff00B");

        assertTrue(result.contains("A"));
        assertTrue(result.endsWith("B"));
    }
}
