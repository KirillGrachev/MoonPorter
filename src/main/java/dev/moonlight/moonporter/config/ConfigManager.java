package dev.moonlight.moonporter.config;

import dev.moonlight.moonporter.MoonPorter;
import dev.moonlight.moonporter.config.type.CargoMode;
import dev.moonlight.moonporter.config.type.DeliveryTrigger;
import dev.moonlight.moonporter.config.type.TitleType;
import dev.moonlight.moonporter.util.HexColorUtil;
import dev.moonlight.moonporter.util.RangeUtil;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private static final String PATH_CARGO_MODE = "settings.cargo.mode";
    private static final String PATH_CARGO_NAME_VISIBLE = "settings.cargo.name_visible";
    private static final String PATH_RESET_FLIGHT = "settings.violations.reset_flight";
    private static final String PATH_RESET_GAMEMODE = "settings.violations.reset_gamemode";
    private static final String PATH_TITLE_ENABLED = "settings.title.enabled";
    private static final String PATH_TITLE_FADE_IN = "settings.title.fade_in";
    private static final String PATH_TITLE_STAY = "settings.title.stay";
    private static final String PATH_TITLE_FADE_OUT = "settings.title.fade_out";
    private static final String PATH_DELIVERY_TRIGGER = "settings.delivery.trigger";
    private static final String PATH_DELIVERY_TIMEOUT = "settings.delivery.timeout";
    private static final String PATH_DELIVERY_RADIUS = "settings.delivery.radius";
    private static final String PATH_BOSSBAR_ENABLED = "settings.delivery.bossbar.enabled";
    private static final String PATH_BOSSBAR_COLOR = "settings.delivery.bossbar.color";
    private static final String PATH_BOSSBAR_TEXT = "settings.delivery.bossbar.text";
    private static final String PATH_COOLDOWN_ENABLED = "settings.cooldown.enabled";
    private static final String PATH_COOLDOWN_TIME = "settings.cooldown.time";
    private static final String PATH_PERMISSIONS_ENABLED = "settings.permissions.enabled";
    private static final String PATH_PERMISSION_OP_BYPASS = "settings.permissions.op_bypass";
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
    private static final String PATH_COMMAND_NO_TIERS = "messages.command.no_tiers";
    private static final String PATH_RELOAD_REPORT = "messages.command.reload_report";

    private boolean enabled;
    private Material material;
    private CargoMode cargoMode;
    private boolean cargoNameVisible;
    private boolean resetFlight;
    private boolean resetGamemode;
    private boolean titleEnabled;
    private int titleFadeIn;
    private int titleStay;
    private int titleFadeOut;
    private DeliveryTrigger deliveryTrigger;
    private long deliveryTimeoutMillis;
    private double deliveryRadiusSquared;
    private boolean bossBarEnabled;
    private BarColor bossBarColor;
    private String bossBarText;
    private boolean cooldownEnabled;
    private long cooldownMillis;
    private boolean permissionsEnabled;
    private boolean opBypass;
    private String permissionAdmin;
    private String permissionUse;
    private String permissionBypassCooldown;
    private List<Integer> npcIds;
    private List<String> allowedWorlds;
    private Set<String> allowedWorldSet;
    private List<String> allowedRegions;
    private List<PorterTier> porterTiers;
    private String prefix;
    private String rewardFormat;
    private String commandNoPermissionMessage;
    private String commandPlayerOnlyMessage;
    private List<String> commandUsageMessage;
    private String reloadSuccessMessage;
    private String commandNoTiersMessage;
    private List<String> reloadReportMessage;

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

        cargoMode = readEnum(PATH_CARGO_MODE, CargoMode.class, CargoMode.BOTH);
        cargoNameVisible = config.getBoolean(PATH_CARGO_NAME_VISIBLE, true);

        resetFlight = config.getBoolean(PATH_RESET_FLIGHT, true);
        resetGamemode = config.getBoolean(PATH_RESET_GAMEMODE, true);

        titleEnabled = config.getBoolean(PATH_TITLE_ENABLED, true);
        titleFadeIn = config.getInt(PATH_TITLE_FADE_IN, 20);
        titleStay = config.getInt(PATH_TITLE_STAY, 40);
        titleFadeOut = config.getInt(PATH_TITLE_FADE_OUT, 20);

        deliveryTrigger = readEnum(PATH_DELIVERY_TRIGGER, DeliveryTrigger.class, DeliveryTrigger.SNEAK_TOGGLE);
        // Единицы и квадрат радиуса кэшируются сразу: в горячем пути
        // (выдача, сдача, тик наблюдателя) не остаётся арифметики.
        deliveryTimeoutMillis = Math.max(1, config.getInt(PATH_DELIVERY_TIMEOUT, 15)) * 1000L;
        deliveryRadiusSquared = square(config.getDouble(PATH_DELIVERY_RADIUS, 0.0D));

        bossBarEnabled = config.getBoolean(PATH_BOSSBAR_ENABLED, true);
        bossBarColor = readEnum(PATH_BOSSBAR_COLOR, BarColor.class, BarColor.YELLOW);
        bossBarText = config.getString(PATH_BOSSBAR_TEXT, "&eDelivery: &f{seconds}s &7left");

        cooldownEnabled = config.getBoolean(PATH_COOLDOWN_ENABLED, false);
        cooldownMillis = Math.max(0, config.getInt(PATH_COOLDOWN_TIME, 30)) * 1000L;

        permissionsEnabled = config.getBoolean(PATH_PERMISSIONS_ENABLED, false);
        opBypass = config.getBoolean(PATH_PERMISSION_OP_BYPASS, false);
        permissionAdmin = config.getString(PATH_PERMISSION_ADMIN, "moonporter.admin");
        permissionUse = config.getString(PATH_PERMISSION_USE, "moonporter.use");
        permissionBypassCooldown = config.getString(PATH_PERMISSION_BYPASS_COOLDOWN, "moonporter.bypass.cooldown");

        npcIds = readNpcIds();
        allowedWorlds = readStringList(PATH_ALLOWED_WORLDS);
        allowedWorldSet = Set.copyOf(allowedWorlds);
        allowedRegions = readStringList(PATH_ALLOWED_REGIONS);

        prefix = HexColorUtil.color(config.getString(PATH_PREFIX, ""));
        rewardFormat = config.getString(PATH_REWARD_FORMAT, "{amount}$");

        commandNoPermissionMessage = readMessageString(PATH_COMMAND_NO_PERMISSION);
        commandPlayerOnlyMessage = readMessageString(PATH_COMMAND_PLAYER_ONLY);
        commandUsageMessage = readColoredList(PATH_COMMAND_USAGE);
        reloadSuccessMessage = readMessageString(PATH_RELOAD_SUCCESS);
        commandNoTiersMessage = readMessageString(PATH_COMMAND_NO_TIERS);
        reloadReportMessage = readColoredList(PATH_RELOAD_REPORT);

        if (reloadReportMessage.isEmpty()) {

            logger.warning("Сообщение '" + PATH_RELOAD_REPORT + "' не найдено или пусто — "
                    + "отчёт перезагрузки показываться не будет.");

        }

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
     * Возводит радиус в квадрат: проверки расстояний работают
     * с distanceSquared и не извлекают корень каждый раз.
     *
     * @param radius радиус в блоках
     * @return квадрат радиуса
     */
    private static double square(double radius) {

        if (radius <= 0.0D) {
            return 0.0D;
        }

        return radius * radius;

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
     * Читает список строк сообщения вида "text: [...]" и окрашивает строки.
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
     * Читает одиночную строку сообщения из конфига.
     * Отсутствующий ключ не подменяется текстом из кода:
     * сообщение отключается, а в консоль уходит warning с путём.
     *
     * @param path путь к строке сообщения
     * @return окрашенная строка либо пустая строка
     */
    private @NotNull String readMessageString(@NotNull String path) {

        String raw = config.getString(path);

        if (raw == null || raw.isEmpty()) {

            logger.warning("Сообщение '" + path + "' не найдено в config.yml — "
                    + "отправляться не будет.");
            return "";

        }

        return HexColorUtil.color(raw);

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

        if (tiers.isEmpty()) {

            logger.warning("Секция '" + PATH_PORTERS + "' не найдена или пуста — "
                    + "уровней груза нет, выдача груза отключена до исправления конфигурации.");

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

        String rawName = section.getString("name");

        if (rawName == null || rawName.isBlank()) {

            logger.warning("В секции '" + PATH_PORTERS + "." + id
                    + "' нет name — именем уровня будет его ключ.");
            rawName = id;

        }

        String name = HexColorUtil.color(rawName);

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

            String path = "messages." + type.getPath();
            ConfigurationSection section = config.getConfigurationSection(path);

            if (section == null) {

                logger.warning("Секция сообщений '" + path
                        + "' не найдена в config.yml — титул отправляться не будет.");
                titles.put(type, TitleMessage.empty());
                continue;

            }

            boolean enabled = section.getBoolean("enabled", true);
            String title = HexColorUtil.color(section.getString("title", ""));
            String subtitle = HexColorUtil.color(section.getString("subtitle", ""));

            if (enabled && title.isEmpty() && subtitle.isEmpty()) {

                logger.warning("В секции сообщений '" + path
                        + "' пустые title и subtitle — титул отправляться не будет.");

            }

            titles.put(type, new TitleMessage(enabled, title, subtitle));

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
    public @NotNull CargoMode getCargoMode() {
        return cargoMode;
    }

    @Override
    public boolean isCargoNameVisible() {
        return cargoNameVisible;
    }

    @Override
    public boolean isResetFlightEnabled() {
        return resetFlight;
    }

    @Override
    public boolean isResetGamemodeEnabled() {
        return resetGamemode;
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
    public long getDeliveryTimeoutMillis() {
        return deliveryTimeoutMillis;
    }

    @Override
    public double getDeliveryRadiusSquared() {
        return deliveryRadiusSquared;
    }

    @Override
    public boolean isBossBarEnabled() {
        return bossBarEnabled;
    }

    @Override
    public @NotNull BarColor getBossBarColor() {
        return bossBarColor;
    }

    @Override
    public @NotNull String getBossBarText() {
        return bossBarText;
    }

    @Override
    public boolean isAllowedWorld(@NotNull String worldName) {
        return allowedWorldSet.contains(worldName);
    }

    @Override
    public boolean isCooldownEnabled() {
        return cooldownEnabled;
    }

    @Override
    public long getCooldownMillis() {
        return cooldownMillis;
    }

    @Override
    public boolean arePermissionsEnabled() {
        return permissionsEnabled;
    }

    @Override
    public boolean isOpBypassEnabled() {
        return opBypass;
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
    public @NotNull String getCommandNoTiersMessage() {
        return commandNoTiersMessage;
    }

    @Override
    public @NotNull List<String> getReloadReportMessage() {
        return reloadReportMessage;
    }

    @Override
    public void reload() {
        loadConfig();
        cacheConfigValues();
    }

    /**
     * Значения прочитанных настроек для отчёта команды reload.
     * Лейблы строк отчёта живут в config.yml (messages.command.reload_report),
     * здесь только значения под плейсхолдеры.
     *
     * @return неизменяемая карта плейсхолдеров отчёта
     */
    public @NotNull Map<String, String> describeValues() {

        Map<String, String> values = new HashMap<>();

        values.put("enabled", String.valueOf(enabled));
        values.put("material", material.name());
        values.put("npc_ids", String.valueOf(npcIds));
        values.put("worlds", String.valueOf(allowedWorlds));
        values.put("regions", String.valueOf(allowedRegions));
        values.put("porters", porterTiers.stream()
                .map(PorterTier::id)
                .collect(Collectors.joining(", ")));
        values.put("delivery", deliveryTrigger.name() + ", timeout " + deliveryTimeoutMillis / 1000L + "s");
        values.put("cooldown", cooldownEnabled ? cooldownMillis / 1000L + "s" : "off");
        values.put("permissions", (permissionsEnabled ? "on" : "off")
                + ", op_bypass " + (opBypass ? "on" : "off"));

        return Collections.unmodifiableMap(values);

    }
}
