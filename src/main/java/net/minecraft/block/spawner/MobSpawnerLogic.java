package net.minecraft.block.spawner;

import com.mojang.logging.LogUtils;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * {@code MobSpawnerLogic}.
 */
public abstract class MobSpawnerLogic {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final String SPAWN_DATA_KEY = "SpawnData";
	private static final int MIN_SPAWN_COUNT = 1;
	private static final int DEFAULT_SPAWN_DELAY = 20;
	private static final int DEFAULT_MIN_SPAWN_DELAY = 200;
	private static final int DEFAULT_MAX_SPAWN_DELAY = 800;
	private static final int DEFAULT_SPAWN_COUNT = 4;
	private static final int DEFAULT_MAX_NEARBY_ENTITIES = 6;
	private static final int DEFAULT_REQUIRED_PLAYER_RANGE = 16;
	private static final int DEFAULT_SPAWN_RANGE = 4;
	private int spawnDelay = DEFAULT_SPAWN_DELAY;
	private Pool<MobSpawnerEntry> spawnPotentials = Pool.empty();
	private @Nullable MobSpawnerEntry spawnEntry;
	private double rotation;
	private double lastRotation;
	private int minSpawnDelay = DEFAULT_MIN_SPAWN_DELAY;
	private int maxSpawnDelay = DEFAULT_MAX_SPAWN_DELAY;
	private int spawnCount = 4;
	private @Nullable Entity renderedEntity;
	private int maxNearbyEntities = 6;
	private int requiredPlayerRange = DEFAULT_REQUIRED_PLAYER_RANGE;
	private int spawnRange = 4;

	public void setEntityId(EntityType<?> type, @Nullable World world, Random random, BlockPos pos) {
		this.getSpawnEntry(world, random, pos).getNbt().putString("id", Registries.ENTITY_TYPE.getId(type).toString());
	}

	private boolean isPlayerInRange(World world, BlockPos pos) {
		return world.isPlayerInRange(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, this.requiredPlayerRange);
	}

	/**
	 * Client tick.
	 *
	 * @param world world
	 * @param pos pos
	 */
	public void clientTick(World world, BlockPos pos) {
		if (!this.isPlayerInRange(world, pos)) {
			this.lastRotation = this.rotation;
		}
		else if (this.renderedEntity != null) {
			Random random = world.getRandom();
			double d = pos.getX() + random.nextDouble();
			double e = pos.getY() + random.nextDouble();
			double f = pos.getZ() + random.nextDouble();
			world.addParticleClient(ParticleTypes.SMOKE, d, e, f, 0.0, 0.0, 0.0);
			world.addParticleClient(ParticleTypes.FLAME, d, e, f, 0.0, 0.0, 0.0);
			if (this.spawnDelay > 0) {
				this.spawnDelay--;
			}

			this.lastRotation = this.rotation;
			this.rotation = (this.rotation + 1000.0F / (this.spawnDelay + 200.0F)) % 360.0;
		}
	}

