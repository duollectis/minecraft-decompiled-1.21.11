package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComposterBlock;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.List;
import java.util.Optional;

/**
 * Расширение рабочей задачи жителя-фермера: компостирует излишки семян и выпекает хлеб из пшеницы.
 * Выполняется при наличии компостера на рабочем месте.
 */
public class FarmerWorkTask extends VillagerWorkTask {

	private static final List<Item> COMPOSTABLES = ImmutableList.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS);
	private static final int COMPOSTER_FULL_LEVEL = 8;
	private static final int COMPOSTER_ALMOST_FULL_LEVEL = 7;
	private static final int MAX_COMPOST_ATTEMPTS = 20;
	private static final int MIN_SEED_SURPLUS = 10;
	private static final int MAX_BREAD_IN_INVENTORY = 36;
	private static final int WHEAT_PER_BREAD = 3;
	private static final int WORLD_EVENT_COMPOSTER = 1500;
	private static final float DROP_OFFSET_Y = 0.5F;

	@Override
	protected void performAdditionalWork(ServerWorld world, VillagerEntity entity) {
		Optional<GlobalPos> jobSite = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE);

		if (jobSite.isEmpty()) {
			return;
		}

		GlobalPos globalPos = jobSite.get();
		BlockState blockState = world.getBlockState(globalPos.pos());

		if (blockState.isOf(Blocks.COMPOSTER)) {
			craftAndDropBread(world, entity);
			compostSeeds(world, entity, globalPos, blockState);
		}
	}

	private void compostSeeds(ServerWorld world, VillagerEntity entity, GlobalPos pos, BlockState composterState) {
		BlockPos blockPos = pos.pos();

		if (composterState.get(ComposterBlock.LEVEL) == COMPOSTER_FULL_LEVEL) {
			composterState = ComposterBlock.emptyFullComposter(entity, composterState, world, blockPos);
		}

		int remaining = MAX_COMPOST_ATTEMPTS;
		int[] seedCounts = new int[COMPOSTABLES.size()];
		SimpleInventory inventory = entity.getInventory();
		BlockState currentState = composterState;

		for (int slot = inventory.size() - 1; slot >= 0 && remaining > 0; slot--) {
			ItemStack stack = inventory.getStack(slot);
			int seedIndex = COMPOSTABLES.indexOf(stack.getItem());

			if (seedIndex == -1) {
				continue;
			}

			int count = stack.getCount();
			int total = seedCounts[seedIndex] + count;
			seedCounts[seedIndex] = total;
			int toCompost = Math.min(Math.min(total - MIN_SEED_SURPLUS, remaining), count);

			if (toCompost <= 0) {
				continue;
			}

			remaining -= toCompost;

			for (int i = 0; i < toCompost; i++) {
				currentState = ComposterBlock.compost(entity, currentState, world, stack, blockPos);

				if (currentState.get(ComposterBlock.LEVEL) == COMPOSTER_ALMOST_FULL_LEVEL) {
					syncComposterEvent(world, composterState, blockPos, currentState);
					return;
				}
			}
		}

		syncComposterEvent(world, composterState, blockPos, currentState);
	}

	private void syncComposterEvent(ServerWorld world, BlockState oldState, BlockPos pos, BlockState newState) {
		world.syncWorldEvent(WORLD_EVENT_COMPOSTER, pos, newState != oldState ? 1 : 0);
	}

	private void craftAndDropBread(ServerWorld world, VillagerEntity villager) {
		SimpleInventory inventory = villager.getInventory();

		if (inventory.count(Items.BREAD) > MAX_BREAD_IN_INVENTORY) {
			return;
		}

		int wheatCount = inventory.count(Items.WHEAT);
		int loaves = Math.min(WHEAT_PER_BREAD, wheatCount / WHEAT_PER_BREAD);

		if (loaves == 0) {
			return;
		}

		inventory.removeItem(Items.WHEAT, loaves * WHEAT_PER_BREAD);
		ItemStack leftover = inventory.addStack(new ItemStack(Items.BREAD, loaves));

		if (!leftover.isEmpty()) {
			villager.dropStack(world, leftover, DROP_OFFSET_Y);
		}
	}
}
