package net.minecraft.world.biome;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Настройки спавна мобов в биоме.
 * <p>
 * Хранит вероятность спавна существ, таблицы спавна по группам ({@link SpawnGroup})
 * и стоимость спавна (energy budget / charge) для конкретных типов сущностей.
 * Иммутабельна после построения через {@link Builder}.
 */
public class SpawnSettings {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final float DEFAULT_CREATURE_SPAWN_PROBABILITY = 0.1F;

	public static final Pool<SpawnSettings.SpawnEntry> EMPTY_ENTRY_POOL = Pool.empty();
	public static final SpawnSettings INSTANCE = new SpawnSettings.Builder().build();

	public static final MapCodec<SpawnSettings> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Codec.floatRange(0.0F, 0.9999999F)
				.optionalFieldOf("creature_spawn_probability", DEFAULT_CREATURE_SPAWN_PROBABILITY)
				.forGetter(settings -> settings.creatureSpawnProbability),
			Codec.simpleMap(
				SpawnGroup.CODEC,
				Pool.createCodec(SpawnSettings.SpawnEntry.CODEC)
					.promotePartial(Util.addPrefix("Spawn data: ", LOGGER::error)),
				StringIdentifiable.toKeyable(SpawnGroup.values())
			)
			.fieldOf("spawners")
			.forGetter(settings -> settings.spawners),
			Codec.simpleMap(
				Registries.ENTITY_TYPE.getCodec(),
				SpawnSettings.SpawnDensity.CODEC,
				Registries.ENTITY_TYPE
			)
			.fieldOf("spawn_costs")
			.forGetter(settings -> settings.spawnCosts)
		)
		.apply(instance, SpawnSettings::new)
	);

	private final float creatureSpawnProbability;
	private final Map<SpawnGroup, Pool<SpawnSettings.SpawnEntry>> spawners;
	private final Map<EntityType<?>, SpawnSettings.SpawnDensity> spawnCosts;

	SpawnSettings(
		float creatureSpawnProbability,
		Map<SpawnGroup, Pool<SpawnSettings.SpawnEntry>> spawners,
		Map<EntityType<?>, SpawnSettings.SpawnDensity> spawnCosts
	) {
		this.creatureSpawnProbability = creatureSpawnProbability;
		this.spawners = ImmutableMap.copyOf(spawners);
		this.spawnCosts = ImmutableMap.copyOf(spawnCosts);
	}

	public Pool<SpawnSettings.SpawnEntry> getSpawnEntries(SpawnGroup spawnGroup) {
		return spawners.getOrDefault(spawnGroup, EMPTY_ENTRY_POOL);
	}

	public SpawnSettings.@Nullable SpawnDensity getSpawnDensity(EntityType<?> entityType) {
		return spawnCosts.get(entityType);
	}

	public float getCreatureSpawnProbability() {
		return creatureSpawnProbability;
	}

	public static class Builder {

		private final Map<SpawnGroup, Pool.Builder<SpawnSettings.SpawnEntry>>
			spawners = Util.mapEnum(SpawnGroup.class, group -> Pool.builder());
		private final Map<EntityType<?>, SpawnSettings.SpawnDensity> spawnCosts = Maps.newLinkedHashMap();
		private float creatureSpawnProbability = DEFAULT_CREATURE_SPAWN_PROBABILITY;

		public SpawnSettings.Builder spawn(SpawnGroup spawnGroup, int weight, SpawnSettings.SpawnEntry entry) {
			spawners.get(spawnGroup).add(entry, weight);
			return this;
		}

		public SpawnSettings.Builder spawnCost(EntityType<?> entityType, double mass, double gravityLimit) {
			spawnCosts.put(entityType, new SpawnSettings.SpawnDensity(gravityLimit, mass));
			return this;
		}

		public SpawnSettings.Builder creatureSpawnProbability(float probability) {
			creatureSpawnProbability = probability;
			return this;
		}

		public SpawnSettings build() {
			return new SpawnSettings(
				creatureSpawnProbability,
				spawners.entrySet()
					.stream()
					.collect(ImmutableMap.toImmutableMap(
						Entry::getKey,
						spawner -> ((Pool.Builder) spawner.getValue()).build()
					)),
				ImmutableMap.copyOf(spawnCosts)
			);
		}
	}

	public record SpawnDensity(double gravityLimit, double mass) {

		public static final Codec<SpawnSettings.SpawnDensity> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.DOUBLE.fieldOf("energy_budget").forGetter(SpawnDensity::gravityLimit),
				Codec.DOUBLE.fieldOf("charge").forGetter(SpawnDensity::mass)
			)
			.apply(instance, SpawnSettings.SpawnDensity::new)
		);
	}

	/**
	 * Запись о спавне одного типа сущности: тип, минимальный и максимальный размер группы.
	 * Если тип сущности принадлежит группе MISC, он заменяется на PIG во избежание некорректного спавна.
	 */
	public record SpawnEntry(EntityType<?> type, int minGroupSize, int maxGroupSize) {

		public static final MapCodec<SpawnSettings.SpawnEntry> CODEC =
			RecordCodecBuilder.<SpawnSettings.SpawnEntry>mapCodec(
				instance -> instance.group(
					Registries.ENTITY_TYPE.getCodec()
						.fieldOf("type")
						.forGetter(SpawnEntry::type),
					Codecs.POSITIVE_INT
						.fieldOf("minCount")
						.forGetter(SpawnEntry::minGroupSize),
					Codecs.POSITIVE_INT
						.fieldOf("maxCount")
						.forGetter(SpawnEntry::maxGroupSize)
				)
				.apply(instance, SpawnSettings.SpawnEntry::new)
			)
			.validate(
				entry -> entry.minGroupSize > entry.maxGroupSize
					? DataResult.error(() -> "minCount needs to be smaller or equal to maxCount")
					: DataResult.success(entry)
			);

		public SpawnEntry(EntityType<?> type, int minGroupSize, int maxGroupSize) {
			// Сущности группы MISC не должны спавниться через обычный механизм — заменяем на PIG
			type = type.getSpawnGroup() == SpawnGroup.MISC ? EntityType.PIG : type;
			this.type = type;
			this.minGroupSize = minGroupSize;
			this.maxGroupSize = maxGroupSize;
		}

		@Override
		public String toString() {
			return EntityType.getId(type) + "*(" + minGroupSize + "-" + maxGroupSize + ")";
		}
	}
}
