package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Задача мозга жителя-фермера, управляющая сбором урожая и посевом семян.
 * Сканирует блоки в радиусе 1 блока и выбирает случайную цель для обработки.
 */
public class FarmerVillagerTask extends MultiTickTask<VillagerEntity> {

	private static final int MAX_RUN_TIME = 200;
	private static final long RESPONSE_COOLDOWN = 40L;
	private static final long NEXT_TARGET_COOLDOWN = 20L;
	private static final double REACH_DISTANCE = 1.0;
	public static final float WALK_SPEED = 0.5F;

	private @Nullable BlockPos currentTarget;
	private long nextResponseTime;
	private int ticksRan;
	private final List<BlockPos> targetPositions = new ArrayList<>();

	public FarmerVillagerTask() {
		super(
				ImmutableMap.of(
						MemoryModuleType.LOOK_TARGET,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.WALK_TARGET,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.SECONDARY_JOB_SITE,
						MemoryModuleState.VALUE_PRESENT
				)
		);
	}

	@Override
	protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
		if (!world.getGameRules().getValue(GameRules.DO_MOB_GRIEFING)) {
			return false;
		}

		if (!entity.getVillagerData().profession().matchesKey(VillagerProfession.FARMER)) {
			return false;
		}

		BlockPos.Mutable mutable = entity.getBlockPos().mutableCopy();
		targetPositions.clear();

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					mutable.set(entity.getX() + dx, entity.getY() + dy, entity.getZ() + dz);

					if (isSuitableTarget(mutable, world)) {
						targetPositions.add(new BlockPos(mutable));
					}
				}
			}
		}

		currentTarget = chooseRandomTarget(world);
		return currentTarget != null;
	}

	private @Nullable BlockPos chooseRandomTarget(ServerWorld world) {
		return targetPositions.isEmpty()
				? null
				: targetPositions.get(world.getRandom().nextInt(targetPositions.size()));
	}

	private boolean isSuitableTarget(BlockPos pos, ServerWorld world) {
		BlockState blockState = world.getBlockState(pos);
		Block block = blockState.getBlock();
		Block below = world.getBlockState(pos.down()).getBlock();
		return block instanceof CropBlock cropBlock && cropBlock.isMature(blockState)
				|| blockState.isAir() && below instanceof FarmlandBlock;
	}

	@Override
	protected void run(ServerWorld world, VillagerEntity entity, long time) {
		if (time <= nextResponseTime || currentTarget == null) {
			return;
		}

		entity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new BlockPosLookTarget(currentTarget));
		entity.getBrain().remember(
				MemoryModuleType.WALK_TARGET,
				new WalkTarget(new BlockPosLookTarget(currentTarget), WALK_SPEED, 1)
		);
	}

	@Override
	protected void finishRunning(ServerWorld world, VillagerEntity entity, long time) {
		entity.getBrain().forget(MemoryModuleType.LOOK_TARGET);
		entity.getBrain().forget(MemoryModuleType.WALK_TARGET);
		ticksRan = 0;
		nextResponseTime = time + RESPONSE_COOLDOWN;
	}

	@Override
	protected void keepRunning(ServerWorld world, VillagerEntity entity, long time) {
		if (currentTarget != null && !currentTarget.isWithinDistance(entity.getEntityPos(), REACH_DISTANCE)) {
			return;
		}

		if (currentTarget != null && time > nextResponseTime) {
			BlockState blockState = world.getBlockState(currentTarget);
			Block block = blockState.getBlock();
			Block below = world.getBlockState(currentTarget.down()).getBlock();

			if (block instanceof CropBlock cropBlock && cropBlock.isMature(blockState)) {
				world.breakBlock(currentTarget, true, entity);
			}

			if (blockState.isAir() && below instanceof FarmlandBlock && entity.hasSeedToPlant()) {
				tryPlantSeed(world, entity, blockState);
			}

			if (block instanceof CropBlock cropBlock && !cropBlock.isMature(blockState)) {
				targetPositions.remove(currentTarget);
				currentTarget = chooseRandomTarget(world);

				if (currentTarget != null) {
					nextResponseTime = time + NEXT_TARGET_COOLDOWN;
					entity.getBrain().remember(
							MemoryModuleType.WALK_TARGET,
							new WalkTarget(new BlockPosLookTarget(currentTarget), WALK_SPEED, 1)
					);
					entity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new BlockPosLookTarget(currentTarget));
				}
			}
		}

		ticksRan++;
	}

	private void tryPlantSeed(ServerWorld world, VillagerEntity entity, BlockState airState) {
		SimpleInventory inventory = entity.getInventory();

		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack stack = inventory.getStack(slot);

			if (stack.isEmpty() || !stack.isIn(ItemTags.VILLAGER_PLANTABLE_SEEDS)) {
				continue;
			}

			if (!(stack.getItem() instanceof BlockItem blockItem)) {
				continue;
			}

			BlockState planted = blockItem.getBlock().getDefaultState();
			world.setBlockState(currentTarget, planted);
			world.emitGameEvent(GameEvent.BLOCK_PLACE, currentTarget, GameEvent.Emitter.of(entity, planted));
			world.playSound(
					null,
					currentTarget.getX(),
					currentTarget.getY(),
					currentTarget.getZ(),
					SoundEvents.ITEM_CROP_PLANT,
					SoundCategory.BLOCKS,
					1.0F,
					1.0F
			);
			stack.decrement(1);

			if (stack.isEmpty()) {
				inventory.setStack(slot, ItemStack.EMPTY);
			}

			break;
		}
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity entity, long time) {
		return ticksRan < MAX_RUN_TIME;
	}
}
