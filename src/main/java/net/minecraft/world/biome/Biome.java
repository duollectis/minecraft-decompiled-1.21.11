package net.minecraft.world.biome;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.OctaveSimplexNoiseSampler;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.LightType;
import net.minecraft.world.WorldView;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributeModifier;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * {@code Biome}.
 */
public final class Biome {

	public static final Codec<Biome> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Biome.Weather.CODEC.forGetter(biome -> biome.weather),
					                    EnvironmentAttributeMap.POSITIONAL_CODEC
							                    .optionalFieldOf("attributes", EnvironmentAttributeMap.EMPTY)
							                    .forGetter(biome -> biome.environmentAttributes),
					                    BiomeEffects.CODEC.fieldOf("effects").forGetter(biome -> biome.effects),
					                    GenerationSettings.CODEC.forGetter(biome -> biome.generationSettings),
					                    SpawnSettings.CODEC.forGetter(biome -> biome.spawnSettings)
			                    )
			                    .apply(instance, Biome::new)
	);
	public static final Codec<Biome> NETWORK_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Biome.Weather.CODEC.forGetter(biome -> biome.weather),
					                    EnvironmentAttributeMap.NETWORK_CODEC
							                    .optionalFieldOf("attributes", EnvironmentAttributeMap.EMPTY)
							                    .forGetter(biome -> biome.environmentAttributes),
					                    BiomeEffects.CODEC.fieldOf("effects").forGetter(biome -> biome.effects)
			                    )
			                    .apply(
					                    instance,
					                    (weather, attributes, effects) -> new Biome(
							                    weather,
							                    attributes,
							                    effects,
							                    GenerationSettings.INSTANCE,
							                    SpawnSettings.INSTANCE
					                    )
			                    )
	);
	public static final Codec<RegistryEntry<Biome>> REGISTRY_CODEC = RegistryElementCodec.of(RegistryKeys.BIOME, CODEC);
	public static final Codec<RegistryEntryList<Biome>>
			REGISTRY_ENTRY_LIST_CODEC =
			RegistryCodecs.entryList(RegistryKeys.BIOME, CODEC);
	private static final OctaveSimplexNoiseSampler TEMPERATURE_NOISE = new OctaveSimplexNoiseSampler(
			new ChunkRandom(new CheckedRandom(1234L)), ImmutableList.of(0)
	);
	static final OctaveSimplexNoiseSampler FROZEN_OCEAN_NOISE = new OctaveSimplexNoiseSampler(
			new ChunkRandom(new CheckedRandom(3456L)), ImmutableList.of(-2, -1, 0)
	);
	@Deprecated(forRemoval = true)
	public static final OctaveSimplexNoiseSampler
			FOLIAGE_NOISE =
			new OctaveSimplexNoiseSampler(new ChunkRandom(new CheckedRandom(2345L)), ImmutableList.of(0));
	private static final int MAX_TEMPERATURE_CACHE_SIZE = 1024;
	private final Biome.Weather weather;
	private final GenerationSettings generationSettings;
	private final SpawnSettings spawnSettings;
	private final EnvironmentAttributeMap environmentAttributes;
	private final BiomeEffects effects;
	private final ThreadLocal<Long2FloatLinkedOpenHashMap> temperatureCache = ThreadLocal.withInitial(() -> {
		Long2FloatLinkedOpenHashMap long2FloatLinkedOpenHashMap = new Long2FloatLinkedOpenHashMap(1024, 0.25F) {
			protected void rehash(int n) {
			}
		};
		long2FloatLinkedOpenHashMap.defaultReturnValue(Float.NaN);
		return long2FloatLinkedOpenHashMap;
	});

	Biome(
			Biome.Weather weather,
			EnvironmentAttributeMap effects,
			BiomeEffects biomeEffects,
			GenerationSettings generationSettings,
			SpawnSettings spawnSettings
	) {
		this.weather = weather;
		this.generationSettings = generationSettings;
		this.spawnSettings = spawnSettings;
		this.environmentAttributes = effects;
		this.effects = biomeEffects;
	}

	public SpawnSettings getSpawnSettings() {
		return this.spawnSettings;
	}

	public boolean hasPrecipitation() {
		return this.weather.hasPrecipitation();
	}

	public Biome.Precipitation getPrecipitation(BlockPos pos, int seaLevel) {
		if (!this.hasPrecipitation()) {
			return Biome.Precipitation.NONE;
		}
		else {
			return this.isCold(pos, seaLevel) ? Biome.Precipitation.SNOW : Biome.Precipitation.RAIN;
		}
	}

	private float computeTemperature(BlockPos pos, int seaLevel) {
		float f = this.weather.temperatureModifier.getModifiedTemperature(pos, this.getTemperature());
		int i = seaLevel + 17;
		if (pos.getY() > i) {
			float g = (float) (TEMPERATURE_NOISE.sample(pos.getX() / 8.0F, pos.getZ() / 8.0F, false) * 8.0);
			return f - (g + pos.getY() - i) * 0.05F / 40.0F;
		}
		else {
			return f;
		}
	}

	@Deprecated
	private float getTemperature(BlockPos blockPos, int seaLevel) {
		long l = blockPos.asLong();
		Long2FloatLinkedOpenHashMap long2FloatLinkedOpenHashMap = this.temperatureCache.get();
		float f = long2FloatLinkedOpenHashMap.get(l);
		if (!Float.isNaN(f)) {
			return f;
		}
		else {
			float g = this.computeTemperature(blockPos, seaLevel);
			if (long2FloatLinkedOpenHashMap.size() == 1024) {
				long2FloatLinkedOpenHashMap.removeFirstFloat();
			}

			long2FloatLinkedOpenHashMap.put(l, g);
			return g;
		}
	}

	public boolean canSetIce(WorldView world, BlockPos blockPos) {
		return this.canSetIce(world, blockPos, true);
	}

	public boolean canSetIce(WorldView world, BlockPos pos, boolean doWaterCheck) {
		if (this.doesNotSnow(pos, world.getSeaLevel())) {
			return false;
		}
		else {
			if (world.isInHeightLimit(pos.getY()) && world.getLightLevel(LightType.BLOCK, pos) < 10) {
				BlockState blockState = world.getBlockState(pos);
				FluidState fluidState = world.getFluidState(pos);
				if (fluidState.getFluid() == Fluids.WATER && blockState.getBlock() instanceof FluidBlock) {
					if (!doWaterCheck) {
						return true;
					}

					boolean
							bl =
							world.isWater(pos.west()) && world.isWater(pos.east()) && world.isWater(pos.north())
									&& world.isWater(pos.south());
					if (!bl) {
						return true;
					}
				}
			}

			return false;
		}
	}

	public boolean isCold(BlockPos pos, int seaLevel) {
		return !this.doesNotSnow(pos, seaLevel);
	}

	public boolean doesNotSnow(BlockPos pos, int seaLevel) {
		return this.getTemperature(pos, seaLevel) >= 0.15F;
	}

	public boolean shouldGenerateLowerFrozenOceanSurface(BlockPos pos, int seaLevel) {
		return this.getTemperature(pos, seaLevel) > 0.1F;
	}

	public boolean canSetSnow(WorldView world, BlockPos pos) {
		if (this.getPrecipitation(pos, world.getSeaLevel()) != Biome.Precipitation.SNOW) {
			return false;
		}
		else {
			if (world.isInHeightLimit(pos.getY()) && world.getLightLevel(LightType.BLOCK, pos) < 10) {
				BlockState blockState = world.getBlockState(pos);
				if ((blockState.isAir() || blockState.isOf(Blocks.SNOW)) && Blocks.SNOW
						.getDefaultState()
						.canPlaceAt(world, pos)) {
					return true;
				}
			}

			return false;
		}
	}

	public GenerationSettings getGenerationSettings() {
		return this.generationSettings;
	}

	public int getGrassColorAt(double x, double z) {
		int i = this.getGrassColor();
		return this.effects.grassColorModifier().getModifiedGrassColor(x, z, i);
	}

	private int getGrassColor() {
		Optional<Integer> optional = this.effects.grassColor();
		return optional.isPresent() ? optional.get() : this.getDefaultGrassColor();
	}

	private int getDefaultGrassColor() {
		double d = MathHelper.clamp(this.weather.temperature, 0.0F, 1.0F);
		double e = MathHelper.clamp(this.weather.downfall, 0.0F, 1.0F);
		return GrassColors.getColor(d, e);
	}

	public int getFoliageColor() {
		return this.effects.foliageColor().orElseGet(this::getDefaultFoliageColor);
	}

	private int getDefaultFoliageColor() {
		double d = MathHelper.clamp(this.weather.temperature, 0.0F, 1.0F);
		double e = MathHelper.clamp(this.weather.downfall, 0.0F, 1.0F);
		return FoliageColors.getColor(d, e);
	}

	public int getDryFoliageColor() {
		return this.effects.dryFoliageColor().orElseGet(this::getDefaultDryFoliageColor);
	}

	private int getDefaultDryFoliageColor() {
		double d = MathHelper.clamp(this.weather.temperature, 0.0F, 1.0F);
		double e = MathHelper.clamp(this.weather.downfall, 0.0F, 1.0F);
		return DryFoliageColors.getColor(d, e);
	}

	public float getTemperature() {
		return this.weather.temperature;
	}

	public EnvironmentAttributeMap getEnvironmentAttributes() {
		return this.environmentAttributes;
	}

	public BiomeEffects getEffects() {
		return this.effects;
	}

	public int getWaterColor() {
		return this.effects.waterColor();
	}

	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private boolean precipitation = true;
		private @Nullable Float temperature;
		private Biome.TemperatureModifier temperatureModifier = Biome.TemperatureModifier.NONE;
		private @Nullable Float downfall;
		private final EnvironmentAttributeMap.Builder environmentAttributeBuilder = EnvironmentAttributeMap.builder();
		private @Nullable BiomeEffects specialEffects;
		private @Nullable SpawnSettings spawnSettings;
		private @Nullable GenerationSettings generationSettings;

		public Biome.Builder precipitation(boolean precipitation) {
			this.precipitation = precipitation;
			return this;
		}

		public Biome.Builder temperature(float temperature) {
			this.temperature = temperature;
			return this;
		}

		public Biome.Builder downfall(float downfall) {
			this.downfall = downfall;
			return this;
		}

		public Biome.Builder addEnvironmentAttributes(EnvironmentAttributeMap map) {
			this.environmentAttributeBuilder.addAll(map);
			return this;
		}

		public Biome.Builder addEnvironmentAttributes(EnvironmentAttributeMap.Builder builder) {
			return this.addEnvironmentAttributes(builder.build());
		}

		public <Value> Biome.Builder setEnvironmentAttribute(EnvironmentAttribute<Value> attribute, Value value) {
			this.environmentAttributeBuilder.with(attribute, value);
			return this;
		}

		public <Value, Parameter> Biome.Builder setEnvironmentAttributeModifier(
				EnvironmentAttribute<Value> attribute,
				EnvironmentAttributeModifier<Value, Parameter> modifier,
				Parameter value
		) {
			this.environmentAttributeBuilder.with(attribute, modifier, value);
			return this;
		}

		public Biome.Builder effects(BiomeEffects effects) {
			this.specialEffects = effects;
			return this;
		}

		public Biome.Builder spawnSettings(SpawnSettings spawnSettings) {
			this.spawnSettings = spawnSettings;
			return this;
		}

		public Biome.Builder generationSettings(GenerationSettings generationSettings) {
			this.generationSettings = generationSettings;
			return this;
		}

		public Biome.Builder temperatureModifier(Biome.TemperatureModifier temperatureModifier) {
			this.temperatureModifier = temperatureModifier;
			return this;
		}

		public Biome build() {
			if (this.temperature != null && this.downfall != null && this.specialEffects != null
					&& this.spawnSettings != null && this.generationSettings != null) {
				return new Biome(
						new Biome.Weather(
								this.precipitation,
								this.temperature,
								this.temperatureModifier,
								this.downfall
						),
						this.environmentAttributeBuilder.build(),
						this.specialEffects,
						this.generationSettings,
						this.spawnSettings
				);
			}
			else {
				throw new IllegalStateException("You are missing parameters to build a proper biome\n" + this);
			}
		}

		@Override
		public String toString() {
			return "BiomeBuilder{\nhasPrecipitation="
					+ this.precipitation
					+ ",\ntemperature="
					+ this.temperature
					+ ",\ntemperatureModifier="
					+ this.temperatureModifier
					+ ",\ndownfall="
					+ this.downfall
					+ ",\nspecialEffects="
					+ this.specialEffects
					+ ",\nmobSpawnSettings="
					+ this.spawnSettings
					+ ",\ngenerationSettings="
					+ this.generationSettings
					+ ",\n}";
		}
	}

	/**
	 * {@code Precipitation}.
	 */
	public static enum Precipitation implements StringIdentifiable {
		NONE("none"),
		RAIN("rain"),
		SNOW("snow");

		public static final Codec<Biome.Precipitation>
				CODEC =
				StringIdentifiable.createCodec(Biome.Precipitation::values);
		private final String name;

		private Precipitation(final String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return this.name;
		}
	}

	/**
	 * {@code TemperatureModifier}.
	 */
	public static enum TemperatureModifier implements StringIdentifiable {
		NONE("none") {
			@Override
			public float getModifiedTemperature(BlockPos pos, float temperature) {
				return temperature;
			}
		},
		FROZEN("frozen") {
			@Override
			public float getModifiedTemperature(BlockPos pos, float temperature) {
				double d = Biome.FROZEN_OCEAN_NOISE.sample(pos.getX() * 0.05, pos.getZ() * 0.05, false) * 7.0;
				double e = Biome.FOLIAGE_NOISE.sample(pos.getX() * 0.2, pos.getZ() * 0.2, false);
				double f = d + e;
				if (f < 0.3) {
					double g = Biome.FOLIAGE_NOISE.sample(pos.getX() * 0.09, pos.getZ() * 0.09, false);
					if (g < 0.8) {
						return 0.2F;
					}
				}

				return temperature;
			}
		};

		private final String name;
		public static final Codec<Biome.TemperatureModifier>
				CODEC =
				StringIdentifiable.createCodec(Biome.TemperatureModifier::values);

		public abstract float getModifiedTemperature(BlockPos pos, float temperature);

		TemperatureModifier(final String name) {
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

	/**
	 * {@code Weather}.
	 */
	record Weather(
			boolean hasPrecipitation,
			float temperature,
			Biome.TemperatureModifier temperatureModifier,
			float downfall
	) {

		public static final MapCodec<Biome.Weather> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    Codec.BOOL.fieldOf("has_precipitation").forGetter(weather -> weather.hasPrecipitation),
						                    Codec.FLOAT.fieldOf("temperature").forGetter(weather -> weather.temperature),
						                    Biome.TemperatureModifier.CODEC
								                    .optionalFieldOf("temperature_modifier", Biome.TemperatureModifier.NONE)
								                    .forGetter(weather -> weather.temperatureModifier),
						                    Codec.FLOAT.fieldOf("downfall").forGetter(weather -> weather.downfall)
				                    )
				                    .apply(instance, Biome.Weather::new)
		);
	}
}
