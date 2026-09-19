package dev.moonlight.moonporter.service;

import dev.moonlight.moonporter.config.MoonPorterConfig;
import dev.moonlight.moonporter.config.TitleMessage;
import dev.moonlight.moonporter.config.type.TitleType;
import dev.moonlight.moonporter.util.HexColorUtil;
import dev.moonlight.moonporter.util.PlaceholderUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Единая точка отправки сообщений игроку и консоли.
 * Строки уже окрашены и закэшированы в ConfigManager —
 * здесь остаётся префикс и подстановка плейсхолдеров.
 */
public final class MessageService {

    private final MoonPorterConfig config;

    public MessageService(@NotNull MoonPorterConfig config) {
        this.config = config;
    }

    /**
     * Отправляет игроку титул из конфига.
     * Тайминги берутся из общего блока settings.title.
     *
     * @param player       получатель
     * @param type         тип титула
     * @param placeholders плейсхолдеры для подстановки
     */
    public void sendTitle(@NotNull Player player,
                          @NotNull TitleType type,
                          @Nullable Map<String, ?> placeholders) {

        if (!config.isTitleEnabled()) {
            return;
        }

        TitleMessage title = config.getTitle(type);

        if (title.isDisabled()) {
            return;
        }

        player.sendTitle(
                PlaceholderUtil.apply(title.title(), placeholders),
                PlaceholderUtil.apply(title.subtitle(), placeholders),
                config.getTitleFadeIn(),
                config.getTitleStay(),
                config.getTitleFadeOut()
        );

    }

    /**
     * Отправляет игроку титул без плейсхолдеров.
     *
     * @param player получатель
     * @param type   тип титула
     */
    public void sendTitle(@NotNull Player player, @NotNull TitleType type) {
        sendTitle(player, type, null);
    }

    /**
     * Отправляет одну строку из конфига с префиксом.
     *
     * @param sender получатель (игрок или консоль)
     * @param text   строка сообщения
     */
    public void sendString(@NotNull CommandSender sender, @NotNull String text) {
        sendString(sender, text, null);
    }

    /**
     * Отправляет одну строку из конфига с плейсхолдерами.
     * Префикс не приклеивается автоматически: он доступен
     * как плейсхолдер {prefix} в любом сообщении.
     *
     * @param sender       получатель (игрок или консоль)
     * @param text         строка сообщения
     * @param placeholders плейсхолдеры для подстановки
     */
    public void sendString(@NotNull CommandSender sender,
                           @NotNull String text,
                           @Nullable Map<String, ?> placeholders) {

        if (text.isEmpty()) {
            return;
        }

        sender.sendMessage(PlaceholderUtil.apply(text, prefixPlaceholders(placeholders)));

    }

    /**
     * Отправляет список строк из конфига с плейсхолдерами.
     * Пустые строки списка сохраняются как отступы.
     *
     * @param sender       получатель (игрок или консоль)
     * @param lines        строки сообщения
     * @param placeholders плейсхолдеры для подстановки
     */
    public void sendLines(@NotNull CommandSender sender,
                          @Nullable List<String> lines,
                          @Nullable Map<String, ?> placeholders) {

        if (lines == null || lines.isEmpty()) {
            return;
        }

        Map<String, ?> values = prefixPlaceholders(placeholders);

        for (String line : lines) {

            if (line.isEmpty()) {

                sender.sendMessage("");
                continue;

            }

            sender.sendMessage(PlaceholderUtil.apply(line, values));

        }

    }

    /**
     * Добавляет плейсхолдер {prefix} к карте подстановок.
     *
     * @param placeholders плейсхолдеры вызывающего кода либо null
     * @return карта с префиксом и исходными плейсхолдерами
     */
    private @NotNull Map<String, ?> prefixPlaceholders(@Nullable Map<String, ?> placeholders) {

        Map<String, Object> values = new HashMap<>();

        values.put("prefix", config.getPrefix());

        if (placeholders != null) {
            values.putAll(placeholders);
        }

        return values;

    }

    /**
     * Подставляет плейсхолдеры и окрашивает строку без префикса.
     * Используется для имён сущностей, а не для чата.
     *
     * @param text         исходная строка
     * @param placeholders плейсхолдеры для подстановки
     * @return готовая строка
     */
    public @NotNull String applyPlaceholders(@NotNull String text,
                                             @Nullable Map<String, ?> placeholders) {
        return HexColorUtil.color(PlaceholderUtil.apply(text, placeholders));
    }

    /**
     * Форматирует награду по шаблону messages.reward_format.
     *
     * @param amount сумма награды
     * @return отформатированная строка
     */
    public @NotNull String formatReward(int amount) {
        return PlaceholderUtil.apply(config.getRewardFormat(), Map.of("amount", amount));
    }
}
