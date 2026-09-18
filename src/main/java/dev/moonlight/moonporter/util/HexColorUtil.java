package dev.moonlight.moonporter.util;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилита для обработки HEX-цветов в строках.
 * Конвертирует формат #RRGGBB в &x&R&R&G&G&B&B.
 */
public final class HexColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    /**
     * Преобразует HEX-коды и амперсанд-коды строки в Minecraft-формат.
     *
     * @param text исходная строка с цветовыми кодами
     * @return строка с преобразованными цветовыми кодами
     */
    public static @NotNull String color(@Nullable String text) {

        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(text);

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {

            String hexCode = matcher.group();

            if (!isValidHexCode(hexCode)) {
                continue;
            }

            result.append(text, lastEnd, matcher.start());
            result.append(toLegacyHex(hexCode));

            lastEnd = matcher.end();

        }

        result.append(text.substring(lastEnd));

        return ChatColor.translateAlternateColorCodes('&', result.toString());

    }

    /**
     * Преобразует #RRGGBB в &x&R&R&G&G&B&B.
     *
     * @param hexCode валидный HEX-код
     * @return строка в legacy-формате
     */
    private static @NotNull String toLegacyHex(@NotNull String hexCode) {

        StringBuilder replacement = new StringBuilder("&x");

        for (int i = 1; i < hexCode.length(); i++) {
            replacement.append("&").append(hexCode.charAt(i));
        }

        return replacement.toString();

    }

    /**
     * Проверяет валидность HEX-кода.
     *
     * @param code строка с HEX-кодом (#RRGGBB)
     * @return true если код валиден, false иначе
     */
    private static boolean isValidHexCode(@NotNull String code) {

        if (code.length() != 7) {
            return false;
        }

        for (int i = 1; i < code.length(); i++) {

            char c = code.charAt(i);

            if (!isHexDigit(c)) {
                return false;
            }

        }

        return true;

    }

    /**
     * Проверяет, является ли символ шестнадцатеричной цифрой.
     *
     * @param c проверяемый символ
     * @return true если символ входит в диапазон 0-9, a-f, A-F
     */
    private static boolean isHexDigit(char c) {
        return Character.isDigit(c)
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }
}
