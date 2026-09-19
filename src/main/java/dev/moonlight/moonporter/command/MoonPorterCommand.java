package dev.moonlight.moonporter.command;

import dev.moonlight.moonporter.config.ConfigManager;
import dev.moonlight.moonporter.config.PorterTier;
import dev.moonlight.moonporter.registry.PorterTierRegistry;
import dev.moonlight.moonporter.service.MessageService;
import dev.moonlight.moonporter.service.PermissionService;
import dev.moonlight.moonporter.service.PorterService;
import dev.moonlight.moonporter.service.ReloadService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Команда /moonporter — перезагрузка конфигурации и выдача груза.
 */
public final class MoonPorterCommand implements TabExecutor {

    private static final String ARG_RELOAD = "reload";
    private static final String ARG_GIVE = "give";
    private static final String ARG_VISUAL = "visual";

    private final ConfigManager config;
    private final PorterTierRegistry tierRegistry;
    private final MessageService messageService;
    private final PorterService porterService;
    private final ReloadService reloadService;
    private final PermissionService permissionService;

    public MoonPorterCommand(@NotNull ConfigManager config,
                             @NotNull PorterTierRegistry tierRegistry,
                             @NotNull MessageService messageService,
                             @NotNull PorterService porterService,
                             @NotNull ReloadService reloadService,
                             @NotNull PermissionService permissionService) {
        this.config = config;
        this.tierRegistry = tierRegistry;
        this.messageService = messageService;
        this.porterService = porterService;
        this.reloadService = reloadService;
        this.permissionService = permissionService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String @NotNull [] args) {

        if (args.length == 0) {

            messageService.sendLines(sender, config.getCommandUsageMessage(), usagePlaceholders(label));
            return true;

        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {

            case ARG_RELOAD -> reload(sender);

            case ARG_GIVE -> giveCargo(sender, label, args);

            case ARG_VISUAL -> tuneVisual(sender, args);

            default -> messageService.sendLines(sender, config.getCommandUsageMessage(), usagePlaceholders(label));

        }

        return true;

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                String @NotNull [] args) {

        if (args.length == 1) {

            if (!hasAdminPermission(sender)) {
                return Collections.emptyList();
            }

            return filter(List.of(ARG_RELOAD, ARG_GIVE, ARG_VISUAL), args[0]);

        }

        if (args.length == 2 && ARG_GIVE.equalsIgnoreCase(args[0])
                && hasAdminPermission(sender)) {

            return filter(List.copyOf(tierRegistry.getTierIds()), args[1]);

        }

        return Collections.emptyList();

    }

    /**
     * Перезагружает конфигурацию и снимает активные грузы.
     *
     * @param sender отправитель команды
     */
    private void reload(@NotNull CommandSender sender) {

        if (!hasPermission(sender)) {
            return;
        }

        reloadService.reload(sender);

    }

    /**
     * Выдаёт груз игроку в обход NPC.
     *
     * @param sender отправитель команды
     * @param label  имя команды, которым её вызвали
     * @param args   аргументы команды
     */
    private void giveCargo(@NotNull CommandSender sender,
                           @NotNull String label,
                           String @NotNull [] args) {

        if (!hasPermission(sender)) {
            return;
        }

        if (!(sender instanceof Player player)) {

            messageService.sendString(sender, config.getCommandPlayerOnlyMessage());
            return;

        }

        if (tierRegistry.size() == 0) {

            messageService.sendString(sender, config.getCommandNoTiersMessage());
            return;

        }

        PorterTier tier = resolveTier(args);

        if (tier == null) {

            messageService.sendLines(sender, config.getCommandUsageMessage(), usagePlaceholders(label));
            return;

        }

        porterService.pickup(player, tier);

    }

    /**
     * Разбирает уровень груза из аргумента команды.
     *
     * @param args аргументы команды
     * @return уровень из реестра, случайный без аргумента либо null, если ключ не найден
     */
    private @Nullable PorterTier resolveTier(String @NotNull [] args) {

        if (args.length < 2) {
            return tierRegistry.getRandomTier();
        }

        return tierRegistry.getTier(args[1]);

    }

    /**
     * Живая подстройка визуализации HANDS на несомом грузе.
     * Финальные значения администратор переносит в config.yml вручную.
     *
     * @param sender отправитель команды
     * @param args   аргументы: forward height left [name_height]
     */
    private void tuneVisual(@NotNull CommandSender sender, String @NotNull [] args) {

        if (!hasPermission(sender)) {
            return;
        }

        if (!(sender instanceof Player)) {

            messageService.sendString(sender, config.getCommandPlayerOnlyMessage());
            return;

        }

        if (args.length < 4) {

            messageService.sendString(sender, config.getCommandUsageMessage());
            return;

        }

        try {

            double forward = Double.parseDouble(args[1]);
            double height = Double.parseDouble(args[2]);
            double left = Double.parseDouble(args[3]);
            double nameHeight = args.length >= 5
                    ? Double.parseDouble(args[4])
                    : config.getNameHeight();

            config.tuneHandsVisual(forward, height, left, nameHeight);

            messageService.sendString(sender, config.getCommandVisualUpdatedMessage(), Map.of(
                    "forward", forward,
                    "height", height,
                    "left", left,
                    "name_height", nameHeight
            ));

        } catch (NumberFormatException exception) {

            messageService.sendString(sender, config.getCommandUsageMessage());

        }
    }

    /**
     * Проверяет право администратора и отправляет сообщение при отказе.
     *
     * @param sender отправитель команды
     * @return true если действие разрешено
     */
    private boolean hasPermission(@NotNull CommandSender sender) {

        if (hasAdminPermission(sender)) {
            return true;
        }

        messageService.sendString(sender, config.getCommandNoPermissionMessage());
        return false;

    }

    /**
     * Проверяет право администратора без отправки сообщений.
     *
     * @param sender отправитель команды
     * @return true если система прав выключена или право выдано
     */
    private boolean hasAdminPermission(@NotNull CommandSender sender) {
        return permissionService.hasAdmin(sender);
    }

    /**
     * Собирает плейсхолдеры справки по команде.
     *
     * @param label имя команды, которым её вызвали
     * @return карта плейсхолдеров
     */
    private @NotNull Map<String, ?> usagePlaceholders(@NotNull String label) {

        return Map.of(
                "label", label,
                "tiers", String.join(", ", tierRegistry.getTierIds()),
                "regions", String.join(", ", config.getAllowedRegions())
        );

    }

    /**
     * Оставляет только значения, начинающиеся с введённого текста.
     *
     * @param values список вариантов
     * @param token  введённый текст
     * @return отфильтрованный список
     */
    public static @NotNull List<String> filter(@NotNull List<String> values, @NotNull String token) {

        String lowerToken = token.toLowerCase(Locale.ROOT);

        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lowerToken))
                .toList();

    }
}
