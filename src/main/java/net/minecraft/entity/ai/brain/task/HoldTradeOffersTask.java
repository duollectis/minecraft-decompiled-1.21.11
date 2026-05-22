package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.village.TradeOffer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Задача мозга жителя, демонстрирующей покупателю предметы из доступных торговых предложений.
 * Циклически показывает результаты сделок, соответствующих предмету в руке покупателя.
 */
public class HoldTradeOffersTask extends MultiTickTask<VillagerEntity> {

	private static final int RUN_INTERVAL = 900;
	private static final int OFFER_SHOWING_INTERVAL = 40;
	private static final double MAX_TRADE_DISTANCE_SQ = 17.0;
	private static final float HELD_ITEM_DROP_CHANCE = 0.085F;
	private static final float NO_DROP_CHANCE = 0.0F;

	private @Nullable ItemStack customerHeldStack;
	private final List<ItemStack> offers = new ArrayList<>();
	private int offerShownTicks;
	private int offerIndex;
	private int ticksLeft;

	public HoldTradeOffersTask(int minRunTime, int maxRunTime) {
		super(
				ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.VALUE_PRESENT),
				minRunTime,
				maxRunTime
		);
	}

	@Override
	public boolean shouldRun(ServerWorld world, VillagerEntity entity) {
		Brain<?> brain = entity.getBrain();

		if (brain.getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET).isEmpty()) {
			return false;
		}

		LivingEntity customer = brain.getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET).get();

		return customer.getType() == EntityType.PLAYER
				&& entity.isAlive()
				&& customer.isAlive()
				&& !entity.isBaby()
				&& entity.squaredDistanceTo(customer) <= MAX_TRADE_DISTANCE_SQ;
	}

	@Override
	public boolean shouldKeepRunning(ServerWorld world, VillagerEntity entity, long time) {
		return shouldRun(world, entity)
				&& ticksLeft > 0
				&& entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET).isPresent();
	}

	@Override
	public void run(ServerWorld world, VillagerEntity entity, long time) {
		super.run(world, entity, time);
		findPotentialCustomer(entity);
		offerShownTicks = 0;
		offerIndex = 0;
		ticksLeft = OFFER_SHOWING_INTERVAL;
	}

	@Override
	public void keepRunning(ServerWorld world, VillagerEntity entity, long time) {
		LivingEntity customer = findPotentialCustomer(entity);
		setupOffers(customer, entity);

		if (offers.isEmpty()) {
			holdNothing(entity);
			ticksLeft = Math.min(ticksLeft, OFFER_SHOWING_INTERVAL);
		} else {
			refreshShownOffer(entity);
		}

		ticksLeft--;
	}

	@Override
	public void finishRunning(ServerWorld world, VillagerEntity entity, long time) {
		super.finishRunning(world, entity, time);
		entity.getBrain().forget(MemoryModuleType.INTERACTION_TARGET);
		holdNothing(entity);
		customerHeldStack = null;
	}

	private void setupOffers(LivingEntity customer, VillagerEntity villager) {
		ItemStack heldStack = customer.getMainHandStack();
		boolean stackChanged = customerHeldStack == null || !ItemStack.areItemsEqual(customerHeldStack, heldStack);

		if (stackChanged) {
			customerHeldStack = heldStack;
			offers.clear();
		}

		if (stackChanged && !customerHeldStack.isEmpty()) {
			loadPossibleOffers(villager);

			if (!offers.isEmpty()) {
				ticksLeft = RUN_INTERVAL;
				holdFirstOffer(villager);
			}
		}
	}

	private void holdFirstOffer(VillagerEntity villager) {
		holdOffer(villager, offers.get(0));
	}

	private void loadPossibleOffers(VillagerEntity villager) {
		for (TradeOffer tradeOffer : villager.getOffers()) {
			if (!tradeOffer.isDisabled() && isPossible(tradeOffer)) {
				offers.add(tradeOffer.copySellItem());
			}
		}
	}

	private boolean isPossible(TradeOffer offer) {
		return ItemStack.areItemsEqual(customerHeldStack, offer.getDisplayedFirstBuyItem())
				|| ItemStack.areItemsEqual(customerHeldStack, offer.getDisplayedSecondBuyItem());
	}

	private static void holdNothing(VillagerEntity villager) {
		villager.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		villager.setEquipmentDropChance(EquipmentSlot.MAINHAND, HELD_ITEM_DROP_CHANCE);
	}

	private static void holdOffer(VillagerEntity villager, ItemStack stack) {
		villager.equipStack(EquipmentSlot.MAINHAND, stack);
		villager.setEquipmentDropChance(EquipmentSlot.MAINHAND, NO_DROP_CHANCE);
	}

	private LivingEntity findPotentialCustomer(VillagerEntity villager) {
		Brain<?> brain = villager.getBrain();
		LivingEntity customer = brain.getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET).get();
		brain.remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(customer, true));
		return customer;
	}

	private void refreshShownOffer(VillagerEntity villager) {
		if (offers.size() < 2 || ++offerShownTicks < OFFER_SHOWING_INTERVAL) {
			return;
		}

		offerIndex++;
		offerShownTicks = 0;

		if (offerIndex > offers.size() - 1) {
			offerIndex = 0;
		}

		holdOffer(villager, offers.get(offerIndex));
	}
}
