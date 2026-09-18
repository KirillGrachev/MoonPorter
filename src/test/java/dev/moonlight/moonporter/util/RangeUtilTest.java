package dev.moonlight.moonporter.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeUtilTest {

    @Test
    @DisplayName("Разбирает диапазон min-max")
    void parsesRange() {
        int[] range = RangeUtil.parse("5-10").orElseThrow();

        assertEquals(5, range[0]);
        assertEquals(10, range[1]);
    }

    @Test
    @DisplayName("Разбирает одиночное значение как диапазон из одной точки")
    void parsesSingleValue() {
        int[] range = RangeUtil.parse("10").orElseThrow();

        assertEquals(10, range[0]);
        assertEquals(10, range[1]);
    }

    @Test
    @DisplayName("Меняет местами перевёрнутый диапазон")
    void swapsInvertedRange() {
        int[] range = RangeUtil.parse("10-5").orElseThrow();

        assertEquals(5, range[0]);
        assertEquals(10, range[1]);
    }

    @Test
    @DisplayName("Возвращает empty для мусора")
    void returnsEmptyForGarbage() {
        assertTrue(RangeUtil.parse(null).isEmpty());
        assertTrue(RangeUtil.parse("").isEmpty());
        assertTrue(RangeUtil.parse("abc").isEmpty());
        assertTrue(RangeUtil.parse("1-2-3").isEmpty());
    }

    @Test
    @DisplayName("Случайное значение всегда в границах диапазона")
    void randomStaysInsideRange() {

        int[] range = {5, 10};

        for (int i = 0; i < 500; i++) {

            int value = RangeUtil.random(range, 0);

            assertTrue(value >= 5 && value <= 10, "Значение вышло за границы: " + value);

        }
    }

    @Test
    @DisplayName("Возвращает значение по умолчанию при невалидном диапазоне")
    void returnsDefaultForInvalidRange() {
        assertEquals(7, RangeUtil.random(new int[]{}, 7));
        assertEquals(7, RangeUtil.random(new int[]{-1, 10}, 7));
    }
}
