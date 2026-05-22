package net.minecraft.world.gen;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

/**
 * Режим адаптации рельефа под структуру при генерации мира.
 * Определяет, как ландшафт подстраивается под размещённую структуру.
 */
public enum StructureTerrainAdaptation implements StringIdentifiable {
	NONE("none"),
	BURY("bury"),
	BEARD_THIN("beard_thin"),
	BEARD_BOX("beard_box"),
	ENCAPSULATE("encapsulate");

	public static final Codec<StructureTerrainAdaptation> CODEC =
		StringIdentifiable.createCodec(StructureTerrainAdaptation::values);

	private final String name;

	StructureTerrainAdaptation(String name) {
		this.name = name;
	}

	@Override
	public String asString() {
		return name;
	}
}
