package net.minecraft.world.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Визуальные эффекты биома: цвет воды, листвы, травы и модификатор цвета травы.
 * Используется при рендеринге мира на клиенте.
 */
public record BiomeEffects(
	int waterColor,
	Optional<Integer> foliageColor,
	Optional<Integer> dryFoliageColor,
	Optional<Integer> grassColor,
	BiomeEffects.GrassColorModifier grassColorModifier
) {

	public static final Codec<BiomeEffects> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Codecs.HEX_RGB.fieldOf("water_color").forGetter(BiomeEffects::waterColor),
			Codecs.HEX_RGB.optionalFieldOf("foliage_color").forGetter(BiomeEffects::foliageColor),
			Codecs.HEX_RGB.optionalFieldOf("dry_foliage_color").forGetter(BiomeEffects::dryFoliageColor),
			Codecs.HEX_RGB.optionalFieldOf("grass_color").forGetter(BiomeEffects::grassColor),
			BiomeEffects.GrassColorModifier.CODEC
				.optionalFieldOf("grass_color_modifier", BiomeEffects.GrassColorModifier.NONE)
				.forGetter(BiomeEffects::grassColorModifier)
		)
		.apply(instance, BiomeEffects::new)
	);

	public static class Builder {

		private OptionalInt waterColor = OptionalInt.empty();
		private Optional<Integer> foliageColor = Optional.empty();
		private Optional<Integer> dryFoliageColor = Optional.empty();
		private Optional<Integer> grassColor = Optional.empty();
		private BiomeEffects.GrassColorModifier grassColorModifier = BiomeEffects.GrassColorModifier.NONE;

		public BiomeEffects.Builder waterColor(int waterColor) {
			this.waterColor = OptionalInt.of(waterColor);
			return this;
		}

		public BiomeEffects.Builder foliageColor(int foliageColor) {
			this.foliageColor = Optional.of(foliageColor);
			return this;
		}

		public BiomeEffects.Builder dryFoliageColor(int dryFoliageColor) {
			this.dryFoliageColor = Optional.of(dryFoliageColor);
			return this;
		}

		public BiomeEffects.Builder grassColor(int grassColor) {
			this.grassColor = Optional.of(grassColor);
			return this;
		}

		public BiomeEffects.Builder grassColorModifier(BiomeEffects.GrassColorModifier grassColorModifier) {
			this.grassColorModifier = grassColorModifier;
			return this;
		}

		public BiomeEffects build() {
			return new BiomeEffects(
				waterColor.orElseThrow(() -> new IllegalStateException("Missing 'water' color.")),
				foliageColor,
				dryFoliageColor,
				grassColor,
				grassColorModifier
			);
		}
	}

	/**
	 * Модификатор цвета травы, применяемый поверх базового цвета из карты цветов.
	 * DARK_FOREST и SWAMP переопределяют цвет специфичной для биома логикой.
	 */
	public enum GrassColorModifier implements StringIdentifiable {
		NONE("none") {
			@Override
			public int getModifiedGrassColor(double x, double z, int color) {
				return color;
			}
		},
		DARK_FOREST("dark_forest") {
			// Усредняет цвет травы с тёмно-зелёным оттенком тёмного леса (0x28340A)
			@Override
			public int getModifiedGrassColor(double x, double z, int color) {
				return (color & 0xFEFEFE) + 0x28340A >> 1;
			}
		},
		SWAMP("swamp") {
			// Цвет травы болота зависит от шума Фолиажа: тёмно-зелёный или оливковый
			@Override
			public int getModifiedGrassColor(double x, double z, int color) {
				double noiseSample = Biome.FOLIAGE_NOISE.sample(x * 0.0225, z * 0.0225, false);
				return noiseSample < -0.1 ? 0x4C763C : 0x6A7039;
			}
		};

		private static final int DARK_FOREST_TINT = 0x28340A;

		private final String name;

		public static final Codec<BiomeEffects.GrassColorModifier>
			CODEC = StringIdentifiable.createCodec(BiomeEffects.GrassColorModifier::values);

		GrassColorModifier(final String name) {
			this.name = name;
		}

		public abstract int getModifiedGrassColor(double x, double z, int color);

		public String getName() {
			return name;
		}

		@Override
		public String asString() {
			return name;
		}
	}
}
