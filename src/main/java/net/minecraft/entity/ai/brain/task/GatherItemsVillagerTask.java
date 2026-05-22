package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.village.VillagerProfession;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Задача мозга жителя, реализующая обмен предметами с другим жителем при встрече.
 * Фермеры делятся пшеницей и едой; жители передают предметы, нужные профессии партнёра.
 */
public class GatherItemsVillagerTask extends MultiTickTask<VillagerEntity> {

	private static final float WALK_SPEED = 0.5F;
	private static final int APPROACH_RANGE = 2;
	private static final double TALK_DISTANCE_SQ = 5.0;
	private static final int WHEAT_SHARE_THRESHOLD = 24;

	private Set<Item> gatherableItems = ImmutableSet.of();

	public GatherItemsVillagerTask() {
		super(
				ImmutableMap.of(
						MemoryModuleType.INTERACTION_TARGET,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.VISIBLE_MOBS,
						MemoryModuleState.VALUE_PRESENT
				)
		);
	}

	@Override
	protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
		return TargetUtil.canSee(entity.getBrain(), MemoryModuleType.INTERACTION_TARGET, EntityType.VILLAGER);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity entity, long time) {
		return shouldRun(world, entity);
	}

	@Override
	protected void run(ServerWorld world, VillagerEntity entity, long time) {
		VillagerEntity partner = (VillagerEntity) entity.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET).get();
		TargetUtil.lookAtAndWalkTowardsEachOther(entity, partner, WALK_SPEED, APPROACH_RANGE);
		gatherableItems = getGatherableItems(entity, partner);
	}

	@Override
	protected void keepRunning(ServerWorld world, VillagerEntity entity, long time) {
		VillagerEntity partner = (VillagerEntity) entity.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET).get();

		if (entity.squaredDistanceTo(partner) > TALK_DISTANCE_SQ) {
			return;
		}

		TargetUtil.lookAtAndWalkTowardsEachOther(entity, partner, WALK_SPEED, APPROACH_RANGE);
		entity.talkWithVillager(world, partner, time);

		boolean isFarmer = entity.getVillagerData().profession().matchesKey(VillagerProfession.FARMER);

		if (entity.canShareFoodForBreeding() && (isFarmer || partner.needsFoodForBreeding())) {
			giveHalfOfStack(entity, VillagerEntity.ITEM_FOOD_VALUES.keySet(), partner);
		}

		if (isFarmer && entity.getInventory().count(Items.WHEAT) > Items.WHEAT.getMaxCount() / 2) {
			giveHalfOfStack(entity, ImmutableSet.of(Items.WHEAT), partner);
		}

		if (!gatherableItems.isEmpty() && entity.getInventory().containsAny(gatherableItems)) {
			giveHalfOfStack(entity, gatherableItems, partner);
		}
	}

	@Override
	protected void finishRunning(ServerWorld world, VillagerEntity entity, long time) {
		entity.getBrain().forget(MemoryModuleType.INTERACTION_TARGET);
	}

	private static Set<Item> getGatherableItems(VillagerEntity entity, VillagerEntity target) {
		ImmutableSet<Item> targetItems = target.getVillagerData().profession().value().gatherableItems();
		ImmutableSet<Item> entityItems = entity.getVillagerData().profession().value().gatherableItems();
		return targetItems.stream().filter(item -> !entityItems.contains(item)).collect(Collectors.toSet());
	}

	private static void giveHalfOfStack(VillagerEntity villager, Set<Item> validItems, LivingEntity target) {
		SimpleInventory inventory = villager.getInventory();
		ItemStack toGive = ItemStack.EMPTY;

		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack stack = inventory.getStack(slot);

			if (stack.isEmpty() || !validItems.contains(stack.getItem())) {
				continue;
			}

			int count = stack.getCount();
			int toTake;

			if (count > stack.getMaxCount() / 2) {
				toTake = count / 2;
			} else if (count > WHEAT_SHARE_THRESHOLD) {
				toTake = count - WHEAT_SHARE_THRESHOLD;
			} else {
				continue;
			}

			stack.decrement(toTake);
			toGive = new ItemStack(stack.getItem(), toTake);
			break;
		}

		if (!toGive.isEmpty()) {
			TargetUtil.give(villager, toGive, target.getEntityPos());
		}
	}
}
