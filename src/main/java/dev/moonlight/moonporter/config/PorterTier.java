package dev.moonlight.moonporter.config;

import org.jetbrains.annotations.NotNull;

/**
 * Неизменяемое описание уровня груза.
 *
 * Уровни не зашиты в код: набор читается из settings.porters конфига,
 * а при пустой секции реестр подставляет встроенный набор по умолчанию.
 *
 * @param id        ключ секции уровня в конфиге (low, normal, ...)
 * @param name      отображаемое имя груза (поддерживает HEX и плейсхолдеры)
 * @param rewardMin минимальная награда
 * @param rewardMax максимальная награда
 * @param weight    нагрузка на игрока (амплитуда замедления)
 */
public record PorterTier(@NotNull String id,
                         @NotNull String name,
                         int rewardMin,
                         int rewardMax,
                         int weight) {
}
