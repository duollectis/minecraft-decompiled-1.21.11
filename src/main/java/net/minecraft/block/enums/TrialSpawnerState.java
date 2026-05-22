package net.minecraft.block.enums;

import net.minecraft.block.ShapeContext;
import net.minecraft.block.spawner.TrialSpawnerConfig;
import net.minecraft.block.spawner.TrialSpawnerData;
import net.minecraft.block.spawner.TrialSpawnerLogic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.OminousItemSpawnerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

/**
 * Состояние спаунера испытания (Trial Spawner).
 * Управляет полным жизненным циклом: от ожидания игроков до выдачи наград и перезарядки.
 */
public enum TrialSpawnerState implements StringIdentifiable {

	/** Спаунер неактивен — не обнаружен ни один игрок в зоне действия. */
	INACTIVE("inactive", 0, TrialSpawnerState.ParticleEmitter.NONE, DisplayRotationSpeed.NONE, false),
	/** Спаунер ожидает игроков — они в зоне, но волна ещё не началась. */
	WAITING_FOR_PLAYERS("waiting_for_players", 4, TrialSpawnerState.ParticleEmitter.WAITING, DisplayRotationSpeed.SLOW, true),
	/** Спаунер активен — идёт волна спавна мобов. */
	ACTIVE("active", 8, TrialSpawnerState.ParticleEmitter.ACTIVE, DisplayRotationSpeed.FAST, true),
	/** Все мобы убиты, спаунер ожидает начала выброса наград. */
	WAITING_FOR_REWARD_EJECTION(
			"waiting_for_reward_ejection",
			8,
			TrialSpawnerState.ParticleEmitter.WAITING,
			DisplayRotationSpeed.NONE,
			false
	),
	/** Спаунер выбрасывает награды игрокам по одному. */
	EJECTING_REWARD("ejecting_reward", 8, TrialSpawnerState.ParticleEmitter.WAITING, DisplayRotationSpeed.NONE, false),
	/** Спаунер на перезарядке — ожидает следующего цикла. */
	COOLDOWN("cooldown", 0, TrialSpawnerState.ParticleEmitter.COOLDOWN, DisplayRotationSpeed.NONE, false);

	private static final float START_EJECTING_REWARDS_COOLDOWN = 40.0F;
	private static final int BETWEEN_EJECTING_REWARDS_COOLDOWN = MathHelper.floor(30.0F);

	private final String id;
	private final int luminance;
	private final double displayRotationSpeed;
	private final TrialSpawnerState.ParticleEmitter particleEmitter;
	private final boolean playsSound;

	TrialSpawnerState(
			final String id,
			final int luminance,
			final TrialSpawnerState.ParticleEmitter particleEmitter,
			final double displayRotationSpeed,
			final boolean playsSound
	) {
		this.id = id;
		this.luminance = luminance;
		this.particleEmitter = particleEmitter;
		this.displayRotationSpeed = displayRotationSpeed;
		this.playsSound = playsSound;
	}

