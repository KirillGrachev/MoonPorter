package dev.moonlight.moonporter.config;

import dev.moonlight.moonporter.MoonPorter;
import dev.moonlight.moonporter.config.type.DeliveryTrigger;
import dev.moonlight.moonporter.config.type.TitleType;
import dev.moonlight.moonporter.util.HexColorUtil;
import dev.moonlight.moonporter.util.RangeUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Читает config.yml один раз при старте и кэширует все значения.
 * Горячий путь (клик по NPC, приседание, тик таймера) не обращается к YAML.
 */
public final class ConfigManager implements MoonPorterConfig {

    private final @Nullable MoonPorter plugin;
    private final Logger logger;
    private FileConfiguration config;

    private static final String PATH_ENABLED = "settings.enabled";
    private static final String PATH_MATERIAL = "settings.material";
    private static final String PATH_TITLE_ENABLED = "settings.title.enabled";
    private static final String PATH_TITLE_FADE_IN = "settings.title.fade_in";
    private static final String PATH_TITLE_STAY = "settings.title.stay";
    private static final String PATH_TITLE_FADE_OUT = "settings.title.fade_out";
    private static final String PATH_DELIVERY_TRIGGER = "settings.delivery.trigger";
    private static final String PATH_DELIVERY_TIMEOUT = "settings.delivery.timeout";
    private static final String PATH_DELIVERY_RADIUS = "settings.delivery.radius";
    private static final String PATH_COOLDOWN_ENABLED = "settings.cooldown.enabled";
    private static final String PATH_COOLDOWN_TIME = "settings.cooldown.time";
    private static final String PATH_PERMISSIONS_ENABLED = "settings.permissions.enabled";
    private static final String PATH_PERMISSION_ADMIN = "settings.permissions.admin";
    private static final String PATH_PERMISSION_USE = "settings.permissions.use";
    private static final String PATH_PERMISSION_BYPASS_COOLDOWN = "settings.permissions.bypass_cooldown";
    private static final String PATH_NPC_IDS = "settings.npc.ids";
    private static final String PATH_ALLOWED_WORLDS = "settings.allowed_worlds";
    private static final String PATH_ALLOWED_REGIONS = "settings.allowed_regions";
    private static final String PATH_PORTERS = "settings.porters";
    private static final String PATH_PREFIX = "messages.prefix";
    private static final String PATH_REWARD_FORMAT = "messages.reward_format";
    private static final String PATH_COMMAND_NO_PERMISSION = "messages.command.no_permission";
    private static final String PATH_COMMAND_PLAYER_ONLY = "messages.command.player_only";
    private static final String PATH_COMMAND_USAGE = "messages.command.usage";
    private static final String PATH_RELOAD_SUCCESS = "messages.command.reload_success";
    private static final String PATH_RELOAD_ENTRY = "messages.command.reload_entry";

    private boolean enabled;
    private Material material;
    private boolean titleEnabled;
    private int titleFadeIn;
    private int titleStay;
    private int titleFadeOut;
    private DeliveryTrigger deliveryTrigger;
    private int deliveryTimeout;
    private double deliveryRadius;
    private boolean cooldownEnabled;
    private int cooldownTime;
    private boolean permissionsEnabled;
    private String permissionAdmin;
    private String permissionUse;
    private String permissionBypassCooldown;
    private List<Integer> npcIds;
    private List<String> allowedWorlds;
    private List<String> allowedRegions;
    private List<PorterTier> porterTiers;
    private String prefix;
    private String rewardFormat;
    private String commandNoPermissionMessage;
    private String commandPlayerOnlyMessage;
    private List<String> commandUsageMessage;
    private String reloadSuccessMessage;
    private String reloadEntryFormat;

    private final Map<TitleType, TitleMessage> titles = new EnumMap<>(TitleType.class);

    public ConfigManager(@NotNull MoonPorter plugin) {

        this.plugin = plugin;
        this.logger = plugin.getLogger();

        saveDefaultConfig();

        loadConfig();
        cacheConfigValues();

    }

    /**
     * Создает менеджер поверх готовой конфигурации, без плагина.
     * Используется тестами: значения читаются из переданного FileConfiguration.
     *
     * @param config готовая конфигурация
     * @param logger логгер для предупреждений о некорректных значениях
     */
    public ConfigManager(@NotNull FileConfiguration config, @NotNull Logger logger) {

        this.plugin = null;
        this.logger = logger;
        this.config = config;

        cacheConfigValues();

    }

    private void saveDefaultConfig() {

        if (plugin != null) {
            plugin.saveDefaultConfig();
        }

    }

    private void loadConfig() {

        if (plugin == null) {
            return;
        }

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

    }

