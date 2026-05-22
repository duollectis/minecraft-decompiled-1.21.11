package net.minecraft.structure;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

/**
 * Определяет поведение жидкостей при размещении структурного шаблона в мире.
 * Используется в {@link StructurePlacementData} для управления водонасыщением блоков.
 */
public enum StructureLiquidSettings implements StringIdentifiable {
	IGNORE_WATERLOGGING("ignore_waterlogging"),
	APPLY_WATERLOGGING("apply_waterlogging");

	// Поле намеренно называется `codec` (строчная буква) для совместимости с местами использования в кодовой базе
	public static final Codec<StructureLiquidSettings> codec =
		StringIdentifiable.createBasicCodec(StructureLiquidSettings::values);

	private final String id;

	StructureLiquidSettings(String id) {
		this.id = id;
	}

	@Override
	public String asString() {
		return id;
	}
}
