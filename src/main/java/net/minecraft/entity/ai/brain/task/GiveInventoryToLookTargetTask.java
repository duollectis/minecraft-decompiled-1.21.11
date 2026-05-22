package net.minecraft.entity.ai.brain.task;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.LookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.AllayBrain;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Задача мозга, бросающая предметы из инвентаря сущности в сторону цели взгляда.
 * Используется Аллаем для доставки предметов к блоку или игроку.
 */
public class GiveInventoryToLookTargetTask<E extends LivingEntity & InventoryOwner> extends MultiTickTask<E> {

	private static final int COMPLETION_RANGE = 3;
	private static final int ITEM_PICKUP_COOLDOWN_TICKS = 60;
	private static final long THROW_SOUND_INTERVAL = 7L;
	private static final double THROW_SOUND_CHANCE = 0.9;
	private static final float THROW_VELOCITY_X = 0.2F;
	private static final float THROW_VELOCITY_Y = 0.3F;
	private static final float THROW_VELOCITY_Z = 0.2F;
	private static final float THROW_SPREAD = 0.2F;
	private final Function<LivingEntity, Optional<LookTarget>> lookTargetFunction;
	private final float speed;

	public GiveInventoryToLookTargetTask(
			Function<LivingEntity, Optional<LookTarget>> lookTargetFunction,
			float speed,
			int runTime
	) {
		super(
				Map.of(
						MemoryModuleType.LOOK_TARGET,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.WALK_TARGET,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS,
						MemoryModuleState.REGISTERED
				),
				runTime
		);
		this.lookTargetFunction = lookTargetFunction;
		this.speed = speed;
	}

	@Override
	protected boolean shouldRun(ServerWorld world, E entity) {
		return hasItemAndTarget(entity);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, E entity, long time) {
		return hasItemAndTarget(entity);
	}

	@Override
	protected void run(ServerWorld world, E entity, long time) {
		lookTargetFunction.apply(entity).ifPresent(target -> TargetUtil.walkTowards(entity, target, speed, COMPLETION_RANGE));
	}

	@Override
	protected void keepRunning(ServerWorld world, E entity, long time) {
		Optional<LookTarget> target = lookTargetFunction.apply(entity);

		if (target.isEmpty()) {
			return;
		}

		LookTarget lookTarget = target.get();
		double distance = lookTarget.getPos().distanceTo(entity.getEyePos());

		if (distance >= COMPLETION_RANGE) {
			return;
		}

		ItemStack thrown = entity.getInventory().removeStack(0, 1);

		if (thrown.isEmpty()) {
			return;
		}

		playThrowSound(entity, thrown, offsetTarget(lookTarget));

		if (entity instanceof AllayEntity allay) {
			AllayBrain.getLikedPlayer(allay).ifPresent(player -> triggerCriterion(lookTarget, thrown, player));
		}

		entity.getBrain().remember(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, ITEM_PICKUP_COOLDOWN_TICKS);
	}

	private void triggerCriterion(LookTarget target, ItemStack stack, ServerPlayerEntity player) {
		Criteria.ALLAY_DROP_ITEM_ON_BLOCK.trigger(player, target.getBlockPos().down(), stack);
	}

	private boolean hasItemAndTarget(E entity) {
		return !entity.getInventory().isEmpty() && lookTargetFunction.apply(entity).isPresent();
	}

	private static Vec3d offsetTarget(LookTarget target) {
		return target.getPos().add(0.0, 1.0, 0.0);
	}

	public static void playThrowSound(LivingEntity entity, ItemStack stack, Vec3d target) {
		TargetUtil.give(entity, stack, target, new Vec3d(THROW_VELOCITY_X, THROW_VELOCITY_Y, THROW_VELOCITY_Z), THROW_SPREAD);
		World world = entity.getEntityWorld();

		if (world.getTime() % THROW_SOUND_INTERVAL == 0L && world.random.nextDouble() < THROW_SOUND_CHANCE) {
			float pitch = Util.<Float>getRandom(AllayEntity.THROW_SOUND_PITCHES, world.getRandom());
			world.playSoundFromEntity(null, entity, SoundEvents.ENTITY_ALLAY_ITEM_THROWN, SoundCategory.NEUTRAL, 1.0F, pitch);
		}
	}
}