    private void cacheConfigValues() {

        enabled = config.getBoolean(PATH_ENABLED, true);
        material = readMaterial();

        titleEnabled = config.getBoolean(PATH_TITLE_ENABLED, true);
        titleFadeIn = config.getInt(PATH_TITLE_FADE_IN, 20);
        titleStay = config.getInt(PATH_TITLE_STAY, 40);
        titleFadeOut = config.getInt(PATH_TITLE_FADE_OUT, 20);

        deliveryTrigger = readEnum(PATH_DELIVERY_TRIGGER, DeliveryTrigger.class, DeliveryTrigger.SNEAK_TOGGLE);
        deliveryTimeout = Math.max(1, config.getInt(PATH_DELIVERY_TIMEOUT, 15));
        deliveryRadius = config.getDouble(PATH_DELIVERY_RADIUS, 0.0D);

        cooldownEnabled = config.getBoolean(PATH_COOLDOWN_ENABLED, false);
        cooldownTime = Math.max(0, config.getInt(PATH_COOLDOWN_TIME, 30));

        permissionsEnabled = config.getBoolean(PATH_PERMISSIONS_ENABLED, false);
        permissionAdmin = config.getString(PATH_PERMISSION_ADMIN, "moonporter.admin");
        permissionUse = config.getString(PATH_PERMISSION_USE, "moonporter.use");
        permissionBypassCooldown = config.getString(PATH_PERMISSION_BYPASS_COOLDOWN, "moonporter.bypass.cooldown");

        npcIds = readNpcIds();
        allowedWorlds = readStringList(PATH_ALLOWED_WORLDS);
        allowedRegions = readStringList(PATH_ALLOWED_REGIONS);

        prefix = HexColorUtil.color(config.getString(PATH_PREFIX, ""));
        rewardFormat = config.getString(PATH_REWARD_FORMAT, "{amount}$");

        commandNoPermissionMessage = HexColorUtil.color(config.getString(PATH_COMMAND_NO_PERMISSION, ""));
        commandPlayerOnlyMessage = HexColorUtil.color(config.getString(PATH_COMMAND_PLAYER_ONLY, ""));
        commandUsageMessage = readColoredList(PATH_COMMAND_USAGE);
        reloadSuccessMessage = HexColorUtil.color(config.getString(PATH_RELOAD_SUCCESS, ""));
        reloadEntryFormat = HexColorUtil.color(config.getString(PATH_RELOAD_ENTRY, " &7• &f{line}"));

        cachePorterTiers();
        cacheTitles();

    }

    /**
     * Разбирает материал груза с откатом к BARREL при опечатке в конфиге.
     *
     * @return валидный материал
     */
    private @NotNull Material readMaterial() {

        String raw = config.getString(PATH_MATERIAL, Material.BARREL.name());

        Material resolved = Material.matchMaterial(raw);

        if (resolved == null || !resolved.isBlock()) {

            logger.warning("Invalid settings.material: '" + raw + "'. Using BARREL.");
            return Material.BARREL;

        }

        return resolved;

    }

    /**
     * Читает список ID NPC, выдающих груз.
     *
     * @return неизменяемый список ID
     */
    private @NotNull List<Integer> readNpcIds() {

        List<Integer> ids = config.getIntegerList(PATH_NPC_IDS);

        if (ids.isEmpty()) {
            logger.warning("No NPC ids configured: " + PATH_NPC_IDS + " is empty.");
        }

        return Collections.unmodifiableList(ids);

    }

