package net.minecraft.world.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * {@code BiomeEffects}.
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

	/**
	 * {@code Builder}.
	 */
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

		/**
		 * Build.
		 *
		 * @return BiomeEffects — результат операции
		 */
		public BiomeEffects build() {
			return new BiomeEffects(
					this.waterColor.orElseThrow(() -> new IllegalStateException("Missing 'water' color.")),
					this.foliageColor,
					this.dryFoliageColor,
					this.grassColor,
					this.grassColorModifier
			);
		}
	}

	/**
	 * {@code GrassColorModifier}.
	 */
	public static enum GrassColorModifier implements StringIdentifiable {
		NONE("none") {
			@Override
			public int getModifiedGrassColor(double x, double z, int color) {
				return color;
			}
		},
		DARK_FOREST("dark_forest") {
			@Override
			public int getModifiedGrassColor(double x, double z, int color) {
				return (color & 16711422) + 2634762 >> 1;
			}
		},
		SWAMP("swamp") {
			@Override
			public int getModifiedGrassColor(double x, double z, int color) {
				double d = Biome.FOLIAGE_NOISE.sample(x * 0.0225, z * 0.0225, false);
				return d < -0.1 ? 5011004 : 6975545;
			}
		};

		private final String name;
		public static final Codec<BiomeEffects.GrassColorModifier>
				CODEC =
				StringIdentifiable.createCodec(BiomeEffects.GrassColorModifier::values);

		public abstract int getModifiedGrassColor(double x, double z, int color);

		GrassColorModifier(final String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public String asString() {
			return this.name;
		}
	}
}
