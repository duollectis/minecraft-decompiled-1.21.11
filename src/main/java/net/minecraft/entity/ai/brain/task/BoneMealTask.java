package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Задача мозга жителя-фермера, применяющая костную муку к незрелым посевам в радиусе 1 блока.
 * Выполняется не чаще одного раза в {@code BONE_MEAL_INTERVAL} тиков и с кулдауном {@code COOLDOWN_TICKS}.
 */
public class BoneMealTask extends MultiTickTask<VillagerEntity> {

	private static final int MAX_DURATION = 80;
	private static final long COOLDOWN_TICKS = 160L;
	private static final int BONE_MEAL_INTERVAL = 10;
	private static final int BONE_MEAL_WALK_SPEED_FACTOR = 1;
	private static final float BONE_MEAL_WALK_SPEED = 0.5F;
	private static final int BONE_MEAL_NEXT_TICK_DELAY = 40;
	private static final int WORLD_EVENT_BONE_MEAL = 1505;
	private static final int BONE_MEAL_PARTICLE_COUNT = 15;
	private static final double BONE_MEAL_REACH_DISTANCE = 1.0;
	private long startTime;
	private long lastEndEntityAge;
	private int duration;
	private Optional<BlockPos> pos = Optional.empty();

	public BoneMealTask() {
		super(ImmutableMap.of(
				MemoryModuleType.LOOK_TARGET,
				MemoryModuleState.VALUE_ABSENT,
				MemoryModuleType.WALK_TARGET,
				MemoryModuleState.VALUE_ABSENT
		));
	}

	@Override
	protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
		if (entity.age % BONE_MEAL_INTERVAL != 0) {
			return false;
		}

		if (lastEndEntityAge != 0L && lastEndEntityAge + COOLDOWN_TICKS > entity.age) {
			return false;
		}

		if (entity.getInventory().count(Items.BONE_MEAL) <= 0) {
			return false;
		}

		pos = findBoneMealPos(world, entity);
		return pos.isPresent();
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity entity, long time) {
		return duration < MAX_DURATION && pos.isPresent();
	}

	private Optional<BlockPos> findBoneMealPos(ServerWorld world, VillagerEntity entity) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		Optional<BlockPos> result = Optional.empty();
		int count = 0;

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					mutable.set(entity.getBlockPos(), dx, dy, dz);
					if (canBoneMeal(mutable, world)) {
						if (world.random.nextInt(++count) == 0) {
							result = Optional.of(mutable.toImmutable());
						}
					}
				}
			}
		}

		return result;
	}

	private boolean canBoneMeal(BlockPos pos, ServerWorld world) {
		BlockState blockState = world.getBlockState(pos);
		Block block = blockState.getBlock();
		return block instanceof CropBlock cropBlock && !cropBlock.isMature(blockState);
	}

	@Override
	protected void run(ServerWorld world, VillagerEntity entity, long time) {
		addLookWalkTargets(entity);
		entity.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BONE_MEAL));
		startTime = time;
		duration = 0;
	}

	private void addLookWalkTargets(VillagerEntity villager) {
		pos.ifPresent(targetPos -> {
			BlockPosLookTarget lookTarget = new BlockPosLookTarget(targetPos);
			villager.getBrain().remember(MemoryModuleType.LOOK_TARGET, lookTarget);
			villager.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(lookTarget, BONE_MEAL_WALK_SPEED, BONE_MEAL_WALK_SPEED_FACTOR));
		});
	}

	@Override
	protected void finishRunning(ServerWorld world, VillagerEntity entity, long time) {
		entity.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		lastEndEntityAge = entity.age;
	}

	@Override
	protected void keepRunning(ServerWorld world, VillagerEntity entity, long time) {
		BlockPos targetPos = pos.get();
		if (time < startTime || !targetPos.isWithinDistance(entity.getEntityPos(), BONE_MEAL_REACH_DISTANCE)) {
			return;
		}

		ItemStack boneMeal = ItemStack.EMPTY;
		SimpleInventory inventory = entity.getInventory();

		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (stack.isOf(Items.BONE_MEAL)) {
				boneMeal = stack;
				break;
			}
		}

		if (!boneMeal.isEmpty() && BoneMealItem.useOnFertilizable(boneMeal, world, targetPos)) {
			world.syncWorldEvent(WORLD_EVENT_BONE_MEAL, targetPos, BONE_MEAL_PARTICLE_COUNT);
			pos = findBoneMealPos(world, entity);
			addLookWalkTargets(entity);
			startTime = time + BONE_MEAL_NEXT_TICK_DELAY;
		}

		duration++;
	}
}