    private @NotNull List<String> readStringList(@NotNull String path) {

        return config.getStringList(path).stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableList());

    }

    /**
     * Читает блок сообщения-списка вида "text: [...]" и окрашивает строки.
     *
     * @param path путь к списку строк
     * @return неизменяемый список окрашенных строк
     */
    private @NotNull List<String> readColoredList(@NotNull String path) {

        return config.getStringList(path).stream()
                .map(HexColorUtil::color)
                .collect(Collectors.toUnmodifiableList());

    }

    /**
     * Читает уровни груза из settings.porters в порядке объявления секций.
     * Отсутствующая секция — пустой список: реестр подставит значения по умолчанию.
     */
    private void cachePorterTiers() {

        List<PorterTier> tiers = new ArrayList<>();

        ConfigurationSection root = config.getConfigurationSection(PATH_PORTERS);

        if (root != null) {

            for (String id : root.getKeys(false)) {

                ConfigurationSection section = root.getConfigurationSection(id);

                if (section == null) {
                    continue;
                }

                tiers.add(readPorterTier(id.toLowerCase(Locale.ROOT), section));

            }
        }

        porterTiers = Collections.unmodifiableList(tiers);

    }

    /**
     * Разбирает одну секцию уровня груза.
     * Отсутствующие поля получают значения по умолчанию.
     *
     * @param id      ключ секции
     * @param section секция уровня
     * @return разобранный уровень
     */
    private @NotNull PorterTier readPorterTier(@NotNull String id, @NotNull ConfigurationSection section) {

        String name = section.getString("name", "&f" + id);

        String rawReward = section.getString("reward", "0-0");

        Optional<int[]> reward = RangeUtil.parse(rawReward);

        if (reward.isEmpty()) {

            logger.warning("Invalid reward format for tier '" + id + "': '" + rawReward
                    + "'. Expected \"min-max\".");

        }

        int rewardMin = reward.map(range -> range[0]).orElse(0);
        int rewardMax = reward.map(range -> range[1]).orElse(0);

        int weight = Math.max(0, Math.min(section.getInt("weight", 1), 10));

        return new PorterTier(id, name, rewardMin, rewardMax, weight);

    }

    private void cacheTitles() {

        titles.clear();

        for (TitleType type : TitleType.values()) {

            ConfigurationSection section = config.getConfigurationSection("messages." + type.getPath());

            if (section == null) {

                titles.put(type, TitleMessage.empty());
                continue;

            }

            titles.put(type, new TitleMessage(
                    section.getBoolean("enabled", true),
                    HexColorUtil.color(section.getString("title", "")),
                    HexColorUtil.color(section.getString("subtitle", ""))
            ));

        }
    }

    private <T extends Enum<T>> @NotNull T readEnum(@NotNull String path,
                                                    @NotNull Class<T> type,
                                                    @NotNull T def) {

        String raw = config.getString(path);

        if (raw == null) {
            return def;
        }

        try {

            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));

        } catch (IllegalArgumentException exception) {

            logger.warning("Invalid value of '" + path + "': '" + raw + "'. Using " + def.name() + ".");
            return def;

        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public @NotNull Material getMaterial() {
        return material;
    }

    @Override
    public boolean isTitleEnabled() {
        return titleEnabled;
    }

    @Override
    public int getTitleFadeIn() {
        return titleFadeIn;
    }

    @Override
    public int getTitleStay() {
        return titleStay;
    }

    @Override
    public int getTitleFadeOut() {
        return titleFadeOut;
    }

    @Override
    public @NotNull DeliveryTrigger getDeliveryTrigger() {
        return deliveryTrigger;
    }

    @Override
    public int getDeliveryTimeout() {
        return deliveryTimeout;
    }

    @Override
    public double getDeliveryRadius() {
        return deliveryRadius;
    }

    @Override
    public boolean isCooldownEnabled() {
        return cooldownEnabled;
    }

    @Override
    public int getCooldownTime() {
        return cooldownTime;
    }

    @Override
    public boolean arePermissionsEnabled() {
        return permissionsEnabled;
    }

    @Override
    public @NotNull String getPermissionAdmin() {
        return permissionAdmin;
    }

    @Override
    public @NotNull String getPermissionUse() {
        return permissionUse;
    }

    @Override
    public @NotNull String getPermissionBypassCooldown() {
        return permissionBypassCooldown;
    }

    @Override
    public @NotNull List<Integer> getNpcIds() {
        return npcIds;
    }

    @Override
    public @NotNull List<String> getAllowedWorlds() {
        return allowedWorlds;
    }

    @Override
    public @NotNull List<String> getAllowedRegions() {
        return allowedRegions;
    }

    @Override
    public @NotNull List<PorterTier> getPorterTiers() {
        return porterTiers;
    }

    @Override
    public @NotNull String getPrefix() {
        return prefix;
    }

    @Override
    public @NotNull TitleMessage getTitle(@NotNull TitleType type) {
        return titles.getOrDefault(type, TitleMessage.empty());
    }

    @Override
    public @NotNull String getRewardFormat() {
        return rewardFormat;
    }

    @Override
    public @NotNull String getCommandNoPermissionMessage() {
        return commandNoPermissionMessage;
    }

    @Override
    public @NotNull String getCommandPlayerOnlyMessage() {
        return commandPlayerOnlyMessage;
    }

    @Override
    public @NotNull List<String> getCommandUsageMessage() {
        return commandUsageMessage;
    }

    @Override
    public @NotNull String getReloadSuccessMessage() {
        return reloadSuccessMessage;
    }

    @Override
    public @NotNull String getReloadEntryFormat() {
        return reloadEntryFormat;
    }

    @Override
    public void reload() {
        loadConfig();
        cacheConfigValues();
    }

    /**
     * Краткое описание прочитанных значений — для отчёта команды reload.
     *
     * @return неизменяемый список строк отчёта
     */
    public @NotNull List<String> describe() {

        List<String> description = new ArrayList<>();

        description.add("material: " + material.name());
        description.add("npc ids: " + npcIds);
        description.add("worlds: " + allowedWorlds);
        description.add("regions: " + allowedRegions);
        description.add("porters: " + porterTiers.stream().map(PorterTier::id).collect(Collectors.toList()));
        description.add("delivery: " + deliveryTrigger.name() + ", timeout " + deliveryTimeout + "s");
        description.add("cooldown: " + (cooldownEnabled ? cooldownTime + "s" : "off"));
        description.add("permissions: " + (permissionsEnabled ? "on" : "off"));

        return Collections.unmodifiableList(description);

    }
}
