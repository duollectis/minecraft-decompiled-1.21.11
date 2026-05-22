package net.minecraft.block.entity;

import com.mojang.datafixers.util.Either;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CreakingHeartBlock;
import net.minecraft.block.MultifaceBlock;
import net.minecraft.block.enums.CreakingHeartState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LargeEntitySpawnHelper;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreakingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.TrailParticleEffect;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Util;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.event.GameEvent;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Блок-сущность сердца скрипуна. Управляет жизненным циклом скрипуна-марионетки:
 * спавном, отслеживанием, уничтожением и генерацией частиц смолы при получении урона.
 */
public class CreakingHeartBlockEntity extends BlockEntity {

	public static final int SPAWN_RANGE = 32;
	private static final int DESPAWN_RANGE = 34;
	private static final int TRAIL_PARTICLE_RANGE = 16;
	private static final int UPDATE_INTERVAL_MIN = 20;
	private static final int TRAIL_SOUND_INTERVAL = 10;
	private static final int TRAIL_PARTICLES_DURATION = 100;
	private static final int TRAIL_PARTICLES_FAST_THRESHOLD = 50;
	private static final int TRAIL_PARTICLE_SLOW_COUNT = 64;
	private static final int PLAYER_DETECTION_RANGE = 30;
	private static final Optional<CreakingEntity> DEFAULT_CREAKING_PUPPET = Optional.empty();

	private @Nullable Either<CreakingEntity, UUID> creakingPuppet;
	private long ticks;
	private int creakingUpdateTimer;
	private int trailParticlesSpawnTimer;
	private @Nullable Vec3d lastCreakingPuppetPos;
	private int comparatorOutput;

