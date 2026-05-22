package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;

/**
 * Блок-сущность колокола. Управляет анимацией звона, резонансом и применением
 * эффекта свечения к рейдерам в радиусе слышимости.
 */
public class BellBlockEntity extends BlockEntity {

	private static final int MAX_RINGING_TICKS = 50;
	private static final int HEARING_CACHE_COOLDOWN_TICKS = 60;
	private static final int AMBIENT_SOUND_COOLDOWN_TICKS = 60;
	private static final int MAX_RESONATING_TICKS = 40;
	private static final int RESONANCE_START_TICK = 5;
	private static final int HEARING_RANGE = 48;
	private static final int MAX_BELL_HEARING_DISTANCE = 32;
	private static final int PARTICLE_RANGE = 48;
	private long lastRingTime;
	public int ringTicks;
	public boolean ringing;
	public Direction lastSideHit;
	private List<LivingEntity> hearingEntities;
	private boolean resonating;
	private int resonateTime;

	public BellBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.BELL, pos, state);
	}

	private static final int RING_EVENT_ID = 1;

	@Override
	public boolean onSyncedBlockEvent(int type, int data) {
		if (type == RING_EVENT_ID) {
			notifyMemoriesOfBell();
			resonateTime = 0;
			lastSideHit = Direction.byIndex(data);
			ringTicks = 0;
			ringing = true;
			return true;
		}

		return super.onSyncedBlockEvent(type, data);
	}

	private static void tick(
			World world,
			BlockPos pos,
			BlockState state,
			BellBlockEntity blockEntity,
			BellBlockEntity.Effect bellEffect
	) {
		if (blockEntity.ringing) {
			blockEntity.ringTicks++;
		}

		if (blockEntity.ringTicks >= MAX_RINGING_TICKS) {
			blockEntity.ringing = false;
			blockEntity.ringTicks = 0;
		}

		if (blockEntity.ringTicks >= RESONANCE_START_TICK
				&& blockEntity.resonateTime == 0
				&& raidersHearBell(pos, blockEntity.hearingEntities)
		) {
			blockEntity.resonating = true;
			world.playSound(null, pos, SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.BLOCKS, 1.0F, 1.0F);
		}

		if (blockEntity.resonating) {
			if (blockEntity.resonateTime < MAX_RESONATING_TICKS) {
				blockEntity.resonateTime++;
			} else {
				bellEffect.run(world, pos, blockEntity.hearingEntities);
				blockEntity.resonating = false;
			}
		}
	}

	public static void clientTick(World world, BlockPos pos, BlockState state, BellBlockEntity blockEntity) {
		tick(world, pos, state, blockEntity, BellBlockEntity::applyParticlesToRaiders);
	}

	public static void serverTick(World world, BlockPos pos, BlockState state, BellBlockEntity blockEntity) {
		tick(world, pos, state, blockEntity, BellBlockEntity::applyGlowToRaiders);
	}

	public void activate(Direction direction) {
		BlockPos blockPos = getPos();
		lastSideHit = direction;
		ringing = ringing ? (ringTicks = 0) == 0 : true;
		world.addSyncedBlockEvent(blockPos, getCachedState().getBlock(), RING_EVENT_ID, direction.getIndex());
	}

	private void notifyMemoriesOfBell() {
		BlockPos blockPos = getPos();

		if (world.getTime() > lastRingTime + HEARING_CACHE_COOLDOWN_TICKS || hearingEntities == null) {
			lastRingTime = world.getTime();
			hearingEntities = world.getNonSpectatingEntities(LivingEntity.class, new Box(blockPos).expand(HEARING_RANGE));
		}

		if (world.isClient()) {
			return;
		}

		for (LivingEntity entity : hearingEntities) {
			if (entity.isAlive() && !entity.isRemoved()
					&& blockPos.isWithinDistance(entity.getEntityPos(), MAX_BELL_HEARING_DISTANCE)) {
				entity.getBrain().remember(MemoryModuleType.HEARD_BELL_TIME, world.getTime());
			}
		}
	}

	private static boolean raidersHearBell(BlockPos pos, List<LivingEntity> hearingEntities) {
		for (LivingEntity livingEntity : hearingEntities) {
			if (livingEntity.isAlive()
					&& !livingEntity.isRemoved()
					&& pos.isWithinDistance(livingEntity.getEntityPos(), 32.0)
					&& livingEntity.getType().isIn(EntityTypeTags.RAIDERS)) {
				return true;
			}
		}

		return false;
	}

	private static void applyGlowToRaiders(World world, BlockPos pos, List<LivingEntity> hearingEntities) {
		hearingEntities
				.stream()
				.filter(entity -> isRaiderEntity(pos, entity))
				.forEach(BellBlockEntity::applyGlowToEntity);
	}

	private static void applyParticlesToRaiders(World world, BlockPos pos, List<LivingEntity> hearingEntities) {
		MutableInt colorCounter = new MutableInt(16700985);
		int nearbyCount = (int) hearingEntities.stream()
				.filter(entity -> pos.isWithinDistance(entity.getEntityPos(), PARTICLE_RANGE))
				.count();

		hearingEntities.stream().filter(entity -> isRaiderEntity(pos, entity)).forEach(entity -> {
			double dist = Math.sqrt(
					(entity.getX() - pos.getX()) * (entity.getX() - pos.getX())
					+ (entity.getZ() - pos.getZ()) * (entity.getZ() - pos.getZ())
			);
			double particleX = pos.getX() + 0.5F + 1.0 / dist * (entity.getX() - pos.getX());
			double particleZ = pos.getZ() + 0.5F + 1.0 / dist * (entity.getZ() - pos.getZ());
			int particleCount = MathHelper.clamp((nearbyCount - 21) / -2, 3, 15);

			for (int step = 0; step < particleCount; step++) {
				int color = colorCounter.addAndGet(5);
				world.addParticleClient(
						TintedParticleEffect.create(ParticleTypes.ENTITY_EFFECT, color),
						particleX,
						pos.getY() + 0.5F,
						particleZ,
						0.0,
						0.0,
						0.0
				);
			}
		});
	}

	private static boolean isRaiderEntity(BlockPos pos, LivingEntity entity) {
		return entity.isAlive() && !entity.isRemoved() && pos.isWithinDistance(entity.getEntityPos(), 48.0) && entity
				.getType()
				.isIn(EntityTypeTags.RAIDERS);
	}

	private static void applyGlowToEntity(LivingEntity entity) {
		entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, HEARING_CACHE_COOLDOWN_TICKS));
	}

	@FunctionalInterface
	interface Effect {

		void run(World world, BlockPos pos, List<LivingEntity> hearingEntities);
	}
}
