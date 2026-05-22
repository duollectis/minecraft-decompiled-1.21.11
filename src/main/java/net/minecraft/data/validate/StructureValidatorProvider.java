package net.minecraft.data.validate;

import com.mojang.logging.LogUtils;
import net.minecraft.data.SnbtProvider;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.datafixer.Schemas;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceType;
import net.minecraft.structure.StructureTemplate;
import org.slf4j.Logger;

/**
 * Реализация {@link SnbtProvider.Tweaker}, которая валидирует и обновляет
 * NBT-данные структур через DataFixer. Предупреждает о структурах со слишком
 * старой версией данных, которые могут потребовать ручного обновления.
 */
public class StructureValidatorProvider implements SnbtProvider.Tweaker {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String PATH_PREFIX = ResourceType.SERVER_DATA.getDirectory() + "/minecraft/structure/";

	/**
	 * Минимальная версия данных структуры, при которой не выводится предупреждение.
	 * Соответствует версии данных Minecraft 1.21.
	 */
	private static final int CURRENT_DATA_VERSION = 4650;

	/**
	 * Версия данных по умолчанию для структур без явно указанной версии.
	 * Соответствует формату до введения DataVersion в NBT структур.
	 */
	private static final int LEGACY_DATA_VERSION = 500;

	@Override
	public NbtCompound write(String name, NbtCompound nbt) {
		return name.startsWith(PATH_PREFIX) ? update(name, nbt) : nbt;
	}

	public static NbtCompound update(String name, NbtCompound nbt) {
		int dataVersion = NbtHelper.getDataVersion(nbt, LEGACY_DATA_VERSION);

		if (dataVersion < CURRENT_DATA_VERSION) {
			LOGGER.warn(
					"SNBT Too old, do not forget to update: {} < {}: {}",
					dataVersion, CURRENT_DATA_VERSION, name
			);
		}

		NbtCompound updatedNbt = DataFixTypes.STRUCTURE.update(Schemas.getFixer(), nbt, dataVersion);
		StructureTemplate structureTemplate = new StructureTemplate();
		structureTemplate.readNbt(Registries.BLOCK, updatedNbt);
		return structureTemplate.writeNbt(new NbtCompound());
	}
}
