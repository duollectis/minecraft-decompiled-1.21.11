package net.minecraft.registry.entry;

import com.mojang.serialization.Lifecycle;
import net.minecraft.registry.VersionedIdentifier;

import java.util.Optional;

/**
 * Метаданные записи реестра: информация об известном паке и жизненный цикл.
 * <p>
 * Используется при загрузке данных из ресурсов и при синхронизации по сети
 * для определения, нужно ли передавать данные клиенту или он уже имеет
 * актуальную версию через известный пак.
 *
 * @param knownPackInfo информация о паке, из которого загружена запись
 *                      (пустой Optional — запись загружена из неизвестного источника)
 * @param lifecycle     жизненный цикл записи (stable / experimental)
 */
public record RegistryEntryInfo(Optional<VersionedIdentifier> knownPackInfo, Lifecycle lifecycle) {
}
