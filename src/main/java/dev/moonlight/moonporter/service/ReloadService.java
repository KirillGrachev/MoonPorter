package dev.moonlight.moonporter.service;

import dev.moonlight.moonporter.config.ConfigManager;
import dev.moonlight.moonporter.registry.PorterTierRegistry;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Перезагрузка конфигурации без рестарта сервера.
 *
 * Все активные грузы снимаются: их настройки (вес, награда, материал)
 * могли измениться, а сессии хранят значения, прочитанные при выдаче.
 * Реестр уровней пересобирается следом за конфигурацией.
 * Текст отчёта целиком берётся из config.yml (messages.command).
 */
public final class ReloadService {

    private final ConfigManager configManager;
    private final PorterTierRegistry tierRegistry;
    private final PorterService porterService;
    private final MessageService messageService;

    public ReloadService(@NotNull ConfigManager configManager,
                         @NotNull PorterTierRegistry tierRegistry,
                         @NotNull PorterService porterService,
                         @NotNull MessageService messageService) {
        this.configManager = configManager;
        this.tierRegistry = tierRegistry;
        this.porterService = porterService;
        this.messageService = messageService;
    }

    /**
     * Перечитывает config.yml, пересобирает уровни и снимает все активные грузы.
     *
     * @param sender получатель отчёта
     */
    public void reload(@NotNull CommandSender sender) {

        int cancelled = porterService.cancelAll();

        configManager.reload();
        tierRegistry.reloadRegistry();

        messageService.sendString(sender, configManager.getReloadSuccessMessage(),
                Map.of("count", cancelled));

        messageService.sendLines(sender,
                configManager.getReloadReportMessage(),
                configManager.describeValues());

    }
}