	/**
	 * Выполняет один тик логики спаунера и возвращает следующее состояние.
	 * Реализует конечный автомат: INACTIVE → WAITING → ACTIVE → EJECTING → COOLDOWN → WAITING.
	 *
	 * @param pos   позиция блока спаунера
	 * @param logic логика спаунера (доступ к данным, конфигурации и методам спавна)
	 * @param world серверный мир
	 * @return следующее состояние спаунера после обработки тика
	 */
	public TrialSpawnerState tick(BlockPos pos, TrialSpawnerLogic logic, ServerWorld world) {
		TrialSpawnerData spawnerData = logic.getData();
		TrialSpawnerConfig spawnerConfig = logic.getConfig();

		return switch (this) {
			case INACTIVE -> spawnerData.setDisplayEntity(logic, world, WAITING_FOR_PLAYERS) == null
					? this
					: WAITING_FOR_PLAYERS;
			case WAITING_FOR_PLAYERS -> {
				if (logic.canActivate(world) == false) {
					spawnerData.deactivate();
					yield this;
				}

				if (spawnerData.hasSpawnData(logic, world.random) == false) {
					yield INACTIVE;
				}

				spawnerData.updatePlayers(world, pos, logic);
				yield spawnerData.players.isEmpty() ? this : ACTIVE;
			}
			case ACTIVE -> {
				if (logic.canActivate(world) == false) {
					spawnerData.deactivate();
					yield WAITING_FOR_PLAYERS;
				}

				if (spawnerData.hasSpawnData(logic, world.random) == false) {
					yield INACTIVE;
				}

				int additionalPlayers = spawnerData.getAdditionalPlayers(pos);
				spawnerData.updatePlayers(world, pos, logic);

				if (logic.isOminous()) {
					spawnOminousItemSpawner(world, pos, logic);
				}

				if (spawnerData.hasSpawnedAllMobs(spawnerConfig, additionalPlayers)) {
					if (spawnerData.areMobsDead()) {
						spawnerData.cooldownEnd = world.getTime() + logic.getCooldownLength();
						spawnerData.totalSpawnedMobs = 0;
						spawnerData.nextMobSpawnsAt = 0L;
						yield WAITING_FOR_REWARD_EJECTION;
					}
				} else if (spawnerData.canSpawnMore(world, spawnerConfig, additionalPlayers)) {
					logic.trySpawnMob(world, pos).ifPresent(uuid -> {
						spawnerData.spawnedMobsAlive.add(uuid);
						spawnerData.totalSpawnedMobs++;
						spawnerData.nextMobSpawnsAt = world.getTime() + spawnerConfig.ticksBetweenSpawn();
						spawnerConfig.spawnPotentials().getOrEmpty(world.getRandom()).ifPresent(spawnData -> {
							spawnerData.spawnData = Optional.of(spawnData);
							logic.updateListeners();
						});
					});
				}

				yield this;
			}
			case WAITING_FOR_REWARD_EJECTION -> {
				if (spawnerData.isCooldownPast(world, START_EJECTING_REWARDS_COOLDOWN, logic.getCooldownLength())) {
					world.playSound(null, pos, SoundEvents.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, SoundCategory.BLOCKS);
					yield EJECTING_REWARD;
				}

				yield this;
			}
			case EJECTING_REWARD -> {
				if (spawnerData.isCooldownAtRepeating(world, BETWEEN_EJECTING_REWARDS_COOLDOWN, logic.getCooldownLength()) == false) {
					yield this;
				}

				if (spawnerData.players.isEmpty()) {
					world.playSound(null, pos, SoundEvents.BLOCK_TRIAL_SPAWNER_CLOSE_SHUTTER, SoundCategory.BLOCKS);
					spawnerData.rewardLootTable = Optional.empty();
					yield COOLDOWN;
				}

				if (spawnerData.rewardLootTable.isEmpty()) {
					spawnerData.rewardLootTable = spawnerConfig.lootTablesToEject().getOrEmpty(world.getRandom());
				}

				spawnerData.rewardLootTable.ifPresent(lootTable -> logic.ejectLootTable(
						world,
						pos,
						(RegistryKey<LootTable>) lootTable
				));
				spawnerData.players.remove(spawnerData.players.iterator().next());
				yield this;
			}
			case COOLDOWN -> {
				spawnerData.updatePlayers(world, pos, logic);

				if (spawnerData.players.isEmpty() == false) {
					spawnerData.totalSpawnedMobs = 0;
					spawnerData.nextMobSpawnsAt = 0L;
					yield ACTIVE;
				}

				if (spawnerData.isCooldownOver(world)) {
					logic.setNotOminous(world, pos);
					spawnerData.reset();
					yield WAITING_FOR_PLAYERS;
				}

				yield this;
			}
		};
	}

