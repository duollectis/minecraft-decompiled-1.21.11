package net.minecraft.world.gen;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

/**
 * Шаги генерации мира, определяющие порядок размещения фич в биомах.
 * Каждый шаг выполняется последовательно при генерации чанка.
 */
public class GenerationStep {

	/**
	 * Перечисление шагов генерации фич биома в порядке их выполнения.
	 */
	public enum Feature implements StringIdentifiable {
		RAW_GENERATION("raw_generation"),
		LAKES("lakes"),
		LOCAL_MODIFICATIONS("local_modifications"),
		UNDERGROUND_STRUCTURES("underground_structures"),
		SURFACE_STRUCTURES("surface_structures"),
		STRONGHOLDS("strongholds"),
		UNDERGROUND_ORES("underground_ores"),
		UNDERGROUND_DECORATION("underground_decoration"),
		FLUID_SPRINGS("fluid_springs"),
		VEGETAL_DECORATION("vegetal_decoration"),
		TOP_LAYER_MODIFICATION("top_layer_modification");

		public static final Codec<Feature> CODEC = StringIdentifiable.createCodec(Feature::values);

		private final String name;

		Feature(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@Override
		public String asString() {
			return name;
		}
	}
}