	/**
	 * Server tick.
	 *
	 * @param world world
	 * @param pos pos
	 */
	public void serverTick(ServerWorld world, BlockPos pos) {
		if (this.isPlayerInRange(world, pos) && world.areSpawnerBlocksEnabled()) {
			if (this.spawnDelay == -1) {
				this.updateSpawns(world, pos);
			}

			if (this.spawnDelay > 0) {
				this.spawnDelay--;
			}
			else {
				boolean bl = false;
				Random random = world.getRandom();
				MobSpawnerEntry mobSpawnerEntry = this.getSpawnEntry(world, random, pos);

				for (int i = 0; i < this.spawnCount; i++) {
					try (ErrorReporter.Logging logging = new ErrorReporter.Logging(this::toString, LOGGER)) {
						ReadView
								readView =
								NbtReadView.create(logging, world.getRegistryManager(), mobSpawnerEntry.getNbt());
						Optional<EntityType<?>> optional = EntityType.fromData(readView);
						if (optional.isEmpty()) {
							this.updateSpawns(world, pos);
							return;
						}

						Vec3d vec3d = readView.<Vec3d>read("Pos", Vec3d.CODEC)
						                      .orElseGet(
								                      () -> new Vec3d(
										                      pos.getX() + (random.nextDouble() - random.nextDouble())
												                      * this.spawnRange + 0.5,
										                      pos.getY() + random.nextInt(3) - 1,
										                      pos.getZ() + (random.nextDouble() - random.nextDouble())
												                      * this.spawnRange + 0.5
								                      )
						                      );
						if (world.isSpaceEmpty(optional.get().getSpawnBox(vec3d.x, vec3d.y, vec3d.z))) {
							BlockPos blockPos = BlockPos.ofFloored(vec3d);
							if (mobSpawnerEntry.getCustomSpawnRules().isPresent()) {
								if (!optional.get().getSpawnGroup().isPeaceful()
										&& world.getDifficulty() == Difficulty.PEACEFUL) {
									continue;
								}

								MobSpawnerEntry.CustomSpawnRules
										customSpawnRules =
										mobSpawnerEntry.getCustomSpawnRules().get();
								if (!customSpawnRules.canSpawn(blockPos, world)) {
									continue;
								}
							}
							else if (!SpawnRestriction.canSpawn(
									optional.get(),
									world,
									SpawnReason.SPAWNER,
									blockPos,
									world.getRandom()
							)) {
								continue;
							}

							Entity entity = EntityType.loadEntityWithPassengers(
									readView, world, SpawnReason.SPAWNER, entityx -> {
										entityx.refreshPositionAndAngles(
												vec3d.x,
												vec3d.y,
												vec3d.z,
												entityx.getYaw(),
												entityx.getPitch()
										);
										return entityx;
									}
							);
							if (entity == null) {
								this.updateSpawns(world, pos);
								return;
							}

							int j = world.getEntitiesByType(
									             TypeFilter.equals(entity.getClass()),
									             new Box(
											             pos.getX(),
											             pos.getY(),
											             pos.getZ(),
											             pos.getX() + 1,
											             pos.getY() + 1,
											             pos.getZ() + 1
									             ).expand(this.spawnRange),
									             EntityPredicates.EXCEPT_SPECTATOR
							             )
							             .size();
							if (j >= this.maxNearbyEntities) {
								this.updateSpawns(world, pos);
								return;
							}

							entity.refreshPositionAndAngles(
									entity.getX(),
									entity.getY(),
									entity.getZ(),
									random.nextFloat() * 360.0F,
									0.0F
							);
							if (entity instanceof MobEntity mobEntity) {
								if (mobSpawnerEntry.getCustomSpawnRules().isEmpty() && !mobEntity.canSpawn(
										world,
										SpawnReason.SPAWNER
								) || !mobEntity.canSpawn(world)) {
									continue;
								}

								boolean
										bl2 =
										mobSpawnerEntry.getNbt().getSize() == 1 && mobSpawnerEntry
												.getNbt()
												.getString("id")
												.isPresent();
								if (bl2) {
									((MobEntity) entity).initialize(
											world,
											world.getLocalDifficulty(entity.getBlockPos()),
											SpawnReason.SPAWNER,
											null
									);
								}

								mobSpawnerEntry.getEquipment().ifPresent(mobEntity::setEquipmentFromTable);
							}

							if (!world.spawnNewEntityAndPassengers(entity)) {
								this.updateSpawns(world, pos);
								return;
							}

							world.syncWorldEvent(2004, pos, 0);
							world.emitGameEvent(entity, GameEvent.ENTITY_PLACE, blockPos);
							if (entity instanceof MobEntity) {
								((MobEntity) entity).playSpawnEffects();
							}

							bl = true;
						}
					}
				}

				if (bl) {
					this.updateSpawns(world, pos);
				}

				return;
			}
		}
	}

	private void updateSpawns(World world, BlockPos pos) {
		Random random = world.random;
		if (this.maxSpawnDelay <= this.minSpawnDelay) {
			this.spawnDelay = this.minSpawnDelay;
		}
		else {
			this.spawnDelay = this.minSpawnDelay + random.nextInt(this.maxSpawnDelay - this.minSpawnDelay);
		}

		this.spawnPotentials
				.getOrEmpty(random)
				.ifPresent(spawnPotential -> this.setSpawnEntry(world, pos, spawnPotential));
		this.sendStatus(world, pos, 1);
	}

