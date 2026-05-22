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
 * Представляет биом — регион мира с определёнными климатическими условиями,
 * визуальными эффектами, настройками генерации и спавна существ.
 * <p>
 * Биом хранит данные о погоде (температура, осадки), визуальных эффектах
 * (цвет воды, листвы, травы), а также настройки генерации мира и спавна мобов.
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
		).apply(instance, Biome::new)
	);

	public static final Codec<Biome> NETWORK_CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Biome.Weather.CODEC.forGetter(biome -> biome.weather),
			EnvironmentAttributeMap.NETWORK_CODEC
				.optionalFieldOf("attributes", EnvironmentAttributeMap.EMPTY)
				.forGetter(biome -> biome.environmentAttributes),
			BiomeEffects.CODEC.fieldOf("effects").forGetter(biome -> biome.effects)
		).apply(
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

	public static final Codec<RegistryEntry<Biome>> REGISTRY_CODEC =
		RegistryElementCodec.of(RegistryKeys.BIOME, CODEC);

	public static final Codec<RegistryEntryList<Biome>> REGISTRY_ENTRY_LIST_CODEC =
		RegistryCodecs.entryList(RegistryKeys.BIOME, CODEC);

	private static final OctaveSimplexNoiseSampler TEMPERATURE_NOISE = new OctaveSimplexNoiseSampler(
		new ChunkRandom(new CheckedRandom(1234L)), ImmutableList.of(0)
	);

	static final OctaveSimplexNoiseSampler FROZEN_OCEAN_NOISE = new OctaveSimplexNoiseSampler(
		new ChunkRandom(new CheckedRandom(3456L)), ImmutableList.of(-2, -1, 0)
	);

	@Deprecated(forRemoval = true)
	public static final OctaveSimplexNoiseSampler FOLIAGE_NOISE = new OctaveSimplexNoiseSampler(
		new ChunkRandom(new CheckedRandom(2345L)), ImmutableList.of(0)
	);

	private static final int MAX_TEMPERATURE_CACHE_SIZE = 1024;
	/** Минимальный порог температуры для выпадения снега. */
	private static final float SNOW_TEMPERATURE_THRESHOLD = 0.15F;
	/** Порог температуры для генерации нижней поверхности замёрзшего океана. */
	private static final float FROZEN_OCEAN_SURFACE_THRESHOLD = 0.1F;
	/** Минимальный уровень освещения для образования льда и снега. */
	private static final int MAX_LIGHT_FOR_ICE = 10;
	/** Смещение уровня моря для расчёта температуры по высоте. */
	private static final int SEA_LEVEL_TEMPERATURE_OFFSET = 17;
	/** Коэффициент снижения температуры с высотой. */
	private static final float TEMPERATURE_HEIGHT_FACTOR = 0.05F / 40.0F;

	private final Biome.Weather weather;
	private final GenerationSettings generationSettings;
	private final SpawnSettings spawnSettings;
	private final EnvironmentAttributeMap environmentAttributes;
	private final BiomeEffects effects;

	/**
	 * Кэш температур по позициям блоков. Используется ThreadLocal для потокобезопасности.
	 * Ограничен {@link #MAX_TEMPERATURE_CACHE_SIZE} записями с вытеснением по LRU.
	 */
	private final ThreadLocal<Long2FloatLinkedOpenHashMap> temperatureCache = ThreadLocal.withInitial(() -> {
		Long2FloatLinkedOpenHashMap cache = new Long2FloatLinkedOpenHashMap(MAX_TEMPERATURE_CACHE_SIZE, 0.25F) {
			@Override
			protected void rehash(int newSize) {
				// Запрещаем рехэш — размер кэша фиксирован, вытеснение через removeFirstFloat()
			}
		};
		cache.defaultReturnValue(Float.NaN);
		return cache;
	});

	Biome(
		Biome.Weather weather,
		EnvironmentAttributeMap environmentAttributes,
		BiomeEffects effects,
		GenerationSettings generationSettings,
		SpawnSettings spawnSettings
	) {
		this.weather = weather;
		this.generationSettings = generationSettings;
		this.spawnSettings = spawnSettings;
		this.environmentAttributes = environmentAttributes;
		this.effects = effects;
	}

	public SpawnSettings getSpawnSettings() {
		return spawnSettings;
	}

	public boolean hasPrecipitation() {
		return weather.hasPrecipitation();
	}

	/**
	 * Определяет тип осадков в данном биоме для указанной позиции.
	 * Если осадков нет — возвращает {@link Precipitation#NONE}.
	 * Если температура ниже порога — снег, иначе — дождь.
	 *
	 * @param pos      позиция блока
	 * @param seaLevel уровень моря в мире
	 * @return тип осадков
	 */
	public Biome.Precipitation getPrecipitation(BlockPos pos, int seaLevel) {
		if (!hasPrecipitation()) {
			return Biome.Precipitation.NONE;
		}

		return isCold(pos, seaLevel) ? Biome.Precipitation.SNOW : Biome.Precipitation.RAIN;
	}

	/**
	 * Вычисляет температуру биома в указанной позиции с учётом высоты и шума.
	 * Выше уровня моря + {@value #SEA_LEVEL_TEMPERATURE_OFFSET} температура снижается.
	 *
	 * @param pos      позиция блока
	 * @param seaLevel уровень моря
	 * @return скорректированная температура
	 */
	private float computeTemperature(BlockPos pos, int seaLevel) {
		float baseTemp = weather.temperatureModifier.getModifiedTemperature(pos, getTemperature());
		int heightThreshold = seaLevel + SEA_LEVEL_TEMPERATURE_OFFSET;

		if (pos.getY() <= heightThreshold) {
			return baseTemp;
		}

		float noiseSample = (float) (TEMPERATURE_NOISE.sample(pos.getX() / 8.0F, pos.getZ() / 8.0F, false) * 8.0);
		return baseTemp - (noiseSample + pos.getY() - heightThreshold) * TEMPERATURE_HEIGHT_FACTOR;
	}

	/**
	 * Возвращает температуру биома в позиции с кэшированием.
	 * Кэш ограничен {@link #MAX_TEMPERATURE_CACHE_SIZE} записями.
	 *
	 * @param blockPos позиция блока
	 * @param seaLevel уровень моря
	 * @return температура в позиции
	 */
	@Deprecated
	private float getTemperature(BlockPos blockPos, int seaLevel) {
		long posKey = blockPos.asLong();
		Long2FloatLinkedOpenHashMap cache = temperatureCache.get();
		float cached = cache.get(posKey);

		if (!Float.isNaN(cached)) {
			return cached;
		}

		float computed = computeTemperature(blockPos, seaLevel);

		if (cache.size() == MAX_TEMPERATURE_CACHE_SIZE) {
			cache.removeFirstFloat();
		}

		cache.put(posKey, computed);
		return computed;
	}

	/**
	 * Проверяет, может ли на данной позиции образоваться лёд.
	 * Учитывает температуру, освещение и наличие воды.
	 *
	 * @param world    вид мира
	 * @param blockPos позиция блока
	 * @return {@code true} если лёд может образоваться
	 */
	public boolean canSetIce(WorldView world, BlockPos blockPos) {
		return canSetIce(world, blockPos, true);
	}

	/**
	 * Проверяет, может ли на данной позиции образоваться лёд.
	 *
	 * @param world          вид мира
	 * @param pos            позиция блока
	 * @param doWaterCheck   если {@code true}, проверяет наличие воды со всех сторон
	 *                       (для предотвращения замерзания у берегов)
	 * @return {@code true} если лёд может образоваться
	 */
	public boolean canSetIce(WorldView world, BlockPos pos, boolean doWaterCheck) {
		if (doesNotSnow(pos, world.getSeaLevel())) {
			return false;
		}

		if (!world.isInHeightLimit(pos.getY()) || world.getLightLevel(LightType.BLOCK, pos) >= MAX_LIGHT_FOR_ICE) {
			return false;
		}

		BlockState blockState = world.getBlockState(pos);
		FluidState fluidState = world.getFluidState(pos);

		if (fluidState.getFluid() != Fluids.WATER || !(blockState.getBlock() instanceof FluidBlock)) {
			return false;
		}

		if (!doWaterCheck) {
			return true;
		}

		boolean surroundedByWater = world.isWater(pos.west())
			&& world.isWater(pos.east())
			&& world.isWater(pos.north())
			&& world.isWater(pos.south());

		return !surroundedByWater;
	}

	public boolean isCold(BlockPos pos, int seaLevel) {
		return !doesNotSnow(pos, seaLevel);
	}

	/**
	 * Проверяет, достаточно ли тепло в данной позиции, чтобы снег не выпадал.
	 *
	 * @param pos      позиция блока
	 * @param seaLevel уровень моря
	 * @return {@code true} если температура выше порога снегопада
	 */
	public boolean doesNotSnow(BlockPos pos, int seaLevel) {
		return getTemperature(pos, seaLevel) >= SNOW_TEMPERATURE_THRESHOLD;
	}

	/**
	 * Определяет, нужно ли генерировать нижнюю поверхность замёрзшего океана.
	 * Используется при генерации мира для биомов типа Frozen Ocean.
	 *
	 * @param pos      позиция блока
	 * @param seaLevel уровень моря
	 * @return {@code true} если температура выше порога для нижней поверхности
	 */
	public boolean shouldGenerateLowerFrozenOceanSurface(BlockPos pos, int seaLevel) {
		return getTemperature(pos, seaLevel) > FROZEN_OCEAN_SURFACE_THRESHOLD;
	}

	/**
	 * Проверяет, может ли на данной позиции выпасть снег.
	 *
	 * @param world вид мира
	 * @param pos   позиция блока
	 * @return {@code true} если снег может выпасть
	 */
	public boolean canSetSnow(WorldView world, BlockPos pos) {
		if (getPrecipitation(pos, world.getSeaLevel()) != Biome.Precipitation.SNOW) {
			return false;
		}

		if (!world.isInHeightLimit(pos.getY()) || world.getLightLevel(LightType.BLOCK, pos) >= MAX_LIGHT_FOR_ICE) {
			return false;
		}

		BlockState blockState = world.getBlockState(pos);
		return (blockState.isAir() || blockState.isOf(Blocks.SNOW))
			&& Blocks.SNOW.getDefaultState().canPlaceAt(world, pos);
	}

	public GenerationSettings getGenerationSettings() {
		return generationSettings;
	}

	/**
	 * Возвращает цвет травы в данной позиции с учётом модификатора цвета.
	 *
	 * @param x координата X
	 * @param z координата Z
	 * @return цвет травы в формате RGB
	 */
	public int getGrassColorAt(double x, double z) {
		int baseColor = getGrassColor();
		return effects.grassColorModifier().getModifiedGrassColor(x, z, baseColor);
	}

	private int getGrassColor() {
		Optional<Integer> override = effects.grassColor();
		return override.isPresent() ? override.get() : getDefaultGrassColor();
	}

	private int getDefaultGrassColor() {
		double temperature = MathHelper.clamp(weather.temperature, 0.0F, 1.0F);
		double downfall = MathHelper.clamp(weather.downfall, 0.0F, 1.0F);
		return GrassColors.getColor(temperature, downfall);
	}

	public int getFoliageColor() {
		return effects.foliageColor().orElseGet(this::getDefaultFoliageColor);
	}

	private int getDefaultFoliageColor() {
		double temperature = MathHelper.clamp(weather.temperature, 0.0F, 1.0F);
		double downfall = MathHelper.clamp(weather.downfall, 0.0F, 1.0F);
		return FoliageColors.getColor(temperature, downfall);
	}

	public int getDryFoliageColor() {
		return effects.dryFoliageColor().orElseGet(this::getDefaultDryFoliageColor);
	}

	private int getDefaultDryFoliageColor() {
		double temperature = MathHelper.clamp(weather.temperature, 0.0F, 1.0F);
		double downfall = MathHelper.clamp(weather.downfall, 0.0F, 1.0F);
		return DryFoliageColors.getColor(temperature, downfall);
	}

	public float getTemperature() {
		return weather.temperature;
	}

	public EnvironmentAttributeMap getEnvironmentAttributes() {
		return environmentAttributes;
	}

	public BiomeEffects getEffects() {
		return effects;
	}

	public int getWaterColor() {
		return effects.waterColor();
	}

	// ==================== Вложенные классы ====================

	/**
	 * Строитель для создания экземпляров {@link Biome}.
	 * Все поля обязательны — вызов {@link #build()} без их установки бросит исключение.
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
			environmentAttributeBuilder.addAll(map);
			return this;
		}

		public Biome.Builder addEnvironmentAttributes(EnvironmentAttributeMap.Builder builder) {
			return addEnvironmentAttributes(builder.build());
		}

		public <Value> Biome.Builder setEnvironmentAttribute(EnvironmentAttribute<Value> attribute, Value value) {
			environmentAttributeBuilder.with(attribute, value);
			return this;
		}

		public <Value, Parameter> Biome.Builder setEnvironmentAttributeModifier(
			EnvironmentAttribute<Value> attribute,
			EnvironmentAttributeModifier<Value, Parameter> modifier,
			Parameter value
		) {
			environmentAttributeBuilder.with(attribute, modifier, value);
			return this;
		}

		public Biome.Builder effects(BiomeEffects effects) {
			specialEffects = effects;
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

		/**
		 * Собирает биом из установленных параметров.
		 *
		 * @return готовый экземпляр {@link Biome}
		 * @throws IllegalStateException если не установлены обязательные параметры
		 */
		public Biome build() {
			if (temperature == null || downfall == null || specialEffects == null
				|| spawnSettings == null || generationSettings == null) {
				throw new IllegalStateException("You are missing parameters to build a proper biome\n" + this);
			}

			return new Biome(
				new Biome.Weather(precipitation, temperature, temperatureModifier, downfall),
				environmentAttributeBuilder.build(),
				specialEffects,
				generationSettings,
				spawnSettings
			);
		}

		@Override
		public String toString() {
			return "BiomeBuilder{\nhasPrecipitation="
				+ precipitation
				+ ",\ntemperature="
				+ temperature
				+ ",\ntemperatureModifier="
				+ temperatureModifier
				+ ",\ndownfall="
				+ downfall
				+ ",\nspecialEffects="
				+ specialEffects
				+ ",\nmobSpawnSettings="
				+ spawnSettings
				+ ",\ngenerationSettings="
				+ generationSettings
				+ ",\n}";
		}
	}

	/**
	 * Тип осадков в биоме.
	 */
	public enum Precipitation implements StringIdentifiable {
		NONE("none"),
		RAIN("rain"),
		SNOW("snow");

		public static final Codec<Biome.Precipitation> CODEC =
			StringIdentifiable.createCodec(Biome.Precipitation::values);

		private final String name;

		Precipitation(final String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}
	}

	/**
	 * Модификатор температуры биома.
	 * Применяется поверх базовой температуры для создания специальных эффектов,
	 * например, случайных холодных пятен в замёрзших океанах.
	 */
	public enum TemperatureModifier implements StringIdentifiable {
		NONE("none") {
			@Override
			public float getModifiedTemperature(BlockPos pos, float temperature) {
				return temperature;
			}
		},
		/**
		 * Создаёт случайные холодные пятна на поверхности замёрзшего океана
		 * с использованием шума Симплекса.
		 */
		FROZEN("frozen") {
			@Override
			public float getModifiedTemperature(BlockPos pos, float temperature) {
				double primaryNoise = Biome.FROZEN_OCEAN_NOISE.sample(pos.getX() * 0.05, pos.getZ() * 0.05, false) * 7.0;
				double secondaryNoise = Biome.FOLIAGE_NOISE.sample(pos.getX() * 0.2, pos.getZ() * 0.2, false);
				double combined = primaryNoise + secondaryNoise;

				if (combined >= 0.3) {
					return temperature;
				}

				double detailNoise = Biome.FOLIAGE_NOISE.sample(pos.getX() * 0.09, pos.getZ() * 0.09, false);
				return detailNoise < 0.8 ? 0.2F : temperature;
			}
		};

		public static final Codec<Biome.TemperatureModifier> CODEC =
			StringIdentifiable.createCodec(Biome.TemperatureModifier::values);

		private final String name;

		TemperatureModifier(final String name) {
			this.name = name;
		}

		/**
		 * Применяет модификатор к базовой температуре биома.
		 *
		 * @param pos         позиция блока
		 * @param temperature базовая температура биома
		 * @return скорректированная температура
		 */
		public abstract float getModifiedTemperature(BlockPos pos, float temperature);

		public String getName() {
			return name;
		}

		@Override
		public String asString() {
			return name;
		}
	}

	/**
	 * Погодные параметры биома: наличие осадков, температура и количество осадков.
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
			).apply(instance, Biome.Weather::new)
		);
	}
}