	private void spawnOminousItemSpawner(ServerWorld world, BlockPos pos, TrialSpawnerLogic logic) {
		TrialSpawnerData spawnerData = logic.getData();
		TrialSpawnerConfig spawnerConfig = logic.getConfig();
		ItemStack itemStack = spawnerData
				.getItemsToDropWhenOminous(world, spawnerConfig, pos)
				.getOrEmpty(world.random)
				.orElse(ItemStack.EMPTY);

		if (itemStack.isEmpty()) {
			return;
		}

		if (shouldCooldownEnd(world, spawnerData) == false) {
			return;
		}

		getPosToSpawnItemSpawner(world, pos, logic, spawnerData).ifPresent(spawnPos -> {
			OminousItemSpawnerEntity spawnerEntity = OminousItemSpawnerEntity.create(world, itemStack);
			spawnerEntity.refreshPositionAfterTeleport(spawnPos);
			world.spawnEntity(spawnerEntity);

			float pitch = (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.2F + 1.0F;
			world.playSound(
					null,
					BlockPos.ofFloored(spawnPos),
					SoundEvents.BLOCK_TRIAL_SPAWNER_SPAWN_ITEM_BEGIN,
					SoundCategory.BLOCKS,
					1.0F,
					pitch
			);
			spawnerData.cooldownEnd = world.getTime() + logic.getOminousConfig().getCooldownLength();
		});
	}

	private static Optional<Vec3d> getPosToSpawnItemSpawner(
			ServerWorld world,
			BlockPos pos,
			TrialSpawnerLogic logic,
			TrialSpawnerData data
	) {
		List<PlayerEntity> eligiblePlayers = data.players
				.stream()
				.map(world::getPlayerByUuid)
				.filter(Objects::nonNull)
				.filter(
						player -> player.isCreative() == false
								&& player.isSpectator() == false
								&& player.isAlive()
								&& player.squaredDistanceTo(pos.toCenterPos()) <= MathHelper.square(logic.getDetectionRadius())
				)
				.toList();

		if (eligiblePlayers.isEmpty()) {
			return Optional.empty();
		}

		Entity target = getRandomEntity(eligiblePlayers, data.spawnedMobsAlive, logic, pos, world);
		return target == null ? Optional.empty() : getPosAbove(target, world);
	}

	private static Optional<Vec3d> getPosAbove(Entity entity, ServerWorld world) {
		Vec3d entityPos = entity.getEntityPos();
		Vec3d targetPos = entityPos.offset(Direction.UP, entity.getHeight() + 2.0F + world.random.nextInt(4));
		BlockHitResult hitResult = world.raycast(
				new RaycastContext(
						entityPos,
						targetPos,
						RaycastContext.ShapeType.VISUAL,
						RaycastContext.FluidHandling.NONE,
						ShapeContext.absent()
				)
		);
		Vec3d spawnPos = hitResult.getBlockPos().toCenterPos().offset(Direction.DOWN, 1.0);
		BlockPos spawnBlock = BlockPos.ofFloored(spawnPos);
		return world.getBlockState(spawnBlock).getCollisionShape(world, spawnBlock).isEmpty()
				? Optional.of(spawnPos)
				: Optional.empty();
	}

	private static @Nullable Entity getRandomEntity(
			List<PlayerEntity> players,
			Set<UUID> entityUuids,
			TrialSpawnerLogic logic,
			BlockPos pos,
			ServerWorld world
	) {
		Stream<Entity> mobStream = entityUuids.stream()
				.map(world::getEntity)
				.filter(Objects::nonNull)
				.filter(
						entity -> entity.isAlive()
								&& entity.squaredDistanceTo(pos.toCenterPos()) <= MathHelper.square(logic.getDetectionRadius())
				);
		List<? extends Entity> candidates = world.random.nextBoolean() ? mobStream.toList() : players;

		if (candidates.isEmpty()) {
			return null;
		}

		return candidates.size() == 1 ? candidates.getFirst() : Util.getRandom(candidates, world.random);
	}

	private boolean shouldCooldownEnd(ServerWorld world, TrialSpawnerData data) {
		return world.getTime() >= data.cooldownEnd;
	}

	public int getLuminance() {
		return luminance;
	}

	public double getDisplayRotationSpeed() {
		return displayRotationSpeed;
	}

	public boolean doesDisplayRotate() {
		return displayRotationSpeed >= 0.0;
	}

	public boolean playsSound() {
		return playsSound;
	}

	public void emitParticles(World world, BlockPos pos, boolean ominous) {
		particleEmitter.emit(world, world.getRandom(), pos, ominous);
	}

	@Override
	public String asString() {
		return id;
	}

	/** Константы скорости вращения отображаемого предмета в блоке спаунера. */
	static class DisplayRotationSpeed {

		static final double NONE = -1.0;
		static final double SLOW = 200.0;
		static final double FAST = 1000.0;

		private DisplayRotationSpeed() {
		}
	}

	/** Константы яркости блока спаунера в зависимости от состояния. */
	static class Luminance {

		static final int NONE = 0;
		static final int LOW = 4;
		static final int HIGH = 8;

		private Luminance() {
		}
	}

	/** Стратегия испускания частиц для каждого состояния спаунера. */
	interface ParticleEmitter {

		ParticleEmitter NONE = (world, random, pos, ominous) -> {};

		ParticleEmitter WAITING = (world, random, pos, ominous) -> {
			if (random.nextInt(2) == 0) {
				Vec3d vec = pos.toCenterPos().addRandom(random, 0.9F);
				emitParticle(ominous ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMALL_FLAME, vec, world);
			}
		};

		ParticleEmitter ACTIVE = (world, random, pos, ominous) -> {
			Vec3d vec = pos.toCenterPos().addRandom(random, 1.0F);
			emitParticle(ParticleTypes.SMOKE, vec, world);
			emitParticle(ominous ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, vec, world);
		};

		ParticleEmitter COOLDOWN = (world, random, pos, ominous) -> {
			Vec3d vec = pos.toCenterPos().addRandom(random, 0.9F);

			if (random.nextInt(3) == 0) {
				emitParticle(ParticleTypes.SMOKE, vec, world);
			}

			if (world.getTime() % 20L == 0L) {
				Vec3d centerAbove = pos.toCenterPos().add(0.0, 0.5, 0.0);
				int particleCount = world.getRandom().nextInt(4) + 20;

				for (int particleIndex = 0; particleIndex < particleCount; particleIndex++) {
					emitParticle(ParticleTypes.SMOKE, centerAbove, world);
				}
			}
		};

		private static void emitParticle(SimpleParticleType type, Vec3d pos, World world) {
			world.addParticleClient(type, pos.getX(), pos.getY(), pos.getZ(), 0.0, 0.0, 0.0);
		}

		void emit(World world, Random random, BlockPos pos, boolean ominous);
	}
}
