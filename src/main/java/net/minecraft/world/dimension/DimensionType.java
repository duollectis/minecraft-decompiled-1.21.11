package net.minecraft.world.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.World;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.timeline.Timeline;

import java.nio.file.Path;

/**
 * Описывает физические и игровые свойства измерения: высоту, освещение, масштаб координат,
 * настройки монстров, скайбокс и атрибуты окружения. Является иммутабельным record-типом,
 * сериализуемым через Codec.
 */
public record DimensionType(
	boolean hasFixedTime,
	boolean hasSkyLight,
	boolean hasCeiling,
	double coordinateScale,
	int minY,
	int height,
	int logicalHeight,
	TagKey<Block> infiniburn,
	float ambientLight,
	MonsterSettings monsterSettings,
	Skybox skybox,
	CardinalLightType cardinalLightType,
	EnvironmentAttributeMap attributes,
	RegistryEntryList<Timeline> timelines
) {

	public static final int SIZE_BITS_Y = BlockPos.SIZE_BITS_Y;
	public static final int MIN_SECTION_HEIGHT = 16;
	public static final int MAX_HEIGHT = (1 << SIZE_BITS_Y) - 32;
	public static final int MAX_COLUMN_HEIGHT = (MAX_HEIGHT >> 1) - 1;
	public static final int MIN_HEIGHT = MAX_COLUMN_HEIGHT - MAX_HEIGHT + 1;
	public static final int MAX_COLUMN_HEIGHT_IN_BLOCKS = MAX_COLUMN_HEIGHT << 4;
	public static final int MIN_HEIGHT_IN_BLOCKS = MIN_HEIGHT << 4;

	public static final Codec<DimensionType> CODEC = createDimensionCodec(EnvironmentAttributeMap.CODEC);
	public static final Codec<DimensionType> NETWORK_CODEC = createDimensionCodec(EnvironmentAttributeMap.NETWORK_CODEC);
	public static final PacketCodec<RegistryByteBuf, RegistryEntry<DimensionType>> PACKET_CODEC =
		PacketCodecs.registryEntry(RegistryKeys.DIMENSION_TYPE);
	public static final float[] MOON_SIZES = new float[]{ 1.0F, 0.75F, 0.5F, 0.25F, 0.0F, 0.25F, 0.5F, 0.75F };
	public static final Codec<RegistryEntry<DimensionType>> REGISTRY_CODEC =
		RegistryElementCodec.of(RegistryKeys.DIMENSION_TYPE, CODEC);

	/**
	 * Валидирующий конструктор: проверяет корректность высоты, минимального Y и логической высоты.
	 * Все значения должны быть кратны {@link #MIN_SECTION_HEIGHT} (16 блоков = 1 секция).
	 */
	public DimensionType(
		boolean hasFixedTime,
		boolean hasSkyLight,
		boolean hasCeiling,
		double coordinateScale,
		int minY,
		int height,
		int logicalHeight,
		TagKey<Block> infiniburn,
		float ambientLight,
		MonsterSettings monsterSettings,
		Skybox skybox,
		CardinalLightType cardinalLightType,
		EnvironmentAttributeMap attributes,
		RegistryEntryList<Timeline> timelines
	) {
		if (height < MIN_SECTION_HEIGHT) {
			throw new IllegalStateException("height has to be at least 16");
		}

		if (minY + height > MAX_COLUMN_HEIGHT + 1) {
			throw new IllegalStateException("min_y + height cannot be higher than: " + (MAX_COLUMN_HEIGHT + 1));
		}

		if (logicalHeight > height) {
			throw new IllegalStateException("logical_height cannot be higher than height");
		}

		if (height % MIN_SECTION_HEIGHT != 0) {
			throw new IllegalStateException("height has to be multiple of 16");
		}

		if (minY % MIN_SECTION_HEIGHT != 0) {
			throw new IllegalStateException("min_y has to be a multiple of 16");
		}

		this.hasFixedTime = hasFixedTime;
		this.hasSkyLight = hasSkyLight;
		this.hasCeiling = hasCeiling;
		this.coordinateScale = coordinateScale;
		this.minY = minY;
		this.height = height;
		this.logicalHeight = logicalHeight;
		this.infiniburn = infiniburn;
		this.ambientLight = ambientLight;
		this.monsterSettings = monsterSettings;
		this.skybox = skybox;
		this.cardinalLightType = cardinalLightType;
		this.attributes = attributes;
		this.timelines = timelines;
	}

	private static Codec<DimensionType> createDimensionCodec(Codec<EnvironmentAttributeMap> attributesCodec) {
		return Codecs.exceptionCatching(
			RecordCodecBuilder.create(
				instance -> instance.group(
					Codec.BOOL
						.optionalFieldOf("has_fixed_time", false)
						.forGetter(DimensionType::hasFixedTime),
					Codec.BOOL.fieldOf("has_skylight").forGetter(DimensionType::hasSkyLight),
					Codec.BOOL.fieldOf("has_ceiling").forGetter(DimensionType::hasCeiling),
					Codec.doubleRange(1.0E-5F, 3.0E7)
						.fieldOf("coordinate_scale")
						.forGetter(DimensionType::coordinateScale),
					Codec.intRange(MIN_HEIGHT, MAX_COLUMN_HEIGHT)
						.fieldOf("min_y")
						.forGetter(DimensionType::minY),
					Codec.intRange(MIN_SECTION_HEIGHT, MAX_HEIGHT)
						.fieldOf("height")
						.forGetter(DimensionType::height),
					Codec.intRange(0, MAX_HEIGHT)
						.fieldOf("logical_height")
						.forGetter(DimensionType::logicalHeight),
					TagKey.codec(RegistryKeys.BLOCK)
						.fieldOf("infiniburn")
						.forGetter(DimensionType::infiniburn),
					Codec.FLOAT.fieldOf("ambient_light").forGetter(DimensionType::ambientLight),
					MonsterSettings.CODEC.forGetter(DimensionType::monsterSettings),
					Skybox.CODEC
						.optionalFieldOf("skybox", Skybox.OVERWORLD)
						.forGetter(DimensionType::skybox),
					CardinalLightType.CODEC
						.optionalFieldOf("cardinal_light", CardinalLightType.DEFAULT)
						.forGetter(DimensionType::cardinalLightType),
					attributesCodec
						.optionalFieldOf("attributes", EnvironmentAttributeMap.EMPTY)
						.forGetter(DimensionType::attributes),
					RegistryCodecs.entryList(RegistryKeys.TIMELINE)
						.optionalFieldOf("timelines", RegistryEntryList.empty())
						.forGetter(DimensionType::timelines)
				).apply(instance, DimensionType::new)
			)
		);
	}

	/**
	 * Вычисляет коэффициент масштабирования координат при переходе между измерениями.
	 * Например, при переходе из Overworld (scale=1) в Nether (scale=8) коэффициент равен 1/8.
	 *
	 * @param fromDimension тип исходного измерения
	 * @param toDimension   тип целевого измерения
	 * @return отношение масштабов координат (from / to)
	 */
	public static double getCoordinateScaleFactor(DimensionType fromDimension, DimensionType toDimension) {
		return fromDimension.coordinateScale() / toDimension.coordinateScale();
	}

	/**
	 * Возвращает путь к директории сохранения для указанного измерения.
	 * Overworld — корень мира, Nether — DIM-1, End — DIM1, остальные — dimensions/namespace/path.
	 *
	 * @param worldRef       ключ измерения
	 * @param worldDirectory корневая директория мира
	 * @return путь к директории сохранения данного измерения
	 */
	public static Path getSaveDirectory(RegistryKey<World> worldRef, Path worldDirectory) {
		if (worldRef == World.OVERWORLD) {
			return worldDirectory;
		}

		if (worldRef == World.END) {
			return worldDirectory.resolve("DIM1");
		}

		if (worldRef == World.NETHER) {
			return worldDirectory.resolve("DIM-1");
		}

		return worldDirectory
			.resolve("dimensions")
			.resolve(worldRef.getValue().getNamespace())
			.resolve(worldRef.getValue().getPath());
	}

	public IntProvider monsterSpawnLightTest() {
		return monsterSettings.monsterSpawnLightTest();
	}

	public int monsterSpawnBlockLightLimit() {
		return monsterSettings.monsterSpawnBlockLightLimit();
	}

	public boolean getSkybox() {
		return skybox == Skybox.END;
	}

	/**
	 * Тип освещения по кардинальным направлениям (стороны света).
	 * DEFAULT — стандартное освещение Overworld/End, NETHER — особое освещение Нижнего мира.
	 */
	public enum CardinalLightType implements StringIdentifiable {
		DEFAULT("default"),
		NETHER("nether");

		public static final Codec<CardinalLightType> CODEC =
			StringIdentifiable.createCodec(CardinalLightType::values);

		private final String name;

		CardinalLightType(String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}
	}

	/**
	 * Настройки спавна монстров для данного типа измерения.
	 */
	public record MonsterSettings(IntProvider monsterSpawnLightTest, int monsterSpawnBlockLightLimit) {

		public static final MapCodec<MonsterSettings> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				IntProvider.createValidatingCodec(0, 15)
					.fieldOf("monster_spawn_light_level")
					.forGetter(MonsterSettings::monsterSpawnLightTest),
				Codec.intRange(0, 15)
					.fieldOf("monster_spawn_block_light_limit")
					.forGetter(MonsterSettings::monsterSpawnBlockLightLimit)
			).apply(instance, MonsterSettings::new)
		);
	}

	/**
	 * Тип скайбокса измерения.
	 * NONE — нет неба (Nether), OVERWORLD — стандартное небо, END — небо Края.
	 */
	public enum Skybox implements StringIdentifiable {
		NONE("none"),
		OVERWORLD("overworld"),
		END("end");

		public static final Codec<Skybox> CODEC = StringIdentifiable.createCodec(Skybox::values);

		private final String name;

		Skybox(String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}
	}
}