	public CreakingHeartBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.CREAKING_HEART, pos, state);
	}

	public static void tick(World world, BlockPos pos, BlockState state, CreakingHeartBlockEntity blockEntity) {
		blockEntity.ticks++;
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}

		int newComparatorOutput = blockEntity.calcComparatorOutput();
		if (blockEntity.comparatorOutput != newComparatorOutput) {
			blockEntity.comparatorOutput = newComparatorOutput;
			world.updateComparators(pos, Blocks.CREAKING_HEART);
		}

		if (blockEntity.trailParticlesSpawnTimer > 0) {
			if (blockEntity.trailParticlesSpawnTimer > TRAIL_PARTICLES_FAST_THRESHOLD) {
				blockEntity.spawnTrailParticles(serverWorld, 1, true);
				blockEntity.spawnTrailParticles(serverWorld, 1, false);
			}

			if (blockEntity.trailParticlesSpawnTimer % TRAIL_SOUND_INTERVAL == 0
					&& blockEntity.lastCreakingPuppetPos != null
			) {
				blockEntity.getCreakingPuppet()
						.ifPresent(creaking -> blockEntity.lastCreakingPuppetPos = creaking.getBoundingBox().getCenter());

				Vec3d heartCenter = Vec3d.ofCenter(pos);
				float progress = 0.2F + 0.8F * (TRAIL_PARTICLES_DURATION - blockEntity.trailParticlesSpawnTimer)
						/ (float) TRAIL_PARTICLES_DURATION;
				Vec3d soundPos = heartCenter
						.subtract(blockEntity.lastCreakingPuppetPos)
						.multiply(progress)
						.add(blockEntity.lastCreakingPuppetPos);
				float volume = blockEntity.trailParticlesSpawnTimer / 2.0F / TRAIL_PARTICLES_DURATION + 0.5F;

				serverWorld.playSound(
						null,
						BlockPos.ofFloored(soundPos),
						SoundEvents.BLOCK_CREAKING_HEART_HURT,
						SoundCategory.BLOCKS,
						volume,
						1.0F
				);
			}

			blockEntity.trailParticlesSpawnTimer--;
		}

		if (blockEntity.creakingUpdateTimer-- < 0) {
			blockEntity.creakingUpdateTimer = blockEntity.world == null
					? UPDATE_INTERVAL_MIN
					: blockEntity.world.random.nextInt(5) + UPDATE_INTERVAL_MIN;

			BlockState newState = getBlockState(world, state, pos, blockEntity);
			if (newState != state) {
				world.setBlockState(pos, newState, 3);
				if (newState.get(CreakingHeartBlock.ACTIVE) == CreakingHeartState.UPROOTED) {
					return;
				}
			}

			if (blockEntity.creakingPuppet == null) {
				trySpawnPuppet(serverWorld, world, pos, newState, blockEntity);
			} else {
				tryDespawnPuppet(world, pos, blockEntity);
			}
		}
	}

	private static void trySpawnPuppet(
			ServerWorld serverWorld,
			World world,
			BlockPos pos,
			BlockState state,
			CreakingHeartBlockEntity blockEntity
	) {
		if (state.get(CreakingHeartBlock.ACTIVE) != CreakingHeartState.AWAKE) {
			return;
		}

		if (!serverWorld.shouldSpawnMonsters()) {
			return;
		}

		PlayerEntity nearestPlayer = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), SPAWN_RANGE, false);
		if (nearestPlayer == null) {
			return;
		}

		CreakingEntity spawned = spawnCreakingPuppet(serverWorld, blockEntity);
		if (spawned == null) {
			return;
		}

		blockEntity.setCreakingPuppet(spawned);
		spawned.playSound(SoundEvents.ENTITY_CREAKING_SPAWN);
		world.playSound(null, blockEntity.getPos(), SoundEvents.BLOCK_CREAKING_HEART_SPAWN, SoundCategory.BLOCKS, 1.0F, 1.0F);
	}

	private static void tryDespawnPuppet(World world, BlockPos pos, CreakingHeartBlockEntity blockEntity) {
		blockEntity.getCreakingPuppet().ifPresent(creaking -> {
			boolean shouldDespawn = (!world.getEnvironmentAttributes()
					.getAttributeValue(EnvironmentAttributes.CREAKING_ACTIVE_GAMEPLAY, pos)
					&& !creaking.isPersistent())
					|| blockEntity.getDistanceToPuppet() > DESPAWN_RANGE
					|| creaking.isStuckWithPlayer();

			if (shouldDespawn) {
				blockEntity.killPuppet(null);
			}
		});
	}

	private static BlockState getBlockState(
			World world,
			BlockState state,
			BlockPos pos,
			CreakingHeartBlockEntity creakingHeart
	) {
		if (!CreakingHeartBlock.shouldBeEnabled(state, world, pos) && creakingHeart.creakingPuppet == null) {
			return state.with(CreakingHeartBlock.ACTIVE, CreakingHeartState.UPROOTED);
		}

		CreakingHeartState newState = world
				.getEnvironmentAttributes()
				.getAttributeValue(EnvironmentAttributes.CREAKING_ACTIVE_GAMEPLAY, pos)
				? CreakingHeartState.AWAKE
				: CreakingHeartState.DORMANT;

		return state.with(CreakingHeartBlock.ACTIVE, newState);
	}

	private double getDistanceToPuppet() {
		return getCreakingPuppet()
				.map(creaking -> Math.sqrt(creaking.squaredDistanceTo(Vec3d.ofBottomCenter(getPos()))))
				.orElse(0.0);
	}

	private void clearCreakingPuppet() {
		creakingPuppet = null;
		markDirty();
	}

	public void setCreakingPuppet(CreakingEntity creakingPuppet) {
		this.creakingPuppet = Either.left(creakingPuppet);
		markDirty();
	}

	public void setCreakingPuppetFromUuid(UUID creakingPuppetUuid) {
		creakingPuppet = Either.right(creakingPuppetUuid);
		ticks = 0L;
		markDirty();
	}

	private Optional<CreakingEntity> getCreakingPuppet() {
		if (creakingPuppet == null) {
			return DEFAULT_CREAKING_PUPPET;
		}

		if (creakingPuppet.left().isPresent()) {
			CreakingEntity creaking = (CreakingEntity) creakingPuppet.left().get();
			if (!creaking.isRemoved()) {
				return Optional.of(creaking);
			}

			setCreakingPuppetFromUuid(creaking.getUuid());
		}

		if (world instanceof ServerWorld serverWorld && creakingPuppet.right().isPresent()) {
			UUID uuid = (UUID) creakingPuppet.right().get();
			if (serverWorld.getEntity(uuid) instanceof CreakingEntity resolved) {
				setCreakingPuppet(resolved);
				return Optional.of(resolved);
			}

			if (ticks >= PLAYER_DETECTION_RANGE) {
				clearCreakingPuppet();
			}

			return DEFAULT_CREAKING_PUPPET;
		}

		return DEFAULT_CREAKING_PUPPET;
	}

	private static @Nullable CreakingEntity spawnCreakingPuppet(
			ServerWorld world,
			CreakingHeartBlockEntity blockEntity
	) {
		BlockPos blockPos = blockEntity.getPos();
		Optional<CreakingEntity> spawnResult = LargeEntitySpawnHelper.trySpawnAt(
				EntityType.CREAKING,
				SpawnReason.SPAWNER,
				world,
				blockPos,
				5,
				TRAIL_PARTICLE_RANGE,
				8,
				LargeEntitySpawnHelper.Requirements.CREAKING,
				true
		);

		if (spawnResult.isEmpty()) {
			return null;
		}

		CreakingEntity creaking = spawnResult.get();
		world.emitGameEvent(creaking, GameEvent.ENTITY_PLACE, creaking.getEntityPos());
		world.sendEntityStatus(creaking, (byte) Entity.MAX_RIDING_COOLDOWN);
		creaking.initHomePos(blockPos);
		return creaking;
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createComponentlessNbt(registries);
	}

	/**
	 * Вызывается при получении урона скрипуном-марионеткой. Запускает анимацию частиц смолы
	 * и генерирует сгустки смолы на ближайших бледных дубовых брёвнах.
	 */
	public void onPuppetDamage() {
		if (getCreakingPuppet().orElse(null) instanceof CreakingEntity creaking
				&& world instanceof ServerWorld serverWorld
				&& trailParticlesSpawnTimer <= 0
		) {
			spawnTrailParticles(serverWorld, UPDATE_INTERVAL_MIN, false);

			if (getCachedState().get(CreakingHeartBlock.ACTIVE) == CreakingHeartState.AWAKE) {
				int resinCount = world.getRandom().nextBetween(2, 3);

				for (int i = 0; i < resinCount; i++) {
					findResinGenerationPos(serverWorld).ifPresent(resinPos -> {
						world.playSound(null, resinPos, SoundEvents.BLOCK_RESIN_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
						world.emitGameEvent(GameEvent.BLOCK_PLACE, resinPos, GameEvent.Emitter.of(getCachedState()));
					});
				}
			}

			trailParticlesSpawnTimer = TRAIL_PARTICLES_DURATION;
			lastCreakingPuppetPos = creaking.getBoundingBox().getCenter();
		}
	}

	/**
	 * Ищет позицию для генерации сгустка смолы рядом с сердцем скрипуна.
	 * Рекурсивно обходит бледные дубовые брёвна и пытается разместить смолу на свободной грани.
	 */
	private Optional<BlockPos> findResinGenerationPos(ServerWorld world) {
		Mutable<BlockPos> result = new MutableObject<>(null);

		BlockPos.iterateRecursively(
				pos, 2, TRAIL_PARTICLE_SLOW_COUNT,
				(currentPos, consumer) -> {
					for (Direction direction : Util.copyShuffled(Direction.values(), world.random)) {
						BlockPos neighbor = currentPos.offset(direction);
						if (world.getBlockState(neighbor).isIn(BlockTags.PALE_OAK_LOGS)) {
							consumer.accept(neighbor);
						}
					}
				},
				(currentPos) -> {
					if (!world.getBlockState(currentPos).isIn(BlockTags.PALE_OAK_LOGS)) {
						return BlockPos.IterationState.ACCEPT;
					}

					for (Direction direction : Util.copyShuffled(Direction.values(), world.random)) {
						BlockPos neighbor = currentPos.offset(direction);
						BlockState neighborState = world.getBlockState(neighbor);
						Direction opposite = direction.getOpposite();

						if (neighborState.isAir()) {
							neighborState = Blocks.RESIN_CLUMP.getDefaultState();
						} else if (neighborState.isOf(Blocks.WATER) && neighborState.getFluidState().isStill()) {
							neighborState = Blocks.RESIN_CLUMP.getDefaultState().with(MultifaceBlock.WATERLOGGED, true);
						}

						if (neighborState.isOf(Blocks.RESIN_CLUMP)
								&& !MultifaceBlock.hasDirection(neighborState, opposite)
						) {
							world.setBlockState(neighbor, neighborState.with(MultifaceBlock.getProperty(opposite), true), 3);
							result.setValue(neighbor);
							return BlockPos.IterationState.STOP;
						}
					}

					return BlockPos.IterationState.ACCEPT;
				}
		);

		return Optional.ofNullable(result.get());
	}

	private void spawnTrailParticles(ServerWorld world, int count, boolean towardsPuppet) {
		if (getCreakingPuppet().orElse(null) == null) {
			return;
		}

		CreakingEntity creaking = (CreakingEntity) getCreakingPuppet().orElse(null);
		if (creaking == null) {
			return;
		}

		int color = towardsPuppet ? 16545810 : 6250335;
		Random random = world.random;

		for (int particleIndex = 0; particleIndex < count; particleIndex++) {
			Box box = creaking.getBoundingBox();
			Vec3d puppetPos = box.getMinPos().add(
					random.nextDouble() * box.getLengthX(),
					random.nextDouble() * box.getLengthY(),
					random.nextDouble() * box.getLengthZ()
			);
			Vec3d heartPos = Vec3d.of(getPos()).add(random.nextDouble(), random.nextDouble(), random.nextDouble());

			Vec3d from = towardsPuppet ? heartPos : puppetPos;
			Vec3d to = towardsPuppet ? puppetPos : heartPos;

			TrailParticleEffect effect = new TrailParticleEffect(to, color, random.nextInt(40) + 10);
			world.spawnParticles(effect, true, true, from.x, from.y, from.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	@Override
	public void onBlockReplaced(BlockPos pos, BlockState oldState) {
		this.killPuppet(null);
	}

	public void killPuppet(@Nullable DamageSource damageSource) {
		if (getCreakingPuppet().orElse(null) == null) {
			return;
		}

		CreakingEntity creaking = (CreakingEntity) getCreakingPuppet().orElse(null);
		if (creaking == null) {
			return;
		}

		if (damageSource == null) {
			creaking.finishCrumbling();
		} else {
			creaking.killFromHeart(damageSource);
			creaking.setCrumbling();
			creaking.setHealth(0.0F);
		}

		clearCreakingPuppet();
	}

	public boolean isPuppet(CreakingEntity creaking) {
		return getCreakingPuppet().map(puppet -> puppet == creaking).orElse(false);
	}

	public int getComparatorOutput() {
		return comparatorOutput;
	}

	/**
	 * Вычисляет выходной сигнал компаратора на основе расстояния до скрипуна-марионетки.
	 * Чем ближе скрипун к сердцу, тем сильнее сигнал (максимум 15).
	 */
	public int calcComparatorOutput() {
		if (creakingPuppet == null || getCreakingPuppet().isEmpty()) {
			return 0;
		}

		double distance = getDistanceToPuppet();
		double normalizedDist = Math.clamp(distance, 0.0, 32.0) / 32.0;
		return 15 - (int) Math.floor(normalizedDist * 15.0);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		view.<UUID>read("creaking", Uuids.INT_STREAM_CODEC)
				.ifPresentOrElse(this::setCreakingPuppetFromUuid, this::clearCreakingPuppet);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		if (creakingPuppet == null) {
			return;
		}

		view.put("creaking", Uuids.INT_STREAM_CODEC, (UUID) creakingPuppet.map(Entity::getUuid, uuid -> uuid));
	}
}