	/**
	 * Читает data.
	 *
	 * @param world world
	 * @param pos pos
	 * @param view view
	 */
	public void readData(@Nullable World world, BlockPos pos, ReadView view) {
		this.spawnDelay = view.getShort("Delay", (short) DEFAULT_SPAWN_DELAY);
		view
				.<MobSpawnerEntry>read("SpawnData", MobSpawnerEntry.CODEC)
				.ifPresent(mobSpawnerEntry -> this.setSpawnEntry(world, pos, mobSpawnerEntry));
		this.spawnPotentials = view.<Pool<MobSpawnerEntry>>read("SpawnPotentials", MobSpawnerEntry.DATA_POOL_CODEC)
		                           .orElseGet(() -> Pool.of(
				                           this.spawnEntry != null ? this.spawnEntry : new MobSpawnerEntry()));
		this.minSpawnDelay = view.getInt("MinSpawnDelay", DEFAULT_MIN_SPAWN_DELAY);
		this.maxSpawnDelay = view.getInt("MaxSpawnDelay", DEFAULT_MAX_SPAWN_DELAY);
		this.spawnCount = view.getInt("SpawnCount", 4);
		this.maxNearbyEntities = view.getInt("MaxNearbyEntities", 6);
		this.requiredPlayerRange = view.getInt("RequiredPlayerRange", DEFAULT_REQUIRED_PLAYER_RANGE);
		this.spawnRange = view.getInt("SpawnRange", 4);
		this.renderedEntity = null;
	}

	/**
	 * Записывает data.
	 *
	 * @param view view
	 */
	public void writeData(WriteView view) {
		view.putShort("Delay", (short) this.spawnDelay);
		view.putShort("MinSpawnDelay", (short) this.minSpawnDelay);
		view.putShort("MaxSpawnDelay", (short) this.maxSpawnDelay);
		view.putShort("SpawnCount", (short) this.spawnCount);
		view.putShort("MaxNearbyEntities", (short) this.maxNearbyEntities);
		view.putShort("RequiredPlayerRange", (short) this.requiredPlayerRange);
		view.putShort("SpawnRange", (short) this.spawnRange);
		view.putNullable("SpawnData", MobSpawnerEntry.CODEC, this.spawnEntry);
		view.put("SpawnPotentials", MobSpawnerEntry.DATA_POOL_CODEC, this.spawnPotentials);
	}

	public @Nullable Entity getRenderedEntity(World world, BlockPos pos) {
		if (this.renderedEntity == null) {
			NbtCompound nbtCompound = this.getSpawnEntry(world, world.getRandom(), pos).getNbt();
			if (nbtCompound.getString("id").isEmpty()) {
				return null;
			}

			this.renderedEntity =
					EntityType.loadEntityWithPassengers(
							nbtCompound,
							world,
							SpawnReason.SPAWNER,
							LoadedEntityProcessor.NOOP
					);
			if (nbtCompound.getSize() == 1 && this.renderedEntity instanceof MobEntity) {
			}
		}

		return this.renderedEntity;
	}

	/**
	 * Обрабатывает status.
	 *
	 * @param world world
	 * @param status status
	 *
	 * @return boolean — результат операции
	 */
	public boolean handleStatus(World world, int status) {
		if (status == 1) {
			if (world.isClient()) {
				this.spawnDelay = this.minSpawnDelay;
			}

			return true;
		}
		else {
			return false;
		}
	}

	protected void setSpawnEntry(@Nullable World world, BlockPos pos, MobSpawnerEntry spawnEntry) {
		this.spawnEntry = spawnEntry;
	}

	private MobSpawnerEntry getSpawnEntry(@Nullable World world, Random random, BlockPos pos) {
		if (this.spawnEntry != null) {
			return this.spawnEntry;
		}
		else {
			this.setSpawnEntry(world, pos, this.spawnPotentials.getOrEmpty(random).orElseGet(MobSpawnerEntry::new));
			return this.spawnEntry;
		}
	}

	/**
	 * Отправляет status.
	 *
	 * @param world world
	 * @param pos pos
	 * @param status status
	 */
	public abstract void sendStatus(World world, BlockPos pos, int status);

	public double getRotation() {
		return this.rotation;
	}

	public double getLastRotation() {
		return this.lastRotation;
	}
}
