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
 * {@code SpawnSettings}.
 */
public class SpawnSettings {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final float CREATURE_SPAWN_PROBABILITY = 0.1F;
	public static final Pool<SpawnSettings.SpawnEntry> EMPTY_ENTRY_POOL = Pool.empty();
	public static final SpawnSettings INSTANCE = new SpawnSettings.Builder().build();
	public static final MapCodec<SpawnSettings> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    Codec
							                    .floatRange(0.0F, 0.9999999F)
							                    .optionalFieldOf("creature_spawn_probability", 0.1F)
							                    .forGetter(settings -> settings.creatureSpawnProbability),
					                    Codec.simpleMap(
							                         SpawnGroup.CODEC,
							                         Pool
									                         .createCodec(SpawnSettings.SpawnEntry.CODEC)
									                         .promotePartial(Util.addPrefix("Spawn data: ", LOGGER::error)),
							                         StringIdentifiable.toKeyable(SpawnGroup.values())
					                         )
					                         .fieldOf("spawners")
					                         .forGetter(settings -> settings.spawners),
					                    Codec
							                    .simpleMap(
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
		return this.spawners.getOrDefault(spawnGroup, EMPTY_ENTRY_POOL);
	}

	public SpawnSettings.@Nullable SpawnDensity getSpawnDensity(EntityType<?> entityType) {
		return this.spawnCosts.get(entityType);
	}

	public float getCreatureSpawnProbability() {
		return this.creatureSpawnProbability;
	}

	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private final Map<SpawnGroup, Pool.Builder<SpawnSettings.SpawnEntry>>
				spawners =
				Util.mapEnum(SpawnGroup.class, group -> Pool.builder());
		private final Map<EntityType<?>, SpawnSettings.SpawnDensity> spawnCosts = Maps.newLinkedHashMap();
		private float creatureSpawnProbability = 0.1F;

		public SpawnSettings.Builder spawn(SpawnGroup spawnGroup, int weight, SpawnSettings.SpawnEntry entry) {
			this.spawners.get(spawnGroup).add(entry, weight);
			return this;
		}

		public SpawnSettings.Builder spawnCost(EntityType<?> entityType, double mass, double gravityLimit) {
			this.spawnCosts.put(entityType, new SpawnSettings.SpawnDensity(gravityLimit, mass));
			return this;
		}

		public SpawnSettings.Builder creatureSpawnProbability(float probability) {
			this.creatureSpawnProbability = probability;
			return this;
		}

		/**
		 * Build.
		 *
		 * @return SpawnSettings — результат операции
		 */
		public SpawnSettings build() {
			return new SpawnSettings(
					this.creatureSpawnProbability,
					this.spawners
							.entrySet()
							.stream()
							.collect(ImmutableMap.toImmutableMap(
									Entry::getKey,
									spawner -> ((Pool.Builder) spawner.getValue()).build()
							)),
					ImmutableMap.copyOf(this.spawnCosts)
			);
		}
	}

	/**
	 * {@code SpawnDensity}.
	 */
	public record SpawnDensity(double gravityLimit, double mass) {

		public static final Codec<SpawnSettings.SpawnDensity> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Codec.DOUBLE.fieldOf("energy_budget").forGetter(spawnDensity -> spawnDensity.gravityLimit),
						                    Codec.DOUBLE.fieldOf("charge").forGetter(spawnDensity -> spawnDensity.mass)
				                    )
				                    .apply(instance, SpawnSettings.SpawnDensity::new)
		);
	}

	/**
	 * {@code SpawnEntry}.
	 */
	public record SpawnEntry(EntityType<?> type, int minGroupSize, int maxGroupSize) {

		public static final MapCodec<SpawnSettings.SpawnEntry>
				CODEC =
				RecordCodecBuilder.<SpawnSettings.SpawnEntry>mapCodec(
						                  instance -> instance.group(
								                                      Registries.ENTITY_TYPE
										                                      .getCodec()
										                                      .fieldOf("type")
										                                      .forGetter(spawnEntry -> spawnEntry.type),
								                                      Codecs.POSITIVE_INT
										                                      .fieldOf("minCount")
										                                      .forGetter(spawnEntry -> spawnEntry.minGroupSize),
								                                      Codecs.POSITIVE_INT.fieldOf("maxCount").forGetter(spawnEntry -> spawnEntry.maxGroupSize)
						                                      )
						                                      .apply(instance, SpawnSettings.SpawnEntry::new)
				                  )
				                  .validate(
						                  (SpawnSettings.SpawnEntry spawnEntry) ->
								                  spawnEntry.minGroupSize > spawnEntry.maxGroupSize
								                  ? DataResult.error(() -> "minCount needs to be smaller or equal to maxCount")
								                  : DataResult.success(spawnEntry)
				                  );

		public SpawnEntry(EntityType<?> type, int minGroupSize, int maxGroupSize) {
			type = type.getSpawnGroup() == SpawnGroup.MISC ? EntityType.PIG : type;
			this.type = type;
			this.minGroupSize = minGroupSize;
			this.maxGroupSize = maxGroupSize;
		}

		@Override
		public String toString() {
			return EntityType.getId(this.type) + "*(" + this.minGroupSize + "-" + this.maxGroupSize + ")";
		}
	}
}
